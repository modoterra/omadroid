#!/usr/bin/env bash
# Shared helpers for setup.sh and start.sh. Safe to source from tests.

omadroid_die() {
  printf 'omadroid: %s\n' "$*" >&2
  exit 1
}

omadroid_export_java_home() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    return 0
  fi
  command -v java >/dev/null 2>&1 || omadroid_die "java not found on PATH. Install a JDK 17+ and put java on PATH."

  local home=""
  home="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/^[[:space:]]*java\.home = / { print $2; exit }')"
  home="${home%"${home##*[![:space:]]}"}"
  [[ -n "$home" && -x "${home}/bin/java" ]] || omadroid_die "could not determine JAVA_HOME from the running JVM"
  export JAVA_HOME="$home"
}

omadroid_export_paths() {
  local root="${1:-}"
  [[ -n "$root" ]] || omadroid_die "repo root required"
  export ANDROID_HOME="${root}/sdk"
  # Older SDK tools treat ANDROID_SDK_HOME as the parent of .android.
  export ANDROID_SDK_HOME="${root}"
  export ANDROID_USER_HOME="${root}/.android"
  export ANDROID_EMULATOR_HOME="${root}/.android"
  export ANDROID_AVD_HOME="${root}/avd"
  mkdir -p "$ANDROID_HOME" "$ANDROID_USER_HOME" "$ANDROID_AVD_HOME"
  export PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${PATH}"
}

# Read sdkmanager --list output on stdin. Print the newest
# system-images;android-*;default;x86_64 package path.
omadroid_pick_aosp_image() {
  local line path api
  local best_api=""
  local best_path=""
  local fallback=""

  while IFS= read -r line || [[ -n "$line" ]]; do
    path="${line%%|*}"
    path="${path#"${path%%[![:space:]]*}"}"
    path="${path%"${path##*[![:space:]]}"}"
    [[ "$path" == system-images\;android-*\;default\;x86_64 ]] || continue

    api="${path#system-images;android-}"
    api="${api%;default;x86_64}"

    if [[ "$api" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
      if [[ -z "$best_api" ]] || awk -v a="$api" -v b="$best_api" 'BEGIN { exit !(a + 0 > b + 0) }'; then
        best_api="$api"
        best_path="$path"
      fi
    elif [[ -z "$fallback" ]]; then
      fallback="$path"
    fi
  done

  if [[ -n "$best_path" ]]; then
    printf '%s\n' "$best_path"
    return 0
  fi
  if [[ -n "$fallback" ]]; then
    printf '%s\n' "$fallback"
    return 0
  fi
  return 1
}

# Read avdmanager list device -c output on stdin.
# Prefer pixel_7, then the first pixel_*, then the first name.
omadroid_pick_device() {
  local name
  local first=""
  local pixel=""

  while IFS= read -r name || [[ -n "$name" ]]; do
    name="${name//$'\r'/}"
    name="${name#"${name%%[![:space:]]*}"}"
    name="${name%"${name##*[![:space:]]}"}"
    [[ -n "$name" ]] || continue
    [[ "$name" == id:* ]] && continue
    [[ "$name" == Available* ]] && continue
    [[ "$name" == "-------"* ]] && continue

    if [[ -z "$first" ]]; then
      first="$name"
    fi
    if [[ "$name" == "pixel_7" ]]; then
      printf '%s\n' "pixel_7"
      return 0
    fi
    if [[ -z "$pixel" && "$name" == pixel_* ]]; then
      pixel="$name"
    fi
  done

  if [[ -n "$pixel" ]]; then
    printf '%s\n' "$pixel"
    return 0
  fi
  if [[ -n "$first" ]]; then
    printf '%s\n' "$first"
    return 0
  fi
  return 1
}

# Print emulator argv after the binary, one flag per line.
# Windowed mode uses -fixed-scale (1:1 guest pixels). Auto-scale on
# HiDPI/XWayland either postage-stamps a float or leaves a tiled
# window mostly empty.
omadroid_emulator_args() {
  local avd_name="${1:-}"
  local headless="${2:-0}"
  shift 2 || true

  [[ -n "$avd_name" ]] || omadroid_die "AVD name required"

  local -a args=(-avd "$avd_name" -no-metrics)
  if [[ "$headless" == 1 ]]; then
    args+=(-no-window -no-audio)
  else
    args+=(-fixed-scale)
  fi
  if [[ $# -gt 0 ]]; then
    args+=("$@")
  fi
  printf '%s\n' "${args[@]}"
}

# Product apps we hide for user 0. Not Launcher3, SystemUI, Settings,
# LatinIME, WebView, or the telephony/providers stack.
omadroid_strip_packages() {
  cat <<'EOF'
com.android.quicksearchbox
com.android.gallery3d
com.android.camera2
com.android.dialer
com.android.contacts
com.android.messaging
com.android.calendar
com.android.deskclock
com.android.music
org.chromium.webview_shell
com.android.dreams.phototable
com.android.egg
EOF
}

omadroid_home_package() {
  printf '%s\n' "com.omadroid.launcher"
}

omadroid_home_activity() {
  printf '%s\n' "com.omadroid.launcher/.HomeActivity"
}
