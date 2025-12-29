# Lunar Client configuration (lunar.toml)

This plugin now ships a dedicated `lunar.toml` that controls **Apollo-based** Lunar Client detection and how the detected mod list is used. The file is copied into the plugin data folder on first run (same behavior as other configs).

Location (Spigot/Paper):
- `plugins/HackedServer/lunar.toml`

Location (Velocity/BungeeCord):
- `plugins/HackedServer/lunar.toml`

The configuration is loaded on startup and on `/hackedserver reload`.

---

## How the data is produced

- The plugin listens for Lunar Client's `lunar:apollo` plugin message and parses the handshake payload.
- The handshake includes a **list of mods** with `id`, `name`, `version`, and `type`.
- Each mod is stored on the player as `LunarModInfo` and used for:
  - marking generic checks (`lunar_client`, `fabric`, `forge`)
  - optional actions (global and per-mod)
  - showing mod list in `/hackedserver check`

No Apollo plugin is required on the server anymore; we parse the payload directly.

---

## Top-level switch

```
enabled = true
```

- If `false`, **all Lunar handling is skipped**:
  - no Lunar mod parsing
  - no Lunar/Fabric/Forge checks set
  - no Lunar actions triggered
  - no Lunar mod list shown

---

## [settings]

```
[settings]
mark_lunar_client = true
mark_fabric = true
mark_forge = true
show_mods_in_check = true
show_mod_versions = false
show_mod_types = false
```

### mark_lunar_client
- When `true`, the player gets the **generic check** `lunar_client` as soon as a Lunar handshake is received.
- This is separate from brand-channel detection in `generic.toml` (it does **not** disable brand-based checks).
- The check is added once per session; actions (see below) fire only when the check appears for the first time.

### mark_fabric
- When `true`, the player gets the **generic check** `fabric` if any Apollo-reported mod has a type containing `FABRIC`.
- Actions fire only on the **first** time the player is observed with Fabric (per session).

### mark_forge
- When `true`, the player gets the **generic check** `forge` if any Apollo-reported mod has a type containing `FORGE`.
- Actions fire only on the **first** time the player is observed with Forge (per session).

### show_mods_in_check
- When `true`, `/hackedserver check <player>` will include a **Lunar Client mods** section if Lunar data exists.
- When `false`, the mod list is still stored internally and still drives actions, but the list is not displayed.

### show_mod_versions
- When `true`, each mod line includes the version (if provided by Apollo).
- Example: `sodium 0.5.8`

### show_mod_types
- When `true`, each mod line includes the Apollo type tag.
- Example: `sodium (FABRIC)`

Formatting details:
- Display name is preferred; if missing, the mod `id` is shown.
- Mod `id`s are normalized to lower case for comparisons.

---

## [actions]

```
[actions]
lunar_client = []
fabric = []
forge = []
```

Each list references **action names** defined in `actions.toml`.

- `lunar_client` runs when the player is **first** detected with Lunar (via Apollo).
- `fabric` runs when Fabric is **first** detected via Apollo.
- `forge` runs when Forge is **first** detected via Apollo.

Example:
```
[actions]
lunar_client = ["alert", "log"]
fabric = ["alert"]
forge = ["alert"]
```

If an action name does not exist in `actions.toml`, it is ignored.

---

## [mod_actions]

```
[mod_actions]
# sodium = ["alert"]
# iris = ["alert"]
```

This section maps **individual mod ids** to action lists.

Rules:
- Keys are **case-insensitive** (internally normalized to lower case).
- Actions fire **only when a mod appears for the first time** compared to the last recorded list.
- Mod actions run **independently** of `mark_fabric` / `mark_forge` (they only depend on `enabled`).

Example:
```
[mod_actions]
sodium = ["alert", "log"]
iris = ["alert"]
```

You can discover the exact mod ids by running:
```
/hackedserver check <player>
```

---

## Message keys (language files)

The Lunar sections in `/hackedserver check` use these language keys:

- `check_lunar_mods`
- `check_lunar_no_mods`
- `lunar_mod_list_format`

If your active language file does not include these keys, the plugin falls back to the bundled defaults so `/hackedserver check` still works.

---

## Full example

```
enabled = true

[settings]
mark_lunar_client = true
mark_fabric = true
mark_forge = true
show_mods_in_check = true
show_mod_versions = true
show_mod_types = false

[actions]
lunar_client = ["alert"]
fabric = ["alert"]
forge = ["alert"]

[mod_actions]
sodium = ["alert"]
iris = ["alert"]
```

Behavior with this config:
- Players on Lunar get the `lunar_client` check and an alert once.
- Players with Fabric get the `fabric` check and an alert once.
- The mod list appears in `/hackedserver check` with versions.
- If `sodium` or `iris` appears for the first time, their actions run.
