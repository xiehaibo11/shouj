cmake_minimum_required(VERSION 3.22.1)

project("clash-verge-rev")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 获取 Android ABI
if(NOT DEFINED ANDROID_ABI)
    message(FATAL_ERROR "ANDROID_ABI is not defined")
endif()

# 映射 Android ABI 到 Go 架构
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GO_ARCH "arm64")
    set(GO_OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../jniLibs/arm64-v8a")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(GO_ARCH "arm")
    set(GO_OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../jniLibs/armeabi-v7a")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(GO_ARCH "amd64")
    set(GO_OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../jniLibs/x86_64")
elseif(ANDROID_ABI STREQUAL "x86")
    set(GO_ARCH "386")
    set(GO_OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../jniLibs/x86")
else()
    message(FATAL_ERROR "Unsupported ANDROID_ABI: ${ANDROID_ABI}")
endif()

message(STATUS "Building for ANDROID_ABI: ${ANDROID_ABI}")
message(STATUS "Go architecture: ${GO_ARCH}")
message(STATUS "Go output directory: ${GO_OUTPUT_DIR}")

# 查找日志库
find_library(log-lib log)

# 导入 Go 编译的库（预先构建）
add_library(clash-core SHARED IMPORTED)
set_target_properties(clash-core PROPERTIES 
    IMPORTED_LOCATION ${GO_OUTPUT_DIR}/libclash.so
)

# 创建 JNI 桥接库
add_library(clash-jni SHARED
    native-lib.cpp
)

# 包含头文件目录
target_include_directories(clash-jni PRIVATE
    ${CMAKE_SOURCE_DIR}/../golang
)

# 链接库
target_link_libraries(clash-jni
    clash-core
    ${log-lib}
    android
)

# 编译选项
target_compile_options(clash-jni PRIVATE
    -Wall
    -Wextra
    -O2
)

# 输出信息
message(STATUS "CMake configuration completed")
message(STATUS "clash-core location: ${GO_OUTPUT_DIR}/libclash.so")
message(STATUS "clash-jni will be built for ${ANDROID_ABI}")
