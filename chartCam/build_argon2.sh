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

if [ ! -f "$BUILD_DIR/iosArm64/libargon2.a" ]; then
    mkdir -p "$BUILD_DIR/iosArm64"
    xcrun -sdk iphoneos clang -arch arm64 -O3 -c argon2.c core.c encoding.c ref.c thread.c blake2/blake2b.c -I. -DARGON2_NO_THREADS
    xcrun -sdk iphoneos ar rcs "$BUILD_DIR/iosArm64/libargon2.a" *.o
    rm *.o
fi

if [ ! -f "$BUILD_DIR/iosSimulatorArm64/libargon2.a" ]; then
    mkdir -p "$BUILD_DIR/iosSimulatorArm64"
    xcrun -sdk iphonesimulator clang -arch arm64 -O3 -c argon2.c core.c encoding.c ref.c thread.c blake2/blake2b.c -I. -DARGON2_NO_THREADS
    xcrun -sdk iphonesimulator ar rcs "$BUILD_DIR/iosSimulatorArm64/libargon2.a" *.o
    rm *.o
fi
