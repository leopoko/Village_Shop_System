package com.github.leopoko.village_shop_system.neoforge;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.Village_shop_systemClient;
import com.github.leopoko.village_shop_system.entity.SeatEntity;
import com.github.leopoko.village_shop_system.registry.ModEntityTypes;
import com.github.leopoko.village_shop_system.registry.ModMenuTypes;
import com.github.leopoko.village_shop_system.screen.PurchaseShelfScreen;
import com.github.leopoko.village_shop_system.screen.RegisterScreen;
import com.github.leopoko.village_shop_system.screen.SellingShelfScreen;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Village_shop_system.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Village_shop_systemNeoForgeClient {

    /**
     * Register menu screen factories via NeoForge's native event.
     * This fires BEFORE FMLClientSetupEvent, ensuring screens are available.
     */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SELLING_SHELF_MENU.get(), SellingShelfScreen::new);
        event.register(ModMenuTypes.SELLING_SHELF_B_MENU.get(), SellingShelfScreen::new);
        event.register(ModMenuTypes.PURCHASE_SHELF_MENU.get(), PurchaseShelfScreen::new);
        event.register(ModMenuTypes.REGISTER_MENU.get(), RegisterScreen::new);
    }

    /**
     * Register entity renderers via NeoForge's native event.
     * Architectury's EntityRendererRegistry is too late when called from FMLClientSetupEvent.
     */
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.SEAT.get(), NoopRenderer::new);
    }

    /**
     * Other client setup (callbacks, etc.).
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(Village_shop_systemClient::initCallbacks);
    }
}
