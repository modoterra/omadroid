#!/usr/bin/env bash
# Cover-crop theme wallpapers to the guest display.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/scripts/lib.sh"

WIDTH=1080
HEIGHT=1920
THEMES="${ROOT}/shell/themes"

command -v magick >/dev/null || omadroid_die "ImageMagick magick is not installed"

count=0
while IFS= read -r -d '' file; do
  tmp="$(mktemp --suffix=".${file##*.}")"
  magick "$file" \
    -strip \
    -resize "${WIDTH}x${HEIGHT}^" \
    -gravity center \
    -extent "${WIDTH}x${HEIGHT}" \
    -quality 90 \
    "$tmp"
  mv "$tmp" "$file"
  count=$((count + 1))
  echo "omadroid: cropped ${file#"$ROOT/"}"
done < <(
  find "$THEMES" -path '*/backgrounds/*' -type f \
    \( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' \) \
    -print0
)

echo "omadroid: cropped ${count} backgrounds to ${WIDTH}x${HEIGHT}"
