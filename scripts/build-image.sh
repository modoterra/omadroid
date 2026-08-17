#!/usr/bin/env bash
# lunch + m the Omadroid product in an AOSP tree. Does not repo sync.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

AOSP_ROOT="${AOSP_ROOT:-}"

usage() {
  cat <<EOF
Build the Omadroid product image.

Usage:
  build-image.sh --aosp <aosp-root>
  AOSP_ROOT=<aosp-root> build-image.sh

Runs prepare-aosp.sh, then lunch $(omadroid_lunch_combo) and m.
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

"${ROOT}/scripts/prepare-aosp.sh" --aosp "$AOSP_ROOT"

export OUT_DIR="${OUT_DIR:-${AOSP_ROOT}/out}"
# Siso's @config//main.star loader is broken on this host; use ninja.
export SOONG_NINJA="${SOONG_NINJA:-ninja}"
# Clippy on aconfig fails depfile_verifier when OUT_DIR is absolute.
export SOONG_DISABLE_CLIPPY="${SOONG_DISABLE_CLIPPY:-true}"
# Reuse the last Soong analysis after Android.bp edits.
export SOONG_INCREMENTAL_ANALYSIS="${SOONG_INCREMENTAL_ANALYSIS:-true}"

cd "$AOSP_ROOT"
# shellcheck disable=SC1091
source build/envsetup.sh
lunch "$(omadroid_lunch_combo)"
m
