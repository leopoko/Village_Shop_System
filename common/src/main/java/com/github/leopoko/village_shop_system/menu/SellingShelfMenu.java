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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SellingShelfMenu extends AbstractContainerMenu {
    private final Container container;
    private final boolean hasGroupUI;
    private final ContainerData data;

    // Client constructor for SellingShelfA (MenuType factory)
    public SellingShelfMenu(int syncId, Inventory playerInv) {
        this(ModMenuTypes.SELLING_SHELF_MENU.get(), syncId, playerInv,
                new SimpleContainer(BaseShelfBlockEntity.TOTAL_SLOTS), false, null);
    }

    // Client factory for SellingShelfB (MenuType factory)
    public static SellingShelfMenu createForShelfB(int syncId, Inventory playerInv) {
        return new SellingShelfMenu(ModMenuTypes.SELLING_SHELF_B_MENU.get(), syncId, playerInv,
                new SimpleContainer(BaseShelfBlockEntity.TOTAL_SLOTS), true, new SimpleContainerData(4));
    }

    // Server constructor for SellingShelfA
    public SellingShelfMenu(int syncId, Inventory playerInv, Container container) {
        this(ModMenuTypes.SELLING_SHELF_MENU.get(), syncId, playerInv, container, false, null);
    }

    // Server constructor for SellingShelfB (with ContainerData for BlockPos)
    public SellingShelfMenu(int syncId, Inventory playerInv, Container container, ContainerData data) {
        this(ModMenuTypes.SELLING_SHELF_B_MENU.get(), syncId, playerInv, container, true, data);
    }

    private SellingShelfMenu(MenuType<?> type, int syncId, Inventory playerInv,
                             Container container, boolean hasGroupUI, ContainerData data) {
        super(type, syncId);
        this.container = container;
        this.hasGroupUI = hasGroupUI;
        this.data = data;
        if (data != null) {
            addDataSlots(data);
        }
        container.startOpen(playerInv.player);

        // Input slots (18 = 3 rows x 6 columns)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new Slot(container, col + row * 6, 8 + col * 18, 18 + row * 18));
            }
        }

        // Output slots (9 = 3 rows x 3 columns) - emerald output
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new OutputSlot(container, BaseShelfBlockEntity.INPUT_SLOTS + col + row * 3,
                        126 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory (27 slots) - centered in wider GUI (194px)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 16 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar (9 slots) - centered in wider GUI
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 16 + col * 18, 142));
        }
    }

    public boolean hasGroupUI() {
        return hasGroupUI;
    }

    /**
     * Returns the block position of the SellingShelfB block entity.
     * Only valid when hasGroupUI() is true and data has been synced.
     */
    public BlockPos getBlockPos() {
        if (data == null) return BlockPos.ZERO;
        return new BlockPos(data.get(0), data.get(1), data.get(2));
    }

    /**
     * Returns true if the block position data has been synced from the server.
     */
    public boolean isDataSynced() {
        return data != null && data.get(3) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int containerSlots = BaseShelfBlockEntity.TOTAL_SLOTS;

            if (index < containerSlots) {
                if (!this.moveItemStackTo(current, containerSlots, containerSlots + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, BaseShelfBlockEntity.INPUT_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
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
