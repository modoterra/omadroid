# AGENTS.md

Omadroid is an Omarchy-inspired OS on Android, for Omarchy. See [basecamp/omarchy](https://github.com/basecamp/omarchy). This host already runs Omarchy.

The local AOSP emulator in this repo is the workbench, not the product. Stock AOSP only: no Play Store, no Play services.

## Commands

- `./scripts/setup.sh` installs the SDK, emulator, AOSP system image, and `omadroid` AVD.
- `./scripts/start.sh` boots the workbench. Windowed boots pass `-fixed-scale`.
- `./scripts/start.sh --headless` boots without a window.
- `./scripts/strip.sh` disables stock product apps for user 0.
- `./scripts/install-launcher.sh` builds `com.omadroid.launcher`, strips product apps, sets HOME, and disables Launcher3.
- `./tests/lib.test.sh` runs parser tests. It does not download the SDK.
- `cd launcher && ./gradlew testDebugUnitTest` runs launcher unit tests.

## Guest

- Flavor must not mention google.
- Packages must not include `com.android.vending` or `com.google.android.gms`.
- The emulator window class is `Emulator`. On this Omarchy host it should float.
