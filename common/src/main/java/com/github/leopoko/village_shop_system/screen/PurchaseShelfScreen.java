package com.github.leopoko.village_shop_system.screen;

import com.github.leopoko.village_shop_system.menu.PurchaseShelfMenu;
import com.github.leopoko.village_shop_system.trade.TradePriceCalculator;
import com.github.leopoko.village_shop_system.trade.TradeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class PurchaseShelfScreen extends AbstractContainerScreen<PurchaseShelfMenu> {
    /** Golden highlight color for the config slot */
    private static final int CONFIG_HIGHLIGHT_COLOR = 0xFFFFAA00;
    /** Position and size of the info "?" button, relative to GUI origin */
    private static final int INFO_X = 65, INFO_Y = 4, INFO_W = 9, INFO_H = 10;
    /** Maximum items shown in the trade info tooltip */
    private static final int MAX_TOOLTIP_ITEMS = 18;

    /** Cached trade info tooltip (built lazily) */
    private List<Component> tradeInfoTooltip;

    public PurchaseShelfScreen(PurchaseShelfMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 194;
        this.imageHeight = 166;
        this.inventoryLabelX = 16;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ShopGuiHelper.renderPanelBackground(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        ShopGuiHelper.renderAllSlots(guiGraphics, this.menu.slots, leftPos, topPos);

        // Draw golden highlight around the config slot
        Slot configSlot = this.menu.slots.get(PurchaseShelfMenu.CONFIG_SLOT_INDEX);
        ShopGuiHelper.renderSlotHighlight(guiGraphics,
                leftPos + configSlot.x - 1, topPos + configSlot.y - 1, CONFIG_HIGHLIGHT_COLOR);

        // Draw info "?" button
        int ix = leftPos + INFO_X;
        int iy = topPos + INFO_Y;
        boolean hovered = mouseX >= ix && mouseX < ix + INFO_W && mouseY >= iy && mouseY < iy + INFO_H;
        int bgColor = hovered ? 0xFF606060 : 0xFF505050;
        guiGraphics.fill(ix, iy, ix + INFO_W, iy + INFO_H, bgColor);
        guiGraphics.fill(ix, iy, ix + INFO_W, iy + 1, 0xFF707070);
        guiGraphics.fill(ix, iy, ix + 1, iy + INFO_H, 0xFF707070);
        guiGraphics.drawString(font, "?", ix + 2, iy + 1, 0xFFFFFF00, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Show trade info tooltip when hovering the "?" button
        int ix = leftPos + INFO_X;
        int iy = topPos + INFO_Y;
        if (mouseX >= ix && mouseX < ix + INFO_W && mouseY >= iy && mouseY < iy + INFO_H) {
            guiGraphics.renderComponentTooltip(font, getTradeInfoTooltip(), mouseX, mouseY);
            return;
        }

        // Render config slot tooltip with price info
        Slot configSlot = this.menu.slots.get(PurchaseShelfMenu.CONFIG_SLOT_INDEX);
        if (isHovering(configSlot.x, configSlot.y, 16, 16, mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            ItemStack configItem = configSlot.getItem();
            if (configItem.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.village_shop_system.purchase_shelf.config_empty"));
            } else {
                tooltip.add(Component.translatable("tooltip.village_shop_system.purchase_shelf.config_set",
                        configItem.getHoverName()));
                int price = TradePriceCalculator.calculateBuyPrice(configItem.getItem(), 1);
                if (price > 0) {
                    tooltip.add(Component.translatable("tooltip.village_shop_system.purchase_shelf.price", price));
                }
            }
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Build and cache the trade info tooltip showing all buyable items.
     */
    private List<Component> getTradeInfoTooltip() {
        if (tradeInfoTooltip != null) return tradeInfoTooltip;

        tradeInfoTooltip = new ArrayList<>();
        tradeInfoTooltip.add(Component.translatable("tooltip.village_shop_system.trade_info_buy_header"));

        if (!TradeRegistry.isInitialized()) {
            if (net.minecraft.client.Minecraft.getInstance().player != null) {
                TradeRegistry.initialize(net.minecraft.client.Minecraft.getInstance().player);
            } else {
                tradeInfoTooltip.add(Component.translatable("tooltip.village_shop_system.price_unknown")
                        .withStyle(ChatFormatting.GRAY));
                return tradeInfoTooltip;
            }
        }

        // Collect buyable items with prices
        Set<Item> buyable = TradeRegistry.getAllBuyableItems();
        List<ItemPriceEntry> entries = new ArrayList<>();
        for (Item item : buyable) {
            int price = TradePriceCalculator.calculateBuyPrice(item, 1);
            if (price > 0) {
                entries.add(new ItemPriceEntry(item, price));
            }
        }

        // Sort by item name
        entries.sort(Comparator.comparing(e -> e.item.getDescription().getString()));

        // Add entries to tooltip (cap at MAX_TOOLTIP_ITEMS)
        int shown = Math.min(entries.size(), MAX_TOOLTIP_ITEMS);
        for (int i = 0; i < shown; i++) {
            ItemPriceEntry entry = entries.get(i);
            Component itemName = entry.item.getDescription();
            tradeInfoTooltip.add(Component.literal(" ")
                    .append(itemName.copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(": " + entry.price + "em").withStyle(ChatFormatting.GOLD)));
        }

        if (entries.size() > MAX_TOOLTIP_ITEMS) {
            tradeInfoTooltip.add(Component.translatable("tooltip.village_shop_system.trade_info_more",
                    entries.size() - MAX_TOOLTIP_ITEMS).withStyle(ChatFormatting.GRAY));
        }

        return tradeInfoTooltip;
    }

    private record ItemPriceEntry(Item item, int price) {}
}
