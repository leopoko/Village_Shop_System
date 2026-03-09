package com.github.leopoko.village_shop_system.block;

import com.github.leopoko.village_shop_system.blockentity.PurchaseShelfBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PurchaseShelfBlock extends AbstractShopBlock {
    public static final MapCodec<PurchaseShelfBlock> CODEC = simpleCodec(PurchaseShelfBlock::new);

    public PurchaseShelfBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PurchaseShelfBlockEntity(pos, state);
    }
}
