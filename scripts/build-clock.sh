#!/usr/bin/env bash
# Gradle-build OmadroidClock and copy it to apps/clock/prebuilt for Soong import.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

AOSP_ROOT="${AOSP_ROOT:-}"

usage() {
  cat <<EOF
Build the Clock APK with Gradle (Omadroid Compose).

Usage:
  build-clock.sh [--aosp <aosp-root>]

Writes apps/clock/prebuilt/OmadroidClock.apk.
With --aosp, signs that APK with the AOSP platform test key.
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

omadroid_export_java_home
omadroid_export_paths "$ROOT"

[[ -x "${ROOT}/apps/clock/gradlew" ]] || omadroid_die "missing apps/clock/gradlew"
if [[ -d "${ANDROID_HOME}" ]]; then
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" >"${ROOT}/apps/clock/local.properties"
fi

(
  cd "${ROOT}/apps/clock"
  ./gradlew --no-daemon assembleDebug
)

src="$(find "${ROOT}/apps/clock/build/outputs/apk/debug" -name '*-debug.apk' -print -quit)"
[[ -n "$src" && -f "$src" ]] || omadroid_die "debug APK missing after assembleDebug"

out_dir="${ROOT}/apps/clock/prebuilt"
mkdir -p "$out_dir"
out="${out_dir}/OmadroidClock.apk"

if [[ -n "$AOSP_ROOT" ]]; then
  AOSP_ROOT="$(cd "$AOSP_ROOT" && pwd)"
  pem="${AOSP_ROOT}/build/target/product/security/platform.x509.pem"
  pk8="${AOSP_ROOT}/build/target/product/security/platform.pk8"
  [[ -f "$pem" && -f "$pk8" ]] || omadroid_die "missing platform test keys under ${AOSP_ROOT}"
  signer=""
  if [[ -d "${ANDROID_HOME}/build-tools" ]]; then
    signer="$(find "${ANDROID_HOME}/build-tools" -name apksigner -type f | sort | tail -n 1)"
  fi
  [[ -n "$signer" && -x "$signer" ]] || omadroid_die "apksigner not found under ${ANDROID_HOME}/build-tools"
  "$signer" sign --key "$pk8" --cert "$pem" --out "$out" --min-sdk-version 26 "$src"
else
  cp -f "$src" "$out"
fi

[[ -f "$out" ]] || omadroid_die "failed to write ${out}"
printf 'omadroid: clock apk -> %s\n' "$out"
