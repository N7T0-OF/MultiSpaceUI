package dev.multispaceui.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.multispaceui.MultiSpaceUI;
import dev.multispaceui.config.MultiSpaceConfig;
import dev.multispaceui.core.WindowManager;
import dev.multispaceui.interaction.SpatialInput;
import dev.multispaceui.mixin.gui.AbstractContainerScreenAccessor;
import dev.multispaceui.rendering.PanelGeometry;
import dev.multispaceui.rendering.PanelGeometry.QuadBasis;
import dev.multispaceui.rendering.SpatialWindowRenderer;
import dev.multispaceui.window.SpatialWindow;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * End-to-end game test of the spatial windows.
 *
 * <p>Run with {@code -Dfabric.client.gametest -Dfabric.client.gametest.modid=multispaceui}
 * (see {@code runGametest} in {@code build.gradle}). The test drives real input: it opens the
 * inventory, moves the pointer onto the panel, clicks a slot, clicks outside the panel, opens
 * a second window and checks that the priority system keeps the focused window large and the
 * background one small and transparent.</p>
 *
 * <p>Every check is recorded; the test fails with the full list once the world is closed, so a
 * single broken expectation never hides the others.</p>
 */
public final class MultiSpaceGameTests implements FabricClientGameTest {

    /** Frames given to the visibility controller to settle after a change. */
    private static final int SETTLE_TICKS = 20;

    /**
     * Mouse buttons use the SDL numbering of Minecraft 26.3: 1 = left, 2 = middle, 3 = right.
     *
     * <p>The old GLFW numbering (0 = left, 1 = right) is gone; sending 0 reaches no button at
     * all and sending 1 would be a left click, which breaks a "right click to open" check.</p>
     */
    private static final int LEFT_BUTTON = 1;
    private static final int RIGHT_BUTTON = 3;

    /**
     * Key code given to the toggle binding before it is pressed. Both the binding and the key
     * event are built from this very value, so the two match whatever numbering the running
     * game uses for keyboard keys.
     */
    private static final int TOGGLE_KEY = 97;

    private final List<String> failures = new ArrayList<>();
    private int checks;

    /** A point of the screen content, in the GUI units the screen was laid out for. */
    private record GuiPoint(double x, double y) {
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        MultiSpaceUI.LOGGER.info("[gametest] MultiSpaceUI game tests start");

        check(MultiSpaceUI.isEnabled(), "the mod is enabled by default");
        check(MultiSpaceConfig.get().enabled, "the configuration enables spatial windows");
        check(!MultiSpaceConfig.get().closeOnClickOutside,
                "a click outside a panel is dropped instead of closing the container");

        leaveAnyWorld(ctx);

        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(true).create()) {
            world.getConnection().waitForChunksRender();
            prepareWorld(ctx, world);
            testInventoryWindow(ctx);
            testClickInsidePanel(ctx);
            testClickOutsideKeepsScreen(ctx);
            testSecondWindowAndPriority(ctx, world);
            testDistanceFalloff(ctx, world);
            testCloseOneWindowKeepsTheOther(ctx);
            testToggleKeybindAndConfig(ctx);
        }

