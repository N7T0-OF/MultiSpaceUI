package dev.multispaceui.client;

import net.fabricmc.api.ClientModInitializer;

/** Client entrypoint. */
public final class MultiSpaceUIClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MultiSpaceUIHooks.bootstrap();
    }
}
