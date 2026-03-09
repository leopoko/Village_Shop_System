package com.github.leopoko.village_shop_system;

import com.github.leopoko.village_shop_system.item.ChairSettingStick;
import com.github.leopoko.village_shop_system.registry.ModMenuTypes;
import com.github.leopoko.village_shop_system.screen.PurchaseShelfScreen;
import com.github.leopoko.village_shop_system.screen.RegisterScreen;
import com.github.leopoko.village_shop_system.screen.SellingShelfScreen;
import com.github.leopoko.village_shop_system.screen.ShopGroupSettingScreen;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.Minecraft;

/**
 * Common client-side initialization. Called from both Fabric and NeoForge client entry points.
 */
public final class Village_shop_systemClient {

    /**
     * Register screen factories for all menu types.
     * On Fabric: called during ClientModInitializer via Architectury MenuRegistry.
     * On NeoForge: NOT called — NeoForge uses RegisterMenuScreensEvent instead.
     */
    public static void registerScreens() {
        MenuRegistry.registerScreenFactory(ModMenuTypes.SELLING_SHELF_MENU.get(), SellingShelfScreen::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PURCHASE_SHELF_MENU.get(), PurchaseShelfScreen::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.REGISTER_MENU.get(), RegisterScreen::new);
    }

    /**
     * Register client callbacks (chair setting stick screen opener, etc.).
     * Safe to call from any lifecycle point after Minecraft instance is available.
     */
    public static void initCallbacks() {
        ChairSettingStick.openSettingsScreen = currentGroup ->
                Minecraft.getInstance().setScreen(new ShopGroupSettingScreen(currentGroup));
    }

    /**
     * Full init for Fabric (screens + callbacks in one call).
     */
    public static void init() {
        registerScreens();
        initCallbacks();
    }

    private Village_shop_systemClient() {}
}
