package com.github.leopoko.village_shop_system.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Centered modal overlay that displays tradeable/purchasable item lists
 * with pagination, close button, and proper text colors for readability.
 */
public class TradeListOverlay {

    public record Entry(ItemStack icon, Component name, Component price) {}

    private static final int OVERLAY_WIDTH = 220;
    private static final int ITEMS_PER_PAGE = 8;
    private static final int ROW_HEIGHT = 18;
    private static final int PADDING = 6;
    private static final int HEADER_HEIGHT = 14;
    private static final int NAV_HEIGHT = 16;
    private static final int CLOSE_SIZE = 12;
    private static final int NAV_BTN_W = 20;

    private final List<Entry> entries;
    private final Component header;
    private final List<Component> footerNotes;
    private boolean visible;
    private int page;

    public TradeListOverlay(List<Entry> entries, Component header, List<Component> footerNotes) {
        this.entries = entries;
        this.header = header;
        this.footerNotes = footerNotes != null ? footerNotes : List.of();
    }

    public void toggle() { visible = !visible; }
    public boolean isVisible() { return visible; }

    private int totalPages() {
        return Math.max(1, (entries.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    }

    private int itemsOnCurrentPage() {
        int start = page * ITEMS_PER_PAGE;
        return Math.min(ITEMS_PER_PAGE, Math.max(0, entries.size() - start));
    }

    private int calcHeight() {
        int h = PADDING;
        h += HEADER_HEIGHT;
        h += 2;
        h += Math.max(1, itemsOnCurrentPage()) * ROW_HEIGHT;
        if (!footerNotes.isEmpty()) {
            h += 4;
            h += footerNotes.size() * 10;
        }
        if (totalPages() > 1) {
            h += 4;
            h += NAV_HEIGHT;
        }
        h += PADDING;
        return h;
    }

    private int getX(int screenW) { return (screenW - OVERLAY_WIDTH) / 2; }
    private int getY(int screenH, int h) { return (screenH - h) / 2; }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
        if (!visible) return;
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int oh = calcHeight();
        int ox = getX(sw);
        int oy = getY(sh, oh);

        // Push z-level above slot item decorations (count text renders at z=300)
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        // Dim background
        g.fill(0, 0, sw, sh, 0x80000000);

        // Panel background
        ShopGuiHelper.renderPanelBackground(g, ox, oy, OVERLAY_WIDTH, oh);

        int cy = oy + PADDING;

        // Header (bold, dark text)
        g.drawString(font, header, ox + PADDING, cy + 2, 0xFF404040, true);

        // Close [X] button
        int closeX = ox + OVERLAY_WIDTH - PADDING - CLOSE_SIZE - 2;
        int closeY = cy;
        boolean closeHov = mouseX >= closeX && mouseX < closeX + CLOSE_SIZE
                && mouseY >= closeY && mouseY < closeY + CLOSE_SIZE;
        int closeBg = closeHov ? 0xFFCC4444 : 0xFF993333;
        g.fill(closeX, closeY, closeX + CLOSE_SIZE, closeY + CLOSE_SIZE, closeBg);
        // 3D raised edges
        g.fill(closeX, closeY, closeX + CLOSE_SIZE, closeY + 1,
                closeHov ? 0xFFDD6666 : 0xFFBB5555);
        g.fill(closeX, closeY, closeX + 1, closeY + CLOSE_SIZE,
                closeHov ? 0xFFDD6666 : 0xFFBB5555);
        g.fill(closeX + 1, closeY + CLOSE_SIZE - 1, closeX + CLOSE_SIZE, closeY + CLOSE_SIZE,
                0xFF662222);
        g.fill(closeX + CLOSE_SIZE - 1, closeY + 1, closeX + CLOSE_SIZE, closeY + CLOSE_SIZE,
                0xFF662222);
        int xTextW = font.width("X");
        g.drawString(font, "X", closeX + (CLOSE_SIZE - xTextW) / 2, closeY + 2,
                0xFFFFFFFF, true);

        cy += HEADER_HEIGHT + 2;

        // Item rows
        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, entries.size());
        for (int i = startIdx; i < endIdx; i++) {
            Entry e = entries.get(i);
            int rowY = cy + (i - startIdx) * ROW_HEIGHT;

            // Item icon (16x16)
            g.renderItem(e.icon, ox + PADDING + 1, rowY + 1);

            // Item name (dark gray, no shadow)
            g.drawString(font, e.name, ox + PADDING + 20, rowY + 5, 0xFF404040, false);

            // Price (right-aligned, with shadow for outline effect)
            int pw = font.width(e.price);
            g.drawString(font, e.price, ox + OVERLAY_WIDTH - PADDING - pw - 2, rowY + 5,
                    0xFFFFFFFF, true);
        }

        cy += (endIdx - startIdx) * ROW_HEIGHT;

        // Footer notes
        if (!footerNotes.isEmpty()) {
            cy += 4;
            for (Component note : footerNotes) {
                g.drawString(font, note, ox + PADDING, cy, 0xFF555555, false);
                cy += 10;
            }
        }

        // Navigation
        if (totalPages() > 1) {
            cy += 4;

            // Previous button
            int prevX = ox + PADDING;
            boolean prevHov = mouseX >= prevX && mouseX < prevX + NAV_BTN_W
                    && mouseY >= cy && mouseY < cy + NAV_HEIGHT;
            drawBtn(g, font, "<", prevX, cy, NAV_BTN_W, NAV_HEIGHT, prevHov, page > 0);

            // Page info
            String pi = (page + 1) + " / " + totalPages();
            int piW = font.width(pi);
            g.drawString(font, pi, ox + (OVERLAY_WIDTH - piW) / 2, cy + 4, 0xFF404040, false);

            // Next button
            int nextX = ox + OVERLAY_WIDTH - PADDING - NAV_BTN_W;
            boolean nextHov = mouseX >= nextX && mouseX < nextX + NAV_BTN_W
                    && mouseY >= cy && mouseY < cy + NAV_HEIGHT;
            drawBtn(g, font, ">", nextX, cy, NAV_BTN_W, NAV_HEIGHT, nextHov,
                    page < totalPages() - 1);
        }

        g.pose().popPose();
    }

