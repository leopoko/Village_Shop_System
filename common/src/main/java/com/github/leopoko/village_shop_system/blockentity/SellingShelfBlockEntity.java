package com.github.leopoko.village_shop_system.blockentity;

import com.github.leopoko.village_shop_system.menu.SellingShelfMenu;
import com.github.leopoko.village_shop_system.registry.ModBlockEntities;
import com.github.leopoko.village_shop_system.trade.TradePriceCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Selling Shelf A: Players insert items, villagers buy them for emeralds.
 * Input: items (top + sides via hopper)
 * Output: emeralds (bottom via hopper)
 */
public class SellingShelfBlockEntity extends BaseShelfBlockEntity {

    public SellingShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SELLING_SHELF.get(), pos, state);
    }

    // --- WorldlyContainer: top+sides=item input, bottom=emerald output ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return getOutputSlots();
        }
        return getInputSlots();
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        // Only allow non-emerald items into input slots from top/sides
        if (direction == Direction.DOWN) return false;
        return index < INPUT_SLOTS && !stack.is(Items.EMERALD);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        // Only allow taking emeralds from output slots via bottom
        return direction == Direction.DOWN && index >= INPUT_SLOTS;
    }

    // --- Trade execution (called by villager AI) ---

    /**
     * Attempt to process trades in this shelf.
     * Scans input slots for sellable items, calculates emerald value,
     * consumes items and produces emeralds in output slots.
     *
     * @return the number of emeralds generated (0 if nothing to trade)
     */
    public int processTrades() {
        int totalEmeralds = 0;

        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            int emeralds = TradePriceCalculator.calculateSellPrice(stack);
            if (emeralds <= 0) continue;

            // Try to insert emeralds into output slots
            int inserted = insertEmeralds(emeralds);
            if (inserted > 0) {
                // Calculate how many items to consume proportionally
                int totalPossible = TradePriceCalculator.calculateSellPrice(stack);
                if (totalPossible > 0) {
                    int itemsToConsume;
                    if (inserted >= totalPossible) {
                        itemsToConsume = stack.getCount();
                    } else {
                        // Partial: consume proportional items
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
     * Try to insert emeralds into output slots.
     * @return number of emeralds actually inserted
     */
    private int insertEmeralds(int count) {
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

    /**
     * Check if this shelf has any items that can be traded.
     */
    public boolean hasTradeableItems() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && TradePriceCalculator.isSellable(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there's space in output slots for emeralds.
     */
    public boolean hasOutputSpace() {
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty() || (existing.is(Items.EMERALD) && existing.getCount() < 64)) {
                return true;
            }
        }
        return false;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.village_shop_system.selling_shelf");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
        return new SellingShelfMenu(syncId, playerInv, this);
    }
}
