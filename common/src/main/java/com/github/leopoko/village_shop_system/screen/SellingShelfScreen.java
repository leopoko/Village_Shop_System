package com.github.leopoko.village_shop_system.screen;

import com.github.leopoko.village_shop_system.blockentity.BaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBBlockEntity;
import com.github.leopoko.village_shop_system.menu.SellingShelfMenu;
import com.github.leopoko.village_shop_system.network.ModPackets;
import com.github.leopoko.village_shop_system.config.ModConfig;
import com.github.leopoko.village_shop_system.trade.TradePriceCalculator;
import com.github.leopoko.village_shop_system.trade.TradeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SellingShelfScreen extends AbstractContainerScreen<SellingShelfMenu> {
    /** Position and size of the info "?" button, relative to GUI origin */
    private static final int INFO_X = 116, INFO_Y = 4, INFO_W = 9, INFO_H = 10;

    /** Trade list overlay (replaces tooltip) */
    private TradeListOverlay tradeListOverlay;

    /** Group UI elements (only for SellingShelfB) */
    private EditBox groupNameField;
    private boolean groupNameInitialized;

    public SellingShelfScreen(SellingShelfMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 194;
        this.imageHeight = menu.hasGroupUI() ? 184 : 166;
        this.inventoryLabelX = 16;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        if (menu.hasGroupUI()) {
            // Shop group text field
            groupNameField = new EditBox(font, leftPos + 46, topPos + 164, 100, 16, Component.empty());
            groupNameField.setMaxLength(64);
            groupNameField.setValue("");
            addWidget(groupNameField);

            // Confirm button
            addRenderableWidget(Button.builder(
                    Component.translatable("screen.village_shop_system.shop_group_inline.confirm"),
                    button -> confirmGroupUpdate()
            ).bounds(leftPos + 150, topPos + 164, 36, 16).build());

            groupNameInitialized = false;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Fill the group name once data is synced
        if (menu.hasGroupUI() && !groupNameInitialized && menu.isDataSynced()) {
            BlockPos pos = menu.getBlockPos();
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof SellingShelfBBlockEntity shelfB) {
                    groupNameField.setValue(shelfB.getShopGroup());
                    groupNameInitialized = true;
                }
            }
        }
    }

    private void confirmGroupUpdate() {
        if (!menu.isDataSynced()) return;
        String groupName = groupNameField.getValue().trim();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ModPackets.sendShopGroupUpdate(mc.player.registryAccess(), menu.getBlockPos(), groupName);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ShopGuiHelper.renderPanelBackground(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        ShopGuiHelper.renderAllSlots(guiGraphics, this.menu.slots, leftPos, topPos);

        // Draw info "?" button
        int ix = leftPos + INFO_X;
        int iy = topPos + INFO_Y;
        boolean hovered = mouseX >= ix && mouseX < ix + INFO_W && mouseY >= iy && mouseY < iy + INFO_H;
        int bgColor = hovered ? 0xFF606060 : 0xFF505050;
        guiGraphics.fill(ix, iy, ix + INFO_W, iy + INFO_H, bgColor);
        guiGraphics.fill(ix, iy, ix + INFO_W, iy + 1, 0xFF707070); // Top highlight
        guiGraphics.fill(ix, iy, ix + 1, iy + INFO_H, 0xFF707070); // Left highlight
        guiGraphics.drawString(font, "?", ix + 2, iy + 1, 0xFFFFFF00, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Draw group UI elements
        if (menu.hasGroupUI()) {
            guiGraphics.drawString(font,
                    Component.translatable("screen.village_shop_system.shop_group_inline.label"),
                    leftPos + 8, topPos + 168, 0x404040, false);

            if (groupNameField != null) {
                groupNameField.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (groupNameField != null && groupNameField.isFocused()) {
            if (keyCode == 256) { // ESC - unfocus field
                groupNameField.setFocused(false);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // ENTER
                confirmGroupUpdate();
                return true;
            }
            // Consume all keys when focused to prevent inventory close
            groupNameField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (groupNameField != null && groupNameField.isFocused()) {
            return groupNameField.charTyped(c, modifiers);
        }
        return super.charTyped(c, modifiers);
    }

    private void addPriceTooltip(ItemStack stack, List<Component> tooltip) {
        if (!TradeRegistry.isInitialized()) {
            // Initialize on client side using the local player entity
            if (Minecraft.getInstance().player != null) {
                TradeRegistry.initialize(Minecraft.getInstance().player);
            } else {
                tooltip.add(Component.translatable("tooltip.village_shop_system.price_unknown")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }
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
            // No ratio available (potion/enchanted book prices depend on component data)
            // Try calculating price from the full ItemStack
            int totalEmeralds = TradePriceCalculator.calculateSellPrice(stack);
            if (totalEmeralds > 0) {
                tooltip.add(Component.translatable("tooltip.village_shop_system.sell_price",
                        totalEmeralds).withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("tooltip.village_shop_system.not_tradeable")
                        .withStyle(ChatFormatting.RED));
            }
        }
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

    /**
     * Build the trade list overlay lazily.
     */
    private void ensureTradeListOverlay() {
        if (tradeListOverlay != null) return;

        if (!TradeRegistry.isInitialized()) {
            if (Minecraft.getInstance().player != null) {
                TradeRegistry.initialize(Minecraft.getInstance().player);
            } else {
                return;
            }
        }

        List<TradeListOverlay.Entry> entries = new ArrayList<>();

        // Collect sellable items from vanilla trades + config
        Set<Item> processed = new java.util.HashSet<>();
        Set<Item> sellable = TradeRegistry.getAllSellableItems();
        for (Item item : sellable) {
            processed.add(item);
            int[] ratio = TradePriceCalculator.getSellTradeRatio(item);
            if (ratio != null) {
                ItemStack icon = new ItemStack(item);
                Component name = item.getDescription();
                String priceStr;
                if (ratio[1] == 1) {
                    priceStr = ratio[0] + "em";
                } else {
                    priceStr = ratio[1] + " \u2192 " + ratio[0] + "em";
                }
                Component price = Component.literal(priceStr).withStyle(ChatFormatting.DARK_GREEN);
                entries.add(new TradeListOverlay.Entry(icon, name, price));
            }
        }

        // Add custom sell price items not already included
        for (var entry : ModConfig.customSellPrices.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey()));
            if (item != null && !processed.contains(item)) {
                int[] ratio = entry.getValue();
                ItemStack icon = new ItemStack(item);
                Component name = item.getDescription();
                String priceStr;
                if (ratio[1] == 1) {
                    priceStr = ratio[0] + "em";
                } else {
                    priceStr = ratio[1] + " \u2192 " + ratio[0] + "em";
                }
                Component price = Component.literal(priceStr).withStyle(ChatFormatting.DARK_GREEN);
                entries.add(new TradeListOverlay.Entry(icon, name, price));
            }
        }

        // Sort by item name
        entries.sort(Comparator.comparing(e -> e.name().getString()));

        List<Component> footerNotes = new ArrayList<>();
        footerNotes.add(Component.translatable("screen.village_shop_system.trade_overlay.food_note"));
        footerNotes.add(Component.translatable("screen.village_shop_system.trade_overlay.tool_note"));
        if (ModConfig.enablePotionSelling) {
            footerNotes.add(Component.translatable("screen.village_shop_system.trade_overlay.potion_note"));
        }
        if (ModConfig.enableEnchantedBookTrading) {
            footerNotes.add(Component.translatable("screen.village_shop_system.trade_overlay.enchanted_book_note"));
        }
        Component header = Component.translatable("screen.village_shop_system.trade_overlay.header_sell")
                .withStyle(ChatFormatting.BOLD);
        tradeListOverlay = new TradeListOverlay(entries, header, footerNotes);
    }
}
