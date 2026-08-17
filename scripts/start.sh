#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

AVD_NAME="omadroid"
HEADLESS=0
EXTRA_ARGS=()

usage() {
  cat <<EOF
Start the local AOSP emulator.

Usage:
  start.sh [--headless] [--] [emulator-args...]

Options:
  --headless   Boot without a window (-no-window -no-audio)
  -h, --help   Show this help

Windowed boots pass -fixed-scale (1:1 guest pixels).
The emulator binary and AVD live under this repo (sdk/, avd/).
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --headless)
      HEADLESS=1
      shift
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

EMULATOR_BIN="${ANDROID_HOME}/emulator/emulator"
[[ -x "$EMULATOR_BIN" ]] || omadroid_die "emulator not installed. Run ${ROOT}/scripts/setup.sh first."

if ! emulator -list-avds 2>/dev/null | grep -qx "$AVD_NAME"; then
  omadroid_die "AVD ${AVD_NAME} is missing. Run ${ROOT}/scripts/setup.sh first."
fi

# The emulator has no Wayland Qt plugin; XWayland is available on this host.
if [[ -z "${QT_QPA_PLATFORM:-}" ]]; then
  export QT_QPA_PLATFORM=xcb
fi

ARGS=()
if [[ ${#EXTRA_ARGS[@]} -gt 0 ]]; then
  mapfile -t ARGS < <(omadroid_emulator_args "$AVD_NAME" "$HEADLESS" "${EXTRA_ARGS[@]}")
else
  mapfile -t ARGS < <(omadroid_emulator_args "$AVD_NAME" "$HEADLESS")
fi

printf 'omadroid: starting %s\n' "$AVD_NAME"
exec "$EMULATOR_BIN" "${ARGS[@]}"
