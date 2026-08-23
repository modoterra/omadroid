# omadroid

An Omarchy-inspired OS on Android, for Omarchy.

[Omarchy](https://github.com/basecamp/omarchy) is a beautiful, modern, opinionated Linux. Omadroid is that idea on a phone: stock AOSP, no Play Store, no Play services.

This machine already runs Omarchy. This repository currently ships a local AOSP workbench so we can build and run the OS here.

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
| `sdk/` | `ANDROID_HOME` (cmdline-tools, emulator, platform-tools, system images) |
| `avd/` | `ANDROID_AVD_HOME` (`omadroid.ini` and the disk) |
| `.android/` | emulator user files |
| `scripts/setup.sh` | install the workbench |
| `scripts/start.sh` | boot the workbench |
| `scripts/strip.sh` | hide stock product apps |
| `scripts/install-launcher.sh` | build and set the Omadroid HOME app |
| `scripts/lib.sh` | path and package-picking helpers |
| `launcher/` | stock HOME app (`com.omadroid.launcher`) |

Those three data directories are gitignored.

`adb` from platform-tools still writes its server lock and key under `~/.android`. That is the tool's own home directory, not the emulator image.

## Tests

```bash
./tests/lib.test.sh
cd launcher && ./gradlew testDebugUnitTest
```

Parser tests do not download the SDK. The Gradle tests need `platforms;android-36` from setup.

## Community

Use common sense and decency. There is no formal code of conduct. We reserve the right to moderate this community to the extent of the law and the policy of the host. Write community@modoterra.xyz if you need us.
