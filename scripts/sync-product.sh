#!/usr/bin/env bash
# Push Soong-built first-party APKs onto a running product guest.
# Needs -writable-system. That flag hangs this goldfish/super boot
# (grey screen, no bootanim). Do not use until that is fixed.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

AOSP_ROOT="${AOSP_ROOT:-}"

usage() {
  cat <<EOF
Push OmadroidLauncher and OmadroidShell onto the running product guest.

Usage:
  sync-product.sh --aosp <aosp-root>
  AOSP_ROOT=<aosp-root> sync-product.sh

The guest must have been started with -writable-system. First run disables
verity and reboots the guest.
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
PRODUCT_OUT="$(omadroid_product_out "$AOSP_ROOT")"

omadroid_export_java_home
omadroid_export_paths "$ROOT"
ADB="${ANDROID_HOME}/platform-tools/adb"
[[ -x "$ADB" ]] || omadroid_die "adb not installed. Run ${ROOT}/scripts/setup.sh first."

launcher_apk="${PRODUCT_OUT}/system_ext/priv-app/OmadroidLauncher/OmadroidLauncher.apk"
shell_apk="${PRODUCT_OUT}/system_ext/priv-app/OmadroidShell/OmadroidShell.apk"
[[ -f "$launcher_apk" ]] || omadroid_die "missing ${launcher_apk}; build OmadroidLauncher first"
[[ -f "$shell_apk" ]] || omadroid_die "missing ${shell_apk}; build OmadroidShell first"

"$ADB" wait-for-device
"$ADB" root >/dev/null
"$ADB" wait-for-device

if ! "$ADB" remount >/dev/null 2>&1; then
  printf 'omadroid: disabling verity (guest will reboot once)\n'
  "$ADB" disable-verity
  "$ADB" reboot
  "$ADB" wait-for-device
  for _ in $(seq 1 90); do
    boot="$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [[ "$boot" == 1 ]]; then
      break
    fi
    sleep 2
  done
  "$ADB" root >/dev/null
  "$ADB" wait-for-device
  "$ADB" remount
fi

"$ADB" push "$launcher_apk" /system_ext/priv-app/OmadroidLauncher/OmadroidLauncher.apk
"$ADB" push "$shell_apk" /system_ext/priv-app/OmadroidShell/OmadroidShell.apk
"$ADB" shell am force-stop com.omadroid.launcher </dev/null
"$ADB" shell am force-stop com.omadroid.shell </dev/null
"$ADB" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME </dev/null

printf 'omadroid: pushed launcher and shell\n'
