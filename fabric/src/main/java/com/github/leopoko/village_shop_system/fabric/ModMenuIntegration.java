package com.github.leopoko.village_shop_system.fabric;

import com.github.leopoko.village_shop_system.config.ModConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu integration for Fabric.
 * Provides a config button in the mod list that opens the Cloth Config screen.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModConfigScreen::create;
    }
}
