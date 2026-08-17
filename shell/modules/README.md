# Built-in modules

Omadroid is the host. **Modules** ship in the product. **Plugins** are
userland and load through `omadroid-shell`.

| Module | id | Role |
| --- | --- | --- |
| Grid | `omadroid.grid` | Divides the viewport into units and allocates rectangles to other modules. |
| Widgets | `omadroid.widgets` | Grid-sized text, field, icon, and button. Theme colors. No own slot. |
| Bar | `omadroid.bar` | Top chrome. One unit tall. Left, Center, and Right anchors. |
| Workspaces | `omadroid.workspaces` | Fill slot between the bars. Will tile inside its allocated units. |
| Workspace switcher | `omadroid.workspace-switcher` | Selects the active workspace. Mounts a switcher on the Bar (Left). |
| Dock | `omadroid.dock` | Bottom chrome. One unit tall. Search field, layout, menu. |

One unit is one line of text, one field, or one button. The unit length
is still TBD (`UNIT_DP` in `ViewportGrid.kt`). The Grid gives leftover
pixels to the Fill slot so Workspaces can tile later. The Dock field
is a `GridField`: one unit tall, no stock EditText chrome.

A module may contribute placements to another module. The workspace
switcher does that for the Bar. It is not a plugin.