    private void drawBtn(GuiGraphics g, Font font, String text, int x, int y,
                         int w, int h, boolean hov, boolean enabled) {
        int bg = enabled ? (hov ? 0xFF808080 : 0xFF686868) : 0xFF585858;
        g.fill(x, y, x + w, y + h, bg);
        // Raised edges
        g.fill(x, y, x + w, y + 1, enabled ? 0xFFA0A0A0 : 0xFF686868);
        g.fill(x, y, x + 1, y + h, enabled ? 0xFFA0A0A0 : 0xFF686868);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFF404040);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFF404040);
        int tc = enabled ? (hov ? 0xFFFFFFFF : 0xFFE0E0E0) : 0xFF909090;
        int tw = font.width(text);
        g.drawString(font, text, x + (w - tw) / 2, y + (h - 8) / 2, tc, enabled);
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (!visible) return false;
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int oh = calcHeight();
        int ox = getX(sw);
        int oy = getY(sh, oh);

        // Close button
        int closeX = ox + OVERLAY_WIDTH - PADDING - CLOSE_SIZE - 2;
        int closeY = oy + PADDING;
        if (mx >= closeX && mx < closeX + CLOSE_SIZE
                && my >= closeY && my < closeY + CLOSE_SIZE) {
            visible = false;
            return true;
        }

        // Nav buttons
        if (totalPages() > 1) {
            int cy = oy + PADDING + HEADER_HEIGHT + 2
                    + itemsOnCurrentPage() * ROW_HEIGHT;
            if (!footerNotes.isEmpty()) {
                cy += 4 + footerNotes.size() * 10;
            }
            cy += 4;

            int prevX = ox + PADDING;
            if (mx >= prevX && mx < prevX + NAV_BTN_W
                    && my >= cy && my < cy + NAV_HEIGHT && page > 0) {
                page--;
                return true;
            }

            int nextX = ox + OVERLAY_WIDTH - PADDING - NAV_BTN_W;
            if (mx >= nextX && mx < nextX + NAV_BTN_W
                    && my >= cy && my < cy + NAV_HEIGHT && page < totalPages() - 1) {
                page++;
                return true;
            }
        }

        // Inside overlay -> consume click
        if (mx >= ox && mx < ox + OVERLAY_WIDTH && my >= oy && my < oy + oh) {
            return true;
        }

        // Outside overlay -> close
        visible = false;
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (!visible) return false;
        if (totalPages() <= 1) return true;
        if (scrollY > 0 && page > 0) {
            page--;
        } else if (scrollY < 0 && page < totalPages() - 1) {
            page++;
        }
        return true;
    }
}
