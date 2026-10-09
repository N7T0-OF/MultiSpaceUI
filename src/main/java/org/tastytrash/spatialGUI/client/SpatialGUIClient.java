package org.tastytrash.spatialGUI.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import org.tastytrash.spatialGUI.compat.VisorCompat;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import org.tastytrash.spatialGUI.SpatialGUI;
//? if fabric {
 import net.fabricmc.api.ClientModInitializer;
 import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
//? } else if neoforge {
/*import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
*///? }

//? if fabric {
 public class SpatialGUIClient implements ClientModInitializer {
//? } else if neoforge {
/*@Mod(value = SpatialGUI.MOD_ID, dist = Dist.CLIENT)
public class SpatialGUIClient {
*///? }

    private static SpatialGUIRenderer renderer;
    private static boolean effectiveFirstPersonMode = false;
    private static boolean switchedToFirstPersonDueToBlock = false;
    private static boolean wasThirdPersonCamera = false;

    //? if fabric {
    @Override
    public void onInitializeClient() {
        renderer = new SpatialGUIRenderer();
        SpatialGUIKeybinds.register();

        ScreenEvents.BEFORE_INIT.register((clientArg, screen, scaledWidth, scaledHeight) -> {
            if (SpatialGUIClient.shouldHookScreen(screen)) {
                renderer.hookScreen(screen);
            }
        });
    }
    //? } else if neoforge {
    /*public SpatialGUIClient(net.neoforged.bus.api.IEventBus modBus) {
        renderer = new SpatialGUIRenderer();

        modBus.addListener((RegisterKeyMappingsEvent e) -> SpatialGUIKeybinds.register(e));

        NeoForge.EVENT_BUS.addListener(this::onScreenInit);
    }

    private void onScreenInit(ScreenEvent.Init.Pre event) {
        var screen = event.getScreen();

        if (SpatialGUIClient.shouldHookScreen(screen)) {
            renderer.hookScreen(screen);
        }
    }
    *///?}

    public static boolean isEnabled() {
        if (VisorCompat.isActive()) {
            return false;
        }
        return SpatialGUI.config.enabled;
    }

    public static boolean shouldHideHud() {
        return isEnabled() && SpatialGUI.config.hudOptions.hideHud
                && renderer() != null && renderer().getHookedScreen() != null;
    }

    public static boolean shouldHideHotbar() {
        return isEnabled() && SpatialGUI.config.hudOptions.hideHotbar
                && renderer() != null && renderer().getHookedScreen() != null;
    }

    public static boolean shouldHideCrosshair() {
        return isEnabled() && getEffectiveFirstPersonMode()
                && !SpatialGUI.config.useCrosshairForFirstPerson
                && renderer() != null && renderer().getHookedScreen() != null;
    }

    public static boolean shouldHookScreen(Screen screen) {
        if (screen == null) return false;



        String id = screen.getClass().getName();
        if (id.contains("TitleScreen")) return false;
        if (id.contains("ReceivingLevelScreen")) return false;
        if (id.contains("LevelLoadingScreen")) return false;
        if (id.contains("ChatScreen")) return false;

        if (!id.contains("$") && !SpatialGUI.config.seenScreens.contains(id)) {
            SpatialGUI.config.seenScreens.add(id);
        }

        if (SpatialGUI.config.disabledScreens.contains(id)) return false;

        boolean categorized = true;
        boolean toggle;
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            toggle = SpatialGUI.config.inventory;
        } else if (screen instanceof AbstractContainerScreen<?>) {
            toggle = SpatialGUI.config.containers;
        } else if (screen instanceof PauseScreen) {
            toggle = SpatialGUI.config.pauseScreen;
        } else {
            categorized = false;
            toggle = false;
        }

        if (categorized) return toggle;
        if (SpatialGUI.config.enabledScreens.contains(id)) return true;
        return SpatialGUI.config.allScreens && Minecraft.getInstance().level != null;
    }

    public static SpatialGUIRenderer renderer() {
        return renderer;
    }

    public static boolean getEffectiveFirstPersonMode() {
        return effectiveFirstPersonMode;
    }

    public static void setEffectiveFirstPersonMode(boolean value) {
        effectiveFirstPersonMode = value;
    }

    public static boolean getSwitchedToFirstPersonDueToBlock() {
        return switchedToFirstPersonDueToBlock;
    }

    public static void setSwitchedToFirstPersonDueToBlock(boolean value) {
        switchedToFirstPersonDueToBlock = value;
    }

    public static boolean getWasThirdPersonCamera() {
        return wasThirdPersonCamera;
    }

    public static void setWasThirdPersonCamera(boolean value) {
        wasThirdPersonCamera = value;
    }

    public static boolean shouldUseFirstPersonMode(Screen screen) {
        if (SpatialGUI.config.autoDetectCameraMode) {
            var client = Minecraft.getInstance();
            return client.options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON;
        } else {
            boolean isInventoryScreen = screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen
                || screen.getClass().getName().contains("InventoryScreen");
            return (isInventoryScreen ? SpatialGUI.config.firstPersonModeInventory : SpatialGUI.config.firstPersonModeContainers)
                || switchedToFirstPersonDueToBlock;
        }
    }
}