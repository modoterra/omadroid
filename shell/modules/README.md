# Built-in modules

Omadroid is the host. **Modules** ship in the product. **Plugins** are
userland and load through `omadroid-shell`.

| Module | id | Role |
| --- | --- | --- |
| Grid | `omadroid.grid` | Divides the viewport into units and allocates rectangles to other modules. |
| Widgets | `omadroid.widgets` | Grid-sized views. Each is a Node. `style()` at init and on theme change walks the tree. |
| Bar | `omadroid.bar` | Top chrome. One unit tall. Left, Center, and Right anchors. |
| Workspaces | `omadroid.workspaces` | Fill slot between the bars. Will tile inside its allocated units. |
| Workspace switcher | `omadroid.workspace-switcher` | Selects the active workspace. Mounts a switcher on the Bar (Left). |
| Dock | `omadroid.dock` | Bottom chrome. One unit tall. Command field, layout, menu. |
| Sheet | `omadroid.sheet` | Screen that slides in from the left over the full viewport. Stack push/pop. |

The Command palette lists `OmadroidCommand` entries. Plugins register those from their manifests. Chrome (workspaces, layout, theme) does not.

One unit is a square. Every pad and margin is the same length
(`spacePx` = unit/8). Workspace inset, chrome blur, and chrome fill
are on `GridStyle` (`workspaceInsetPx`, `chromeBlurPx`,
`chromeFillAlpha`). Corners are square. The unit length is still
TBD (`UNIT_DP`). The Grid gives leftover pixels to the Fill slot.

A module may contribute placements to another module. The workspace
switcher does that for the Bar. It is not a plugin.
