package com.github.leopoko.village_shop_system.block;

import com.github.leopoko.village_shop_system.blockentity.SellingShelfBBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SellingShelfBBlock extends AbstractShopBlock {
    public static final MapCodec<SellingShelfBBlock> CODEC = simpleCodec(SellingShelfBBlock::new);

    public SellingShelfBBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SellingShelfBBlockEntity(pos, state);
    }
}
