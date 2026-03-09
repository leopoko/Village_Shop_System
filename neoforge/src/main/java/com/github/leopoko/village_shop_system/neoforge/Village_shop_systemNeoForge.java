package com.github.leopoko.village_shop_system.neoforge;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.config.ModConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(Village_shop_system.MOD_ID)
public final class Village_shop_systemNeoForge {
    public Village_shop_systemNeoForge(ModContainer container) {
        // Run our common setup.
        Village_shop_system.init();

        // Register config screen factory for NeoForge mod list
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> ModConfigScreen.create(parent));
    }
}
