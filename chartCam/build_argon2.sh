#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ARGON2_SRC="$DIR/src/nativeInterop/cinterop/argon2"
BUILD_DIR="$DIR/build/argon2"
mkdir -p "$BUILD_DIR"

if [ ! -f "$ARGON2_SRC/argon2.c" ]; then
    echo "Argon2 source not found, skipping."
    exit 0
fi

cd "$ARGON2_SRC"

XCODE_DIR="$(xcode-select -p 2>/dev/null || echo "/Applications/Xcode.app/Contents/Developer")"
TOOLCHAIN_BIN="$XCODE_DIR/Toolchains/XcodeDefault.xctoolchain/usr/bin"
IPHONEOS_SDK="$XCODE_DIR/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS.sdk"
IPHONESIM_SDK="$XCODE_DIR/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator.sdk"

if command -v xcrun >/dev/null 2>&1 && xcrun -sdk iphoneos clang -v >/dev/null 2>&1; then
    CLANG_IOS="xcrun -sdk iphoneos clang"
    AR_IOS="xcrun -sdk iphoneos ar"
    CLANG_SIM="xcrun -sdk iphonesimulator clang"
    AR_SIM="xcrun -sdk iphonesimulator ar"
else
    CLANG_IOS="$TOOLCHAIN_BIN/clang -isysroot $IPHONEOS_SDK"
    AR_IOS="$TOOLCHAIN_BIN/ar"
    CLANG_SIM="$TOOLCHAIN_BIN/clang -isysroot $IPHONESIM_SDK"
    AR_SIM="$TOOLCHAIN_BIN/ar"
fi

if [ ! -f "$BUILD_DIR/iosArm64/libargon2.a" ]; then
    mkdir -p "$BUILD_DIR/iosArm64"
    $CLANG_IOS -arch arm64 -O3 -c argon2.c core.c encoding.c ref.c thread.c blake2/blake2b.c gcm_crypto.c -I. -DARGON2_NO_THREADS
    $AR_IOS rcs "$BUILD_DIR/iosArm64/libargon2.a" *.o
    rm *.o
fi

if [ ! -f "$BUILD_DIR/iosSimulatorArm64/libargon2.a" ]; then
    mkdir -p "$BUILD_DIR/iosSimulatorArm64"
    $CLANG_SIM -arch arm64 -O3 -c argon2.c core.c encoding.c ref.c thread.c blake2/blake2b.c gcm_crypto.c -I. -DARGON2_NO_THREADS
    $AR_SIM rcs "$BUILD_DIR/iosSimulatorArm64/libargon2.a" *.o
    rm *.o
fi
