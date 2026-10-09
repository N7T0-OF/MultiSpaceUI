//? if fabric {
package org.tastytrash.spatialGUI.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

public class SpatialGUIModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> me.shedaniel.autoconfig.AutoConfigClient.getConfigScreen(SpatialGUIConfig.class, parent).get();
    }
}
//? }