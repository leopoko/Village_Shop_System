package com.github.leopoko.village_shop_system.fabric;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.registry.ModBlockEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.world.WorldlyContainer;

public final class Village_shop_systemFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        Village_shop_system.init();

        // Register Fabric Transfer API for industrial pipe / Create mod compatibility
        registerItemStorage();
    }

    private void registerItemStorage() {
        ItemStorage.SIDED.registerForBlockEntities(
                (blockEntity, direction) -> InventoryStorage.of((WorldlyContainer) blockEntity, direction),
                ModBlockEntities.SELLING_SHELF.get(),
                ModBlockEntities.SELLING_SHELF_B.get(),
                ModBlockEntities.PURCHASE_SHELF.get(),
                ModBlockEntities.REGISTER_BLOCK.get()
        );
    }
}
