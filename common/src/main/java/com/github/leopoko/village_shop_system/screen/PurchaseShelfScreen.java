package com.github.leopoko.village_shop_system.screen;

import com.github.leopoko.village_shop_system.config.ModConfig;
import com.github.leopoko.village_shop_system.menu.PurchaseShelfMenu;
import com.github.leopoko.village_shop_system.trade.TradePriceCalculator;
import com.github.leopoko.village_shop_system.trade.TradeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PurchaseShelfScreen extends AbstractContainerScreen<PurchaseShelfMenu> {
    /** Golden highlight color for the config slot */
    private static final int CONFIG_HIGHLIGHT_COLOR = 0xFFFFAA00;
    /** Position and size of the info "?" button, relative to GUI origin */
    private static final int INFO_X = 65, INFO_Y = 4, INFO_W = 9, INFO_H = 10;
    /** Trade list overlay (replaces tooltip) */
    private TradeListOverlay tradeListOverlay;

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

        // Render trade list overlay on top
        if (tradeListOverlay != null) {
            tradeListOverlay.render(guiGraphics, font, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle overlay clicks first
        if (tradeListOverlay != null && tradeListOverlay.isVisible()) {
            if (tradeListOverlay.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Check "?" button click to toggle overlay
        int ix = leftPos + INFO_X;
        int iy = topPos + INFO_Y;
        if (mouseX >= ix && mouseX < ix + INFO_W && mouseY >= iy && mouseY < iy + INFO_H) {
            ensureTradeListOverlay();
            if (tradeListOverlay != null) {
                tradeListOverlay.toggle();
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tradeListOverlay != null && tradeListOverlay.isVisible()) {
            if (tradeListOverlay.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            int slotIndex = this.hoveredSlot.index;

            // Config slot: show purchase info merged into single tooltip
            if (slotIndex == PurchaseShelfMenu.CONFIG_SLOT_INDEX) {
                ItemStack configItem = this.hoveredSlot.getItem();
                List<Component> tooltip = new ArrayList<>();
                if (configItem.isEmpty()) {
                    tooltip.add(Component.translatable("tooltip.village_shop_system.purchase_shelf.config_empty"));
                } else {
                    // Include the item's normal tooltip first
                    tooltip.addAll(getTooltipFromContainerItem(configItem));
                    // Then add price info
                    tooltip.add(Component.translatable("tooltip.village_shop_system.purchase_shelf.config_set",
                            configItem.getHoverName()).withStyle(ChatFormatting.GOLD));
                    int price = TradePriceCalculator.calculateBuyPrice(configItem, 1);
                    if (price > 0) {
                        tooltip.add(Component.translatable("tooltip.village_shop_system.purchase_shelf.price", price)
                                .withStyle(ChatFormatting.GREEN));
                    }
                }
                guiGraphics.renderTooltip(this.font, tooltip,
                        configItem.isEmpty() ? java.util.Optional.empty() : configItem.getTooltipImage(),
                        mouseX, mouseY);
                return;
            }

            // Input slots (emerald slots): show normal tooltip
            if (this.hoveredSlot.hasItem()) {
                ItemStack stack = this.hoveredSlot.getItem();
                guiGraphics.renderTooltip(this.font, getTooltipFromContainerItem(stack),
                        stack.getTooltipImage(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Build the trade list overlay lazily.
     */
    private void ensureTradeListOverlay() {
        if (tradeListOverlay != null) return;

        Minecraft mc = Minecraft.getInstance();
        if (!TradeRegistry.isInitialized()) {
            if (mc.player != null) {
                TradeRegistry.initialize(mc.player);
            } else {
                return;
            }
        }

        List<TradeListOverlay.Entry> entries = new ArrayList<>();

        // Collect buyable items from vanilla trades + config
        Set<Item> processed = new HashSet<>();
        Set<Item> buyable = TradeRegistry.getAllBuyableItems();
        for (Item item : buyable) {
            processed.add(item);
            int price = TradePriceCalculator.calculateBuyPrice(item, 1);
            if (price > 0) {
                ItemStack icon = new ItemStack(item);
                Component name = item.getDescription();
                Component priceComp = Component.literal(price + "em").withStyle(ChatFormatting.GOLD);
                entries.add(new TradeListOverlay.Entry(icon, name, priceComp));
            }
        }

        // Add custom buy price items not already included
        for (var entry : ModConfig.customBuyPrices.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey()));
            if (item != null && !processed.contains(item)) {
                int price = entry.getValue();
                ItemStack icon = new ItemStack(item);
                Component name = item.getDescription();
                Component priceComp = Component.literal(price + "em").withStyle(ChatFormatting.GOLD);
                entries.add(new TradeListOverlay.Entry(icon, name, priceComp));
            }
        }

        // Sort regular items by name
        entries.sort(Comparator.comparing(e -> e.name().getString()));

        // Add enchanted book entries (if enabled)
        if (ModConfig.enableEnchantedBookTrading && mc.player != null) {
            List<TradeListOverlay.Entry> bookEntries = new ArrayList<>();
            Registry<Enchantment> enchReg = mc.player.registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT);
            for (Holder.Reference<Enchantment> holder : enchReg.holders().toList()) {
                int maxLevel = holder.value().getMaxLevel();
                for (int lvl = 1; lvl <= maxLevel; lvl++) {
                    ItemStack book = createEnchantedBook(holder, lvl);
                    int price = TradePriceCalculator.calculateBuyPrice(book, 1);
                    if (price > 0) {
                        Component name = Enchantment.getFullname(holder, lvl);
                        Component priceComp = Component.literal(price + "em")
                                .withStyle(ChatFormatting.GOLD);
                        bookEntries.add(new TradeListOverlay.Entry(book, name, priceComp));
                    }
                }
            }
            bookEntries.sort(Comparator.comparing(e -> e.name().getString()));
            entries.addAll(bookEntries);
        }

        Component header = Component.translatable("screen.village_shop_system.trade_overlay.header_buy")
                .withStyle(ChatFormatting.BOLD);
        tradeListOverlay = new TradeListOverlay(entries, header, List.of());
    }

    private static ItemStack createEnchantedBook(Holder<Enchantment> holder, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        builder.set(holder, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
        return book;
    }
}
