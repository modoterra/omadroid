#!/usr/bin/env bash
# Unit tests for scripts/lib.sh. Fixture-only; no network, no SDK.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=../scripts/lib.sh
source "$ROOT/scripts/lib.sh"

FAILS=0

assert_eq() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$expected" == "$actual" ]]; then
    printf 'ok  %s\n' "$name"
  else
    printf 'not ok  %s\n  expected: %s\n  actual:   %s\n' "$name" "$expected" "$actual" >&2
    FAILS=$((FAILS + 1))
  fi
}

assert_fail() {
  local name="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    printf 'not ok  %s\n  expected command to fail\n' "$name" >&2
    FAILS=$((FAILS + 1))
  else
    printf 'ok  %s\n' "$name"
  fi
}

SDK_LIST_FIXTURE="$(
  cat <<'EOF'
Installed packages:
  Path                                              | Version | Description
  -------                                           | ------- | -------
  cmdline-tools;latest                              | 19.0    | Android SDK Command-line Tools (latest)

Available Packages:
  Path                                              | Version | Description                          | Location
  -------                                           | ------- | -------                              | -------
  emulator                                          | 36.2.12 | Android Emulator                     | emulator/
  platforms;android-36                              | 2       | Android SDK Platform 36              | platforms/android-36/
  system-images;android-34;default;x86_64           | 3       | Intel x86_64 Atom System Image       | system-images/android-34/default/x86_64/
  system-images;android-34;google_apis;x86_64       | 14      | Google APIs Intel x86_64 Atom System Image
  system-images;android-35;aosp_atd;x86_64          | 1       | AOSP ATD Intel x86_64 System Image
  system-images;android-35;default;x86_64           | 7       | Intel x86_64 Atom System Image       | system-images/android-35/default/x86_64/
  system-images;android-36;default;arm64-v8a        | 1       | ARM 64 v8a System Image
  system-images;android-36;default;x86_64           | 1       | Intel x86_64 Atom System Image       | system-images/android-36/default/x86_64/
  system-images;android-36;google_apis;x86_64       | 1       | Google APIs Intel x86_64 Atom System Image
  system-images;android-36;google_apis_playstore;x86_64 | 1   | Google Play Intel x86_64 Atom System Image
  system-images;android-Baklava;default;x86_64      | 4       | Preview Intel x86_64 Atom System Image
EOF
)"

assert_eq "picks newest numeric default x86_64 AOSP image" \
  "system-images;android-36;default;x86_64" \
  "$(omadroid_pick_aosp_image <<<"$SDK_LIST_FIXTURE")"

assert_eq "ignores google_apis and playstore even when listed first" \
  "system-images;android-35;default;x86_64" \
  "$(omadroid_pick_aosp_image <<'EOF'
  system-images;android-35;google_apis_playstore;x86_64 | 1
  system-images;android-34;default;x86_64           | 3
  system-images;android-35;default;x86_64           | 7
  system-images;android-35;google_apis;x86_64       | 14
EOF
)"

assert_eq "prefers dotted API 36.1 over 36" \
  "system-images;android-36.1;default;x86_64" \
  "$(omadroid_pick_aosp_image <<'EOF'
  system-images;android-36;default;x86_64           | 1
  system-images;android-36.1;default;x86_64         | 1
EOF
)"

assert_eq "falls back to preview default image when no numeric AOSP exists" \
  "system-images;android-Baklava;default;x86_64" \
  "$(omadroid_pick_aosp_image <<'EOF'
  system-images;android-35;google_apis;x86_64       | 14
  system-images;android-Baklava;default;x86_64      | 4
EOF
)"

assert_fail "fails when no default x86_64 image is listed" \
  omadroid_pick_aosp_image <<'EOF'
  system-images;android-36;google_apis;x86_64       | 1
  system-images;android-36;default;arm64-v8a        | 1
EOF

assert_eq "prefers pixel_7 when listed" \
  "pixel_7" \
  "$(omadroid_pick_device <<'EOF'
pixel_6
medium_phone
pixel_7
tv_4k
EOF
)"

assert_eq "falls back to first pixel_* when pixel_7 is absent" \
  "pixel_8" \
  "$(omadroid_pick_device <<'EOF'
medium_phone
pixel_8
pixel_9
EOF
)"

assert_eq "falls back to first device when no pixel profile exists" \
  "medium_phone" \
  "$(omadroid_pick_device <<'EOF'
medium_phone
tv_4k
EOF
)"

assert_fail "fails when device list is empty" \
  omadroid_pick_device <<'EOF'
EOF

assert_eq "windowed start uses 1:1 guest pixels" \
  $'-avd\nomadroid\n-no-metrics\n-fixed-scale' \
  "$(omadroid_emulator_args omadroid 0)"

assert_eq "headless start has no window and no fixed-scale" \
  $'-avd\nomadroid\n-no-metrics\n-no-window\n-no-audio' \
  "$(omadroid_emulator_args omadroid 1)"

assert_eq "extra emulator args are appended after the defaults" \
  $'-avd\nomadroid\n-no-metrics\n-fixed-scale\n-gpu\nswiftshader_indirect' \
  "$(omadroid_emulator_args omadroid 0 -gpu swiftshader_indirect)"

assert_eq "product device is goldfish emu64x" \
  "emu64x" \
  "$(omadroid_product_device)"

