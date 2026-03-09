package com.github.leopoko.village_shop_system.registry;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.item.ChairSettingStick;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Village_shop_system.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> SELLING_SHELF_ITEM = ITEMS.register("selling_shelf",
            () -> new BlockItem(ModBlocks.SELLING_SHELF.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SELLING_SHELF_B_ITEM = ITEMS.register("selling_shelf_b",
            () -> new BlockItem(ModBlocks.SELLING_SHELF_B.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PURCHASE_SHELF_ITEM = ITEMS.register("purchase_shelf",
            () -> new BlockItem(ModBlocks.PURCHASE_SHELF.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> REGISTER_BLOCK_ITEM = ITEMS.register("register_block",
            () -> new BlockItem(ModBlocks.REGISTER_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CHAIR_SETTING_STICK = ITEMS.register("chair_setting_stick",
            () -> new ChairSettingStick(new Item.Properties().stacksTo(1)));

    public static void register() {
        ITEMS.register();
    }

    private ModItems() {}
}
