#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
CORE_DIR="${ROOT_DIR}/core"
JNI_LIBS_DIR="${ROOT_DIR}/apps/android/app/src/main/jniLibs"

echo "=== Centwise Android Native Library Builder ==="

# Check/find Android SDK and NDK
SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"

if [ -z "${ANDROID_NDK_HOME:-}" ]; then
    if [ -d "${SDK_DIR}/ndk" ]; then
        # Find latest installed NDK version
        LATEST_NDK="$(find "${SDK_DIR}/ndk" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
        if [ -n "${LATEST_NDK}" ]; then
            export ANDROID_NDK_HOME="${LATEST_NDK}"
            echo "Found NDK: ${ANDROID_NDK_HOME}"
        fi
    fi
fi

if [ -z "${ANDROID_NDK_HOME:-}" ] || [ ! -d "${ANDROID_NDK_HOME}" ]; then
    echo "ERROR: Android NDK not found!"
    echo "Please install the NDK using Android Studio:"
    echo "  1. Open Android Studio -> Settings -> Languages & Frameworks -> Android SDK"
    echo "  2. In SDK Tools tab, check 'NDK (Side by side)'"
    echo "  3. Click Apply"
    exit 1
fi

echo "Building Rust core for arm64-v8a and x86_64..."
cd "${CORE_DIR}"

cargo ndk -t arm64-v8a -t x86_64 \
    -o "${JNI_LIBS_DIR}" \
    build --release -p centwise-ffi

echo "=== Success! Native libraries generated ==="
ls -la "${JNI_LIBS_DIR}/arm64-v8a/libcentwise_ffi.so" 2>/dev/null || true
ls -la "${JNI_LIBS_DIR}/x86_64/libcentwise_ffi.so" 2>/dev/null || true
