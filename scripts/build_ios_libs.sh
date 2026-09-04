#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
CORE_DIR="${ROOT_DIR}/core"
IOS_DIR="${ROOT_DIR}/apps/ios"
FFI_LIB_DIR="${IOS_DIR}/Centwise/Core/FFI/lib"
FFI_GEN_DIR="${IOS_DIR}/Centwise/Core/FFI/generated"

echo "=== Centwise iOS Native Library Builder ==="

# 1. Resolve Xcode developer directory
if [ -z "${DEVELOPER_DIR:-}" ]; then
    if [ -d "/Applications/Xcode.app/Contents/Developer" ]; then
        export DEVELOPER_DIR="/Applications/Xcode.app/Contents/Developer"
    fi
fi

# 2. Enforce iOS 16.0 deployment target so clang/cc-rs never defaults to SDK version (e.g. 26.5)
export IPHONEOS_DEPLOYMENT_TARGET="16.0"

echo "Using DEVELOPER_DIR: ${DEVELOPER_DIR:-$(xcode-select -p)}"
echo "Enforcing IPHONEOS_DEPLOYMENT_TARGET: ${IPHONEOS_DEPLOYMENT_TARGET}"

cd "${CORE_DIR}"

echo "Building Rust core for aarch64-apple-ios-sim, x86_64-apple-ios, and aarch64-apple-ios..."
cargo build --release -p centwise-ffi --target aarch64-apple-ios-sim
cargo build --release -p centwise-ffi --target x86_64-apple-ios
cargo build --release -p centwise-ffi --target aarch64-apple-ios

# 3. Create destination folders
mkdir -p "${FFI_LIB_DIR}/iphonesimulator" "${FFI_LIB_DIR}/iphoneos"

# 4. Create universal fat library for Simulator (Apple Silicon + Intel)
lipo -create \
  "${CORE_DIR}/target/aarch64-apple-ios-sim/release/libcentwise_ffi.a" \
  "${CORE_DIR}/target/x86_64-apple-ios/release/libcentwise_ffi.a" \
  -output "${FFI_LIB_DIR}/iphonesimulator/libcentwise_ffi.a"

# 5. Copy device library for physical iPhones
cp "${CORE_DIR}/target/aarch64-apple-ios/release/libcentwise_ffi.a" \
   "${FFI_LIB_DIR}/iphoneos/libcentwise_ffi.a"

# 6. Also keep universal library at root lib dir for compatibility
cp "${FFI_LIB_DIR}/iphonesimulator/libcentwise_ffi.a" \
   "${FFI_LIB_DIR}/libcentwise_ffi.a"

echo "Regenerating Swift bindings..."
cargo run -p uniffi-bindgen -- generate \
  --library "${CORE_DIR}/target/aarch64-apple-ios-sim/release/libcentwise_ffi.a" \
  --language swift \
  --config "${CORE_DIR}/centwise-ffi/uniffi.toml" \
  --out-dir "${FFI_GEN_DIR}"

if command -v xcodegen >/dev/null 2>&1; then
    echo "Updating Xcode project with xcodegen..."
    (cd "${IOS_DIR}" && xcodegen generate)
fi

echo "=== Success! iOS native libraries and Swift bindings generated for iOS 16.0 ==="
lipo -info "${FFI_LIB_DIR}/iphonesimulator/libcentwise_ffi.a"
lipo -info "${FFI_LIB_DIR}/iphoneos/libcentwise_ffi.a"
