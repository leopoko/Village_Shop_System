package com.github.leopoko.village_shop_system.menu;

import com.github.leopoko.village_shop_system.blockentity.BaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.PurchaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.registry.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PurchaseShelfMenu extends AbstractContainerMenu {
    private final Container container;
    /** The slot index (in menu.slots) of the config ghost slot */
    public static final int CONFIG_SLOT_INDEX = BaseShelfBlockEntity.TOTAL_SLOTS; // slot 27 in container

    // Client constructor
    public PurchaseShelfMenu(int syncId, Inventory playerInv) {
        this(syncId, playerInv, new SimpleContainer(PurchaseShelfBlockEntity.PURCHASE_TOTAL_SLOTS));
    }

    // Server constructor
    public PurchaseShelfMenu(int syncId, Inventory playerInv, Container container) {
        super(ModMenuTypes.PURCHASE_SHELF_MENU.get(), syncId);
        this.container = container;
        container.startOpen(playerInv.player);

        // Input slots (18 = 3 rows x 6 columns) - emerald input
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new EmeraldOnlySlot(container, col + row * 6, 8 + col * 18, 18 + row * 18));
            }
        }

        // Output slots (9 = 3 rows x 3 columns) - purchased items output
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new OutputSlot(container, BaseShelfBlockEntity.INPUT_SLOTS + col + row * 3,
                        126 + col * 18, 18 + row * 18));
            }
        }

        // Config ghost slot - displays the configured purchase item
        // Positioned between the emerald input area and output area
        addSlot(new ConfigSlot(container, PurchaseShelfBlockEntity.CONFIG_SLOT, 116, 36));

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

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Handle config slot: copy carried item as template (don't consume)
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (slot instanceof ConfigSlot) {
                ItemStack carried = getCarried();
                if (carried.isEmpty()) {
                    // Clicking with empty hand clears the config
                    container.setItem(PurchaseShelfBlockEntity.CONFIG_SLOT, ItemStack.EMPTY);
                } else {
                    // Set config to carried item type (don't consume the item)
                    container.setItem(PurchaseShelfBlockEntity.CONFIG_SLOT,
                            new ItemStack(carried.getItem(), 1));
                }
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot instanceof ConfigSlot) return ItemStack.EMPTY;
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int containerSlots = BaseShelfBlockEntity.TOTAL_SLOTS;

            if (index < containerSlots) {
                // Move from container to player inventory
                if (!this.moveItemStackTo(current, containerSlots + 1, containerSlots + 1 + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index > containerSlots) {
                // Move from player inventory to container (only emeralds to input)
                if (current.is(Items.EMERALD)) {
                    if (!this.moveItemStackTo(current, 0, BaseShelfBlockEntity.INPUT_SLOTS, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
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

    private static class EmeraldOnlySlot extends Slot {
        public EmeraldOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.EMERALD);
        }
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

    /**
     * Ghost/config slot: displays the configured item but doesn't hold real items.
     * Click with an item to set as purchase target. Click with empty hand to clear.
     */
    private static class ConfigSlot extends Slot {
        public ConfigSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // Handled by clicked() override
        }

        @Override
        public boolean mayPickup(Player player) {
            return false; // Can't take from config slot
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
