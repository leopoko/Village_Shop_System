package com.github.leopoko.village_shop_system.fabric;

import com.github.leopoko.village_shop_system.Village_shop_system;
import net.fabricmc.api.ModInitializer;

public final class Village_shop_systemFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        Village_shop_system.init();
    }
}
