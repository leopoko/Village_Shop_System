package com.github.leopoko.village_shop_system.blockentity;

import com.github.leopoko.village_shop_system.menu.PurchaseShelfMenu;
import com.github.leopoko.village_shop_system.registry.ModBlockEntities;
import com.github.leopoko.village_shop_system.trade.TradePriceCalculator;
import net.minecraft.world.Container;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Purchase Shelf: Emeralds go in, configured items come out.
 * Input: emeralds (top + sides via hopper)
 * Output: purchased items (bottom via hopper)
 */
public class PurchaseShelfBlockEntity extends BaseShelfBlockEntity {
    /** Extra slot index for the configured item display */
    public static final int CONFIG_SLOT = TOTAL_SLOTS;
    public static final int PURCHASE_TOTAL_SLOTS = TOTAL_SLOTS + 1;

    /** The item this shelf is configured to purchase from villagers */
    private ItemStack configuredItem = ItemStack.EMPTY;

    public PurchaseShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PURCHASE_SHELF.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return PURCHASE_TOTAL_SLOTS;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == CONFIG_SLOT) return configuredItem;
        return super.getItem(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == CONFIG_SLOT) {
            setConfiguredItem(stack);
            return;
        }
        super.setItem(slot, stack);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == CONFIG_SLOT) return ItemStack.EMPTY;
        return super.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == CONFIG_SLOT) return ItemStack.EMPTY;
        return super.removeItemNoUpdate(slot);
    }

    public ItemStack getConfiguredItem() {
        return configuredItem;
    }

    public void setConfiguredItem(ItemStack item) {
        this.configuredItem = item.isEmpty() ? ItemStack.EMPTY : new ItemStack(item.getItem(), 1);
        setChanged();
        syncToClient();
    }

    // --- Trade execution (called by villager AI) ---

    /**
     * Process purchases: consume emeralds from input, produce configured items in output.
     *
     * @return the number of items produced (0 if nothing to trade)
     */
    public int processPurchases() {
        if (configuredItem.isEmpty()) return 0;

        int pricePerItem = TradePriceCalculator.calculateBuyPrice(configuredItem.getItem(), 1);
        if (pricePerItem <= 0) return 0;

        int totalProduced = 0;
        int availableEmeralds = countEmeraldsInInput();

        while (availableEmeralds >= pricePerItem) {
            // Check output space
            if (!canInsertItem(configuredItem)) break;

            // Consume emeralds
            int consumed = consumeEmeralds(pricePerItem);
            if (consumed < pricePerItem) break; // Shouldn't happen but safety check

            // Produce item
            insertOutputItem(configuredItem.copy());
            availableEmeralds -= pricePerItem;
            totalProduced++;
        }

        if (totalProduced > 0) {
            setChanged();
            syncToClient();
        }
        return totalProduced;
    }

    /**
     * Count total emeralds across all input slots.
     */
    private int countEmeraldsInInput() {
        int total = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.is(Items.EMERALD)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Consume a specified number of emeralds from input slots.
     * @return number actually consumed
     */
    private int consumeEmeralds(int count) {
        int remaining = count;
        for (int i = 0; i < INPUT_SLOTS && remaining > 0; i++) {
            ItemStack stack = items.get(i);
            if (stack.is(Items.EMERALD)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                if (stack.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
                remaining -= take;
            }
        }
        return count - remaining;
    }

    /**
     * Check if an item can be inserted into output slots.
     */
    private boolean canInsertItem(ItemStack item) {
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(existing, item) && existing.getCount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Insert an item into the first available output slot.
     */
    private void insertOutputItem(ItemStack item) {
        // Try to merge with existing stacks first
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, item)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toInsert = Math.min(item.getCount(), space);
                    existing.grow(toInsert);
                    item.shrink(toInsert);
                    if (item.isEmpty()) return;
                }
            }
        }
        // Place in empty slot
        for (int i = INPUT_SLOTS; i < TOTAL_SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, item);
                return;
            }
        }
    }

    /**
     * Check if this shelf has emeralds and a configured item for trading.
     */
    public boolean canTrade() {
        if (configuredItem.isEmpty()) return false;
        int price = TradePriceCalculator.calculateBuyPrice(configuredItem.getItem(), 1);
        return price > 0 && countEmeraldsInInput() >= price && canInsertItem(configuredItem);
    }

    // --- WorldlyContainer: top+sides=emerald input, bottom=item output ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return getOutputSlots();
        return getInputSlots();
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        // Only allow emeralds into input slots from top/sides
        if (direction == Direction.DOWN) return false;
        return index < INPUT_SLOTS && stack.is(Items.EMERALD);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        // Only allow taking purchased items from output slots via bottom
        return direction == Direction.DOWN && index >= INPUT_SLOTS;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.village_shop_system.purchase_shelf");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
        return new PurchaseShelfMenu(syncId, playerInv, (Container) this);
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!configuredItem.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            tag.put("ConfiguredItem", configuredItem.save(registries, itemTag));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ConfiguredItem")) {
            configuredItem = ItemStack.parse(registries, tag.getCompound("ConfiguredItem")).orElse(ItemStack.EMPTY);
        }
    }
}
