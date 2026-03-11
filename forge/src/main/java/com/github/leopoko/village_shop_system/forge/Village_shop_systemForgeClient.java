package com.github.leopoko.village_shop_system.forge;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.Village_shop_systemClient;
import com.github.leopoko.village_shop_system.config.ModConfigScreen;
import com.github.leopoko.village_shop_system.registry.ModEntityTypes;
import com.github.leopoko.village_shop_system.registry.ModMenuTypes;
import com.github.leopoko.village_shop_system.screen.PurchaseShelfScreen;
import com.github.leopoko.village_shop_system.screen.RegisterScreen;
import com.github.leopoko.village_shop_system.screen.SellingShelfScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Village_shop_system.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Village_shop_systemForgeClient {

    /**
     * Register entity renderers via Forge's native event.
     */
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.SEAT.get(), NoopRenderer::new);
    }

    /**
     * Client setup: register menu screens and callbacks.
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.SELLING_SHELF_MENU.get(), SellingShelfScreen::new);
            MenuScreens.register(ModMenuTypes.SELLING_SHELF_B_MENU.get(), SellingShelfScreen::new);
            MenuScreens.register(ModMenuTypes.PURCHASE_SHELF_MENU.get(), PurchaseShelfScreen::new);
            MenuScreens.register(ModMenuTypes.REGISTER_MENU.get(), RegisterScreen::new);
            Village_shop_systemClient.initCallbacks();
        });
    }

    /**
     * Register config screen factory. Called from the main mod class with a dist check.
     */
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> ModConfigScreen.create(parent)));
    }
}
