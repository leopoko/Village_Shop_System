package com.github.leopoko.village_shop_system.forge;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.blockentity.BaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.registry.ModBlockEntities;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod(Village_shop_system.MOD_ID)
public final class Village_shop_systemForge {
    public Village_shop_systemForge() {
        // Register Architectury event bus BEFORE any DeferredRegister calls
        EventBuses.registerModEventBus(Village_shop_system.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        Village_shop_system.init();

        // Register IItemHandler capability for industrial pipe / Create mod compatibility
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, this::onAttachCapabilities);

        // Register config screen factory for Forge mod list (client only)
        if (FMLEnvironment.dist.isClient()) {
            Village_shop_systemForgeClient.registerConfigScreen();
        }
    }

    private void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity be = event.getObject();
        if (be instanceof BaseShelfBlockEntity shelf) {
            event.addCapability(
                    new ResourceLocation(Village_shop_system.MOD_ID, "item_handler"),
                    new ICapabilityProvider() {
                        private LazyOptional<IItemHandler> handler = null;

                        private LazyOptional<IItemHandler> getHandler(@Nullable Direction side) {
                            if (handler == null || !handler.isPresent()) {
                                handler = LazyOptional.of(() -> new SidedInvWrapper(shelf, side != null ? side : Direction.UP));
                            }
                            return handler;
                        }

                        @Override
                        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                                return getHandler(side).cast();
                            }
                            return LazyOptional.empty();
                        }
                    }
            );
        }
    }
}
