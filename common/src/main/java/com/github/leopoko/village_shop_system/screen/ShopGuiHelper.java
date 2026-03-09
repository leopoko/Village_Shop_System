package com.github.leopoko.village_shop_system.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

/**
 * Helper for rendering vanilla-style GUI backgrounds programmatically.
 * Used by shop screens to avoid texture size constraints.
 */
public class ShopGuiHelper {
    // Vanilla-style colors
    private static final int PANEL_BG = 0xFFC6C6C6;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int BORDER_DARK = 0xFF555555;
    private static final int BORDER_DARKER = 0xFF373737;
    private static final int SLOT_BORDER_DARK = 0xFF373737;
    private static final int SLOT_BORDER_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_BG = 0xFF8B8B8B;

    /**
     * Draw a vanilla-style raised 3D panel background.
     */
    public static void renderPanelBackground(GuiGraphics g, int x, int y, int w, int h) {
        // Main fill
        g.fill(x, y, x + w, y + h, PANEL_BG);

        // Raised 3D border - top/left highlight (2px)
        g.fill(x, y, x + w - 1, y + 1, BORDER_LIGHT);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, BORDER_LIGHT);
        g.fill(x, y + 1, x + 1, y + h - 1, BORDER_LIGHT);
        g.fill(x + 1, y + 2, x + 2, y + h - 2, BORDER_LIGHT);

        // Raised 3D border - bottom/right shadow (2px)
        g.fill(x + 1, y + h - 1, x + w, y + h, BORDER_DARK);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, BORDER_DARKER);
        g.fill(x + w - 1, y + 1, x + w, y + h, BORDER_DARK);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, BORDER_DARKER);
    }

    /**
     * Draw a single vanilla-style recessed slot background (18x18).
     * @param x top-left x of the 18x18 slot area (slot.x - 1)
     * @param y top-left y of the 18x18 slot area (slot.y - 1)
     */
    public static void renderSlotBackground(GuiGraphics g, int x, int y) {
        // Top/left shadow (dark)
        g.fill(x, y, x + 17, y + 1, SLOT_BORDER_DARK);
        g.fill(x, y + 1, x + 1, y + 17, SLOT_BORDER_DARK);
        // Bottom/right highlight (light)
        g.fill(x + 1, y + 17, x + 18, y + 18, SLOT_BORDER_LIGHT);
        g.fill(x + 17, y + 1, x + 18, y + 17, SLOT_BORDER_LIGHT);
        // Interior
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BG);
    }

    /**
     * Draw slot backgrounds for all slots in the menu.
     */
    public static void renderAllSlots(GuiGraphics g, Iterable<Slot> slots, int leftPos, int topPos) {
        for (Slot slot : slots) {
            renderSlotBackground(g, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
    }

    /**
     * Draw a highlighted border around a slot (e.g., for config/ghost slots).
     * @param color border color (ARGB)
     */
    public static void renderSlotHighlight(GuiGraphics g, int x, int y, int color) {
        // Draw 1px colored border around the 18x18 slot area
        g.fill(x - 1, y - 1, x + 19, y, color);     // Top
        g.fill(x - 1, y + 18, x + 19, y + 19, color); // Bottom
        g.fill(x - 1, y, x, y + 18, color);           // Left
        g.fill(x + 18, y, x + 19, y + 18, color);     // Right
    }
}
