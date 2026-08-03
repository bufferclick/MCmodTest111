# HandyUtils — Useful Client Mod for 1.21.11 [Fabric]

**A genuinely useful, polished, client-only Fabric mod that you'll actually keep installed.**

> Works on **any server**, no server installation needed. Lightweight, zero bloat, all QoL.

---

## Why this mod exists

Most client mods do ONE thing. This one gives you **the 5 features every player ends up installing separately**:

| Feature | Why it's useful |
|---------|----------------|
| **Smooth Zoom** | Spot bases, mobs, builds. Like OptiFine zoom but smoother |
| **Fullbright Toggle** | See in caves/mines without potions. Save torches, find diamonds |
| **Info HUD** | FPS, ping, coords, biome, light level, direction — at a glance |
| **Armor HUD + Durability Warning** | Never break your elytra/tools unexpectedly |
| **Shulker Preview** | Hover any shulker/box item, hold Shift to see ALL contents |
| **Inventory Sorting** | Middle-click any chest or press R to sort. No more clutter |
| **Death Tracker** | Auto saves death coords, never lose items again |

All client-side, minimal performance impact.

---

## 🎮 Controls (configurable in Controls menu)

- **C** — Hold to Zoom (scroll wheel while zooming to adjust strength)
- **H** — Toggle Fullbright
- **R** — Sort inventory / container
- **Middle Click** — Sort chest/furnace/shulker container
- **Shift** — Show full shulker box contents in tooltip

---

## 💬 Commands (client commands)

All commands are `/handy ...` — client-only, no server needed:

```
/handy              - help
/handy hud          - toggle Info HUD
/handy fullbright   - toggle fullbright
/handy death        - show last death position
/handy zoom <1-12>  - set zoom strength (e.g. /handy zoom 4)
```

---

## ✨ Features in Detail

### 1. Smooth Zoom (C)
- Hold **C** for smooth cinematic zoom, inspired by Logical Zoom & OkZoomer
- Scroll wheel while zoomed adjusts from 1.5x to 12x
- Config saved in `config/handyutils.json`
- Mixin: modifies `GameRenderer.getFov()` without touching options

### 2. Fullbright (H)
- Toggleable fullbright for mining/building/caving
- Uses gamma override + `LightTexture` mixin for 1.21.11 where vanilla clamps gamma
- Saves your original gamma and restores cleanly
- Visual indicator in HUD: `[FULLBRIGHT]`

### 3. Info HUD
- Minimal HUD top-left (configurable position):
  - **FPS** colored by performance (green/yellow/red)
  - **Ping** (shows SP for singleplayer)
  - **XYZ** coords (block + precise)
  - **Facing** direction + yaw
  - **Biome** name
  - **Light level** (block + sky + total)
  - **Slime chunk** indicator (planned when seed known)
  - **Status** line for zoom/fullbright
  - **Death** coords memory
- Toggle with `/handy hud` or config
- Semi-transparent background, doesn't clutter screen

### 4. Armor HUD
- Shows armor icons above hotbar, with durability numbers
- Low durability warning (<40% shows number, <15% tool warns near crosshair)
- Never lose your gear mid-fight

### 5. Shulker Preview (Shift)
- Uses new 1.21.11 data component `minecraft:container` (`DataComponents.CONTAINER`)
- Any container item (shulker, chest from creative pick, barrels etc.) shows contents
- Vanilla shows 5 items, we show:
  - Normal: first 5 + count hint
  - **Shift**: ALL items with names & counts
- No need to place shulkers to see what's inside!

### 6. Inventory Sorting
- **Middle-click** chest/container to sort
- **R** in inventory to sort player inventory (singleplayer sorts directly, multiplayer hints until full packet logic)
- Merges stacks first, then sorts alphabetically by item ID
- Works in any `AbstractContainerScreen` (chests, barrels, shulkers, furnaces, hoppers)

### 7. Death Tracker
- Automatically detects death via `Player#isDeadOrDying()` + health check
- Saves XYZ, dimension, time
- Shows in HUD and chat
- Command `/handy death` to recall
- No more F3 + screenshot after lava death!

---

## 🛠 Technical Details (1.21.11 Fabric, Mojang Mappings)

