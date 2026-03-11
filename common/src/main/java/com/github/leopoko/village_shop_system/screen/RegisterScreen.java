package com.github.leopoko.village_shop_system.screen;

import com.github.leopoko.village_shop_system.blockentity.RegisterBlockEntity;
import com.github.leopoko.village_shop_system.menu.RegisterMenu;
import com.github.leopoko.village_shop_system.network.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RegisterScreen extends AbstractContainerScreen<RegisterMenu> {
    private EditBox groupNameField;
    private boolean groupNameInitialized;

    public RegisterScreen(RegisterMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        // Shop group text field
        groupNameField = new EditBox(font, leftPos + 46, topPos + 164, 82, 16, Component.empty());
        groupNameField.setMaxLength(64);
        groupNameField.setValue("");
        addWidget(groupNameField);

        // Confirm button
        addRenderableWidget(Button.builder(
                Component.translatable("screen.village_shop_system.shop_group_inline.confirm"),
                button -> confirmGroupUpdate()
        ).bounds(leftPos + 132, topPos + 164, 36, 16).build());

        groupNameInitialized = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Fill the group name once data is synced
        if (!groupNameInitialized && menu.isDataSynced()) {
            BlockPos pos = menu.getBlockPos();
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof RegisterBlockEntity register) {
                    groupNameField.setValue(register.getShopGroup());
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
            ModPackets.sendShopGroupUpdate(menu.getBlockPos(), groupName);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ShopGuiHelper.renderPanelBackground(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        ShopGuiHelper.renderAllSlots(guiGraphics, this.menu.slots, leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Draw group label
        guiGraphics.drawString(font,
                Component.translatable("screen.village_shop_system.shop_group_inline.label"),
                leftPos + 8, topPos + 168, 0x404040, false);

        // Render edit box manually (added via addWidget, not addRenderableWidget)
        if (groupNameField != null) {
            groupNameField.render(guiGraphics, mouseX, mouseY, partialTick);
        }
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
}
