#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "LlamaJNI", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "LlamaJNI", __VA_ARGS__)

#ifdef HAVE_LLAMA_CPP
#include "llama.h"
#endif

extern "C" {

// Minimal llama holder
struct llama_holder {
#ifdef HAVE_LLAMA_CPP
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
#else
    // Keep struct non-empty for C compatibility when llama.cpp is stubbed
    void* unused = nullptr;
#endif
};

// Match nested object: com.helldeck.llm.llamacpp.LlamaCppLLM$LlamaNativeBridge
JNIEXPORT jlong JNICALL
Java_com_helldeck_llm_llamacpp_LlamaCppLLM_00024LlamaNativeBridge_nativeInit(
        JNIEnv *env,
        jobject /* this */,
        jstring modelPath,
        jint contextSize) {
#ifdef HAVE_LLAMA_CPP
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Initializing llama.cpp model: %s (ctx_size=%d)", path, contextSize);
    llama_backend_init();
    llama_model_params mparams = llama_model_default_params();
    llama_model * model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(modelPath, path);
    if (!model) {
        LOGE("Failed to load model file");
        return 0L;
    }
    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = contextSize > 0 ? contextSize : 2048;
    llama_context * ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        LOGE("Failed to create llama context");
        llama_model_free(model);
        return 0L;
    }
    auto * holder = new llama_holder();
    holder->model = model;
    holder->ctx = ctx;
    return reinterpret_cast<jlong>(holder);
#else
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Initializing llama.cpp model: %s (ctx_size=%d)", path, contextSize);
    env->ReleaseStringUTFChars(modelPath, path);
    return 0L;
#endif
}

JNIEXPORT jstring JNICALL
Java_com_helldeck_llm_llamacpp_LlamaCppLLM_00024LlamaNativeBridge_nativeGenerate(
        JNIEnv *env,
        jobject /* this */,
        jlong modelHandle,
        jstring prompt,
        jint maxTokens,
        jfloat temperature,
        jfloat topP,
        jint seed) {
#ifdef HAVE_LLAMA_CPP
    auto * holder = reinterpret_cast<llama_holder *>(modelHandle);
    if (!holder || !holder->model || !holder->ctx) {
        return env->NewStringUTF("");
    }
    const char *promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string result;
    // Reset KV cache for a fresh generation
    llama_memory_clear(llama_get_memory(holder->ctx), true);
    std::string fullPrompt(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);

    // Tokenize prompt
    std::vector<llama_token> tokens;
    tokens.resize(fullPrompt.size() + 8);
    int add_bos = 1;
    const struct llama_vocab * vocab = llama_model_get_vocab(holder->model);
    int n_toks = llama_tokenize(vocab, fullPrompt.c_str(), fullPrompt.length(), tokens.data(), (int)tokens.size(), add_bos, true);
    if (n_toks < 0) n_toks = 0;
    tokens.resize((size_t)n_toks);

    // Feed prompt tokens
    const int n_batch = 512;
    for (int i = 0; i < (int)tokens.size(); i += n_batch) {
        int n_eval = std::min(n_batch, (int)tokens.size() - i);
        if (llama_decode(holder->ctx, llama_batch_get_one(tokens.data() + i, n_eval)) != 0) {
            LOGE("llama_decode failed on prompt");
            return env->NewStringUTF("");
        }
    }

    const int n_vocab = llama_vocab_n_tokens(vocab);
    const llama_token eos = llama_vocab_eos(vocab);
    int produced = 0;
    while (produced < maxTokens) {
        const float * logits = llama_get_logits(holder->ctx);
        if (!logits) break;
        // Greedy pick
        int best_id = 0;
        float best = -1e30f;
        for (int i = 0; i < n_vocab; ++i) {
            float val = logits[i];
            if (val > best) { best = val; best_id = i; }
        }
        if (best_id == eos) break;
        // Convert token to piece
        char buf[512];
        int n = (int) llama_token_to_piece(vocab, best_id, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, (size_t) n);
        // Decode the newly generated token
        llama_token tok = (llama_token) best_id;
        if (llama_decode(holder->ctx, llama_batch_get_one(&tok, 1)) != 0) {
            LOGE("llama_decode failed on token");
            break;
        }
        produced++;
    }
    return env->NewStringUTF(result.c_str());
#else
    const char *promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generating text (max_tokens=%d, temp=%.2f)", maxTokens, temperature);
    std::string result = ""; // stub returns empty
    env->ReleaseStringUTFChars(prompt, promptStr);
    return env->NewStringUTF(result.c_str());
#endif
}

JNIEXPORT void JNICALL
Java_com_helldeck_llm_llamacpp_LlamaCppLLM_00024LlamaNativeBridge_nativeFree(
        JNIEnv *env,
        jobject /* this */,
        jlong modelHandle) {
#ifdef HAVE_LLAMA_CPP
    auto * holder = reinterpret_cast<llama_holder *>(modelHandle);
    if (holder) {
        if (holder->ctx) llama_free(holder->ctx);
        if (holder->model) llama_model_free(holder->model);
        delete holder;
    }
    llama_backend_free();
#else
    LOGI("Freeing llama.cpp model handle: %ld", modelHandle);
#endif
}

} // extern "C"
