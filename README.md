# KarnMining

A lightweight, client-side Fabric mining assistant for Minecraft **Java Edition 1.21.11**.

Pick any block, press a key, and KarnMining finds the nearest match, plans an
efficient route to it (mining through obstacles only when necessary), mines it
with the tool you are holding, and immediately searches for the next one —
continuously, until you turn it off.

> ⚠️ **Multiplayer warning:** Using KarnMining on multiplayer servers may
> violate server rules and can result in a ban. Only use it where automation
> is explicitly allowed.

## Features

- **Fully standalone** — no Mod Menu or any other mod required. Just Fabric
  Loader, Fabric API, and KarnMining.
- **Configuration menu** — open it with **Crouch + G** (rebindable).
- **Activation** — **Sneak + L** by default (rebindable). Can be switched to
  key-only mode in the menu.
- **Block selection** — a vanilla-styled, searchable, scrollable grid of every
  obtainable block in the current game registry, with icons and a green
  outline on the currently selected block.
- **Efficient pathfinding** — a budgeted A* search that prefers a clear
  no-mining route and only digs when a route requires it, favoring cheap
  blocks so nothing is destroyed unnecessarily.
- **Uses your held tool** — KarnMining never swaps your items or hands you
  tools; it mines exactly as fast as your current pickaxe/axe/etc. allows.
- **Lightweight** — scanning and pathfinding are spread across client ticks,
  results are cached, and paths are only recalculated when the target, the
  world, or the route actually changes.
- **Vanilla integration** — both keybinds appear under **Options → Controls →
  Key Binds → KarnMining**, and the config menu links straight to the vanilla
  Controls screen.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11.
3. Drop `karnmining-1.0.0.jar` into your `mods` folder.

## Usage

| Action | Default |
| --- | --- |
| Open configuration menu | **Crouch + G** |
| Toggle KarnMining | **Sneak + L** (or **L** in key-only mode) |

In the menu you can enable/disable the mod, choose the target block, switch
between *Sneak + key* and *key only* activation, and adjust the search radius
(24–64 blocks). Both keys can be rebound in the vanilla Controls screen.

When toggled, KarnMining prints **"KM is Enabled"** / **"KM is Disabled"** in
chat.

## How it works

1. **Scan** — finds the nearest loaded block of the selected type within the
   search radius (checked in distance order, spread over ticks).
2. **Plan** — an A* pathfinder first tries a route that breaks no blocks; if
   none exists, it plans a route that mines through the cheapest necessary
   blocks.
3. **Walk** — follows the route, sprinting on open ground, jumping when
   needed, and clearing only the blocks that stand in the way.
4. **Mine** — breaks the target using the vanilla interaction manager with
   your held tool, facing the block and syncing look packets to the server.
5. **Repeat** — immediately scans for the next nearest target.

The mod pauses while any menu is open, when the player dies, or in spectator
mode. Configuration is stored in `config/karnmining.json`.

## Development

- Minecraft **1.21.11**, Fabric Loader **0.19.3**, Fabric API
  **0.141.6+1.21.11**, Yarn mappings, Java **21**.
- Requires **Mod Menu**? No. It requires nothing beyond Fabric API.

Build the jar:

```sh
./gradlew build
```

The distributable jar is written to `build/libs/`.

## Releases via GitHub Actions

The repository ships a `.github/workflows/build.yml` that:

- builds the jar on every push / pull request and uploads it as an
  **artifact** (Actions → workflow run → *Upload build artifacts*), and
- publishes a **GitHub Release** with the jar attached whenever a tag like
  `v1.0.0` is pushed:

```sh
git tag v1.0.0
git push origin v1.0.0
```

## License

MIT — see [LICENSE](LICENSE). Author: Kärn.
