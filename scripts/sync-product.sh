#!/usr/bin/env bash
# Push Soong-built first-party APKs onto a running product guest.
# Uses a tmpfs overlay on /system_ext so we do not need -writable-system
# or disable-verity (those hang this goldfish/super boot).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

AOSP_ROOT="${AOSP_ROOT:-}"

usage() {
  cat <<EOF
Push first-party Omadroid APKs onto the running product guest.

Usage:
  sync-product.sh --aosp <aosp-root>
  AOSP_ROOT=<aosp-root> sync-product.sh

Builds the Gradle HOME APK (Omadroid Compose), then pushes it and the
Soong OmadroidShell APK. Also builds and installs Contacts, Gallery,
and Clock so they appear in the HOME menu. After m OmadroidShell, run
this instead of rebuilding super.img. The overlay lasts until the
guest reboots. User-installed first-party apps persist on userdata.
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

"${ROOT}/scripts/build-launcher.sh" --aosp "$AOSP_ROOT"
"${ROOT}/scripts/build-contacts.sh" --aosp "$AOSP_ROOT"
"${ROOT}/scripts/build-gallery.sh" --aosp "$AOSP_ROOT"
"${ROOT}/scripts/build-clock.sh" --aosp "$AOSP_ROOT"
launcher_apk="${ROOT}/launcher/prebuilt/OmadroidLauncher.apk"
shell_apk="${PRODUCT_OUT}/system_ext/priv-app/OmadroidShell/OmadroidShell.apk"
contacts_apk="${ROOT}/apps/contacts/prebuilt/OmadroidContacts.apk"
gallery_apk="${ROOT}/apps/gallery/prebuilt/OmadroidGallery.apk"
clock_apk="${ROOT}/apps/clock/prebuilt/OmadroidClock.apk"
[[ -f "$launcher_apk" ]] || omadroid_die "missing ${launcher_apk}; build-launcher.sh failed"
[[ -f "$shell_apk" ]] || omadroid_die "missing ${shell_apk}; build OmadroidShell first"
[[ -f "$contacts_apk" ]] || omadroid_die "missing ${contacts_apk}; build-contacts.sh failed"
[[ -f "$gallery_apk" ]] || omadroid_die "missing ${gallery_apk}; build-gallery.sh failed"
[[ -f "$clock_apk" ]] || omadroid_die "missing ${clock_apk}; build-clock.sh failed"

if ! omadroid_wait_for_boot "$ADB" 60; then
  omadroid_die "guest never reached boot_completed"
fi

"$ADB" root >/dev/null
if ! omadroid_wait_for_boot "$ADB" 30; then
  omadroid_die "guest dropped off after adb root"
fi

if ! "$ADB" shell mount </dev/null | grep -q 'overlay on /system_ext '; then
  if ! "$ADB" shell </dev/null 'mkdir -p /mnt/scratch && mount -t tmpfs -o mode=0755 tmpfs /mnt/scratch && mkdir -p /mnt/scratch/system_ext/upper /mnt/scratch/system_ext/work && mount -t overlay overlay -o lowerdir=/system_ext,upperdir=/mnt/scratch/system_ext/upper,workdir=/mnt/scratch/system_ext/work /system_ext'
  then
    omadroid_die "could not overlay /system_ext"
  fi
fi

"$ADB" push "$launcher_apk" /system_ext/priv-app/OmadroidLauncher/OmadroidLauncher.apk
"$ADB" push "$shell_apk" /system_ext/priv-app/OmadroidShell/OmadroidShell.apk
"$ADB" install -r -g "$contacts_apk"
"$ADB" install -r -g "$gallery_apk"
"$ADB" install -r -g "$clock_apk"
"$ADB" logcat -c -b crash </dev/null || true
"$ADB" shell am force-stop com.omadroid.launcher </dev/null
"$ADB" shell am force-stop com.omadroid.shell </dev/null
"$ADB" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME </dev/null

if ! omadroid_check_home "$ADB"; then
  omadroid_die "HOME crashed or never resumed after push (logcat -b crash)"
fi

printf 'omadroid: pushed launcher, shell, Contacts, Gallery, Clock; HOME is up\n'
