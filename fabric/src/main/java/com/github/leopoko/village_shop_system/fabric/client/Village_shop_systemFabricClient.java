package com.github.leopoko.village_shop_system.fabric.client;

import com.github.leopoko.village_shop_system.Village_shop_systemClient;
import net.fabricmc.api.ClientModInitializer;

public final class Village_shop_systemFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Village_shop_systemClient.init();
    }
}
