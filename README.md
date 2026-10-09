# MultiSpaceUI

**Several spatial windows at once — container screens become 3D panels floating in the world, and more than one stays open.**

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen)](#requirements)
[![Loader](https://img.shields.io/badge/Fabric-Loader%20%3E%3D%200.19.3-orange)](#requirements)
[![Java](https://img.shields.io/badge/Java-25-blue)](#requirements)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

MultiSpaceUI is built on two layers:

| Layer | Where it comes from | Licence | What it owns |
| ----- | ------------------- | ------- | ------------ |
| **Spatial GUI core** | verbatim copy of [Spatial-GUI](https://github.com/tastytrash/Spatial-GUI) by tastytrash | **MIT**, see [`LICENSE.spatial-gui.txt`](LICENSE.spatial-gui.txt) | the *active* window: panel placement, camera, off-screen capture, mouse mapping, config screen |
| **Windowed window layer** | original code written for this repository | **MIT**, see [`LICENSE`](LICENSE) | every window *except* the active one: lifecycle, background panels, focus, visibility, input routing, own config |

The active panel is therefore drawn **and** measured by the core, and MultiSpaceUI only adds
what a windowed mod needs: several windows, focus priority, distance fade, and safe click
routing. Splitting the work this way is what keeps the pointer, the panel and the click on
exactly the same geometry — the layer never re-derives a placement the core already owns.

> 🇫🇷 **French version :** [README.fr.md](README.fr.md)

---

## Features

- **Multi-window** — your inventory, a chest and a crafting table can be open at the same
  time; each gets its own panel. Exactly one window is *active* (the one the game sends input
  to); the others stay alive in the background and keep their content up to date.
- **Focus priority** — aiming at a panel focuses it: it grows back to full scale and full
  opacity, and becomes the only window that receives clicks. The focused window is never
  hidden by the distance rule.
- **Distance fade** — panels further than `maxDistance` become invisible and inert; between
  `minScale`/`minOpacity` and the focused values everything interpolates smoothly (no jumps).
- **Safe click routing** — a click that does not land on the panel of the active window is
  **dropped**, so a container screen can never interpret it as "clicked outside" and close
  itself. Clicks are never routed to a window other than the focused one.
- **Vanilla and mod screens** — white-list / black-list by fully qualified class name, plus
  three category switches (containers, inventory, other/mod screens).
- **Two camera modes** — first person and third person, chosen in the config.
- **Animations, optional** — open/close and scale transitions can be turned off entirely
  (motion-sickness friendly).
- **Persistent config** — two plain JSON files in `.minecraft/config/`, hand-editable while
  the game is closed and reloaded at launch, every value clamped to a sane range.
- **Performance conscious** — no GUI rebuild per frame, no raw OpenGL calls, one off-screen
  buffer only per open window.

## Requirements

| Component | Version |
| --------- | ------- |
| Minecraft | **26.3** (see [Compatibility with future versions](#compatibility-with-future-versions)) |
| Java | **25** (bytecode target 21) |
| Fabric Loader | **>= 0.19.3** (validated with 0.19.5) |
| Fabric API | any build for 26.3 (validated with **0.161.0+26.3**) |
| Cloth Config | **>= 26.3.158** — *hard* dependency (the Spatial GUI core's config screen) |
| Mod Menu | optional, only to open the core's config from its own list |

MultiSpaceUI is a **client-side** mod: it does not need to be installed on the server.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.3.
2. Put **Fabric API** and **Cloth Config** in your `mods` folder (Mod Menu is optional).
3. Drop `multispaceui-mc26.3-v<version>.jar` into `mods`.
4. Launch the game. The mod is enabled by default.

The JAR name always carries the Minecraft version *and* the mod version, so several builds
can sit in the same folder without overwriting each other.

## Controls

MultiSpaceUI bindings start **unbound** — assign them in
`Options → Controls → MultiSpaceUI`:

| Action | Default key | Description |
| ------ | ----------- | ----------- |
| Toggle MultiSpaceUI | *unbound* | Turns spatial windows on/off; when off, every window folds and vanilla full-screen GUIs are used. The choice is persisted. |
| Focus next window | *unbound* | Cycles the active window. |
| Close active window | *unbound* | Closes the active window only; its menu is closed server-side, the other windows stay open. |

The Spatial GUI core registers one binding of its own, in its own category:

| Action | Default key | Description |
| ------ | ----------- | ----------- |
| Open Spatial GUI Config | *unbound* | Opens the core's configuration screen (also reachable through Mod Menu). |

Mouse, while a window is open:

- **Look at a panel and click** → that window is focused and receives the click.
- **Click outside every panel** → the click is dropped; nothing happens
  (`closeOnClickOutside: false`).
- **Scroll over a panel** → routed to the screen; **scroll outside** → dropped, like clicks.
- **Dragging a panel edge to resize it is not implemented yet** (see
  [Known limitations](#known-limitations)).

## Configuration

There are **two** configuration files, one per layer — both are created on first launch,
reloaded at startup, and every value is clamped to a sane range so a hand-edited file cannot
break rendering.

| File | Layer | What it changes |
| ---- | ----- | --------------- |
| `.minecraft/config/multispaceui.json` | MultiSpaceUI | windows, focus, distances, click routing, screen lists |
| `.minecraft/config/spatial-gui.json` | Spatial GUI core | where and how the **active** panel is drawn: camera mode, distance, offsets, scale, FOV, HUD, blur, core screen categories |

All thresholds are *starting values*: the visibility controller smooths every change, so no
value produces a jump on screen.

### `multispaceui.json` — global

| Key | Default | Meaning |
| --- | ------- | ------- |
| `enabled` | `true` | Master switch (same as the toggle keybind). |
| `perspective` | `"FIRST_PERSON"` | `"FIRST_PERSON"` or `"THIRD_PERSON"`: pitch used by the anchors of the *background* windows. The active panel follows the core's own camera mode. |
| `multiWindowEnabled` | `true` | Allow several screens to stay open at once. |
| `animationsEnabled` | `true` | Open/close and transition animations. |

### Panel placement (world units / degrees)

| Key | Default | Meaning |
| --- | ------- | ------- |
| `panelDistance` | `1.6` | Reference distance of a window, in blocks: how far a window is allowed to drift from the player before it starts to fade. |
| `panelsFollowPlayer` | `true` | `false` = windows stay anchored in the world where they opened; the distance fade then follows how far the player walks away from them. |
| `windowSpacingDegrees` | `18` | Angular spacing between two consecutive windows of the fan layout. |
| `backgroundPushBack` | `0.35` | Extra depth pushed onto every window that is not the active one. |

> The *size, distance and angle* of the active panel are **not** here: they belong to the
> core (`spatial-gui.json`, keys `screenDistance`, `screenScale`, `screenYawOffset`, …).

### Size and opacity

| Key | Default | Meaning |
| --- | ------- | ------- |
| `baseScale` | `1.0` | Scale of the focused window. |
| `minScale` / `maxScale` | `0.35` / `1.15` | Bounds of the scale interpolation. |
| `minOpacity` / `maxOpacity` | `0.12` / `1.0` | Bounds of the opacity interpolation. |

### Priority system

| Key | Default | Meaning |
| --- | ------- | ------- |
| `maxDistance` | `6.0` | Beyond this distance a window is invisible and inert — except the one being looked at: focus wins over distance. |
| `focusScaleIntensity` | `0.75` | How much a non-focused window shrinks (`0` = none, `1` = down to `minScale`). |
| `focusFadeIntensity` | `0.85` | How much a non-focused window fades. |
| `transitionSpeed` | `9.0` | Interpolation speed; higher = snappier. |

### Interaction and rendering

| Key | Default | Meaning |
| --- | ------- | ------- |
| `crosshairTargeting` | `true` | Focus follows the crosshair instead of the pointer. |
| `closeOnClickOutside` | `false` | Keep off to guarantee a click outside never closes a container. |
| `renderScalePercent` | `100` | Off-screen buffer resolution of a **background** window, in % of its size (25–100; lower = faster). |
| `linearFiltering` | `true` | Texture filtering of the background panels. |

### Screen compatibility

| Key | Default | Meaning |
| --- | ------- | ------- |
| `spatialContainers` | `true` | Chests, furnaces, crafting… become windows. |
| `spatialInventory` | `true` | Player inventory becomes a window. |
| `spatialOtherScreens` | `false` | Anything else (mod screens) only when whitelisted. |
| `includedScreens` | `[]` | Force these screens into a window even when their category is off. |
| `excludedScreens` | `[]` | Never turn these screens into a window. |

Screens are identified by their **fully qualified class name**, e.g.
`net.minecraft.client.gui.screens.inventory.ChestScreen`.
`excludedScreens` always wins.

### Which screen really becomes a window

A screen becomes a spatial window only when **both** layers accept it:

```
spatial window = SpatialGUIClient.shouldHookScreen(screen)   // core categories + its lists
              AND ScreenCompatibility.isWindowable(screen)   // the table above
```

So a screen switched off in `spatial-gui.json` stays a normal full-screen GUI no matter what
`multispaceui.json` says, and the other way round. When in doubt, switch the screen on in
**both** files.

### `spatial-gui.json` — the keys that matter here

| Key | Meaning |
| --- | ------- |
| `screenDistance`, `screenSideOffset`, `screenHeightOffset`, `screenYawOffset`, `screenPitchOffset`, `screenScale` | Placement of the active panel in **third person**. |
| `firstPersonScreen*` (same suffixes) | Same, in **first person**. |
| `guiScale`, `autoCalculateGuiScale`, `autoScaleByFov`, `renderScalePercent` | Resolution and size of the active panel. |
| `inventory`, `containers`, `pauseScreen`, `allScreens`, `enabledScreens`, `disabledScreens` | Core-side screen categories and lists (AND-ed with ours, see above). |
| `firstPersonModeInventory`, `firstPersonModeContainers`, `autoDetectCameraMode`, `useCrosshairForFirstPerson` | Which camera mode the core uses; `autoDetectCameraMode` lets it pick. |
| `hudOptions.hideHotbar`, `hudOptions.hideHud`, `blurWorldBackground`, `worldBlurStrength`, `filtering.*`, `screenAlpha` | HUD, blur and filtering while a screen is open. |

## Architecture (how it works)

One frame, three capture points — no GUI is extracted twice:

1. `LevelRenderer.render` (TAIL) — captures this frame's projection, view rotation and camera
   position, so panels and world can never drift apart.
2. `Gui.extractRenderState` — the active windowed screen is **skipped**: the core extracts it
   into its own off-screen target, so the panel and the HUD never draw the same widgets twice.
3. `GameRenderer.render` (TAIL) — `SpatialWindowRenderer` captures every *background* window
   into its own `TextureTarget`, then draws the panels as world-space quads under the frame's
   projection, restoring the previous one in a `finally` block.

Responsibilities:

| Class | Layer | Role |
| ----- | ----- | ---- |
| `InventoryRenderer`, `RenderUtil`, `AnimationUtil` | core | computes and draws the active panel, its animation and its mouse mapping. |
| `SpatialGUIConfig`, `SpatialGUIKeybinds`, `SpatialGUIModMenu` | core | core config (cloth-config), its keybind and its Mod Menu entry. |
| `WindowManager` | MultiSpaceUI | Window lifecycle: open, replace, activate, close, background persistence. |
| `WindowCapture` | MultiSpaceUI | One `GuiRenderState` + `GuiRenderer` + `TextureTarget` per background window. |
| `SpatialWindowRenderer` | MultiSpaceUI | Frame capture, placement of the background panels, drawing, and the placement of the active panel *borrowed from the core's own quad*. |
| `WindowTargeting` | MultiSpaceUI | Crosshair / cursor ray → panel hit test; the **nearest** panel wins, because that is the one drawn on top. |
| `WindowVisibilityController` | MultiSpaceUI | `focusWeight`, `distanceWeight`, `targetScale`, `targetOpacity`, `interactionEnabled` + temporal smoothing. |
| `SpatialInput` | MultiSpaceUI | Maps the pointer onto the focused panel, focuses the clicked window, and **drops** clicks and scrolls that miss every panel. |
| `MultiSpaceConfig` | MultiSpaceUI | Persistent client config (`multispaceui.json`). |
| `ScreenCompatibility` | MultiSpaceUI | White-list / black-list evaluation, AND-ed with the core's own filter. |

Placement rule of thumb:

- **Active panel** — its quad is read straight from the core (`getQuadBasis()`) and rotated
  into this frame's view space. Nothing is re-derived: centre, animated scale and camera
  anchoring are, by construction, exactly what is drawn.
- **Background panels** — same fan layout, one panel per window, pushed back by
  `backgroundPushBack`, scaled with the core's own quad scale so a background window and the
  active one never disagree about size.

## Building and testing

Requires **JDK 25** (`JAVA_HOME` must point to it).

```bash
./gradlew build              # unit tests + JAR, then copies it into copy-jar-here/
./gradlew runClient          # launches a development client
./gradlew test               # 24 JUnit tests: geometry, visibility, configuration
./gradlew runClientGameTest  # 47 end-to-end checks inside a real, interactive client
```

The delivered JAR is `copy-jar-here/multispaceui-mc<mc-version>-v<mod-version>.jar`
(`copy-jar-here/` is a local drop box, git-ignored: binaries are published as GitHub
Releases). Rebuilding the same version updates its own JAR and never clobbers another one.

The game test drives real input (pointer, mouse buttons, scroll wheel, key bindings) in a
live world: it opens the inventory, points at a slot, clicks it, clicks outside every panel,
opens a chest as a second window, walks 14 blocks away from both panels and finally binds the
toggle key. Every check is logged as `[gametest] PASS ...` (or `FAIL ...`), the run ends with
a single `AssertionError` listing every failure, and the screenshots it takes land in
`build/run/clientGameTest/screenshots/`.

### Tests executed

| Command | Result | When |
| ------- | ------ | ---- |
| `./gradlew test` | **24/24 unit tests, BUILD SUCCESSFUL** | configuration edit, final run |
| `./gradlew runClientGameTest` | **47/47 checks passed, BUILD SUCCESSFUL**, 5 screenshots | configuration edit, final run |
| `./gradlew build` | **BUILD SUCCESSFUL**, JAR written to `copy-jar-here/` | after both of the above |

## Test matrix

"Build passes" is not enough: the mod must be **playable**, with interactive 3D interfaces.
**Last run: 47/47 game-test checks and 24/24 unit tests, both green.**

| # | Test | Result | Verified by |
| - | ---- | ------ | ----------- |
| 1 | `gradlew build` succeeds on its own | ✅ pass | `BUILD SUCCESSFUL` |
| 2 | JAR delivered in `copy-jar-here/` with MC + mod version in the name | ✅ pass | `multispaceui-mc26.3-v0.1.0.jar` |
| 3 | Client boots with both mixin sets applied, no Mixin error | ✅ pass | the game test boots and drives a real client for the whole run |
| 4 | HUD renders normally with the mod enabled | ✅ pass | screenshot `0000_multispaceui-01-inventory-window.png` |
| 5 | Container screen appears as a 3D panel, no dark backdrop left behind | ✅ pass | `the panel is placed in view space`, `the panel content is extracted into its own texture` |
| 6 | Panel content is not mirrored / UV orientation correct | ✅ pass | `the pointer maps onto the slot inside the panel (… / slot 141,187)` + screenshot |
| 7 | Vanilla hotbar stays drawn while a window is open | ✅ pass | screenshot `0000_…` (hotbar, health and XP still drawn) |
| 8 | Click inside a panel reaches the right slot (item drag works) | ✅ pass | `a click inside the panel is routed to the screen`, `clicking the slot picks the stack up` |
| 9 | Click outside a panel never closes the container | ✅ pass | `a click far outside every panel is dropped`, `the very same container screen survives a click outside the panel`, `the click outside neither picked up nor dropped anything` |
| 10 | Scroll inside a panel is routed, scroll outside is dropped | ✅ pass | `scroll inside the panel is routed to the screen`, `scroll far outside every panel is dropped as well` |
| 11 | Looking at a second window focuses and re-scales it | ✅ pass | `the crosshair focuses the new window`, `the background window loses the focus`, `background window is smaller than the focused one (0.35 < 1.0)` |
| 12 | Two windows open at once, background window persists | ✅ pass | `using the chest opens a second window`, `the background window is still visible`, `clicks are never routed to a window that is not focused`, screenshot `0003_multispaceui-04-two-windows.png` |
| 13 | Panel shrinks and fades with distance, gone past `maxDistance` | ✅ pass | `past maxDistance is gone / fades out / shrinks to its minimum` (player teleported 14 blocks away) |
| 14 | Focused window is never hidden by the distance rule | ✅ pass | `the window being looked at is never hidden by the distance rule`, `and it keeps a readable opacity` + unit test `focusedWindowIsNeverHiddenByTheDistanceRule` |
| 15 | Toggle keybind disables the mod and folds all windows | ✅ pass | `the toggle keybind switches the mod off`, `switching the mod off folds every window`, `the switch is persisted in multispaceui.json and read back` |
| 16 | Config edits are applied and persisted across restarts | ✅ pass | `a configuration edit is written to disk and read back (2.5)` + 7 unit tests on `MultiSpaceConfig` |

Test #10 notes: `mouseScrolled` is routed through the same drop predicate as `mouseClicked`,
and the game test exercises both sides of it (over a panel → the screen receives the scroll,
outside every panel → dropped). No vanilla container screen scrolls its own content, so only
the routing is observable — there was no scrollable widget to watch end to end.

Test #6 and #8 notes: the core glides its camera with the pointer (its parallax effect), so a
panel keeps sliding a little after every pointer move. The test therefore chases the slot
until it stops moving under the pointer — the same fixed point a player's hand settles on —
and reads the search and the pointer mapping in a single frame.

## Compatibility with future versions

Minecraft 26.3 ships **unobfuscated**, so the build uses no mappings: the Fabric Loom plugin
does not remap Minecraft or mods. Every vanilla target is referenced through an explicit
signature (`@At(target = ...)`) instead of an obfuscated name, and the version-specific call
sites live in the `mixin` packages. Porting to a later version therefore means re-checking
those signatures (with `javap` or a decompiler) rather than re-mapping the whole mod.

## Known limitations

- The window **taskbar** (list of open windows) and the **in-game settings screen** are
  **not implemented** — edit `multispaceui.json` (or the core's config) for now.
- **Dragging a panel to move it or resize it is not implemented**; the `dragSensitivity` /
  `resizeEnabled` keys of older builds were removed rather than left doing nothing.
- Window **positions are not persisted** across sessions yet.
- Keybinds are unbound by default and must be assigned once in `Options → Controls`.
- Multi-window requires the *container* menus to stay open server-side; closing a window
  sends its own `ServerboundContainerClosePacket`, so a vanilla server may close the shared
  menu when two container screens are open — this needs more testing on 26.3 servers.
- Only `en_us` is translated for MultiSpaceUI's own strings; the core ships `en_us`, `pt_br`
  and `ru_ru`.
- The Spatial GUI core keeps its own upstream issue list in
  `src/main/java/org/tastytrash/spatialGUI/TODO` (toasts drawn over the GUI, hand bob reset
  when entering/leaving a screen, EMI compatibility warnings). Those are inherited as-is.
- Tested on a single machine (OpenGL, NVIDIA). Sodium/Iris compatibility is designed for but
  not yet verified on a Sodium installation.

## Credits and licences

- **MultiSpaceUI** — the windowed layer, written for this repository, [MIT](LICENSE).
- [**Spatial-GUI**](https://github.com/tastytrash/Spatial-GUI) by **tastytrash** — **MIT**.
  Its source code and resources are **included in this repository** (`src/main/java/org/tastytrash/`,
  `src/main/resources/assets/spatial-gui/`, `spatial-gui.mixins.json`) as the rendering core
  this mod is built on. The upstream licence text is kept verbatim in
  [`LICENSE.spatial-gui.txt`](LICENSE.spatial-gui.txt), and the jar carries it too. Changes
  were limited to what MultiSpaceUI needed (decoration 26.3, hooks for the windowed layer).
- [**Windowed GUIs**](https://github.com/Antikythera-Studios/Windowed-GUIs) by guivnf —
  **proprietary, all rights reserved** (no copying, modification or redistribution).
  Consulted for **functional analysis only** (which behaviours a windowed-GUI mod must
  provide). **No code, no asset, no texture was copied**, and neither the sources nor the
  binaries are redistributed in this repository (the local `exemple/` copy is git-ignored).
- Minecraft is a trademark of Mojang AB. This mod is not affiliated with Mojang or Microsoft.
