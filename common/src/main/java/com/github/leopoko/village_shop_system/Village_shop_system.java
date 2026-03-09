package com.github.leopoko.village_shop_system;

import com.github.leopoko.village_shop_system.registry.ModBlockEntities;
import com.github.leopoko.village_shop_system.registry.ModBlocks;
import com.github.leopoko.village_shop_system.registry.ModCreativeTabs;
import com.github.leopoko.village_shop_system.registry.ModItems;
import com.github.leopoko.village_shop_system.registry.ModMenuTypes;
import com.github.leopoko.village_shop_system.network.ModPackets;

public final class Village_shop_system {
    public static final String MOD_ID = "village_shop_system";

    public static void init() {
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        ModCreativeTabs.register();
        ModPackets.register();
    }
}
