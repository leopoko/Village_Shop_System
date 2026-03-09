package com.github.leopoko.village_shop_system.blockentity;

import com.github.leopoko.village_shop_system.menu.SellingShelfMenu;
import com.github.leopoko.village_shop_system.registry.ModBlockEntities;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroup;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroupManager;
import com.github.leopoko.village_shop_system.trade.TradePriceCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Selling Shelf B: Same as A but linked to accounting system via shop group.
 * Emeralds from trades are forwarded to the group's registers instead of
 * being stored locally in output slots.
 */
public class SellingShelfBBlockEntity extends BaseShelfBlockEntity {
    private String shopGroup = "";

    public SellingShelfBBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SELLING_SHELF_B.get(), pos, state);
    }

    public String getShopGroup() {
        return shopGroup;
    }

    public void setShopGroup(String shopGroup) {
        this.shopGroup = shopGroup;
        setChanged();
        syncToClient();
    }

    // --- Trade execution (called by villager AI) ---

    /**
     * Process trades and forward emeralds to registers in the shop group.
     * If no group or no registers available, emeralds go to local output slots.
     *
     * @return the number of emeralds generated
     */
    public int processTrades() {
        int totalEmeralds = 0;

        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            int emeralds = TradePriceCalculator.calculateSellPrice(stack);
            if (emeralds <= 0) continue;

            // Try to send emeralds to registers first, fall back to local output
            int inserted;
            if (!shopGroup.isEmpty() && level instanceof ServerLevel serverLevel) {
                inserted = sendEmeraldsToRegisters(serverLevel, emeralds);
                // Any remaining go to local output
                int remaining = emeralds - inserted;
                if (remaining > 0) {
                    inserted += insertLocalEmeralds(remaining);
                }
            } else {
                inserted = insertLocalEmeralds(emeralds);
            }

            if (inserted > 0) {
                int totalPossible = TradePriceCalculator.calculateSellPrice(stack);
                if (totalPossible > 0) {
                    int itemsToConsume;
                    if (inserted >= totalPossible) {
                        itemsToConsume = stack.getCount();
                    } else {
                        itemsToConsume = (int) Math.ceil((double) inserted / totalPossible * stack.getCount());
                    }
                    stack.shrink(Math.min(itemsToConsume, stack.getCount()));
                    if (stack.isEmpty()) {
                        items.set(i, ItemStack.EMPTY);
                    }
                    totalEmeralds += inserted;
                }
            }
        }

        if (totalEmeralds > 0) {
            setChanged();
            syncToClient();
        }
        return totalEmeralds;
    }

    /**
     * Send emeralds to registers in the shop group.
     * @return number of emeralds successfully sent
     */
    private int sendEmeraldsToRegisters(ServerLevel serverLevel, int count) {
        ShopGroupManager manager = ShopGroupManager.get(serverLevel);
        ShopGroup group = manager.getGroup(shopGroup);
        if (group == null) return 0;

        List<BlockPos> registerPositions = group.getRegisterPositions();
        int remaining = count;

        for (BlockPos regPos : registerPositions) {
            if (remaining <= 0) break;
            if (!serverLevel.isLoaded(regPos)) continue;

            BlockEntity be = serverLevel.getBlockEntity(regPos);
            if (be instanceof RegisterBlockEntity register) {
                int inserted = register.insertEmeralds(remaining);
                remaining -= inserted;
            }
        }

        return count - remaining;
    }

    /**
     * Insert emeralds into local output slots.
     */
    private int insertLocalEmeralds(int count) {
        int remaining = count;
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS && remaining > 0; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty()) {
                int toInsert = Math.min(remaining, 64);
                items.set(i, new ItemStack(Items.EMERALD, toInsert));
                remaining -= toInsert;
            } else if (existing.is(Items.EMERALD) && existing.getCount() < 64) {
                int space = 64 - existing.getCount();
                int toInsert = Math.min(remaining, space);
                existing.grow(toInsert);
                remaining -= toInsert;
            }
        }
        return count - remaining;
    }

    public boolean hasTradeableItems() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && TradePriceCalculator.isSellable(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOutputSpace() {
        // Check local output space
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty() || (existing.is(Items.EMERALD) && existing.getCount() < 64)) {
                return true;
            }
        }
        // Also check registers if in a group
        if (!shopGroup.isEmpty() && level instanceof ServerLevel serverLevel) {
            ShopGroupManager manager = ShopGroupManager.get(serverLevel);
            ShopGroup group = manager.getGroup(shopGroup);
            if (group != null) {
                for (BlockPos regPos : group.getRegisterPositions()) {
                    if (!serverLevel.isLoaded(regPos)) continue;
                    BlockEntity be = serverLevel.getBlockEntity(regPos);
                    if (be instanceof RegisterBlockEntity register && register.hasEmeraldSpace()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // --- WorldlyContainer: same as Selling Shelf A ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return getOutputSlots();
        return getInputSlots();
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        if (direction == Direction.DOWN) return false;
        return index < INPUT_SLOTS && !stack.is(Items.EMERALD);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && index >= INPUT_SLOTS;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        if (!shopGroup.isEmpty()) {
            return Component.translatable("block.village_shop_system.selling_shelf_b")
                    .append(" - ")
                    .append(Component.translatable("tooltip.village_shop_system.shop_group", shopGroup));
        }
        return Component.translatable("block.village_shop_system.selling_shelf_b");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
        return new SellingShelfMenu(syncId, playerInv, this);
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("ShopGroup", shopGroup);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shopGroup = tag.getString("ShopGroup");
    }
}
