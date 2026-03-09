package com.github.leopoko.village_shop_system.item;

import com.github.leopoko.village_shop_system.blockentity.RegisterBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBBlockEntity;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroupManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Chair Setting Stick: Multi-mode tool for configuring shop groups, chairs, and standing positions.
 * Modes cycle with Shift+Right Click:
 * 0 = Shop Group Setting Mode (opens GUI to set group name)
 * 1 = Chair Setting Mode (click stairs to register as chair)
 * 2 = Standing Position Mode (click block to register standing position)
 */
public class ChairSettingStick extends Item {
    public static final String TAG_MODE = "StickMode";
    public static final String TAG_SHOP_GROUP = "ShopGroup";

    /**
     * Client-side callback to open the shop group settings screen.
     * Set by Village_shop_systemClient.init(). Receives the current group name.
     */
    public static Consumer<String> openSettingsScreen;

    public enum Mode {
        SHOP_GROUP("item.village_shop_system.chair_setting_stick.mode.shop_group"),
        CHAIR("item.village_shop_system.chair_setting_stick.mode.chair"),
        STANDING_POSITION("item.village_shop_system.chair_setting_stick.mode.standing_position");

        private final String translationKey;

        Mode(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getDisplayName() {
            return Component.translatable(translationKey);
        }

        public Mode next() {
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public ChairSettingStick(Properties properties) {
        super(properties);
    }

    public static Mode getMode(ItemStack stack) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (customData != null) {
                int mode = customData.copyTag().getInt(TAG_MODE);
                Mode[] values = Mode.values();
                if (mode >= 0 && mode < values.length) return values[mode];
            }
        }
        return Mode.SHOP_GROUP;
    }

    public static String getShopGroup(ItemStack stack) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (customData != null) {
                return customData.copyTag().getString(TAG_SHOP_GROUP);
            }
        }
        return "";
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY,
                data -> {
                    var tag = data.copyTag();
                    tag.putInt(TAG_MODE, mode.ordinal());
                    return net.minecraft.world.item.component.CustomData.of(tag);
                });
    }

    public static void setShopGroup(ItemStack stack, String group) {
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY,
                data -> {
                    var tag = data.copyTag();
                    tag.putString(TAG_SHOP_GROUP, group);
                    return net.minecraft.world.item.component.CustomData.of(tag);
                });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Mode mode = getMode(stack);

        // Shift+right click on block: cycle mode
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                Mode newMode = mode.next();
                setMode(stack, newMode);
                player.displayClientMessage(
                        Component.translatable("item.village_shop_system.chair_setting_stick.mode_changed",
                                newMode.getDisplayName()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        String shopGroup = getShopGroup(stack);

        // Shop group mode: apply group to SellingShelfB/Register, or open settings screen
        if (mode == Mode.SHOP_GROUP) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SellingShelfBBlockEntity || be instanceof RegisterBlockEntity) {
                // Apply the stick's group to the block
                if (level.isClientSide) return InteractionResult.SUCCESS;
                if (shopGroup.isEmpty()) {
                    player.displayClientMessage(
                            Component.translatable("item.village_shop_system.chair_setting_stick.no_group"), true);
                    return InteractionResult.FAIL;
                }
                ServerLevel serverLevel = (ServerLevel) level;
                if (be instanceof SellingShelfBBlockEntity shelfB) {
                    String oldGroup = shelfB.getShopGroup();
                    if (!oldGroup.isEmpty()) {
                        ShopGroupManager.get(serverLevel).removeSellingShelfB(oldGroup, pos);
                    }
                    shelfB.setShopGroup(shopGroup);
                    ShopGroupManager.get(serverLevel).addSellingShelfB(shopGroup, pos);
                } else {
                    RegisterBlockEntity register = (RegisterBlockEntity) be;
                    String oldGroup = register.getShopGroup();
                    if (!oldGroup.isEmpty()) {
                        ShopGroupManager.get(serverLevel).removeRegister(oldGroup, pos);
                    }
                    register.setShopGroup(shopGroup);
                    ShopGroupManager.get(serverLevel).addRegister(shopGroup, pos);
                }
                player.displayClientMessage(
                        Component.translatable("item.village_shop_system.chair_setting_stick.group_applied",
                                shopGroup), true);
                return InteractionResult.SUCCESS;
            }
            // Not a shop block: open settings screen on client
            if (level.isClientSide && openSettingsScreen != null) {
                openSettingsScreen.accept(shopGroup);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.isClientSide) return InteractionResult.SUCCESS;

        ServerLevel serverLevel = (ServerLevel) level;

        switch (mode) {
            case CHAIR -> {
                if (shopGroup.isEmpty()) {
                    player.displayClientMessage(
                            Component.translatable("item.village_shop_system.chair_setting_stick.no_group"), true);
                    return InteractionResult.FAIL;
                }
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof StairBlock) {
                    ShopGroupManager.get(serverLevel).addChair(shopGroup, pos);
                    player.displayClientMessage(
                            Component.translatable("item.village_shop_system.chair_setting_stick.chair_set",
                                    shopGroup), true);
                    return InteractionResult.SUCCESS;
                } else {
                    player.displayClientMessage(
                            Component.translatable("item.village_shop_system.chair_setting_stick.not_stairs"), true);
                    return InteractionResult.FAIL;
                }
            }
            case STANDING_POSITION -> {
                if (shopGroup.isEmpty()) {
                    player.displayClientMessage(
                            Component.translatable("item.village_shop_system.chair_setting_stick.no_group"), true);
                    return InteractionResult.FAIL;
                }
                ShopGroupManager.get(serverLevel).addStandingPosition(shopGroup, pos.above());
                player.displayClientMessage(
                        Component.translatable("item.village_shop_system.chair_setting_stick.position_set",
                                shopGroup), true);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Mode mode = getMode(stack);

        // Shift+right click in air: cycle mode
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                Mode newMode = mode.next();
                setMode(stack, newMode);
                player.displayClientMessage(
                        Component.translatable("item.village_shop_system.chair_setting_stick.mode_changed",
                                newMode.getDisplayName()), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // In shop group mode, right click in air opens settings GUI
        if (mode == Mode.SHOP_GROUP) {
            if (level.isClientSide && openSettingsScreen != null) {
                String shopGroup = getShopGroup(stack);
                openSettingsScreen.accept(shopGroup);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }
}
