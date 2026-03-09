package com.github.leopoko.village_shop_system.neoforge;

import com.github.leopoko.village_shop_system.Village_shop_system;
import net.neoforged.fml.common.Mod;

@Mod(Village_shop_system.MOD_ID)
public final class Village_shop_systemNeoForge {
    public Village_shop_systemNeoForge() {
        // Run our common setup.
        Village_shop_system.init();
    }
}
