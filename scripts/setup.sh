#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

# Official "Command line tools only" package from
# https://developer.android.com/studio#command-line-tools-only
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip"
CMDLINE_TOOLS_SHA256="4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583"
AVD_NAME="omadroid"
AVD_RAM_MB="4096"

WORKDIR=""

cleanup() {
  if [[ -n "$WORKDIR" && -d "$WORKDIR" ]]; then
    rm -rf "$WORKDIR"
  fi
}

trap cleanup EXIT

ini_set() {
  local file="$1"
  local key="$2"
  local value="$3"
  if [[ -f "$file" ]] && grep -q "^${key}=" "$file"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$file"
  else
    printf '%s=%s\n' "$key" "$value" >>"$file"
  fi
}

accept_licenses() {
  printf 'omadroid: accepting Android SDK licenses\n'
  set +o pipefail
  yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null
  local status="${PIPESTATUS[1]}"
  set -o pipefail
  [[ "$status" -eq 0 ]] || omadroid_die "failed to accept Android SDK licenses"
}

install_cmdline_tools() {
  local sdkmanager_bin="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
  if [[ -x "$sdkmanager_bin" ]]; then
    printf 'omadroid: cmdline-tools already installed\n'
    return 0
  fi

  WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/omadroid-cmdline.XXXXXX")"
  local zip="${WORKDIR}/commandlinetools.zip"

  printf 'omadroid: downloading Android SDK command-line tools\n'
  curl -fL --retry 3 --retry-delay 2 -o "$zip" "$CMDLINE_TOOLS_URL"
  printf '%s  %s\n' "$CMDLINE_TOOLS_SHA256" "$zip" | sha256sum -c -

  unzip -q "$zip" -d "${WORKDIR}/unpacked"
  [[ -d "${WORKDIR}/unpacked/cmdline-tools" ]] || omadroid_die "zip did not contain cmdline-tools/"

  mkdir -p "${ANDROID_HOME}/cmdline-tools"
  rm -rf "${ANDROID_HOME}/cmdline-tools/latest"
  mv "${WORKDIR}/unpacked/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"
  [[ -x "$sdkmanager_bin" ]] || omadroid_die "sdkmanager missing after unpack"
}

install_sdk_packages() {
  printf 'omadroid: installing emulator, platform-tools, and build packages\n'
  sdkmanager --sdk_root="$ANDROID_HOME" --channel=0 \
    "platform-tools" \
    "emulator" \
    "platforms;android-36" \
    "build-tools;36.0.0"

  printf 'omadroid: listing system images\n'
  local list image
  list="$(sdkmanager --sdk_root="$ANDROID_HOME" --list --channel=0)"
  image="$(omadroid_pick_aosp_image <<<"$list")" || omadroid_die "no AOSP default x86_64 system image in the stable SDK channel"

  printf 'omadroid: installing %s\n' "$image"
  sdkmanager --sdk_root="$ANDROID_HOME" --channel=0 "$image"
  OMADROID_IMAGE="$image"
}

create_avd() {
  local image="$1"
  local device compact

  compact="$(avdmanager list device -c)"
  device="$(omadroid_pick_device <<<"$compact")" || omadroid_die "avdmanager listed no hardware profiles"

  if avdmanager list avd -c 2>/dev/null | grep -qx "$AVD_NAME"; then
    printf 'omadroid: AVD %s already exists (image/device unchanged)\n' "$AVD_NAME"
  else
    printf 'omadroid: creating AVD %s (%s, %s)\n' "$AVD_NAME" "$device" "$image"
    printf 'no\n' | avdmanager create avd -n "$AVD_NAME" -k "$image" -d "$device"
  fi

  local config="${ANDROID_AVD_HOME}/${AVD_NAME}.avd/config.ini"
  [[ -f "$config" ]] || omadroid_die "missing ${config} after AVD create"
  ini_set "$config" "hw.ramSize" "$AVD_RAM_MB"
  ini_set "$config" "hw.gpu.enabled" "yes"
}

omadroid_export_java_home
omadroid_export_paths "$ROOT"

printf 'omadroid: JAVA_HOME=%s\n' "$JAVA_HOME"
printf 'omadroid: ANDROID_HOME=%s\n' "$ANDROID_HOME"

install_cmdline_tools
accept_licenses
install_sdk_packages
create_avd "$OMADROID_IMAGE"

printf '\n'
printf 'omadroid: setup complete\n'
printf 'omadroid: AOSP image %s\n' "$OMADROID_IMAGE"
printf 'omadroid: start with %s/scripts/start.sh\n' "$ROOT"
