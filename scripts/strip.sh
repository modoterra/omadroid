#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

omadroid_export_java_home
omadroid_export_paths "$ROOT"

ADB="${ANDROID_HOME}/platform-tools/adb"
[[ -x "$ADB" ]] || omadroid_die "adb not installed. Run ${ROOT}/scripts/setup.sh first."

"$ADB" wait-for-device
"$ADB" root >/dev/null
"$ADB" wait-for-device

failed=0
while IFS= read -r pkg || [[ -n "$pkg" ]]; do
  [[ -n "$pkg" ]] || continue
  # adb shell reads stdin; close it so the package list is not consumed.
  if ! out="$("$ADB" shell pm disable-user --user 0 "$pkg" </dev/null 2>&1)"; then
    printf 'omadroid: could not disable %s: %s\n' "$pkg" "$out" >&2
    failed=$((failed + 1))
    continue
  fi
  printf 'omadroid: disabled %s\n' "$pkg"
done < <(omadroid_strip_packages)

[[ "$failed" -eq 0 ]] || omadroid_die "failed to disable ${failed} package(s)"
printf 'omadroid: strip complete\n'
