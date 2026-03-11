package com.github.leopoko.village_shop_system.block;

import com.github.leopoko.village_shop_system.blockentity.RegisterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RegisterBlock extends AbstractShopBlock {

    public RegisterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RegisterBlockEntity(pos, state);
    }
}
