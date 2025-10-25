# Helldeck LLM Model Directory

This directory contains the GGUF model files for the on-device LLM used by Helldeck's AI enhancement features.

## Required Models

### Default Model (Recommended)
- **Name:** Qwen2.5-1.5B-Instruct-Q4_K_M
- **Size:** ~0.9-1.2 GB
- **File:** `qwen2.5-1.5b-instruct-Q4_K_M.gguf`
- **Source:** [TheBloke/Qwen2.5-1.5B-Instruct-GGUF](https://huggingface.co/TheBloke/Qwen2.5-1.5B-Instruct-GGUF)
- **Use Case:** Mid-range and high-end Android devices (4GB+ RAM)
- **Performance:** Best fluency and consistency for paraphrasing tasks

### Lite Model (Low-End Devices)
- **Name:** TinyLlama-1.1B-Chat-v1.0-Q4_K_M
- **Size:** ~0.6-0.8 GB
- **File:** `tinyllama-1.1b-chat-Q4_K_M.gguf`
- **Source:** [TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF](https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF)
- **Use Case:** Low-end Android devices (<4GB RAM)
- **Performance:** Smaller footprint, still coherent for basic paraphrasing

## Installation

### Option 1: Manual Download
1. Download the appropriate `.gguf` file from HuggingFace
2. Place it in this `models/` directory
3. The app will automatically detect and load it on first run

### Option 2: Play Asset Delivery (PAD)
For production builds, models should be included as install-time assets via Play Asset Delivery:
- Configure PAD in `app/build.gradle`
- Models will be unpacked to `filesDir/models/` on first launch

## Model Selection Logic

The app automatically selects the appropriate model based on:
- Available device RAM
- User preference (can be set in Settings)
- Model file availability

**Selection Priority:**
1. User-selected model (if specified in settings)
2. Qwen2.5-1.5B if RAM >= 4GB
3. TinyLlama if RAM < 4GB
4. Disable AI features if no model available

## Model Manifest

The app uses `models/manifest.json` to track available models:

```json
{
  "models": [
    {
      "id": "qwen2.5-1.5b-instruct",
      "name": "Qwen 2.5 1.5B Instruct",
      "file": "qwen2.5-1.5b-instruct-Q4_K_M.gguf",
      "size_mb": 1024,
      "min_ram_gb": 4,
      "default": true
    },
    {
      "id": "tinyllama-1.1b-chat",
      "name": "TinyLlama 1.1B Chat",
      "file": "tinyllama-1.1b-chat-Q4_K_M.gguf",
      "size_mb": 700,
      "min_ram_gb": 2,
      "default": false
    }
  ],
  "version": 1
}
```

## License Compliance

⚠️ **Important:** Verify license compatibility before distributing models:
- **Qwen2.5:** Check license at model repository
- **TinyLlama:** Apache 2.0 (redistribution-friendly)

If a model's license prohibits redistribution, consider:
- User-initiated download flow
- Hosting models separately
- Providing download instructions only

## Performance Expectations

### Qwen2.5-1.5B (Default)
- Warmup time: ~1-2 seconds
- Per-card generation: <180ms (after warmup)
- Cache hit rate: >70% after a few rounds
- Quality: High fluency, maintains style

### TinyLlama-1.1B (Lite)
- Warmup time: ~0.5-1 second
- Per-card generation: <120ms (after warmup)
- Cache hit rate: >70% after a few rounds
- Quality: Good for simple rewrites

## Troubleshooting

**Model not loading:**
- Verify file exists in `models/` or `filesDir/models/`
- Check file integrity (compare checksums)
- Review logcat for JNI errors

**Out of memory:**
- Switch to TinyLlama in settings
- Reduce context size in `LlamaCppLLM.kt`
- Clear generation cache

**Slow generation:**
- First generation is always slower (warmup)
- Check cache hit rate in logs
- Verify model quantization (Q4_K_M recommended)

## Future Improvements

- [ ] Add support for grammar-constrained decoding
- [ ] Implement model quantization variants (Q3, Q5)
- [ ] Add telemetry for generation time p50/p95
- [ ] Support user-provided custom models