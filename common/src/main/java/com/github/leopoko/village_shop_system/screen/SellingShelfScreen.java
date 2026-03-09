package com.github.leopoko.village_shop_system.screen;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.blockentity.BaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.menu.SellingShelfMenu;
import com.github.leopoko.village_shop_system.trade.TradePriceCalculator;
import com.github.leopoko.village_shop_system.trade.TradeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SellingShelfScreen extends AbstractContainerScreen<SellingShelfMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Village_shop_system.MOD_ID, "textures/gui/selling_shelf.png");

    public SellingShelfScreen(SellingShelfMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            int slotIndex = this.hoveredSlot.index;

            // Only show price tooltip for input slots (0 to INPUT_SLOTS-1)
            if (slotIndex < BaseShelfBlockEntity.INPUT_SLOTS) {
                List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(stack));
                addPriceTooltip(stack, tooltip);
                guiGraphics.renderTooltip(this.font, tooltip, stack.getTooltipImage(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void addPriceTooltip(ItemStack stack, List<Component> tooltip) {
        if (!TradeRegistry.isInitialized()) {
            tooltip.add(Component.translatable("tooltip.village_shop_system.price_unknown")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        int[] ratio = TradePriceCalculator.getSellTradeRatio(stack.getItem());
        if (ratio != null) {
            int emeralds = ratio[0];
            int itemsNeeded = ratio[1];
            if (itemsNeeded == 1) {
                tooltip.add(Component.translatable("tooltip.village_shop_system.sell_price",
                        emeralds).withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("tooltip.village_shop_system.sell_price_ratio",
                        itemsNeeded, emeralds).withStyle(ChatFormatting.GREEN));
            }

            // Show total for current stack
            int totalEmeralds = TradePriceCalculator.calculateSellPrice(stack);
            if (totalEmeralds > 0 && stack.getCount() > 1) {
                tooltip.add(Component.translatable("tooltip.village_shop_system.sell_total",
                        stack.getCount(), totalEmeralds).withStyle(ChatFormatting.DARK_GREEN));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.village_shop_system.not_tradeable")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
