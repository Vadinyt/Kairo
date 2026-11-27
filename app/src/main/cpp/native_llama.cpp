#include <jni.h>
#include <algorithm>
#include <cstdint>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>
#include "llama.h"

namespace {
struct State {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    llama_sampler * sampler = nullptr;
    std::string path;
    std::vector<llama_token> cached;
    int remaining = 0;
    ~State() {
        if (sampler) llama_sampler_free(sampler);
        if (ctx) llama_free(ctx);
        if (model) llama_model_free(model);
    }
};
std::unique_ptr<State> state;
std::once_flag init_flag;
// Calls are serialized by the Kotlin process-wide mutex.
void require(bool ok, const char * msg) { if (!ok) throw std::runtime_error(msg); }
void fail(JNIEnv * env, const char * msg) {
    if (!env->ExceptionCheck()) {
        auto cls = env->FindClass("java/lang/IllegalStateException");
        if (cls) env->ThrowNew(cls, msg);
    }
}
void invalidate() {
    if (state) { state->cached.clear(); state->remaining = 0; }
}
}

extern "C" JNIEXPORT void JNICALL
Java_com_kairo_agent_engine_NativeLlama_nativeLoad(JNIEnv * env, jobject, jstring path, jint threads) {
    try {
        const char * raw = env->GetStringUTFChars(path, nullptr);
        require(raw != nullptr, "Unable to read model path.");
        std::string filename(raw);
        env->ReleaseStringUTFChars(path, raw);
        if (state && state->path == filename) return;
        state.reset();
        std::call_once(init_flag, [] { llama_backend_init(); });
        auto next = std::make_unique<State>();
        auto mp = llama_model_default_params();
        mp.n_gpu_layers = 0;
        mp.use_mmap = true;
        next->model = llama_model_load_from_file(filename.c_str(), mp);
        require(next->model != nullptr, "Unable to load GGUF. Check model and available RAM.");
        require(!llama_model_has_encoder(next->model), "Encoder models are unsupported.");
        auto cp = llama_context_default_params();
        cp.n_ctx = static_cast<uint32_t>(std::min(4096, llama_model_n_ctx_train(next->model)));
        require(cp.n_ctx >= 512, "Model context is too small.");
        cp.n_batch = 256;
        cp.n_ubatch = 256;
        cp.n_threads = std::clamp(static_cast<int>(threads), 1, 6);
        cp.n_threads_batch = cp.n_threads;
        cp.no_perf = false;
        next->ctx = llama_init_from_model(next->model, cp);
        require(next->ctx != nullptr, "Unable to allocate context.");
        next->path = filename;
        state = std::move(next);
    } catch (const std::exception & e) { fail(env, e.what()); }
}

