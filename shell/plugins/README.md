# Plugins

Plugins are userland. They load through `omadroid-shell`. Built-in
chrome (Bar, Workspaces, the workspace switcher) lives under
`shell/modules/`, not here.

A plugin is a directory with `manifest.json`. First-party ids use the
`omadroid.` prefix. User plugins will use another prefix.

| Plugin | id | kinds | Soong module |
| --- | --- | --- | --- |
| Home | `omadroid.home` | `home` | `OmadroidLauncher` |
| Contacts | `omadroid.contacts` | `contacts` | `OmadroidContacts` |
| Gallery | `omadroid.gallery` | `gallery` | `OmadroidGallery` |
| Clock | `omadroid.clock` | `clock` | `OmadroidClock` |

`home` is an Omadroid kind. Omarchy has no HOME plugin because Hyprland
is the window manager. On Android, HOME is the desktop.

The host that loads these manifests is `omadroid-shell`
(`com.omadroid.shell`). It is HOME. It starts the `home` plugin
(`OmadroidLauncher`). Launcher3 is omitted from the image.

`OmadroidLauncher` also reads these manifests. Every non-`home` plugin
registers a Command palette item under its plugin id and a row in the
Menu Apps section.
