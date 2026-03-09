package com.github.leopoko.village_shop_system.block;

import com.github.leopoko.village_shop_system.blockentity.SellingShelfBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SellingShelfBlock extends AbstractShopBlock {
    public static final MapCodec<SellingShelfBlock> CODEC = simpleCodec(SellingShelfBlock::new);

    public SellingShelfBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SellingShelfBlockEntity(pos, state);
    }
}
