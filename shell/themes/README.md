# Themes

Omadroid uses the same `colors.toml` hex tokens as [Omarchy](https://omarchy.org/) themes. One file colors the product.

Each theme may include a `backgrounds/` directory copied from Omarchy. HOME draws the first wallpaper under the bar, workspaces, and dock. Default slug is `tokyo-night`.

Wallpapers are cover-cropped to the guest display (`1080x1920`, center gravity) so HOME does not decode multi-megapixel landscapes. Re-run `scripts/crop-backgrounds.sh` after adding images.
