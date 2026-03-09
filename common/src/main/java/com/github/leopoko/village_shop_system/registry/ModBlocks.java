package com.github.leopoko.village_shop_system.registry;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.block.PurchaseShelfBlock;
import com.github.leopoko.village_shop_system.block.RegisterBlock;
import com.github.leopoko.village_shop_system.block.SellingShelfBBlock;
import com.github.leopoko.village_shop_system.block.SellingShelfBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Village_shop_system.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> SELLING_SHELF = BLOCKS.register("selling_shelf",
            () -> new SellingShelfBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));

    public static final RegistrySupplier<Block> SELLING_SHELF_B = BLOCKS.register("selling_shelf_b",
            () -> new SellingShelfBBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));

    public static final RegistrySupplier<Block> PURCHASE_SHELF = BLOCKS.register("purchase_shelf",
            () -> new PurchaseShelfBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));

    public static final RegistrySupplier<Block> REGISTER_BLOCK = BLOCKS.register("register_block",
            () -> new RegisterBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.STONE)));

    public static void register() {
        BLOCKS.register();
    }

    private ModBlocks() {}
}
