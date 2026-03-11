package com.github.leopoko.village_shop_system.block;

import com.github.leopoko.village_shop_system.blockentity.SellingShelfBBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SellingShelfBBlock extends AbstractShopBlock {

    public SellingShelfBBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SellingShelfBBlockEntity(pos, state);
    }
}
