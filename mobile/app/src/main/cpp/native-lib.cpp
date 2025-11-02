#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "ClashCore-JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {
    void nativeInit(const char* homeDir, const char* versionName);
    void nativeReset();
    void nativeForceGc();
    int nativeStartTun(int fd, int mtu);
    void nativeStopTun();
    int nativeLoadConfig(const char* configPath);
    long long nativeQueryTraffic();
    char* nativeGetVersion();
    void freeString(char* str);
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeInit(
        JNIEnv* env, jobject, jstring homeDir, jstring versionName) {
    const char* homeDirStr = env->GetStringUTFChars(homeDir, nullptr);
    const char* versionNameStr = env->GetStringUTFChars(versionName, nullptr);
    LOGI("Initializing core: %s, home: %s", versionNameStr, homeDirStr);
    nativeInit(homeDirStr, versionNameStr);
    env->ReleaseStringUTFChars(homeDir, homeDirStr);
    env->ReleaseStringUTFChars(versionName, versionNameStr);
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeReset(
        JNIEnv*, jobject) {
    LOGI("Resetting core");
    nativeReset();
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeForceGc(
        JNIEnv*, jobject) {
    LOGI("Force GC");
    nativeForceGc();
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeStartTun(
        JNIEnv*, jobject, jint fd, jint mtu) {
    if (fd <= 0) {
        LOGE("Invalid fd: %d", fd);
        return -1;
    }
    if (mtu <= 0 || mtu > 65535) {
        LOGE("Invalid MTU: %d", mtu);
        return -2;
    }
    LOGI("Starting TUN: fd=%d, mtu=%d", fd, mtu);
    int result = nativeStartTun(fd, mtu);
    if (result != 0) {
        LOGE("Failed to start TUN: %d", result);
    }
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeStopTun(
        JNIEnv*, jobject) {
    LOGI("Stopping TUN");
    nativeStopTun();
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeLoadConfig(
        JNIEnv* env, jobject, jstring configPath) {
    if (configPath == nullptr) {
        LOGE("Config path is null");
        return -1;
    }
    const char* configPathStr = env->GetStringUTFChars(configPath, nullptr);
    if (configPathStr == nullptr) {
        LOGE("Failed to get config path string");
        return -2;
    }
    LOGI("Loading config: %s", configPathStr);
    int result = nativeLoadConfig(configPathStr);
    if (result != 0) {
        LOGE("Failed to load config: %d", result);
    }
    env->ReleaseStringUTFChars(configPath, configPathStr);
    return result;
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeQueryTraffic(
        JNIEnv*, jobject) {
    return nativeQueryTraffic();
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeGetVersion(
        JNIEnv* env, jobject) {
    char* version = nativeGetVersion();
    if (version == nullptr) {
        LOGE("Failed to get version");
        return env->NewStringUTF("Unknown");
    }
    jstring result = env->NewStringUTF(version);
    freeString(version);
    return result;
}
