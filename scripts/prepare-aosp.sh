#!/usr/bin/env bash
# Link this repo into an AOSP checkout so lunch can see omadroid_x86_64.
# Does not download AOSP.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

AOSP_ROOT="${AOSP_ROOT:-}"

usage() {
  cat <<EOF
Link Omadroid into an AOSP tree.

Usage:
  prepare-aosp.sh --aosp <aosp-root>
  AOSP_ROOT=<aosp-root> prepare-aosp.sh

Creates:
  <aosp>/vendor/modoterra/omadroid  -> this repo
  <aosp>/device/modoterra/omadroid  -> this repo's device/

Then, in that AOSP tree:
  source build/envsetup.sh
  lunch $(omadroid_lunch_combo)
  m
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --aosp)
      AOSP_ROOT="${2:-}"
      shift 2
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    *)
      omadroid_die "unknown argument: $1"
      ;;
  esac
done

[[ -n "$AOSP_ROOT" ]] || omadroid_die "pass --aosp <dir> or set AOSP_ROOT"
AOSP_ROOT="$(cd "$AOSP_ROOT" && pwd)"

[[ -f "${AOSP_ROOT}/build/envsetup.sh" ]] || omadroid_die "${AOSP_ROOT} is not an AOSP tree (missing build/envsetup.sh)"
[[ -f "${AOSP_ROOT}/device/generic/goldfish/64bitonly/product/sdk_phone64_x86_64.mk" ]] || \
  omadroid_die "${AOSP_ROOT} has no goldfish sdk_phone64_x86_64 product"

vendor_link="${AOSP_ROOT}/vendor/modoterra/omadroid"
device_link="${AOSP_ROOT}/device/modoterra/omadroid"

mkdir -p "${AOSP_ROOT}/vendor/modoterra" "${AOSP_ROOT}/device/modoterra"
ln -sfn "$ROOT" "$vendor_link"
ln -sfn "${ROOT}/device" "$device_link"

[[ -L "$vendor_link" && -e "$vendor_link/omadroid.mk" ]] || omadroid_die "vendor link failed"
[[ -L "$device_link" && -e "$device_link/omadroid_x86_64.mk" ]] || omadroid_die "device link failed"

printf 'omadroid: vendor -> %s\n' "$vendor_link"
printf 'omadroid: device -> %s\n' "$device_link"
printf 'omadroid: lunch %s\n' "$(omadroid_lunch_combo)"
