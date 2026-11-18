#!/usr/bin/env bash
set -euo pipefail

if [[ -d "${CARGO_HOME:-$HOME/.cargo}/bin" ]]; then
    export PATH="${CARGO_HOME:-$HOME/.cargo}/bin:$PATH"
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUST_DIR="$REPO_ROOT/rust-core"
JNILIBS_DIR="$REPO_ROOT/android-app/app/src/main/jniLibs"
API_LEVEL="${ANDROID_API_LEVEL:-29}"

resolve_ndk_dir() {
    if [[ -n "${ANDROID_NDK_HOME:-}" && -d "$ANDROID_NDK_HOME" ]]; then
        echo "$ANDROID_NDK_HOME"
        return
    fi
    if [[ -n "${ANDROID_NDK_ROOT:-}" && -d "$ANDROID_NDK_ROOT" ]]; then
        echo "$ANDROID_NDK_ROOT"
        return
    fi
    local sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [[ -n "$sdk_root" && -d "$sdk_root/ndk" ]]; then
        local latest
        latest="$(ls -1 "$sdk_root/ndk" | sort -V | tail -n 1)"
        if [[ -n "$latest" && -d "$sdk_root/ndk/$latest" ]]; then
            echo "$sdk_root/ndk/$latest"
            return
        fi
    fi
    echo "Error: Could not locate Android NDK. Set ANDROID_NDK_HOME or ANDROID_HOME." >&2
    exit 1
}

resolve_host_tag() {
    local os arch
    os="$(uname -s)"
    arch="$(uname -m)"
    case "$os-$arch" in
        Linux-x86_64)  echo "linux-x86_64" ;;
        Darwin-arm64)  echo "darwin-aarch64" ;;
        Darwin-x86_64) echo "darwin-x86_64" ;;
        *)
            echo "Error: Unsupported host platform: $os-$arch" >&2
            exit 1
            ;;
    esac
}

NDK_DIR="$(resolve_ndk_dir)"
HOST_TAG="$(resolve_host_tag)"
TOOLCHAIN_BIN="$NDK_DIR/toolchains/llvm/prebuilt/$HOST_TAG/bin"
if [[ ! -d "$TOOLCHAIN_BIN" && "$HOST_TAG" == "darwin-aarch64" ]]; then
    TOOLCHAIN_BIN="$NDK_DIR/toolchains/llvm/prebuilt/darwin-x86_64/bin"
fi

if [[ ! -d "$TOOLCHAIN_BIN" ]]; then
    echo "Error: NDK LLVM toolchain not found at $TOOLCHAIN_BIN" >&2
    exit 1
fi

build_abi() {
    local rust_target="$1"
    local android_abi="$2"
    local clang_triple="$3"
    local env_prefix env_lower
    env_prefix="$(echo "$rust_target" | tr '[:lower:]-' '[:upper:]_')"
    env_lower="$(echo "$rust_target" | tr '-' '_')"

    echo "==> Building $rust_target ($android_abi) with NDK $(basename "$NDK_DIR")"
    env \
        "CC_${env_lower}=$TOOLCHAIN_BIN/${clang_triple}${API_LEVEL}-clang" \
        "CXX_${env_lower}=$TOOLCHAIN_BIN/${clang_triple}${API_LEVEL}-clang++" \
        "AR_${env_lower}=$TOOLCHAIN_BIN/llvm-ar" \
        "CARGO_TARGET_${env_prefix}_LINKER=$TOOLCHAIN_BIN/${clang_triple}${API_LEVEL}-clang" \
        "CARGO_TARGET_${env_prefix}_AR=$TOOLCHAIN_BIN/llvm-ar" \
        cargo build --manifest-path "$RUST_DIR/Cargo.toml" --target "$rust_target" --release

    mkdir -p "$JNILIBS_DIR/$android_abi"
    cp "$RUST_DIR/target/$rust_target/release/libclass_recorder_core.so" \
       "$JNILIBS_DIR/$android_abi/libclass_recorder_core.so"

    if [[ -x "$TOOLCHAIN_BIN/llvm-strip" ]]; then
        "$TOOLCHAIN_BIN/llvm-strip" --strip-unneeded \
            "$JNILIBS_DIR/$android_abi/libclass_recorder_core.so"
    fi
    echo "    Copied -> $JNILIBS_DIR/$android_abi/libclass_recorder_core.so"
}

build_abi "aarch64-linux-android" "arm64-v8a" "aarch64-linux-android"

if [[ "${1:-}" == "--with-x86_64" ]]; then
    build_abi "x86_64-linux-android" "x86_64" "x86_64-linux-android"
fi
