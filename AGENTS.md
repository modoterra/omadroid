# AGENTS.md

Omadroid is an Omarchy-inspired OS on Android, for Omarchy. See [basecamp/omarchy](https://github.com/basecamp/omarchy). Public site: [omadroid.dev](https://omadroid.dev). This host already runs Omarchy.

This repository is the source of the product image and of every first-party plugin. AOSP is an external checkout, not vendored here.

The host process is `omadroid-shell` (`com.omadroid.shell`, Soong `OmadroidShell`). It is the product HOME app. It loads first-party plugin manifests and starts the `home` plugin (`omadroid.home` / `OmadroidLauncher`). `OmadroidLauncher` is not a HOME app.

Stock AOSP only: no Play Store, no Play services.

## Commands

- `./scripts/setup.sh` installs the workbench SDK, emulator, AOSP system image, and `omadroid` AVD.
- `./scripts/start.sh` boots the workbench. Windowed boots pass `-fixed-scale`.
- `./scripts/start.sh --headless` boots without a window.
- `./scripts/start.sh --product --aosp <tree>` boots the built `omadroid_x86_64` image.
- `./scripts/strip.sh` disables stock product apps for user 0 on the workbench image.
- `./scripts/install-launcher.sh` builds `com.omadroid.launcher`, strips product apps, sets HOME, and disables Launcher3.
- `./scripts/prepare-aosp.sh --aosp <tree>` links this repo into an AOSP checkout for `lunch omadroid_x86_64-aosp_current-userdebug`.
- `./scripts/build-image.sh --aosp <tree>` lunches and runs `m` with `SOONG_INCREMENTAL_ANALYSIS=true`.
- `./tests/lib.test.sh` runs parser tests. It does not download the SDK or AOSP.
- `cd launcher && ./gradlew testDebugUnitTest` runs launcher unit tests.
- `cd shell && ./gradlew testDebugUnitTest` runs host plugin-registry tests.

## Guest

- Flavor must not mention google.
- Packages must not include `com.android.vending` or `com.google.android.gms`.
- The emulator window class is `Emulator`. On this Omarchy host it should float.

## Product

- Do not add Gallery, Dialer, QSB, or Launcher3 to `PRODUCT_PACKAGES`.
- Do not remove SystemUI, Settings, or LatinIME until `omadroid-shell` replaces them.
- First-party plugin ids use the `omadroid.` prefix and live in `shell/plugins/`.
- Themes are Omarchy `colors.toml` palettes in `shell/themes/`. Default is `tokyo-night`. Use those hex tokens for all UI color.
