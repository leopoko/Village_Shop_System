package com.github.leopoko.village_shop_system.menu;

import com.github.leopoko.village_shop_system.blockentity.BaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RegisterMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    // Client constructor
    public RegisterMenu(int syncId, Inventory playerInv) {
        this(syncId, playerInv, new SimpleContainer(BaseShelfBlockEntity.INPUT_SLOTS), new SimpleContainerData(4));
    }

    // Server constructor (backwards compatible)
    public RegisterMenu(int syncId, Inventory playerInv, Container container) {
        this(syncId, playerInv, container, new SimpleContainerData(4));
    }

    // Server constructor with ContainerData for BlockPos
    public RegisterMenu(int syncId, Inventory playerInv, Container container, ContainerData data) {
        super(ModMenuTypes.REGISTER_MENU.get(), syncId);
        this.container = container;
        this.data = data;
        addDataSlots(data);
        container.startOpen(playerInv.player);

        // Emerald slots (18 = 3 rows x 6 columns) - output only
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new OutputSlot(container, col + row * 6, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    /**
     * Returns the block position of the Register block entity.
     * Only valid after data has been synced from the server.
     */
    public BlockPos getBlockPos() {
        return new BlockPos(data.get(0), data.get(1), data.get(2));
    }

    /**
     * Returns true if the block position data has been synced from the server.
     */
    public boolean isDataSynced() {
        return data.get(3) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int containerSlots = BaseShelfBlockEntity.INPUT_SLOTS;

            if (index < containerSlots) {
                if (!this.moveItemStackTo(current, containerSlots, containerSlots + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (current.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
