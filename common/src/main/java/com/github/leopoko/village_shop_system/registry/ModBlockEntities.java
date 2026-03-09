package com.github.leopoko.village_shop_system.registry;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.blockentity.PurchaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.RegisterBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Village_shop_system.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistrySupplier<BlockEntityType<SellingShelfBlockEntity>> SELLING_SHELF =
            BLOCK_ENTITIES.register("selling_shelf", () ->
                    BlockEntityType.Builder.of(SellingShelfBlockEntity::new,
                            ModBlocks.SELLING_SHELF.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistrySupplier<BlockEntityType<SellingShelfBBlockEntity>> SELLING_SHELF_B =
            BLOCK_ENTITIES.register("selling_shelf_b", () ->
                    BlockEntityType.Builder.of(SellingShelfBBlockEntity::new,
                            ModBlocks.SELLING_SHELF_B.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistrySupplier<BlockEntityType<PurchaseShelfBlockEntity>> PURCHASE_SHELF =
            BLOCK_ENTITIES.register("purchase_shelf", () ->
                    BlockEntityType.Builder.of(PurchaseShelfBlockEntity::new,
                            ModBlocks.PURCHASE_SHELF.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistrySupplier<BlockEntityType<RegisterBlockEntity>> REGISTER_BLOCK =
            BLOCK_ENTITIES.register("register_block", () ->
                    BlockEntityType.Builder.of(RegisterBlockEntity::new,
                            ModBlocks.REGISTER_BLOCK.get()).build(null));

    public static void register() {
        BLOCK_ENTITIES.register();
    }

    private ModBlockEntities() {}
}
