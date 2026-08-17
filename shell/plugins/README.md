# First-party plugins

These plugins ship with Omadroid. They are the source of the product
image together with `device/` and `omadroid.mk`.

A plugin is a directory with `manifest.json`. First-party ids use the
`omadroid.` prefix.

| Plugin | id | kinds | Soong module |
| --- | --- | --- | --- |
| Home | `omadroid.home` | `home` | `OmadroidLauncher` |

`home` is an Omadroid kind. Omarchy has no HOME plugin because Hyprland
is the window manager. On Android, HOME is the desktop.

The host that loads these manifests is `omadroid-shell`. It is not
wired yet. Until it is, `OmadroidLauncher` is installed as the product
HOME app and Launcher3 is omitted from the image.
