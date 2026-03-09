package com.github.leopoko.village_shop_system.trade;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.*;

/**
 * Scans vanilla villager trades to build lookup tables of item → emerald prices.
 * Populated lazily on first access (requires an Entity for MerchantOffer creation).
 *
 * Sell prices: how many emeralds a villager pays for items (item → emerald)
 * Buy prices: how many emeralds a villager charges for items (emerald → item)
 *
 * Prices are stored as encoded ints: (emeralds << 16) | itemCount
 */
public final class TradeRegistry {
    /** item → encoded trade (emeralds, itemCount) for selling items to villagers */
    private static final Map<Item, Integer> SELL_PRICES = new HashMap<>();
    /** item → encoded trade (emeralds, itemCount) for buying items from villagers */
    private static final Map<Item, Integer> BUY_PRICES = new HashMap<>();
    private static boolean initialized = false;

    private TradeRegistry() {}

    /**
     * Initialize by scanning vanilla trades. Must be called with a valid entity
     * (used by trade listing factories to create offers).
     * Safe to call multiple times; only runs once.
     */
    public static void initialize(Entity entity) {
        if (initialized) return;
        initialized = true;
        scanVanillaTrades(entity);
    }

    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Get the sell trade for an item (villagers buying from player).
     * Returns encoded trade or -1 if not tradeable.
     */
    public static int getSellTrade(Item item) {
        return SELL_PRICES.getOrDefault(item, -1);
    }

    /**
     * Get the buy trade for an item (villagers selling to player).
     * Returns encoded trade or -1 if not tradeable.
     */
    public static int getBuyTrade(Item item) {
        return BUY_PRICES.getOrDefault(item, -1);
    }

    /** Check if an item can be sold to villagers */
    public static boolean isSellable(Item item) {
        return SELL_PRICES.containsKey(item);
    }

    /** Check if an item can be bought from villagers */
    public static boolean isBuyable(Item item) {
        return BUY_PRICES.containsKey(item);
    }

    public static Set<Item> getAllSellableItems() {
        return Collections.unmodifiableSet(SELL_PRICES.keySet());
    }

    public static Set<Item> getAllBuyableItems() {
        return Collections.unmodifiableSet(BUY_PRICES.keySet());
    }

    // --- Encoding helpers ---

    /** Encode emerald count and item count into a single int */
    public static int encodeTrade(int emeralds, int itemCount) {
        return (emeralds << 16) | (itemCount & 0xFFFF);
    }

    /** Decode emerald count from encoded trade */
    public static int decodeEmeralds(int encoded) {
        return encoded >>> 16;
    }

    /** Decode item count from encoded trade */
    public static int decodeItemCount(int encoded) {
        return encoded & 0xFFFF;
    }

    // --- Internal scanning ---

    private static void scanVanillaTrades(Entity entity) {
        for (Map.Entry<VillagerProfession, Int2ObjectMap<VillagerTrades.ItemListing[]>> entry
                : VillagerTrades.TRADES.entrySet()) {
            Int2ObjectMap<VillagerTrades.ItemListing[]> levelMap = entry.getValue();
            for (VillagerTrades.ItemListing[] listings : levelMap.values()) {
                scanListings(listings, entity);
            }
        }

        // Wandering trader
        for (VillagerTrades.ItemListing[] listings : VillagerTrades.WANDERING_TRADER_TRADES.values()) {
            scanListings(listings, entity);
        }
    }

    private static void scanListings(VillagerTrades.ItemListing[] listings, Entity entity) {
        if (listings == null) return;
        for (VillagerTrades.ItemListing listing : listings) {
            try {
                MerchantOffer offer = listing.getOffer(entity, entity.getRandom());
                if (offer != null) {
                    processOffer(offer);
                }
            } catch (Exception ignored) {
                // Some listings may fail without proper world context
            }
        }
    }

    private static void processOffer(MerchantOffer offer) {
        ItemStack costA = offer.getBaseCostA();
        ItemStack costB = offer.getCostB();
        ItemStack result = offer.getResult();

        // Pattern 1: Villager buys items from player → gives emeralds
        // costA = items (not emeralds), result = emeralds
        if (result.is(Items.EMERALD) && !costA.is(Items.EMERALD)) {
            Item item = costA.getItem();
            int emeralds = result.getCount();
            int itemCount = costA.getCount();
            int newEncoded = encodeTrade(emeralds, itemCount);

            // Keep the best ratio (highest emeralds per item)
            int current = SELL_PRICES.getOrDefault(item, -1);
            if (current == -1 || compareRatio(newEncoded, current) > 0) {
                SELL_PRICES.put(item, newEncoded);
            }
        }

        // Pattern 2: Villager sells items to player for emeralds
        // costA = emeralds, result = items (not emeralds), no secondary cost
        if (costA.is(Items.EMERALD) && !result.is(Items.EMERALD) && costB.isEmpty()) {
            Item item = result.getItem();
            int emeralds = costA.getCount();
            int itemCount = result.getCount();
            int newEncoded = encodeTrade(emeralds, itemCount);

            // Keep the cheapest price (lowest emeralds per item)
            int current = BUY_PRICES.getOrDefault(item, -1);
            if (current == -1 || compareRatio(newEncoded, current) < 0) {
                BUY_PRICES.put(item, newEncoded);
            }
        }
    }

    /**
     * Compare two encoded trade ratios (emeralds/items).
     * Returns positive if a has higher ratio, negative if lower, 0 if equal.
     */
    private static int compareRatio(int encodedA, int encodedB) {
        // Compare emeralds_a / items_a vs emeralds_b / items_b
        // Cross-multiply to avoid floats: emeralds_a * items_b vs emeralds_b * items_a
        long a = (long) decodeEmeralds(encodedA) * decodeItemCount(encodedB);
        long b = (long) decodeEmeralds(encodedB) * decodeItemCount(encodedA);
        return Long.compare(a, b);
    }
}
