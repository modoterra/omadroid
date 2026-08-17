#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

omadroid_export_java_home
omadroid_export_paths "$ROOT"

ADB="${ANDROID_HOME}/platform-tools/adb"
[[ -x "$ADB" ]] || omadroid_die "adb not installed. Run ${ROOT}/scripts/setup.sh first."
[[ -x "${ROOT}/launcher/gradlew" ]] || omadroid_die "missing launcher/gradlew"

printf 'sdk.dir=%s\n' "$ANDROID_HOME" >"${ROOT}/launcher/local.properties"

(
  cd "${ROOT}/launcher"
  ./gradlew --no-daemon assembleDebug testDebugUnitTest
)

apk="$(find "${ROOT}/launcher/build/outputs/apk/debug" -name '*-debug.apk' -print -quit)"
[[ -n "$apk" && -f "$apk" ]] || omadroid_die "debug APK missing after assembleDebug"

"$ADB" wait-for-device
"$ADB" root >/dev/null
"$ADB" wait-for-device

"${ROOT}/scripts/strip.sh"

"$ADB" install -r -t "$apk"

home_pkg="$(omadroid_home_package)"
home_activity="$(omadroid_home_activity)"

if ! "$ADB" shell cmd role add-role-holder android.app.role.HOME "$home_pkg"; then
  "$ADB" shell cmd package set-home-activity "$home_activity"
fi

"$ADB" shell pm disable-user --user 0 com.android.launcher3 >/dev/null
"$ADB" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME

printf 'omadroid: HOME is %s\n' "$home_activity"
