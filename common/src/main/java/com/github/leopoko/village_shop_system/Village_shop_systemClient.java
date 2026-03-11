package com.github.leopoko.village_shop_system;

import com.github.leopoko.village_shop_system.block.AbstractShopBlock;
import com.github.leopoko.village_shop_system.item.ChairSettingStick;
import com.github.leopoko.village_shop_system.registry.ModEntityTypes;
import com.github.leopoko.village_shop_system.registry.ModMenuTypes;
import com.github.leopoko.village_shop_system.screen.PurchaseShelfScreen;
import com.github.leopoko.village_shop_system.screen.RegisterScreen;
import com.github.leopoko.village_shop_system.screen.SellingShelfScreen;
import com.github.leopoko.village_shop_system.screen.ShopGroupSettingScreen;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.Minecraft;
import com.github.leopoko.village_shop_system.entity.SeatEntityRenderer;

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
        MenuRegistry.registerScreenFactory(ModMenuTypes.SELLING_SHELF_B_MENU.get(), SellingShelfScreen::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.PURCHASE_SHELF_MENU.get(), PurchaseShelfScreen::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.REGISTER_MENU.get(), RegisterScreen::new);
    }

    /**
     * Register client callbacks (chair setting stick screen opener, block group screen, etc.).
     * Safe to call from any lifecycle point after Minecraft instance is available.
     */
    public static void initCallbacks() {
        // Chair setting stick: opens group setting screen (stick mode)
        ChairSettingStick.openSettingsScreen = currentGroup ->
                Minecraft.getInstance().setScreen(new ShopGroupSettingScreen(currentGroup));

        // Block group setting: sneak+right-click SellingShelfB/Register (block mode)
        AbstractShopBlock.openBlockGroupScreen = (currentGroup, blockPos) ->
                Minecraft.getInstance().setScreen(new ShopGroupSettingScreen(currentGroup, blockPos));
    }

    /**
     * Register entity renderers. Called early from both loaders.
     */
    public static void registerEntityRenderers() {
        EntityRendererRegistry.register(ModEntityTypes.SEAT, SeatEntityRenderer::new);
    }

    /**
     * Full init for Fabric (screens + callbacks in one call).
     */
    public static void init() {
        registerScreens();
        registerEntityRenderers();
        initCallbacks();
    }

    private Village_shop_systemClient() {}
}
