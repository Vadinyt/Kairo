# Local Fast v3 — embedded GGUF preview

Experimental arm64 Android JNI build. It runs text inference inside Kairo, without Termux, localhost HTTP, root or changes to CPU scheduling policy. The upstream revision is pinned in CMake.

1. Place an instruction-tuned single-file GGUF in Downloads, e.g. your existing Qwen 2.5 1.5B Instruct Q4_K_M.
2. In Kairo Settings, tap Import GGUF model. The file is copied into private no-backup storage, which requires additional disk space.
3. Enable Use embedded model. The chat header displays GGUF. Start a new conversation.
4. Disable the switch to return to the existing AUTO/HTTP path.

CPU-only, 4096-token context, maximum 256 generated tokens per call; 4 GiB import cap, not a guarantee of available RAM. Single-file text chat templates supported by llama_chat_apply_template only. Image input requires a different backend. The model loads lazily and is reused. Speed uses native performance counters; RAM is process PSS measured at prompt completion. Logcat tag KairoNative shows timings. Runtime performance and supported GGUF variants must be verified on a real device.

llama.cpp is MIT-licensed; see app/src/main/assets/licenses/.
