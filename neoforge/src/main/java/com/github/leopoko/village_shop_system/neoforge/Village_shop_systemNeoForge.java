package com.github.leopoko.village_shop_system.neoforge;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.config.ModConfigScreen;
import com.github.leopoko.village_shop_system.registry.ModBlockEntities;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

@Mod(Village_shop_system.MOD_ID)
public final class Village_shop_systemNeoForge {
    public Village_shop_systemNeoForge(IEventBus modEventBus, ModContainer container) {
        // Run our common setup.
        Village_shop_system.init();

        // Register IItemHandler capability for industrial pipe / Create mod compatibility
        modEventBus.addListener(this::registerCapabilities);

        // Register config screen factory for NeoForge mod list
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> ModConfigScreen.create(parent));
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Selling Shelf A: items in (top/sides), emeralds out (bottom)
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SELLING_SHELF.get(),
                (be, dir) -> new SidedInvWrapper(be, dir != null ? dir : Direction.UP));

        // Selling Shelf B: same I/O as A, with shop group accounting
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SELLING_SHELF_B.get(),
                (be, dir) -> new SidedInvWrapper(be, dir != null ? dir : Direction.UP));

        // Purchase Shelf: emeralds in (top/sides), items out (bottom)
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.PURCHASE_SHELF.get(),
                (be, dir) -> new SidedInvWrapper(be, dir != null ? dir : Direction.UP));

        // Register: no input, emeralds out (bottom only)
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.REGISTER_BLOCK.get(),
                (be, dir) -> new SidedInvWrapper(be, dir != null ? dir : Direction.DOWN));
    }
}
