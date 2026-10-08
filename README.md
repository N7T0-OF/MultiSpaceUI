# MultiSpaceUI

**Floating spatial windows for Minecraft — container screens become 3D panels in the world.**

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen)](#requirements)
[![Loader](https://img.shields.io/badge/Fabric-Loader%20%3E%3D%200.19.3-orange)](#requirements)
[![Java](https://img.shields.io/badge/Java-25-blue)](#requirements)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

MultiSpaceUI merges two ideas into one mod: **windowed GUIs** (several screens open at once,
movable and resizable) and **spatial GUIs** (interfaces rendered as panels floating in the
world instead of covering the screen).

The panel you are looking at stays large and fully readable; the others shrink, fade and
eventually disappear with distance. A single priority model drives everything:

> **focus** (crosshair) > **distance** > **orientation** → `targetScale`, `targetOpacity`,
> `interactionEnabled`, all smoothed over time.

> 🇫🇷 **Version française :** [README.fr.md](README.fr.md)

---

## Features

- **Multi-window** — a chest, your inventory and a crafting table can be open at the same
  time; each one gets its own panel. Only one window is "active" (the one the game sends
  input to), the others stay alive in the background.
- **Focus priority** — aiming at a panel focuses it: it grows back to full scale and full
  opacity, and becomes the only window that receives clicks. The focused window is never
  hidden by the distance rule.
- **Distance fade** — panels further than `maxDistance` become invisible and inert; between
  `minScale`/`minOpacity` and the focused values everything interpolates smoothly (no jumps).
- **Safe click routing** — a click that does not land on the panel of the active window is
  **dropped**, so a container screen can never interpret it as "clicked outside" and close
  itself. Clicks are never routed to a window other than the one you clicked.
- **Vanilla and mod screens** — white-list / black-list by fully qualified class name, plus
  global switches for containers, inventory and "other" (mod) screens.
- **Two camera modes** — first person and third person, chosen in the config.
- **Animations, optional** — open/close and scale transitions can be turned off entirely
  (motion-sickness friendly).
- **Persistent config** — a plain JSON file in `.minecraft/config/`, hand-editable while the
  game is closed and reloaded at launch.
- **Performance conscious** — no GUI rebuild per frame, no raw OpenGL calls, off-screen
  buffers only for open windows; designed to stay compatible with Sodium/Iris.

---

## Requirements

| Component      | Version                          |
| -------------- | -------------------------------- |
| Minecraft      | **26.3** (later versions: see [Compatibility](#compatibility-with-future-versions)) |
| Java           | **25** (bytecode target 21)      |
| Fabric Loader  | >= 0.19.3                        |
| Fabric API     | any build for 26.3               |

MultiSpaceUI is a **client-side** mod: it does not need to be installed on the server.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.3.
2. Install **Fabric API** in your `mods` folder.
3. Drop `multispaceui-mc26.3-v<version>.jar` into `mods`.
4. Launch the game. The mod is enabled by default.

The JAR name always carries the Minecraft version, so several builds can coexist in the
same folder.

## Controls

All bindings start **unbound** — assign them in
`Options → Controls → MultiSpaceUI`:

| Action                | Default key | Description                                            |
| --------------------- | ----------- | ------------------------------------------------------ |
| Toggle MultiSpaceUI   | *unbound*   | Turns spatial windows on/off; when off, all windows fold and vanilla full-screen GUIs are used. |
| Focus next window     | *unbound*   | Cycles the active window.                              |
| Close active window   | *unbound*   | Closes the active window only; its menu is closed server-side, the other windows stay open. |

Mouse:

- **Look at a panel and click** → that window is focused (and the click is applied to it).
- **Drag a panel** → move it (`dragSensitivity`).
- **Drag a panel edge** → resize it (disable with `resizeEnabled: false`).
- **Click outside every panel** → nothing happens by default (`closeOnClickOutside: false`).

## Configuration

File: **`.minecraft/config/multispaceui.json`** — created on first launch, reloaded at
startup. Every value is clamped to a sane range on load, so a hand-edited file cannot break
rendering. All thresholds are *starting values*: the visibility controller smooths every
change, no value produces a jump on screen.

### Global

| Key | Default | Meaning |
| --- | ------- | ------- |
| `enabled` | `true` | Master switch (same as the toggle keybind). |
| `perspective` | `"FIRST_PERSON"` | `"FIRST_PERSON"` or `"THIRD_PERSON"`. |
| `multiWindowEnabled` | `true` | Allow several screens to stay open at once. |
| `animationsEnabled` | `true` | Open/close and transition animations. |

### Panel placement (world units / degrees)

| Key | Default | Meaning |
| --- | ------- | ------- |
| `panelDistance` | `1.6` | Distance from the camera. |
| `panelWidth` | `1.25` | Panel width in blocks at full scale; height follows the window aspect ratio. |
| `panelSideOffset`, `panelHeightOffset` | `0.0` | Translation of the whole layout. |
| `panelYawOffset`, `panelPitchOffset` | `0.0` | Rotation of the whole layout. |
| `panelsFollowPlayer` | `true` | `false` = panels stay anchored in the world where they opened. |
| `windowSpacingDegrees` | `18` | Angular spacing between two windows of the fan. |
| `backgroundPushBack` | `0.35` | Extra depth for non-focused windows. |
| `resizeEnabled` | `true` | Drag panel edges to resize. |

### Size and opacity

| Key | Default | Meaning |
| --- | ------- | ------- |
| `baseScale` | `1.0` | Scale of the focused window. |
| `minScale` / `maxScale` | `0.35` / `1.15` | Bounds of the scale interpolation. |
| `minOpacity` / `maxOpacity` | `0.12` / `1.0` | Bounds of the opacity interpolation. |

### Priority system

| Key | Default | Meaning |
| --- | ------- | ------- |
| `maxDistance` | `6.0` | Beyond this distance a window is invisible and inert. |
| `focusScaleIntensity` | `0.75` | How much a non-focused window shrinks (`0` = none, `1` = down to `minScale`). |
| `focusFadeIntensity` | `0.85` | How much a non-focused window fades. |
| `crosshairBoost` | `1.0` | Extra priority for the window targeted by the crosshair. |
| `transitionSpeed` | `9.0` | Interpolation speed; higher = snappier. |

### Interaction

| Key | Default | Meaning |
| --- | ------- | ------- |
| `crosshairTargeting` | `true` | Focus follows the crosshair. |
| `strictClickRouting` | `true` | Only the focused window reacts to clicks. |
| `closeOnClickOutside` | `false` | Keep off to guarantee a click outside never closes a container. |
| `dragSensitivity` | `1.0` | Panel drag speed. |

### Rendering

| Key | Default | Meaning |
| --- | ------- | ------- |
| `showHud`, `showHotbar`, `showCrosshair` | `true` | What stays visible while a window is open. |
| `renderScalePercent` | `100` | Off-screen buffer resolution in % of the window size (lower = faster). |
| `linearFiltering` | `true` | Texture filtering of the panels. |
| `blurBackground`, `worldBlurStrength` | `false` / `0.6` | World blur behind the screen. |

### Screen compatibility

| Key | Default | Meaning |
| --- | ------- | ------- |
| `spatialContainers` | `true` | Chests, furnaces, crafting… become panels. |
| `spatialInventory` | `true` | Player inventory becomes a panel. |
| `spatialOtherScreens` | `false` | Anything else (mod screens) only when whitelisted. |
| `includedScreens` | `{}` | Force these screens into a spatial window even when their category is off. |
| `excludedScreens` | `{}` | Never turn these screens into a spatial window. |

Screens are identified by their **fully qualified class name**, e.g.
`net.minecraft.client.gui.screens.inventory.ChestScreen` or
`me.guivnf.mods.windowedguis.client.window.ConfigScreen`. `excludedScreens` always wins.

## Architecture (how it works)

One frame, three capture points — no GUI is extracted twice:

1. `LevelRenderer.render` (TAIL) — captures the projection, view rotation and camera
   position for this frame.
2. `Gui.extractRenderState` — the windowed screen is **skipped**: it is extracted by its own
   off-screen capture instead, so the panel and the HUD never draw the same widgets twice.
3. `GameRenderer.render` (TAIL) — `SpatialWindowRenderer` extracts each open window into its
   own `TextureTarget`, then draws the panels as world-space quads with a perspective
   projection, and restores the previous projection in a `finally` block.

| Class | Role |
| ----- | ---- |
| `WindowManager` | Window lifecycle: open, replace, activate, close, background persistence. |
| `WindowCapture` | One `GuiRenderState` + `GuiRenderer` + `TextureTarget` per window. |
| `WindowTargeting` | Crosshair / cursor ray → panel hit test. |
| `WindowVisibilityController` | `focusWeight`, `distanceWeight`, `orientationWeight`, `targetScale`, `targetOpacity`, `interactionEnabled` + temporal smoothing. |
| `SpatialInput` | Maps the cursor into panel coordinates, focuses the clicked window, drops clicks outside a panel. |
| `MultiSpaceConfig` | Persistent client config. |
| `ScreenCompatibility` | White-list / black-list evaluation. |

## Building

Requires **JDK 25** (`JAVA_HOME` must point to it).

```bash
./gradlew build        # builds, then copies the JAR into copy-jar-here/
./gradlew runClient    # launches a development client
```

The delivered JAR is `copy-jar-here/multispaceui-mc<mc-version>-v<mod-version>.jar`.
Rebuilding the same version overwrites its own JAR and never clobbers another version.

## Test matrix

"Build passes" is not enough: the mod must be **playable**, with interactive 3D interfaces.

| # | Test | Status |
| - | ---- | ------ |
| 1 | `gradlew build` succeeds on its own | ✅ pass |
| 2 | JAR delivered in `copy-jar-here/` with MC + mod version in the name | ✅ pass |
| 3 | Client boots with the mod, no Mixin apply error | ✅ pass |
| 4 | HUD renders normally with the mod enabled | ✅ pass |
| 5 | Container screen appears as a 3D panel, no dark backdrop left behind | ✅ pass |
| 6 | Panel content is not mirrored / UV orientation correct | ✅ pass (visual) |
| 7 | Vanilla hotbar stays drawn while a window is open | ✅ pass |
| 8 | Click inside a panel reaches the right slot (item drag works) | ⏳ pending |
| 9 | Click outside a panel never closes the container | ⏳ pending |
| 10 | Scroll inside a panel | ⏳ pending |
| 11 | Looking at a second window focuses and re-scales it | ⏳ pending |
| 12 | Two windows open at once, background window persists | ⏳ pending |
| 13 | Panel shrinks and fades with distance, gone past `maxDistance` | ⏳ pending |
| 14 | Focused window is never hidden by the distance rule | ⏳ pending |
| 15 | Toggle keybind disables the mod and folds all windows | ⏳ pending |
| 16 | Config edits are applied and persisted across restarts | ⏳ pending |

## Compatibility with future versions

Minecraft 26.3 ships **unobfuscated**, so the build uses no mappings. Every vanilla target is
referenced through an explicit signature (`@At(target = ...)`) instead of an obfuscated name,
and the version-specific call sites are isolated in the `mixin` package. Porting to a later
version therefore means re-checking those signatures (with `javap` or a decompiler) rather
than re-mapping the whole mod.

## Known limitations

- The window **taskbar** (list of open windows) and the **in-game settings screen** are
  designed (config keys exist) but **not implemented yet** — edit `multispaceui.json` for now.
- Window **positions are not persisted** across sessions yet.
- Keybinds are unbound by default and must be assigned once in `Options → Controls`.
- Multi-window requires the *container* menus to stay open server-side; closing a window
  sends its own `ServerboundContainerClosePacket`, so a vanilla server may close the shared
  menu when two container screens are open — this needs more testing on 26.3 servers.
- Tested on a single machine (OpenGL, NVIDIA). Sodium/Iris compatibility is designed for but
  not yet verified on a Sodium installation.

## Credits and licences

- **MultiSpaceUI** — original implementation for this repository, [MIT](LICENSE).
- [**Spatial-GUI**](https://github.com/tastytrash/Spatial-GUI) by tastytrash — **MIT**.
  Studied as an *API reference map* for Minecraft 26.3 (how `GuiRenderer`, screen extraction
  and off-screen targets are wired). **No code was copied**: MultiSpaceUI has its own
  architecture, priority model and input routing.
- [**Windowed GUIs**](https://github.com/Antikythera-Studios/Windowed-GUIs) by guivnf —
  **proprietary, all rights reserved** (no copying, modification or redistribution).
  Consulted for *functional analysis only* (which behaviours a windowed-GUI mod must provide).
  **No code, no asset, no texture was copied**, and neither the sources nor the binaries are
  redistributed in this repository.

Minecraft is a trademark of Mojang AB. This mod is not affiliated with Mojang or Microsoft.
