package com.github.leopoko.village_shop_system.item;

import com.github.leopoko.village_shop_system.blockentity.RegisterBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBBlockEntity;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroup;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroupManager;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

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
        STANDING_POSITION("item.village_shop_system.chair_setting_stick.mode.standing_position"),
        DELETE("item.village_shop_system.chair_setting_stick.mode.delete");

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
        if (stack.hasTag()) {
            int mode = stack.getTag().getInt(TAG_MODE);
            Mode[] values = Mode.values();
            if (mode >= 0 && mode < values.length) return values[mode];
        }
        return Mode.SHOP_GROUP;
    }

    public static String getShopGroup(ItemStack stack) {
        if (stack.hasTag()) {
            return stack.getTag().getString(TAG_SHOP_GROUP);
        }
        return "";
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode.ordinal());
    }

    public static void setShopGroup(ItemStack stack, String group) {
        stack.getOrCreateTag().putString(TAG_SHOP_GROUP, group);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Mode mode = getMode(stack);

        // Shift+right click on block: cycle mode (display handled by inventoryTick)
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                Mode newMode = mode.next();
                setMode(stack, newMode);
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
                ShopGroupManager.get(serverLevel).addChair(shopGroup, pos);
                player.displayClientMessage(
                        Component.translatable("item.village_shop_system.chair_setting_stick.chair_set",
                                shopGroup), true);
                return InteractionResult.SUCCESS;
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
            case DELETE -> {
                if (shopGroup.isEmpty()) {
                    player.displayClientMessage(
                            Component.translatable("item.village_shop_system.chair_setting_stick.no_group"), true);
                    return InteractionResult.FAIL;
                }
                ShopGroupManager manager = ShopGroupManager.get(serverLevel);
                ShopGroup group = manager.getGroup(shopGroup);
                if (group == null) {
                    player.displayClientMessage(
                            Component.translatable("item.village_shop_system.chair_setting_stick.nothing_to_delete"), true);
                    return InteractionResult.FAIL;
                }
                // Try to remove chair at this position
                if (group.getChairPositions().contains(pos)) {
                    manager.removeChair(shopGroup, pos);
                    player.displayClientMessage(
                            Component.translatable("item.village_shop_system.chair_setting_stick.chair_removed",
                                    shopGroup), true);
                    return InteractionResult.SUCCESS;
                }
                // Try to remove standing position above this block
                BlockPos abovePos = pos.above();
                if (group.getStandingPositions().contains(abovePos)) {
                    manager.removeStandingPosition(shopGroup, abovePos);
                    player.displayClientMessage(
                            Component.translatable("item.village_shop_system.chair_setting_stick.position_removed",
                                    shopGroup), true);
                    return InteractionResult.SUCCESS;
                }
                player.displayClientMessage(
                        Component.translatable("item.village_shop_system.chair_setting_stick.nothing_to_delete"), true);
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;

        // Only show info/particles when held in main hand or offhand
        boolean isHeld = isSelected || player.getOffhandItem() == stack;
        if (!isHeld) return;

        long gameTime = level.getGameTime();

        // Show current mode in action bar every 20 ticks
        if (gameTime % 20 == 0) {
            Mode mode = getMode(stack);
            String shopGroup = getShopGroup(stack);
            // Build component with proper style propagation (§ codes don't propagate to child components)
            Component modeInfo = Component.translatable(
                    "item.village_shop_system.chair_setting_stick.holding_info",
                    mode.getDisplayName()).withStyle(ChatFormatting.YELLOW);
            if (shopGroup.isEmpty()) {
                player.displayClientMessage(modeInfo, true);
            } else {
                Component groupInfo = Component.translatable(
                        "item.village_shop_system.chair_setting_stick.holding_info_group_label",
                        shopGroup).withStyle(ChatFormatting.GRAY);
                player.displayClientMessage(
                        Component.empty().append(modeInfo).append(Component.literal(" ")).append(groupInfo),
                        true);
            }
        }

        // Show particles every 10 ticks
        if (gameTime % 10 != 0) return;

        String shopGroup = getShopGroup(stack);
        if (shopGroup.isEmpty()) return;

        ServerLevel serverLevel = (ServerLevel) level;
        ShopGroupManager manager = ShopGroupManager.get(serverLevel);
        ShopGroup group = manager.getGroup(shopGroup);
        if (group == null) return;

        // Chairs: green particles
        for (BlockPos pos : group.getChairPositions()) {
            serverLevel.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    3, 0.25, 0.25, 0.25, 0);
        }
        // Standing positions: white rising particles
        for (BlockPos pos : group.getStandingPositions()) {
            serverLevel.sendParticles(player, ParticleTypes.END_ROD, true,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.25, 0.25, 0.25, 0);
        }
        // Selling shelves B: enchantment particles
        for (BlockPos pos : group.getSellingShelfBPositions()) {
            serverLevel.sendParticles(player, ParticleTypes.ENCHANT, true,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    5, 0.3, 0.3, 0.3, 0.5);
        }
        // Registers: composter particles (green/gold)
        for (BlockPos pos : group.getRegisterPositions()) {
            serverLevel.sendParticles(player, ParticleTypes.COMPOSTER, true,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    5, 0.3, 0.3, 0.3, 0);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Mode mode = getMode(stack);

        // Shift+right click in air: cycle mode (display handled by inventoryTick)
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                Mode newMode = mode.next();
                setMode(stack, newMode);
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