extern "C" JNIEXPORT void JNICALL
Java_com_kairo_agent_engine_NativeLlama_nativePrepare(JNIEnv * env, jobject, jbyteArray prompt, jfloat temp, jint max_tokens) {
    try {
        require(state != nullptr, "No model loaded.");
        auto & s = *state;
        const jsize len = env->GetArrayLength(prompt);
        require(len > 0 && len <= 131072, "Input is too large.");
        std::string user(static_cast<size_t>(len), '\0');
        env->GetByteArrayRegion(prompt, 0, len, reinterpret_cast<jbyte *>(user.data()));
        if (env->ExceptionCheck()) return;
        const char * tmpl = llama_model_chat_template(s.model, nullptr);
        require(tmpl != nullptr, "This GGUF has no chat template.");
        llama_chat_message msg{"user", user.c_str()};
        std::vector<char> formatted(static_cast<size_t>(len) * 2 + 1024);
        int n = llama_chat_apply_template(tmpl, &msg, 1, true, formatted.data(), static_cast<int32_t>(formatted.size()));
        require(n > 0, "Unsupported GGUF chat template.");
        if (static_cast<size_t>(n) >= formatted.size()) {
            formatted.resize(static_cast<size_t>(n) + 1);
            n = llama_chat_apply_template(tmpl, &msg, 1, true, formatted.data(), static_cast<int32_t>(formatted.size()));
            require(n > 0 && static_cast<size_t>(n) < formatted.size(), "Chat template failed.");
        }
        auto * vocab = llama_model_get_vocab(s.model);
        std::vector<llama_token> tokens(static_cast<size_t>(n) + 8);
        int count = llama_tokenize(vocab, formatted.data(), n, tokens.data(), static_cast<int32_t>(tokens.size()), true, true);
        if (count < 0) {
            tokens.resize(static_cast<size_t>(-count));
            count = llama_tokenize(vocab, formatted.data(), n, tokens.data(), static_cast<int32_t>(tokens.size()), true, true);
        }
        require(count > 0, "Tokenization failed.");
        tokens.resize(static_cast<size_t>(count));
        int room = static_cast<int>(llama_n_ctx(s.ctx)) - count;
        require(room > 0, "Prompt exceeds the local context. Start a shorter conversation.");
        s.remaining = std::min(room, std::clamp(static_cast<int>(max_tokens), 1, 512));
        if (s.sampler) { llama_sampler_free(s.sampler); s.sampler = nullptr; }
        s.sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
        require(s.sampler != nullptr, "Sampler initialization failed.");
        llama_sampler_chain_add(s.sampler, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(s.sampler, llama_sampler_init_top_p(0.95f, 1));
        llama_sampler_chain_add(s.sampler, llama_sampler_init_temp(std::max(0.01f, static_cast<float>(temp))));
        llama_sampler_chain_add(s.sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        size_t keep = 0;
        while (keep < s.cached.size() && keep + 1 < tokens.size() && s.cached[keep] == tokens[keep]) ++keep;
        auto memory = llama_get_memory(s.ctx);
        if (keep == 0 || !llama_memory_seq_rm(memory, 0, static_cast<llama_pos>(keep), -1)) {
            llama_memory_clear(memory, false);
            keep = 0;
        }
        s.cached.resize(keep);
        llama_perf_context_reset(s.ctx);
        for (size_t offset = keep; offset < tokens.size();) {
            int32_t batch = static_cast<int32_t>(std::min<size_t>(llama_n_batch(s.ctx), tokens.size() - offset));
            require(llama_decode(s.ctx, llama_batch_get_one(tokens.data() + offset, batch)) == 0, "Prompt evaluation failed.");
            s.cached.insert(s.cached.end(), tokens.begin() + offset, tokens.begin() + offset + batch);
            offset += static_cast<size_t>(batch);
        }
    } catch (const std::exception & e) { invalidate(); fail(env, e.what()); }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_kairo_agent_engine_NativeLlama_nativeNext(JNIEnv * env, jobject) {
    try {
        require(state && state->sampler, "No active inference.");
        auto & s = *state;
        if (s.remaining <= 0) return nullptr;
        auto * vocab = llama_model_get_vocab(s.model);
        llama_token token = llama_sampler_sample(s.sampler, s.ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) { s.remaining = 0; return nullptr; }
        std::vector<char> piece(256);
        int n = llama_token_to_piece(vocab, token, piece.data(), static_cast<int32_t>(piece.size()), 0, false);
        if (n < 0) {
            piece.resize(static_cast<size_t>(-n));
            n = llama_token_to_piece(vocab, token, piece.data(), static_cast<int32_t>(piece.size()), 0, false);
        }
        require(n >= 0, "Token decoding failed.");
        --s.remaining;
        require(llama_decode(s.ctx, llama_batch_get_one(&token, 1)) == 0, "Token evaluation failed.");
        s.cached.push_back(token);
        auto out = env->NewByteArray(n);
        if (out && n) env->SetByteArrayRegion(out, 0, n, reinterpret_cast<const jbyte *>(piece.data()));
        return out;
    } catch (const std::exception & e) { invalidate(); fail(env, e.what()); return nullptr; }
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_kairo_agent_engine_NativeLlama_nativeMetrics(JNIEnv * env, jobject) {
    if (!state || !state->ctx) return nullptr;
    const auto p = llama_perf_context(state->ctx);
    const jdouble values[] = { static_cast<double>(p.n_p_eval), p.t_p_eval_ms, static_cast<double>(p.n_eval), p.t_eval_ms };
    auto out = env->NewDoubleArray(4);
    if (out) env->SetDoubleArrayRegion(out, 0, 4, values);
    return out;
}
extern "C" JNIEXPORT void JNICALL
Java_com_kairo_agent_engine_NativeLlama_nativeUnload(JNIEnv *, jobject) { state.reset(); }