- **MC**: 1.21.11
- **Java**: 21
- **Loader**: 0.19.3+
- **Loom**: 1.17-SNAPSHOT (official Mojang mappings)
- **Fabric API**: 0.141.6+1.21.11
- **Split sourcesets**: `main` (common) + `client` (all features are client-only, but we keep `environment: client` in mod json)
- **Mixins**:
  - `GameRendererMixin` — zoom FOV modifier
  - `AbstractContainerScreenMixin` — middle-click sort
  - `LightTextureMixin` — fullbright gamma clamp bypass (best-effort for 1.21.11)
- **APIs used**:
  - `HudElementRegistry.attachElementBefore/After` (new 1.21.11 HUD API, `HudRenderCallback` deprecated)
  - `KeyMappingHelper`
  - `ItemTooltipCallback` for shulker preview
  - `ClientCommandRegistrationCallback` for `/handy`
  - `ClientTickEvents.END_CLIENT_TICK`

---

## 📁 Project Structure

```
src/
  main/java/com/bufferclick/handyutils/HandyUtilsMod.java
  client/java/com/bufferclick/handyutils/
    client/HandyUtilsClient.java
    config/HandyConfig.java (GSON json in config/handyutils.json)
    feature/
      ZoomFeature.java
      FullbrightFeature.java
      InventorySorter.java
      ShulkerPreview.java
      DeathTracker.java
    hud/
      InfoHud.java
      ArmorHud.java
    mixin/
      GameRendererMixin.java
      AbstractContainerScreenMixin.java
      LightTextureMixin.java
  main/resources/
    fabric.mod.json
    handyutils.mixins.json
    assets/handyutils/lang/en_us.json
    assets/handyutils/icon.png
  client/resources/
    handyutils.client.mixins.json
config/handyutils.json (generated at runtime)
```

---

## 🔧 Installation

### For Players
1. Install Fabric Loader for 1.21.11: https://fabricmc.net/use/
2. Download Fabric API 0.141.6+ for 1.21.11
3. Put `handyutils-1.0.0.jar` + `fabric-api` jar into `mods/` folder
4. Launch, configure keys in Options > Controls

### For Developers (building)

This project uses Gradle wrapper. The wrapper jar is not committed due to sandbox network limits. Do one of:

```bash
# Option A: use system gradle to bootstrap wrapper
gradle wrapper --gradle-version 9.5.1
./gradlew build

# Option B: download wrapper jar manually
curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v9.5.1/gradle/wrapper/gradle-wrapper.jar
./gradlew build

# Output jar
ls build/libs/
```

Requires JDK 21 (`java -version` should show 21).

---

## ⚙️ Config

File `config/handyutils.json` is auto-generated. Example:

```json
{
  "enableInfoHud": true,
  "enableArmorHud": true,
  "enableFullbright": false,
  "enableZoom": true,
  "zoomDivisor": 4.0,
  "fullbrightGamma": 12.0,
  "hudX": 6,
  "hudY": 6,
  "infoHudShowFps": true,
  "infoHudShowPing": true,
  "infoHudShowCoords": true
}
```

Edit while game closed or use `/handy` commands.

---

## 🚀 Why people will actually use it

- **No bloat**: One mod replaces 5-6 single-feature mods
- **Truly client-only**: Join any vanilla/SMP server with it
- **No conflicts**: Avoids overwriting vanilla options permanently
- **Polished**: Smooth zoom transition, semi-transparent HUD, shift-to-expand tooltips, safety checks
- **Useful every session**: Zoom for exploring, Fullbright for caving, HUD for coords/light, Armor HUD for survival, Shulker Preview for storage, Sorting for chests, Death memory for recovery

Inspired by popular mods: Logical Zoom, Gamma Utils, ShulkerBoxTooltip, Inventory Sorter, MiniHUD — but lightweight & combined.

---

## 📝 Roadmap / Ideas for contributors

- [ ] Config screen via ModMenu + Cloth Config
- [ ] Light overlay (F7 style) for spawn-proofing
- [ ] CPS / keystrokes HUD toggle
- [ ] Copy chat line via right-click (ChatScreenMixin)
- [ ] Full grid GUI for shulker preview (Bundle-style rendering)
- [ ] Better multiplayer sorting via packet loop (click handling)
- [ ] Slime chunk seed detection (need seed sync mod)

PRs welcome!

---

## 📄 License

MIT — free to learn, fork, use.

---

## 🙏 Credits

- FabricMC for loader & Loom & Fabric API
- Mojang for Minecraft 1.21.11
- Inspiration from OkZoomer, Logical Zoom, Gamma Utils, ShulkerBoxTooltip, InventorySorter mods

Enjoy! — bufferclick / HandyUtils