        if (!failures.isEmpty()) {
            throw new AssertionError(checks + " checks, " + failures.size() + " failed: "
                    + String.join(" | ", failures));
        }
        // Happy path: let the framework verify that the run ends on the title screen.
        MultiSpaceUI.LOGGER.info("[gametest] {} checks passed", checks);
        ctx.waitFor(mc -> mc.gui.screen() instanceof TitleScreen, 600);
    }

    // ----------------------------------------------------------------------------------
    // World setup
    // ----------------------------------------------------------------------------------

    /** Quits a world that was already open, so the run always starts from the title screen. */
    private void leaveAnyWorld(ClientGameTestContext ctx) {
        if (!read(ctx, mc -> mc.level != null)) {
            return;
        }
        onClient(ctx, mc -> {
            if (mc.gui.screen() != null) {
                mc.gui.setScreen(null);
            }
        });
        ctx.setScreen(() -> new PauseScreen(false));
        ctx.waitForScreen(PauseScreen.class);
        ctx.clickScreenButton("menu.saveAndQuit");
        ctx.waitFor(mc -> mc.gui.screen() instanceof TitleScreen);
    }

    /**
     * Survival keeps the inventory screen (and therefore its slot positions) predictable, and
     * one hotbar stack gives the click test something to pick up.
     */
    private void prepareWorld(ClientGameTestContext ctx, TestSingleplayerContext world) {
        world.getServer().<RuntimeException>runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.setGameMode(GameType.SURVIVAL);
            Inventory inventory = player.getInventory();
            inventory.clearContent();
            inventory.setItem(0, new ItemStack(Items.STONE, 5));
            inventory.setChanged();
        });
        ctx.waitTicks(10);
    }

    // ----------------------------------------------------------------------------------
    // Tests
    // ----------------------------------------------------------------------------------

    private void testInventoryWindow(ClientGameTestContext ctx) {
        ctx.getInput().pressKey(options -> options.keyInventory);
        ctx.waitFor(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?>);
        ctx.waitTicks(SETTLE_TICKS);

        check(read(ctx, mc -> WindowManager.get().windows().size()) == 1,
                "opening the inventory creates exactly one window");
        SpatialWindow inventory = read(ctx, mc -> WindowManager.get().active());
        check(inventory != null, "the inventory window is the active window");
        if (inventory == null) {
            return;
        }
        check(read(ctx, mc -> SpatialInput.isManaged(mc.gui.screen())),
                "the inventory screen is owned by a window instead of the HUD");
        check(read(ctx, mc -> SpatialWindowRenderer.instance.targeting().hasPlacement()),
                "the panel is placed in view space");
        check(read(ctx, mc -> inventory.isVisible()), "the focused panel is visible");
        check(read(ctx, mc -> inventory.isFocused()), "the crosshair focuses the panel");
        check(read(ctx, mc -> inventory.targetScale()) >= MultiSpaceConfig.get().baseScale * 0.9f,
                "the focused panel keeps its full size");
        check(read(ctx, mc -> inventory.targetOpacity()) >= MultiSpaceConfig.get().maxOpacity * 0.9f,
                "the focused panel stays fully opaque");
        check(read(ctx, mc -> SpatialWindowRenderer.instance.hasCaptureContent(inventory)),
                "the panel content is extracted into its own texture");
        ctx.takeScreenshot("multispaceui-01-inventory-window");
    }

    private void testClickInsidePanel(ClientGameTestContext ctx) {
        SpatialWindow inventory = read(ctx, mc -> WindowManager.get().active());
        if (inventory == null) {
            check(false, "the inventory window is still active before the click test");
            return;
        }

        GuiPoint slot = read(ctx, mc -> {
            if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
                return null;
            }
            AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) screen;
            for (Slot candidate : screen.getMenu().slots) {
                if (candidate.hasItem()) {
                    return new GuiPoint(access.multispaceui$getLeftPos() + candidate.x + 8,
                            access.multispaceui$getTopPos() + candidate.y + 8);
                }
            }
            return null;
        });
        check(slot != null, "an item sits in a slot of the inventory");
        if (slot == null) {
            return;
        }

        // The base glides its camera with the pointer, so the panel slides a little after
        // every pointer move. Follow the slot until it stops moving under the pointer,
        // which is the fixed point a player's hand settles on.
        double[] pixel = settleCursorOn(ctx, inventory, slot.x(), slot.y());
        check(pixel != null, "the slot can be pointed at with the pointer");
        if (pixel == null) {
            return;
        }

        SlotProbe probe = probeSlot(ctx, inventory, slot.x(), slot.y());
        // The panel keeps gliding a little while the pointer rests on it, so the pointer is
        // chased onto the slot again until a single frame reads both as one.
        for (int attempt = 0; attempt < 3 && probe != null
                && (Math.abs(probe.mappedX() - slot.x()) > 4.0
                || Math.abs(probe.mappedY() - slot.y()) > 4.0); attempt++) {
            moveCursorTo(ctx, probe.foundX(), probe.foundY());
            probe = probeSlot(ctx, inventory, slot.x(), slot.y());
        }
        double guiX = probe == null ? Double.NaN : probe.mappedX();
        double guiY = probe == null ? Double.NaN : probe.mappedY();
        check(probe != null
                        && Math.abs(guiX - slot.x()) <= 4.0
                        && Math.abs(guiY - slot.y()) <= 4.0,
                "the pointer maps onto the slot inside the panel (panel "
                        + (int) guiX + "," + (int) guiY + " / slot " + (int) slot.x() + "," + (int) slot.y()
                        + (probe == null ? "" : " / cursor " + (int) probe.cursorX() + "," + (int) probe.cursorY()) + ")");
        check(!read(ctx, mc -> SpatialInput.shouldDropClick(mc.gui.screen())),
                "a click inside the panel is routed to the screen instead of being dropped");
        // Scrolling shares that routing decision (MouseHandlerMixin routes mouseScrolled the
        // very same way as mouseClicked), so it must reach the screen while over the panel.
        ctx.getInput().scroll(-1.0);
        ctx.waitTicks(2);
        check(!read(ctx, mc -> SpatialInput.shouldDropClick(mc.gui.screen())),
                "scroll inside the panel is routed to the screen");

        // The panel glides on: point at the slot one last time so the click lands on it.
        settleCursorOn(ctx, inventory, slot.x(), slot.y());
        ctx.getInput().pressMouse(LEFT_BUTTON);
        ctx.waitTicks(5);
        check(read(ctx, mc -> mc.gui.screen() instanceof AbstractContainerScreen<?> screen
                        && !screen.getMenu().getCarried().isEmpty()),
                "clicking the slot picks the stack up");
        ctx.takeScreenshot("multispaceui-02-click-inside-panel");

        // Put the stack back so the remaining checks start from a clean inventory.
        ctx.getInput().pressMouse(LEFT_BUTTON);
        ctx.waitTicks(5);
    }

    private void testClickOutsideKeepsScreen(ClientGameTestContext ctx) {
        Screen before = read(ctx, mc -> mc.gui.screen());
        if (before == null) {
            check(false, "the container screen is open before the outside click test");
            return;
        }
        String carriedBefore = describeCarried(ctx);

        moveCursorTo(ctx, 8.0, 8.0);
        check(read(ctx, mc -> SpatialInput.shouldDropClick(mc.gui.screen())),
                "a click far outside every panel is dropped");
        ctx.getInput().scroll(1.0);
        ctx.waitTicks(2);
        check(read(ctx, mc -> SpatialInput.shouldDropClick(mc.gui.screen())),
                "scroll far outside every panel is dropped as well");
        ctx.getInput().pressMouse(LEFT_BUTTON);
        ctx.waitTicks(5);

        check(read(ctx, mc -> mc.gui.screen()) == before,
                "the very same container screen survives a click outside the panel");
        check(read(ctx, mc -> WindowManager.get().hasWindows()),
                "the window is still open after a click outside the panel");
        check(carriedBefore.equals(describeCarried(ctx)),
                "the click outside neither picked up nor dropped anything (" + carriedBefore + ")");
        ctx.takeScreenshot("multispaceui-03-click-outside-panel");
    }

    /** Carried stack of the open container, as a comparable string. */
    private static String describeCarried(ClientGameTestContext ctx) {
        return read(ctx, mc -> {
            if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
                return "no-container-screen";
            }
            ItemStack carried = screen.getMenu().getCarried();
            return carried.isEmpty() ? "empty" : carried.getItem() + "x" + carried.getCount();
        });
    }

    private void testSecondWindowAndPriority(ClientGameTestContext ctx, TestSingleplayerContext world) {
        // 1. Fold the inventory screen away: its window must stay open in the background.
        ctx.getInput().pressKey(options -> options.keyInventory);
        ctx.waitFor(mc -> mc.gui.screen() == null);
        ctx.waitTicks(5);
        SpatialWindow background = read(ctx, mc -> WindowManager.get().active());
        check(background != null, "the inventory window stays open in the background");
        if (background == null) {
            return;
        }

        // 2. Put a chest two blocks in front of the player and open it with a right click.
        BlockPos chest = world.getServer().<BlockPos, RuntimeException>computeOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            float yaw = (float) Math.toRadians(player.getYRot());
            BlockPos pos = BlockPos.containing(player.getX() - Math.sin(yaw) * 2.0,
                    player.getY(),
                    player.getZ() + Math.cos(yaw) * 2.0);
            server.overworld().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
            if (server.overworld().getBlockEntity(pos) instanceof ChestBlockEntity blockEntity) {
                blockEntity.clearContent();
                blockEntity.setItem(0, new ItemStack(Items.APPLE, 3));
            }
            return pos;
        });

        ctx.getInput().lookAt(chest);
        ctx.waitTicks(5);
        ctx.getInput().pressMouse(RIGHT_BUTTON);
        ctx.waitFor(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?>);
        ctx.waitTicks(SETTLE_TICKS);

        check(read(ctx, mc -> WindowManager.get().windows().size()) == 2,
                "using the chest opens a second window");
        SpatialWindow foreground = read(ctx, mc -> WindowManager.get().active());
        check(foreground != null && foreground != background, "the chest becomes the active window");
        if (foreground == null) {
            return;
        }

        // 3. Pointer on the centre: cursor and crosshair then agree on the focused window.
        double[] centre = read(ctx, mc -> new double[]{
                SpatialWindowRenderer.instance.frameWidth() * 0.5,
                SpatialWindowRenderer.instance.frameHeight() * 0.5});
        moveCursorTo(ctx, centre[0], centre[1]);
        ctx.waitTicks(5);

        check(read(ctx, mc -> foreground.isFocused()), "the crosshair focuses the new window");
        check(read(ctx, mc -> !background.isFocused()), "the background window loses the focus");

        float foregroundScale = read(ctx, mc -> foreground.targetScale());
        float backgroundScale = read(ctx, mc -> background.targetScale());
        float foregroundOpacity = read(ctx, mc -> foreground.targetOpacity());
        float backgroundOpacity = read(ctx, mc -> background.targetOpacity());
        check(backgroundScale < foregroundScale - 0.05f,
                "the background window is smaller than the focused one ("
                        + backgroundScale + " < " + foregroundScale + ")");
        check(backgroundOpacity < foregroundOpacity,
                "the background window is more transparent than the focused one ("
                        + backgroundOpacity + " < " + foregroundOpacity + ")");
        check(read(ctx, mc -> background.isVisible()), "the background window is still visible");
        check(!read(ctx, mc -> background.isInteractionEnabled()),
                "clicks are never routed to a window that is not focused");
        ctx.takeScreenshot("multispaceui-04-two-windows");
    }

    private void testCloseOneWindowKeepsTheOther(ClientGameTestContext ctx) {
        SpatialWindow closing = read(ctx, mc -> WindowManager.get().active());
        onClient(ctx, mc -> WindowManager.get().closeActive());
        ctx.waitTicks(SETTLE_TICKS);

        int remaining = read(ctx, mc -> WindowManager.get().windows().size());
        check(remaining == 1, "closing one window keeps the other one open (" + remaining + " left)");
        SpatialWindow next = read(ctx, mc -> WindowManager.get().active());
        check(next != null && next != closing, "the remaining window takes over as the active one");
        check(read(ctx, mc -> mc.gui.screen() != null),
                "the remaining window is installed as the screen the game talks to");
        ctx.takeScreenshot("multispaceui-05-one-window-left");
    }

    /**
     * Tests 13 and 14: a background window standing further away than {@code maxDistance}
     * shrinks, fades and finally disappears, while the window the player looks at is never
     * taken away by that rule - focus wins over distance.
     */
    private void testDistanceFalloff(ClientGameTestContext ctx, TestSingleplayerContext world) {
        SpatialWindow lookedAt = read(ctx, mc -> WindowManager.get().active());
        SpatialWindow background = read(ctx, mc -> WindowManager.get().windows().stream()
                .filter(window -> window != lookedAt)
                .findFirst()
                .orElse(null));
        if (lookedAt == null || background == null) {
            check(false, "both windows are open before the distance test");
            return;
        }

        MultiSpaceConfig cfg = MultiSpaceConfig.get();
        boolean follow = cfg.panelsFollowPlayer;
        double[] origin = read(ctx, mc -> new double[]{
                mc.player.getX(), mc.player.getY(), mc.player.getZ()});
        try {
            // Freeze both panels where they stand, then walk away from them.
            onClient(ctx, mc -> MultiSpaceConfig.get().panelsFollowPlayer = false);
            world.getServer().<RuntimeException>runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.teleportTo(player.getX() + 14.0, player.getY(), player.getZ());
            });
            world.getConnection().waitForClientboundPackets();
            ctx.waitTicks(SETTLE_TICKS);

            float fadedOpacity = read(ctx, mc -> background.targetOpacity());
            float shrunkScale = read(ctx, mc -> background.targetScale());
            check(!read(ctx, mc -> background.isVisible()),
                    "a background window past maxDistance is gone (opacity " + fadedOpacity + ")");
            check(fadedOpacity <= 0.01f,
                    "a background window past maxDistance fades out (" + fadedOpacity + ")");
            check(shrunkScale <= cfg.minScale + 1.0E-3f,
                    "a background window past maxDistance shrinks to its minimum (" + shrunkScale + ")");
            check(read(ctx, mc -> lookedAt.isVisible()),
                    "the window being looked at is never hidden by the distance rule");
            float keptOpacity = read(ctx, mc -> lookedAt.targetOpacity());
            check(keptOpacity >= cfg.maxOpacity * 0.5f,
                    "and it keeps a readable opacity (" + keptOpacity + ")");
        } finally {
            world.getServer().<RuntimeException>runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.teleportTo(origin[0], origin[1], origin[2]);
            });
            onClient(ctx, mc -> MultiSpaceConfig.get().panelsFollowPlayer = follow);
            world.getConnection().waitForClientboundPackets();
            ctx.waitTicks(SETTLE_TICKS);
        }
    }

    /**
     * Tests 15 and 16: the toggle binding switches the mod off and folds every window, and a
     * configuration edit is written to disk and read back.
     */
    private void testToggleKeybindAndConfig(ClientGameTestContext ctx) {
        // Key bindings only fire while no screen is open: fold the last window into the
        // background first, exactly like a player leaving the inventory.
        ctx.getInput().pressKey(options -> options.keyInventory);
        ctx.waitFor(mc -> mc.gui.screen() == null);
        ctx.waitTicks(5);
        check(read(ctx, mc -> WindowManager.get().hasWindows()),
                "a window stays open in the background once the screen is folded away");

        // The bindings ship unbound. Give the toggle one exactly like the Controls screen
        // does: setKey() only writes the field, while KeyboardHandler looks the pressed key
        // up in a static key -> binding index that resetMapping() rebuilds.
        KeyMapping toggle = read(ctx, mc -> KeyMapping.get("key.multispaceui.toggle"));
        check(toggle != null, "the toggle binding is registered in Controls");
        if (toggle == null) {
            return;
        }
        onClient(ctx, mc -> {
            toggle.setKey(InputConstants.Type.KEYBOARD.getOrCreate(TOGGLE_KEY));
            KeyMapping.resetMapping();
        });

        ctx.getInput().pressKey(TOGGLE_KEY);
        ctx.waitTicks(5);
        check(!MultiSpaceUI.isEnabled(), "the toggle keybind switches the mod off");
        check(!read(ctx, mc -> WindowManager.get().hasWindows()),
                "switching the mod off folds every window");

        MultiSpaceConfig.load();
        check(!MultiSpaceConfig.get().enabled,
                "the switch is persisted in multispaceui.json and read back");

        ctx.getInput().pressKey(TOGGLE_KEY);
        ctx.waitTicks(5);
        check(MultiSpaceUI.isEnabled(), "the toggle keybind switches the mod back on");

        float original = MultiSpaceConfig.get().panelDistance;
        MultiSpaceConfig.get().panelDistance = 2.5f;
        MultiSpaceConfig.save();
        MultiSpaceConfig.load();
        check(Math.abs(MultiSpaceConfig.get().panelDistance - 2.5f) < 1.0E-3f,
                "a configuration edit is written to disk and read back ("
                        + MultiSpaceConfig.get().panelDistance + ")");
        MultiSpaceConfig.get().panelDistance = original;
        MultiSpaceConfig.save();

        // Leave the binding the way we found it.
        onClient(ctx, mc -> {
            toggle.setKey(InputConstants.UNKNOWN);
            KeyMapping.resetMapping();
        });
    }

    // ----------------------------------------------------------------------------------
    // Pointer helpers
    // ----------------------------------------------------------------------------------

    private double[] pixelForGui(ClientGameTestContext ctx, SpatialWindow window,
                                 double targetX, double targetY) {
        return read(ctx, mc -> pixelForGuiNow(window, targetX, targetY));
    }

    /**
     * Same search, but evaluated in the caller's own frame: it returns the pixel that shows
     * {@code (targetX, targetY)} <i>right now</i>, next to whatever else that frame reads.
     *
     * <p>The Spatial GUI base glides its camera with the pointer, so a panel never quite
     * stops moving while the pointer travels. Reading the search and the input mapping in
     * one frame is what keeps the two on the same geometry.</p>
     */
    private static double[] pixelForGuiNow(SpatialWindow window, double targetX, double targetY) {
        SpatialWindowRenderer renderer = SpatialWindowRenderer.instance;
        QuadBasis basis = renderer.basisOf(window);
        Matrix4fc projection = renderer.projectionMatrix();
        int width = renderer.frameWidth();
        int height = renderer.frameHeight();
        if (basis == null || projection == null || width <= 0 || height <= 0) {
            return null;
        }
        float[] corners = PanelGeometry.projectCorners(basis, projection, width, height);
        if (corners == null) {
            return null;
        }
        double minX = width;
        double maxX = 0;
        double minY = height;
        double maxY = 0;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, corners[i * 2]);
            maxX = Math.max(maxX, corners[i * 2]);
            minY = Math.min(minY, corners[i * 2 + 1]);
            maxY = Math.max(maxY, corners[i * 2 + 1]);
        }
        minX = Math.max(0, Math.min(width - 1, minX));
        maxX = Math.max(0, Math.min(width - 1, maxX));
        minY = Math.max(0, Math.min(height - 1, minY));
        maxY = Math.max(0, Math.min(height - 1, maxY));
        if (maxX <= minX || maxY <= minY) {
            return null;
        }

        double step = Math.max(4.0, Math.max(maxX - minX, maxY - minY) / 40.0);
        double bestX = -1;
        double bestY = -1;
        double bestError = Double.MAX_VALUE;
        for (int pass = 0; pass < 2; pass++) {
            double fromX = pass == 0 ? minX : bestX - step * 2.0;
            double toX = pass == 0 ? maxX : bestX + step * 2.0;
            double fromY = pass == 0 ? minY : bestY - step * 2.0;
            double toY = pass == 0 ? maxY : bestY + step * 2.0;
            if (pass == 1) {
                bestError = Double.MAX_VALUE;
            }
            for (double y = Math.max(0, fromY); y <= Math.min(height - 1, toY); y += step) {
                for (double x = Math.max(0, fromX); x <= Math.min(width - 1, toX); x += step) {
                    double[] gui = guiAt(basis, projection, width, height, window, x, y);
                    if (gui == null) {
                        continue;
                    }
                    double error = Math.abs(gui[0] - targetX) + Math.abs(gui[1] - targetY);
                    if (error < bestError) {
                        bestError = error;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
            if (bestX < 0) {
                return null;
            }
            step /= 8.0;
        }
        if (bestError > 8.0) {
            // Nothing on the panel maps close enough to the asked point: better no pixel at
            // all than a pixel that would make the following clicks land somewhere else.
            return null;
        }
        return new double[]{bestX, bestY};
    }

    /** Where the pointer sits after pointing at a GUI point, once the panel stopped sliding. */
    private double[] settleCursorOn(ClientGameTestContext ctx, SpatialWindow window,
                                    double guiX, double guiY) {
        double[] pixel = pixelForGui(ctx, window, guiX, guiY);
        for (int attempt = 0; attempt < 6 && pixel != null; attempt++) {
            moveCursorTo(ctx, pixel[0], pixel[1]);
            double[] again = pixelForGui(ctx, window, guiX, guiY);
            if (again == null) {
                break;
            }
            if (Math.abs(again[0] - pixel[0]) < 0.5 && Math.abs(again[1] - pixel[1]) < 0.5) {
                return pixel;
            }
            // The pointer moved the camera, so the panel slid away: follow it.
            pixel = again;
        }
        return pixel;
    }

    private record SlotProbe(double foundX, double foundY, double mappedX, double mappedY,
                             double cursorX, double cursorY) {}

    /** In one frame: the pixel showing a GUI point, and where the pointer maps to. */
    private SlotProbe probeSlot(ClientGameTestContext ctx, SpatialWindow window,
                                double guiX, double guiY) {
        return read(ctx, mc -> {
            double[] found = pixelForGuiNow(window, guiX, guiY);
            if (found == null) {
                return null;
            }
            return new SlotProbe(found[0], found[1], SpatialInput.mapX(0.0), SpatialInput.mapY(0.0),
                    SpatialWindowRenderer.instance.targeting().cursorX(),
                    SpatialWindowRenderer.instance.targeting().cursorY());
        });
    }

    /** GUI coordinates of the panel content under the framebuffer pixel (px, py). */
    private static double[] guiAt(QuadBasis basis, Matrix4fc projection, int width, int height,
                                  SpatialWindow window, double px, double py) {
        Vector3f ray = PanelGeometry.viewRay(projection, px, py, width, height);
        Vector3f hit = PanelGeometry.intersect(ray, basis);
        if (hit == null) {
            return null;
        }
        return new double[]{hit.x() * window.innerWidth(), (1.0 - hit.y()) * window.innerHeight()};
    }

    /**
     * Puts the pointer on the framebuffer pixel {@code (targetX, targetY)}.
     *
     * <p>The gametest API moves the cursor in window units while the mod reads framebuffer
     * pixels, so the position is verified frame by frame and corrected with the measured
     * ratio instead of assuming both units coincide.</p>
     */
    private void moveCursorTo(ClientGameTestContext ctx, double targetX, double targetY) {
        double[] estimate = read(ctx, mc -> {
            var window = mc.getWindow();
            double ratioX = window.getScreenWidth() / (double) Math.max(1, window.getWidth());
            double ratioY = window.getScreenHeight() / (double) Math.max(1, window.getHeight());
            return new double[]{targetX * ratioX, targetY * ratioY};
        });

        for (int attempt = 0; attempt < 3; attempt++) {
            double tryX = estimate[0];
            double tryY = estimate[1];
            ctx.getInput().setCursorPos(tryX, tryY);
            ctx.waitTicks(3);
            double[] actual = read(ctx, mc -> new double[]{
                    SpatialWindowRenderer.instance.targeting().cursorX(),
                    SpatialWindowRenderer.instance.targeting().cursorY()});
            if (Math.abs(actual[0] - targetX) <= 2.0 && Math.abs(actual[1] - targetY) <= 2.0) {
                return;
            }
            double ratioX = Math.abs(actual[0]) > 0.5 ? targetX / actual[0] : 1.0;
            double ratioY = Math.abs(actual[1]) > 0.5 ? targetY / actual[1] : 1.0;
            estimate = new double[]{tryX * ratioX, tryY * ratioY};
        }
        MultiSpaceUI.LOGGER.warn("[gametest] pointer did not reach ({}, {})", targetX, targetY);
    }

    // ----------------------------------------------------------------------------------
    // Plumbing
    // ----------------------------------------------------------------------------------

    private void check(boolean condition, String label) {
        checks++;
        if (condition) {
            MultiSpaceUI.LOGGER.info("[gametest] PASS {}", label);
        } else {
            failures.add(label);
            MultiSpaceUI.LOGGER.error("[gametest] FAIL {}", label);
        }
    }

    /** Runs {@code action} on the render thread and waits for it. */
    private static void onClient(ClientGameTestContext ctx, Consumer<Minecraft> action) {
        ctx.<RuntimeException>runOnClient(action::accept);
    }

    /** Reads client state on the render thread, so the values are never read across frames. */
    private static <T> T read(ClientGameTestContext ctx, Function<Minecraft, T> supplier) {
        return ctx.<T, RuntimeException>computeOnClient(supplier::apply);
    }
}
