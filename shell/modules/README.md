# Built-in modules

Omadroid is the host. **Modules** ship in the product. **Plugins** are
userland and load through `omadroid-shell`.

| Module | id | Role |
| --- | --- | --- |
| Bar | `omadroid.bar` | Top chrome. Left, Center, and Right anchors. |
| Workspaces | `omadroid.workspaces` | Stretch between the top Bar and the bottom launcher. |
| Workspace switcher | `omadroid.workspace-switcher` | Selects the active workspace. Mounts a switcher on the Bar (Left). |

A module may contribute placements to another module. The workspace
switcher does that for the Bar. It is not a plugin.
