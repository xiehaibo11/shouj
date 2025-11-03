#include <jni.h>
#include <string>
#include <dlfcn.h>
#include <android/log.h>

#define TAG "ClashCore-JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ==================== 动态加载 Go 函数 ====================

// Go 函数类型定义
typedef void (*nativeInit_t)(const char*, const char*);
typedef void (*nativeReset_t)();
typedef void (*nativeForceGc_t)();
typedef int (*nativeStartTun_t)(int, int);
typedef void (*nativeStopTun_t)();
typedef int (*nativeLoadConfig_t)(const char*);
typedef long long (*nativeQueryTraffic_t)();
typedef char* (*nativeGetVersion_t)();

// 函数指针
static nativeInit_t go_nativeInit = nullptr;
static nativeReset_t go_nativeReset = nullptr;
static nativeForceGc_t go_nativeForceGc = nullptr;
static nativeStartTun_t go_nativeStartTun = nullptr;
static nativeStopTun_t go_nativeStopTun = nullptr;
static nativeLoadConfig_t go_nativeLoadConfig = nullptr;
static nativeQueryTraffic_t go_nativeQueryTraffic = nullptr;
static nativeGetVersion_t go_nativeGetVersion = nullptr;

static void* libclash_handle = nullptr;

// 初始化：动态加载 libclash.so
static bool initGoFunctions() {
    if (libclash_handle != nullptr) {
        return true;  // 已经加载
    }
    
    LOGI("Loading libclash.so dynamically...");
    
    // dlopen libclash.so
    libclash_handle = dlopen("libclash.so", RTLD_NOW | RTLD_GLOBAL);
    if (libclash_handle == nullptr) {
        LOGE("Failed to dlopen libclash.so: %s", dlerror());
        return false;
    }
    
    // dlsym 加载所有函数
    go_nativeInit = (nativeInit_t)dlsym(libclash_handle, "nativeInit");
    go_nativeReset = (nativeReset_t)dlsym(libclash_handle, "nativeReset");
    go_nativeForceGc = (nativeForceGc_t)dlsym(libclash_handle, "nativeForceGc");
    go_nativeStartTun = (nativeStartTun_t)dlsym(libclash_handle, "nativeStartTun");
    go_nativeStopTun = (nativeStopTun_t)dlsym(libclash_handle, "nativeStopTun");
    go_nativeLoadConfig = (nativeLoadConfig_t)dlsym(libclash_handle, "nativeLoadConfig");
    go_nativeQueryTraffic = (nativeQueryTraffic_t)dlsym(libclash_handle, "nativeQueryTraffic");
    go_nativeGetVersion = (nativeGetVersion_t)dlsym(libclash_handle, "nativeGetVersion");
    
    // 检查关键函数
    if (go_nativeStartTun == nullptr) {
        LOGE("Failed to load nativeStartTun: %s", dlerror());
        return false;
    }
    
    LOGI("✅ libclash.so functions loaded successfully");
    return true;
}

// ==================== JNI 包装函数 ====================

extern "C" JNIEXPORT void JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeInit(
        JNIEnv* env, jobject, jstring homeDir, jstring versionName) {
    if (!initGoFunctions()) {
        LOGE("Failed to initialize Go functions");
        return;
    }
    if (go_nativeInit == nullptr) {
        LOGE("nativeInit not available");
        return;
    }
    
    const char* homeDirStr = env->GetStringUTFChars(homeDir, nullptr);
    const char* versionNameStr = env->GetStringUTFChars(versionName, nullptr);
    LOGI("🚀 Initializing core: %s, home: %s", versionNameStr, homeDirStr);
    go_nativeInit(homeDirStr, versionNameStr);
    env->ReleaseStringUTFChars(homeDir, homeDirStr);
    env->ReleaseStringUTFChars(versionName, versionNameStr);
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeReset(
        JNIEnv*, jobject) {
    if (!initGoFunctions() || go_nativeReset == nullptr) return;
    LOGI("Resetting core");
    go_nativeReset();
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeForceGc(
        JNIEnv*, jobject) {
    if (!initGoFunctions() || go_nativeForceGc == nullptr) return;
    go_nativeForceGc();
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeStartTun(
        JNIEnv*, jobject, jint fd, jint mtu) {
    if (!initGoFunctions()) {
        LOGE("❌ Failed to initialize Go functions");
        return -997;
    }
    if (go_nativeStartTun == nullptr) {
        LOGE("❌ nativeStartTun not available");
        return -996;
    }
    
    if (fd <= 0) {
        LOGE("❌ Invalid fd: %d", fd);
        return -1;
    }
    if (mtu <= 0 || mtu > 65535) {
        LOGE("❌ Invalid MTU: %d", mtu);
        return -2;
    }
    
    LOGI("🔧 Starting Mihomo TUN: fd=%d, mtu=%d", fd, mtu);
    int result = go_nativeStartTun(fd, mtu);
    if (result == 0) {
        LOGI("✅ Mihomo TUN started successfully");
    } else {
        LOGE("❌ Failed to start TUN: %d", result);
    }
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeStopTun(
        JNIEnv*, jobject) {
    if (!initGoFunctions() || go_nativeStopTun == nullptr) return;
    LOGI("🛑 Stopping TUN");
    go_nativeStopTun();
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeLoadConfig(
        JNIEnv* env, jobject, jstring configPath) {
    if (!initGoFunctions()) {
        LOGE("Failed to initialize Go functions");
        return -997;
    }
    if (go_nativeLoadConfig == nullptr) {
        LOGE("nativeLoadConfig not available");
        return -996;
    }
    
    if (configPath == nullptr) {
        LOGE("Config path is null");
        return -1;
    }
    const char* configPathStr = env->GetStringUTFChars(configPath, nullptr);
    if (configPathStr == nullptr) {
        LOGE("Failed to get config path string");
        return -2;
    }
    LOGI("📄 Loading config: %s", configPathStr);
    int result = go_nativeLoadConfig(configPathStr);
    if (result != 0) {
        LOGE("Failed to load config: %d", result);
    }
    env->ReleaseStringUTFChars(configPath, configPathStr);
    return result;
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeQueryTraffic(
        JNIEnv*, jobject) {
    if (!initGoFunctions() || go_nativeQueryTraffic == nullptr) return 0LL;
    return go_nativeQueryTraffic();
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeGetVersion(
        JNIEnv* env, jobject) {
    if (!initGoFunctions() || go_nativeGetVersion == nullptr) {
        return env->NewStringUTF("Mihomo 1.18.1 (Go core)");
    }
    char* version = go_nativeGetVersion();
    if (version == nullptr) {
        LOGE("Failed to get version");
        return env->NewStringUTF("Unknown");
    }
    jstring result = env->NewStringUTF(version);
    // Note: Go's //export functions return malloc'd strings that should be freed
    // But we don't have access to freeString here, so just accept the small leak
    return result;
}
