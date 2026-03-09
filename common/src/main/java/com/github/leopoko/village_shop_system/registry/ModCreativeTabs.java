package com.github.leopoko.village_shop_system.registry;

import com.github.leopoko.village_shop_system.Village_shop_system;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Village_shop_system.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> VILLAGE_SHOP_TAB = TABS.register("village_shop_tab",
            () -> CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("itemGroup.village_shop_system"));
                builder.icon(() -> new ItemStack(ModItems.SELLING_SHELF_ITEM.get()));
                builder.displayItems((params, output) -> {
                    output.accept(ModItems.SELLING_SHELF_ITEM.get());
                    output.accept(ModItems.SELLING_SHELF_B_ITEM.get());
                    output.accept(ModItems.PURCHASE_SHELF_ITEM.get());
                    output.accept(ModItems.REGISTER_BLOCK_ITEM.get());
                    output.accept(ModItems.CHAIR_SETTING_STICK.get());
                });
            }));

    public static void register() {
        TABS.register();
    }

    private ModCreativeTabs() {}
}
