//? if fabric {
package org.tastytrash.spatialGUI.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class VulkanModCompat {
    private static Boolean isVulkanModLoaded = null;

    public static boolean isVulkanModLoaded() {
        if (isVulkanModLoaded != null) {
            return isVulkanModLoaded;
        }

        isVulkanModLoaded = FabricLoader.getInstance().isModLoaded("vulkanmod");
        return isVulkanModLoaded;
    }
}
//?} else if neoforge {
/*package org.tastytrash.spatialGUI.compat;

public final class VulkanModCompat {
    public static boolean isVulkanModLoaded() {
        return false;
    }
}
*///?}
