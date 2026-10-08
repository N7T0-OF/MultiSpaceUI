package dev.multispaceui;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared entrypoint: logger, mod constants and configuration bootstrap. */
public final class MultiSpaceUI implements ModInitializer {
    public static final String MOD_ID = "multispaceui";
    public static final Logger LOGGER = LoggerFactory.getLogger("MultiSpaceUI");

    @Override
    public void onInitialize() {
        LOGGER.info("MultiSpaceUI initialized");
    }

    /**
     * One switch every mixin consults before touching vanilla: the mod is on
     * when the client config says so. Reading it is cheap (a field), so it is
     * safe to call from render and input paths.
     */
    public static boolean isEnabled() {
        return dev.multispaceui.config.MultiSpaceConfig.get().enabled;
    }
}
