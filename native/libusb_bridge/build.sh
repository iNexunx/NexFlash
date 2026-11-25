#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/native/out"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
if [ -z "$NDK" ]; then
  echo "NDK not found" >&2; exit 1
fi
ABI="${1:-arm64-v8a}"
API="${API:-24}"
BUILD="$ROOT/native/build/libusb_bridge-$ABI"
cmake -S "$ROOT/native/libusb_bridge" -B "$BUILD" \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-$API" \
  -DCMAKE_BUILD_TYPE=MinSizeRel
cmake --build "$BUILD" -j
mkdir -p "$OUT/$ABI"
cp "$BUILD/libusb_bridge.so" "$OUT/$ABI/libusb_bridge.so"
llvm-strip --strip-unneeded "$OUT/$ABI/libusb_bridge.so" || true
ls -lh "$OUT/$ABI/libusb_bridge.so"
