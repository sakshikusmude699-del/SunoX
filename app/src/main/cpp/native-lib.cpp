#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <mutex>
#include "oboe_audio_engine.h"

#define LOG_TAG "NativeLib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static OboeAudioEngine* g_engine = nullptr;

extern "C" {

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeInit(JNIEnv* env, jobject /* thiz */) {
    if (g_engine == nullptr) {
        g_engine = new OboeAudioEngine();
        LOGI("Native engine created");
    }
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeRelease(JNIEnv* env, jobject /* thiz */) {
    if (g_engine) {
        delete g_engine;
        g_engine = nullptr;
        LOGI("Native engine released");
    }
}

JNIEXPORT jboolean JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeStart(JNIEnv* env, jobject /* thiz */) {
    if (!g_engine) return JNI_FALSE;
    return g_engine->start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeStop(JNIEnv* env, jobject /* thiz */) {
    if (g_engine) g_engine->stop();
}

JNIEXPORT jboolean JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeIsRunning(JNIEnv* env, jobject /* thiz */) {
    return (g_engine && g_engine->isRunning()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jfloat JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeGetInputLevel(JNIEnv* env, jobject /* thiz */) {
    return g_engine ? g_engine->getInputLevel() : 0.0f;
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetPreferredInputDeviceId(JNIEnv* env, jobject /* thiz */, jint deviceId) {
    if (g_engine) g_engine->setPreferredInputDeviceId(static_cast<int32_t>(deviceId));
}

JNIEXPORT jint JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeGetRoutedInputDeviceId(JNIEnv* env, jobject /* thiz */) {
    return g_engine ? static_cast<jint>(g_engine->getRoutedInputDeviceId()) : 0;
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetMasterGain(JNIEnv* env, jobject /* thiz */, jfloat gain) {
    if (g_engine) {
        std::lock_guard<std::mutex> lock(g_engine->processorMutex());
        g_engine->processor().setMasterGain(gain);
    }
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetBandGains(JNIEnv* env, jobject /* thiz */, jfloatArray gains) {
    if (!g_engine || !gains) return;
    jfloat* arr = env->GetFloatArrayElements(gains, nullptr);
    if (arr) {
        jsize len = env->GetArrayLength(gains);
        std::lock_guard<std::mutex> lock(g_engine->processorMutex());
        g_engine->processor().setGainsFromDb(arr, len);
        env->ReleaseFloatArrayElements(gains, arr, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetLowBoost(JNIEnv* env, jobject /* thiz */, jfloat db) {
    if (g_engine) {
        float linear = std::pow(10.0f, db / 20.0f);
        std::lock_guard<std::mutex> lock(g_engine->processorMutex());
        g_engine->processor().setBandGain(0, linear);
        g_engine->processor().setBandGain(1, linear);
    }
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetHighBoost(JNIEnv* env, jobject /* thiz */, jfloat db) {
    if (g_engine) {
        float linear = std::pow(10.0f, db / 20.0f);
        std::lock_guard<std::mutex> lock(g_engine->processorMutex());
        g_engine->processor().setBandGain(4, linear);
        g_engine->processor().setBandGain(5, linear);
    }
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetCompressionParams(JNIEnv* env, jobject /* thiz */,
    jfloat attackMs, jfloat releaseMs, jfloat ratio, jfloat thresholdDb, jfloat kneeDb) {
    if (g_engine) {
        std::lock_guard<std::mutex> lock(g_engine->processorMutex());
        g_engine->processor().setCompressionParams(
            static_cast<float>(attackMs), static_cast<float>(releaseMs),
            static_cast<float>(ratio), static_cast<float>(thresholdDb), static_cast<float>(kneeDb));
    }
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetBandCompressionThresholds(JNIEnv* env, jobject /* thiz */, jfloatArray thresholds) {
    if (!g_engine || !thresholds) return;
    jfloat* arr = env->GetFloatArrayElements(thresholds, nullptr);
    if (arr) {
        jsize len = env->GetArrayLength(thresholds);
        std::lock_guard<std::mutex> lock(g_engine->processorMutex());
        g_engine->processor().setBandCompressionThresholds(arr, len);
        env->ReleaseFloatArrayElements(thresholds, arr, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetPrescribedGains(JNIEnv* env, jobject /* thiz */, jfloatArray gains) {
    if (!g_engine || !gains) return;
    jfloat* arr = env->GetFloatArrayElements(gains, nullptr);
    if (arr) {
        jsize len = env->GetArrayLength(gains);
        std::lock_guard<std::mutex> lock(g_engine->processorMutex());
        g_engine->processor().setPrescribedGains(arr, len);
        env->ReleaseFloatArrayElements(gains, arr, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetExpansionParams(JNIEnv* env, jobject /* thiz */, jfloat thresholdDb, jfloat ratio) {
    if (g_engine) {
        std::lock_guard<std::mutex> lock(g_engine->processorMutex());
        g_engine->processor().setExpansionParams(static_cast<float>(thresholdDb), static_cast<float>(ratio));
    }
}

JNIEXPORT void JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeSetBoostQuietSounds(JNIEnv* env, jobject /* thiz */, jfloat percent) {
    if (g_engine) {
        std::lock_guard<std::mutex> lock(g_engine->processorMutex());
        g_engine->processor().setBoostQuietSounds(static_cast<float>(percent));
    }
}

JNIEXPORT jshortArray JNICALL
Java_com_soundamplifier_audio_NativeAudioEngine_nativeGetVisualizationData(JNIEnv* env, jobject /* thiz */) {
    if (!g_engine) return nullptr;
    constexpr int COUNT = 512;
    int16_t buffer[COUNT];
    g_engine->getVisualizationData(buffer, COUNT);
    jshortArray result = env->NewShortArray(COUNT);
    if (result) env->SetShortArrayRegion(result, 0, COUNT, buffer);
    return result;
}

} // extern "C"
