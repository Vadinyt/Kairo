# Local Fast v4

- Exact standalone requests for status, local time/date, available storage/RAM, supported calculations and conversions, clock, ordinary settings, explicit timers and 24-hour alarms go through the existing Android tool dispatcher without model inference.
- No inferred targets, model-generated tool names or compound requests on the direct path. Existing other agent calls and confirmation logic remain unchanged.
- Embedded CPU threads: 2–6, context: 2048 or 4096, response budget: 128–512 tokens. Context changes reload on demand; thread changes reuse the existing model/context. Lower context reduces KV cache allocation.
- Native generated token count is measured directly; the Android direct route is labelled separately. GGUF import is unchanged; 4 GiB maximum and actual RAM constraints still apply. Q4 instruction-tuned 3–4B text models are candidates, not validated models.
- CPU-only build. GPU acceleration requires separate backend and device testing; no changes to thermal or CPU scheduling policy.
