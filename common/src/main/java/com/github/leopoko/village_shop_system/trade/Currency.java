package com.github.leopoko.village_shop_system.trade;

import com.github.leopoko.village_shop_system.config.ModConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The item used as money by shelves and registers (config: {@code currencyItem}, default emerald).
 * <p>
 * Vanilla villager trades are still decoded in emeralds by {@link TradeRegistry};
 * the resulting amount is paid out as this item 1:1.
 */
public final class Currency {
    private static final Logger LOGGER = LoggerFactory.getLogger("village_shop_system");

    private static String cachedId;
    private static Item cachedItem = Items.EMERALD;

    private Currency() {}

    /** Resolve the configured currency item. Falls back to emerald if the id is invalid or unknown. */
    public static Item getItem() {
        String id = ModConfig.currencyItem;
        if (id == null || id.isBlank()) id = "minecraft:emerald";
        if (!id.equals(cachedId)) {
            cachedId = id;
            ResourceLocation rl = ResourceLocation.tryParse(id.trim());
            Item item = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (item == null || item == Items.AIR) {
                LOGGER.warn("Unknown currencyItem '{}' in config, falling back to minecraft:emerald", id);
                item = Items.EMERALD;
            }
            cachedItem = item;
        }
        return cachedItem;
    }

    public static boolean is(ItemStack stack) {
        return !stack.isEmpty() && stack.is(getItem());
    }

    public static ItemStack create(int count) {
        return new ItemStack(getItem(), count);
    }

    /** Whether {@code existing} can receive more currency (empty, or a non-full currency stack). */
    public static boolean canAccept(ItemStack existing) {
        if (existing.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(existing, create(1))
                && existing.getCount() < existing.getMaxStackSize();
    }

    /**
     * Insert currency into {@code items[from, to)}.
     * @return number actually inserted
     */
    public static int insert(NonNullList<ItemStack> items, int from, int to, int count) {
        int remaining = count;
        for (int i = from; i < to && remaining > 0; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty()) {
                ItemStack created = create(1);
                int toInsert = Math.min(remaining, created.getMaxStackSize());
                created.setCount(toInsert);
                items.set(i, created);
                remaining -= toInsert;
            } else if (canAccept(existing)) {
                int toInsert = Math.min(remaining, existing.getMaxStackSize() - existing.getCount());
                existing.grow(toInsert);
                remaining -= toInsert;
            }
        }
        return count - remaining;
    }

    /** Whether any slot in {@code items[from, to)} can receive more currency. */
    public static boolean hasSpace(NonNullList<ItemStack> items, int from, int to) {
        for (int i = from; i < to; i++) {
            if (canAccept(items.get(i))) return true;
        }
        return false;
    }
}
