package com.github.leopoko.village_shop_system.registry;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.menu.PurchaseShelfMenu;
import com.github.leopoko.village_shop_system.menu.RegisterMenu;
import com.github.leopoko.village_shop_system.menu.SellingShelfMenu;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Village_shop_system.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<SellingShelfMenu>> SELLING_SHELF_MENU =
            MENUS.register("selling_shelf", () -> new MenuType<>(SellingShelfMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<SellingShelfMenu>> SELLING_SHELF_B_MENU =
            MENUS.register("selling_shelf_b", () -> new MenuType<>(SellingShelfMenu::createForShelfB, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<PurchaseShelfMenu>> PURCHASE_SHELF_MENU =
            MENUS.register("purchase_shelf", () -> new MenuType<>(PurchaseShelfMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistrySupplier<MenuType<RegisterMenu>> REGISTER_MENU =
            MENUS.register("register_block", () -> new MenuType<>(RegisterMenu::new, FeatureFlags.VANILLA_SET));

    public static void register() {
        MENUS.register();
    }

    private ModMenuTypes() {}
}
