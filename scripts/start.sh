#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

AVD_NAME="omadroid"
HEADLESS=0
PRODUCT=0
AOSP_ROOT="${AOSP_ROOT:-}"
EXTRA_ARGS=()

usage() {
  cat <<EOF
Start the local AOSP emulator.

Usage:
  start.sh [--headless] [--] [emulator-args...]
  start.sh --product [--aosp <aosp-root>] [--headless] [--] [emulator-args...]

Options:
  --product    Boot the built omadroid_x86_64 image (not the workbench AVD)
  --aosp DIR   AOSP tree that holds out/target/product/emu64x
  --headless   Boot without a window (-no-window -no-audio)
  -h, --help   Show this help

Windowed boots pass -fixed-scale (1:1 guest pixels).
The workbench emulator binary and AVD live under this repo (sdk/, avd/).
--product prefers the emulator from the AOSP prebuilts.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --headless)
      HEADLESS=1
      shift
      ;;
    --product)
      PRODUCT=1
      shift
      ;;
    --aosp)
      AOSP_ROOT="${2:-}"
      shift 2
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    --)
      shift
      EXTRA_ARGS+=("$@")
      break
      ;;
    *)
      EXTRA_ARGS+=("$1")
      shift
      ;;
  esac
done

omadroid_export_java_home
omadroid_export_paths "$ROOT"

# The emulator has no Wayland Qt plugin; XWayland is available on this host.
if [[ -z "${QT_QPA_PLATFORM:-}" ]]; then
  export QT_QPA_PLATFORM=xcb
fi

ARGS=()
EMULATOR_BIN=""

if [[ "$PRODUCT" == 1 ]]; then
  [[ -n "$AOSP_ROOT" ]] || omadroid_die "pass --aosp <dir> or set AOSP_ROOT"
  AOSP_ROOT="$(cd "$AOSP_ROOT" && pwd)"
  PRODUCT_OUT="$(omadroid_product_out "$AOSP_ROOT")"
  [[ -f "${PRODUCT_OUT}/system.img" ]] || \
    omadroid_die "no product image at ${PRODUCT_OUT}. Build with scripts/build-image.sh first."

  if [[ -x "${AOSP_ROOT}/prebuilts/android-emulator/linux-x86_64/emulator" ]]; then
    EMULATOR_BIN="${AOSP_ROOT}/prebuilts/android-emulator/linux-x86_64/emulator"
  else
    EMULATOR_BIN="${ANDROID_HOME}/emulator/emulator"
  fi
  [[ -x "$EMULATOR_BIN" ]] || omadroid_die "emulator not found. Run setup.sh or use an AOSP tree with prebuilts/android-emulator."

  export ANDROID_PRODUCT_OUT="$PRODUCT_OUT"
  product_data_dir="${ROOT}/avd/omadroid-product"
  product_data_img="${product_data_dir}/userdata-qemu.img"
  mkdir -p "$product_data_dir"
  if [[ ! -f "$product_data_img" ]]; then
    [[ -f "${PRODUCT_OUT}/userdata.img" ]] || \
      omadroid_die "no userdata.img in ${PRODUCT_OUT}"
    cp "${PRODUCT_OUT}/userdata.img" "$product_data_img"
  fi
  product_ramdisk="${product_data_dir}/initrd.img"
  omadroid_merge_product_ramdisk "$PRODUCT_OUT" "$product_ramdisk"
  if [[ ${#EXTRA_ARGS[@]} -gt 0 ]]; then
    mapfile -t ARGS < <(omadroid_product_emulator_args "$PRODUCT_OUT" "$HEADLESS" \
      -datadir "$product_data_dir" -data "$product_data_img" \
      -ramdisk "$product_ramdisk" -partition-size 2048 "${EXTRA_ARGS[@]}")
  else
    mapfile -t ARGS < <(omadroid_product_emulator_args "$PRODUCT_OUT" "$HEADLESS" \
      -datadir "$product_data_dir" -data "$product_data_img" \
      -ramdisk "$product_ramdisk" -partition-size 2048)
  fi
  printf 'omadroid: starting product image %s\n' "$PRODUCT_OUT"
else
  EMULATOR_BIN="${ANDROID_HOME}/emulator/emulator"
  [[ -x "$EMULATOR_BIN" ]] || omadroid_die "emulator not installed. Run ${ROOT}/scripts/setup.sh first."

  if ! emulator -list-avds 2>/dev/null | grep -qx "$AVD_NAME"; then
    omadroid_die "AVD ${AVD_NAME} is missing. Run ${ROOT}/scripts/setup.sh first."
  fi

  if [[ ${#EXTRA_ARGS[@]} -gt 0 ]]; then
    mapfile -t ARGS < <(omadroid_emulator_args "$AVD_NAME" "$HEADLESS" "${EXTRA_ARGS[@]}")
  else
    mapfile -t ARGS < <(omadroid_emulator_args "$AVD_NAME" "$HEADLESS")
  fi
  printf 'omadroid: starting %s\n' "$AVD_NAME"
fi

exec "$EMULATOR_BIN" "${ARGS[@]}"
