package org.tastytrash.spatialGUI.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.tastytrash.spatialGUI.SpatialGUI;

//? if fabric {
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//?} else {
/*import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?}
//? if fabric && >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
 //?} else if fabric {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}

public class SpatialGUIKeybinds {
    private static KeyMapping openConfig;
    private static boolean registered = false;

    //? if fabric && >1.21.1 {
    public static final KeyMapping.Category SPATIAL_GUI_CATEGORY =
            KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("spatial-gui", "main_category"));
    //?} else if neoforge && >1.21.1 {
    /*public static final KeyMapping.Category SPATIAL_GUI_CATEGORY =
            new KeyMapping.Category(net.minecraft.resources.Identifier.fromNamespaceAndPath("spatial-gui", "main_category"));
    *///?}

    private static KeyMapping createKey() {
        //? if >1.21.1 {
        return new KeyMapping(
                "key.spatial-gui.open_config",
                InputConstants.UNKNOWN.getType(),
                InputConstants.UNKNOWN.getValue(),
                SPATIAL_GUI_CATEGORY
        );
        //?} else {
        /*return new KeyMapping(
                "key.spatial-gui.open_config",
                InputConstants.UNKNOWN.getType(),
                InputConstants.UNKNOWN.getValue(),
                "key.category.spatial-gui.main_category"
        );
        *///?}
    }

    private static void tick(Minecraft mc) {
        while (openConfig.consumeClick()) {
            Screen screen = me.shedaniel.autoconfig.AutoConfigClient.getConfigScreen(SpatialGUIConfig.class, null).get();
            //? if >=26.1 {
            mc.setScreenAndShow(screen);
            //?} else {
            /*mc.setScreen(screen);
             *///?}
        }
    }

    //? if fabric && >=26.1.2 {
    public static void register() {
        openConfig = KeyMappingHelper.registerKeyMapping(createKey());
        ClientTickEvents.END_CLIENT_TICK.register(SpatialGUIKeybinds::tick);
    }
    //?} else if fabric {
    /*public static void register() {
        openConfig = KeyBindingHelper.registerKeyBinding(createKey());
        ClientTickEvents.END_CLIENT_TICK.register(SpatialGUIKeybinds::tick);
    }
    *///?} else {
    /*public static void register(RegisterKeyMappingsEvent event) {
        if (registered) return;
        registered = true;
        //? if >1.21.1 {
        /^event.registerCategory(SPATIAL_GUI_CATEGORY);
        ^///?}
        openConfig = createKey();
        event.register(openConfig);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick(Minecraft.getInstance()));
    }
    *///?}
}