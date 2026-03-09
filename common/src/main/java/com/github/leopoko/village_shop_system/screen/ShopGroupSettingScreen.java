package com.github.leopoko.village_shop_system.screen;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.network.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Client-side screen for setting the shop group name on the Chair Setting Stick.
 * Opens when the player right-clicks with the stick in Shop Group Setting mode.
 */
public class ShopGroupSettingScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.village_shop_system.shop_group_setting");
    private static final Component CONFIRM = Component.translatable("screen.village_shop_system.shop_group_setting.confirm");
    private static final Component LABEL = Component.translatable("screen.village_shop_system.shop_group_setting.label");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 80;

    private final String currentGroup;
    private EditBox nameField;

    public ShopGroupSettingScreen(String currentGroup) {
        super(TITLE);
        this.currentGroup = currentGroup;
    }

    @Override
    protected void init() {
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;

        // Text input field
        nameField = new EditBox(font, x + 8, y + 30, GUI_WIDTH - 16, 18, Component.empty());
        nameField.setMaxLength(64);
        nameField.setValue(currentGroup);
        nameField.setResponder(s -> {}); // No-op
        addWidget(nameField);
        setInitialFocus(nameField);

        // Confirm button
        addRenderableWidget(Button.builder(CONFIRM, button -> {
            confirmAndClose();
        }).bounds(x + GUI_WIDTH / 2 - 50, y + 55, 100, 20).build());
    }

    private void confirmAndClose() {
        String groupName = nameField.getValue().trim();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ModPackets.sendStickGroupUpdate(mc.player.registryAccess(), groupName);
        }
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter key confirms
        if (keyCode == 257 /* GLFW_KEY_ENTER */ || keyCode == 335 /* GLFW_KEY_KP_ENTER */) {
            confirmAndClose();
            return true;
        }
        // Escape closes without saving
        if (keyCode == 256 /* GLFW_KEY_ESCAPE */) {
            onClose();
            return true;
        }
        // Let the edit box handle input
        if (nameField.isFocused()) {
            return nameField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (nameField.isFocused()) {
            return nameField.charTyped(c, modifiers);
        }
        return super.charTyped(c, modifiers);
    }

    /**
     * Override to draw our custom panel background instead of the default blur effect.
     * In MC 1.21.1, the default renderBackground() adds a blur overlay that obscures content.
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent fullscreen dim (no blur)
        guiGraphics.fill(0, 0, width, height, 0x80000000);

        // Panel background
        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;
        guiGraphics.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xF0101010);
        guiGraphics.fill(x + 1, y + 1, x + GUI_WIDTH - 1, y + GUI_HEIGHT - 1, 0xF0303030);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // super.render() calls renderBackground() first, then renders widgets (button)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = (width - GUI_WIDTH) / 2;
        int y = (height - GUI_HEIGHT) / 2;

        // Title
        guiGraphics.drawCenteredString(font, title, width / 2, y + 6, 0xFFFFFF);

        // Label
        guiGraphics.drawString(font, LABEL, x + 8, y + 20, 0xA0A0A0);

        // Render edit box (added via addWidget, not addRenderableWidget, so must render manually)
        nameField.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
