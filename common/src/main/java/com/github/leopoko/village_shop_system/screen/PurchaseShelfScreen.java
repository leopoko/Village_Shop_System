package com.github.leopoko.village_shop_system.screen;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.menu.PurchaseShelfMenu;
import com.github.leopoko.village_shop_system.trade.TradePriceCalculator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PurchaseShelfScreen extends AbstractContainerScreen<PurchaseShelfMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Village_shop_system.MOD_ID, "textures/gui/purchase_shelf.png");

    public PurchaseShelfScreen(PurchaseShelfMenu menu, Inventory playerInv, Component title) {
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
}