assert_eq "product-out path is under the AOSP tree" \
  "/mnt/data/aosp/out/target/product/emu64x" \
  "$(omadroid_product_out /mnt/data/aosp)"

PRODUCT_ARGS_DIR="$(mktemp -d)"
touch "${PRODUCT_ARGS_DIR}/system.img"
assert_eq "product start uses sysdir without forcing kernel" \
  $'-sysdir\n'"${PRODUCT_ARGS_DIR}"$'\n-no-metrics\n-fixed-scale' \
  "$(omadroid_product_emulator_args "$PRODUCT_ARGS_DIR" 0)"
touch "${PRODUCT_ARGS_DIR}/kernel-ranchu"
assert_eq "product start passes kernel-ranchu when present" \
  $'-sysdir\n'"${PRODUCT_ARGS_DIR}"$'\n-no-metrics\n-kernel\n'"${PRODUCT_ARGS_DIR}/kernel-ranchu"$'\n-fixed-scale' \
  "$(omadroid_product_emulator_args "$PRODUCT_ARGS_DIR" 0)"
rm -rf "$PRODUCT_ARGS_DIR"

STRIP="$(omadroid_strip_packages)"
assert_eq "strip list includes QuickSearchBox" "com.android.quicksearchbox" \
  "$(printf '%s\n' "$STRIP" | grep -Fx 'com.android.quicksearchbox')"
assert_eq "strip list includes Gallery" "com.android.gallery3d" \
  "$(printf '%s\n' "$STRIP" | grep -Fx 'com.android.gallery3d')"
assert_eq "strip list includes Dialer" "com.android.dialer" \
  "$(printf '%s\n' "$STRIP" | grep -Fx 'com.android.dialer')"
assert_fail "strip list does not include Launcher3" \
  grep -Fxq 'com.android.launcher3' <<<"$STRIP"
assert_fail "strip list does not include SystemUI" \
  grep -Fxq 'com.android.systemui' <<<"$STRIP"
assert_fail "strip list does not include Settings" \
  grep -Fxq 'com.android.settings' <<<"$STRIP"

assert_eq "HOME package is omadroid-shell" \
  "com.omadroid.shell" \
  "$(omadroid_home_package)"

assert_eq "home plugin package is the launcher" \
  "com.omadroid.launcher" \
  "$(omadroid_home_plugin_package)"

assert_eq "product name is omadroid_x86_64" \
  "omadroid_x86_64" \
  "$(omadroid_product_name)"

assert_eq "lunch combo is the goldfish userdebug product" \
  "omadroid_x86_64-aosp_current-userdebug" \
  "$(omadroid_lunch_combo)"

REMOVE="$(omadroid_product_packages_remove)"
assert_eq "product removes Launcher3QuickStep" "Launcher3QuickStep" \
  "$(printf '%s\n' "$REMOVE" | grep -Fx 'Launcher3QuickStep')"
assert_eq "product removes Launcher3" "Launcher3" \
  "$(printf '%s\n' "$REMOVE" | grep -Fx 'Launcher3')"
assert_eq "product removes Gallery2" "Gallery2" \
  "$(printf '%s\n' "$REMOVE" | grep -Fx 'Gallery2')"
assert_eq "product removes Dialer" "Dialer" \
  "$(printf '%s\n' "$REMOVE" | grep -Fx 'Dialer')"
assert_eq "product removes Stk" "Stk" \
  "$(printf '%s\n' "$REMOVE" | grep -Fx 'Stk')"
assert_fail "product does not remove SystemUI" \
  grep -Fxq 'SystemUI' <<<"$REMOVE"
assert_fail "product does not remove Settings" \
  grep -Fxq 'Settings' <<<"$REMOVE"
assert_fail "product does not remove LatinIME" \
  grep -Fxq 'LatinIME' <<<"$REMOVE"

assert_eq "first-party plugins include omadroid.home" "omadroid.home" \
  "$(omadroid_first_party_plugin_ids "$ROOT" | grep -Fx 'omadroid.home')"

while IFS= read -r module || [[ -n "$module" ]]; do
  [[ -n "$module" ]] || continue
  if ! grep -q "\"$module\"" "$ROOT/launcher/Android.bp"; then
    printf 'not ok  OmadroidLauncher overrides %s\n' "$module" >&2
    FAILS=$((FAILS + 1))
  else
    printf 'ok  OmadroidLauncher overrides %s\n' "$module"
  fi
done < <(omadroid_product_packages_remove)

assert_eq "AndroidProducts.mk names the x86_64 product" "omadroid_x86_64.mk" \
  "$(grep -o 'omadroid_x86_64.mk' "$ROOT/device/AndroidProducts.mk" | head -1)"

assert_eq "product includes OmadroidShell" "OmadroidShell" \
  "$(grep -o 'OmadroidShell' "$ROOT/omadroid.mk" | head -1)"

STAGE="$(mktemp -d)"
mkdir -p "${STAGE}/app" "${STAGE}/vendor"
printf 'android_app {}\n' >"${STAGE}/app/Android.bp"
omadroid_stage_soong_app "$STAGE" "${STAGE}/vendor" app
assert_eq "stage copies Android.bp" "android_app {}" \
  "$(tr -d '\n' <"${STAGE}/vendor/app/Android.bp")"
rm -rf "$STAGE"

if [[ "$FAILS" -ne 0 ]]; then
  printf '%s test(s) failed\n' "$FAILS" >&2
  exit 1
fi

printf 'all tests passed\n'
