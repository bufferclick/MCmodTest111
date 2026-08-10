# KarnMining

A lightweight, client-side Fabric mining assistant for Minecraft 1.21.11.
Choose a block from a searchable list of every registered block, then let
KarnMining find the nearest loaded match, navigate to it, mine it with the
item currently in your hand, and repeat.

> **Server warning:** Automated movement and mining may violate server rules
> and can get you banned. Use KarnMining only where automation is allowed.

A ready-to-install build is included in the repository as
[`KarnMining-1.0.0.jar`](./KarnMining-1.0.0.jar). The Modrinth listing copy is
available in [`modrinth.md`](./modrinth.md).

## Use

1. Install Fabric Loader, Fabric API, Mod Menu, and KarnMining.
2. Open **Mods → KarnMining → Configure**.
3. Press **Choose a Block...**, search, and select a target.
4. In a world, hold Sneak and press **L** to enable or disable KarnMining.

The `L` binding is rebindable under **Options → Controls → Key Binds →
KarnMining**, and the config screen links directly to that vanilla screen.
The Sneak modifier follows Minecraft's own Sneak binding.

KarnMining first looks for a route that does not break blocks. If one exists,
it sprints along it. Otherwise, it calculates a mining route and clears only
the blocks needed to reach the target. It never swaps your held item.

The search radius defaults to 48 blocks and can be changed from 24 to 64 in
the config screen. Scanning and pathfinding are spread across client ticks to
avoid long frame stalls.

## Profile-card description

**Automatically seek and mine blocks.**

(35 characters.)

## Development

- Minecraft: 1.21.11
- Fabric Loader: 0.19.3+
- Java: 21
- Version: 1.0.0

Build with:

```sh
./gradlew build
```

The distributable JAR is written to `build/libs/`.
