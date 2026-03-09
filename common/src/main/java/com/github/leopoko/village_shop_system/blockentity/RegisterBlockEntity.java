package com.github.leopoko.village_shop_system.blockentity;

import com.github.leopoko.village_shop_system.menu.RegisterMenu;
import com.github.leopoko.village_shop_system.registry.ModBlockEntities;
import net.minecraft.world.Container;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Register (cash register): Collects emeralds from the accounting system.
 * Only has emerald slots. Bottom hopper output.
 */
public class RegisterBlockEntity extends BaseShelfBlockEntity {
    private String shopGroup = "";

    public RegisterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REGISTER_BLOCK.get(), pos, state);
    }

    public String getShopGroup() {
        return shopGroup;
    }

    public void setShopGroup(String shopGroup) {
        this.shopGroup = shopGroup;
        setChanged();
        syncToClient();
    }

    // --- Accounting: receive emeralds from selling shelves ---

    /**
     * Insert emeralds into the register's slots.
     * Called by SellingShelfBBlockEntity when forwarding emeralds.
     * Uses ALL slots (both input and output areas) for emerald storage.
     *
     * @param count number of emeralds to insert
     * @return number of emeralds actually inserted
     */
    public int insertEmeralds(int count) {
        int remaining = count;
        for (int i = 0; i < TOTAL_SLOTS && remaining > 0; i++) {
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
        if (remaining < count) {
            setChanged();
            syncToClient();
        }
        return count - remaining;
    }

    /**
     * Check if the register has space for more emeralds.
     */
    public boolean hasEmeraldSpace() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty() || (existing.is(Items.EMERALD) && existing.getCount() < 64)) {
                return true;
            }
        }
        return false;
    }

    // --- WorldlyContainer: no input via hopper, bottom=emerald output ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            // All register slots are accessible from the bottom for hopper extraction
            int[] allSlots = new int[TOTAL_SLOTS];
            for (int i = 0; i < TOTAL_SLOTS; i++) allSlots[i] = i;
            return allSlots;
        }
        return new int[0]; // No hopper input for register
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return false; // No hopper input
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        // Only emeralds from bottom
        return direction == Direction.DOWN && stack.is(Items.EMERALD);
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        if (!shopGroup.isEmpty()) {
            return Component.translatable("block.village_shop_system.register_block")
                    .append(" - ")
                    .append(Component.translatable("tooltip.village_shop_system.shop_group", shopGroup));
        }
        return Component.translatable("block.village_shop_system.register_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
        SimpleContainerData data = new SimpleContainerData(4);
        data.set(0, worldPosition.getX());
        data.set(1, worldPosition.getY());
        data.set(2, worldPosition.getZ());
        data.set(3, 1); // valid flag
        return new RegisterMenu(syncId, playerInv, (Container) this, data);
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
