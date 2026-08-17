# omadroid

An Omarchy-inspired OS on Android, for Omarchy. Site: [omadroid.dev](https://omadroid.dev).

[Omarchy](https://github.com/basecamp/omarchy) is a beautiful, modern, opinionated Linux. Omadroid is that idea on a phone: stock AOSP, no Play Store, no Play services.

This repository is the source of the Omadroid product image and of every first-party plugin. AOSP is the base we compile against, the way Arch is under Omarchy. Google’s `sdk_phone64` zip is only a workbench, not the product.

This machine already runs Omarchy.

## Product image

The product is `omadroid_x86_64`. It inherits the generic goldfish/ranchu phone ([sdk_phone64_x86_64](https://android.googlesource.com/device/generic/goldfish/+/refs/heads/main/64bitonly/product/sdk_phone64_x86_64.mk)), omits the stock `/product` apps and Launcher3, and installs `omadroid-shell` as HOME. The `omadroid.home` plugin (`OmadroidLauncher`) is the home UI.

AOSP stays a local `repo sync` ([download](https://source.android.com/docs/setup/download), [build](https://source.android.com/docs/setup/build/building)). Do not vendor that tree here.

```bash
repo init --partial-clone -b android-latest-release \
  -u https://android.googlesource.com/platform/manifest
repo sync -c -j8
./scripts/prepare-aosp.sh --aosp /path/to/aosp
```

Then in the AOSP tree:

```bash
source build/envsetup.sh
lunch omadroid_x86_64-aosp_current-userdebug
m
```

`prepare-aosp.sh` links this repo to `vendor/modoterra/omadroid` and `device/modoterra/omadroid`. It also applies `device/patches/soong-skip-absolute-host-paths-in-test-package.patch` so Soong can package tests when `OUT_DIR` is absolute. First-party plugins live in `shell/plugins/`. To have `repo sync` place the checkout itself, copy `device/local_manifests/omadroid.xml` into `<aosp>/.repo/local_manifests/`.

## Workbench

Need a JDK 17 or newer on `PATH`. Then:

```bash
./scripts/setup.sh
```

That downloads Google's command-line tools, checks the zip SHA-256, accepts the SDK licenses, installs `platform-tools`, the emulator, and the newest stable `system-images;android-*;default;x86_64` package, and creates an AVD named `omadroid`.

First run is a few gigabytes.

```bash
./scripts/start.sh
```

Boot the built product image instead of the workbench AVD:

```bash
./scripts/start.sh --product --aosp /path/to/aosp
```

Windowed boots use `-fixed-scale` so the guest is 1:1 device pixels. Auto-scale on a HiDPI or XWayland host either shrinks the phone to a postage stamp or leaves a tiled window mostly empty.

On Omarchy / Hyprland, float the window whose class is `Emulator`:

```lua
o.window("^Emulator$", { float = true })
```

Headless (adb only):

```bash
./scripts/start.sh --headless
```

Install the stock Omadroid HOME app (strips the product apps first):

```bash
./scripts/install-launcher.sh
```

If the window comes up with a black screen or a GPU error, start again with software rendering:

```bash
./scripts/start.sh -- -gpu swiftshader_indirect
```

## Check that the guest is AOSP

After the emulator window appears, from another terminal:

```bash
./sdk/platform-tools/adb wait-for-device
./sdk/platform-tools/adb shell getprop sys.boot_completed
./sdk/platform-tools/adb shell getprop ro.build.flavor
./sdk/platform-tools/adb shell pm list packages
```

`ro.build.flavor` should not mention google. `pm list packages` should not include `com.android.vending` or `com.google.android.gms`. AOSP images allow `adb root`.

## Layout

| Path | Role |
| --- | --- |
| `device/` | lunch product, overlay, privapp allowlist |
| `omadroid.mk` | `PRODUCT_PACKAGES` add/remove |
| `shell/` | host process `omadroid-shell` (HOME; loads plugins) |
| `shell/plugins/` | first-party plugins (`omadroid.home`, …) |
| `launcher/` | HOME app sources (Gradle workbench + Soong `OmadroidLauncher`) |
| `scripts/prepare-aosp.sh` | link this repo into an AOSP checkout |
| `sdk/` | workbench `ANDROID_HOME` (gitignored) |
| `avd/` | workbench AVD (gitignored) |
| `.android/` | emulator user files (gitignored) |

Those three data directories are gitignored.

`adb` from platform-tools still writes its server lock and key under `~/.android`. That is the tool's own home directory, not the emulator image.

## Tests

```bash
./tests/lib.test.sh
cd launcher && ./gradlew testDebugUnitTest
cd shell && ./gradlew testDebugUnitTest
```

Parser tests do not download the SDK. The Gradle tests need `platforms;android-36` from setup.
