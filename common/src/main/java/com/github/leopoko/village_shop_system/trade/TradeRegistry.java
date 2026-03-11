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
 *
 * Uses a two-pass approach:
 * 1. Hardcoded vanilla trade table (guaranteed to be correct)
 * 2. Dynamic scan of VillagerTrades.TRADES as supplement
 */
public final class TradeRegistry {
    /** item → encoded trade (emeralds, itemCount) for selling items to villagers */
    private static final Map<Item, Integer> SELL_PRICES = new HashMap<>();
    /** item → encoded trade (emeralds, itemCount) for buying items from villagers */
    private static final Map<Item, Integer> BUY_PRICES = new HashMap<>();
    private static boolean initialized = false;

    private TradeRegistry() {}

    /**
     * Initialize by loading hardcoded trades and scanning vanilla trades.
     * Must be called with a valid entity (used by trade listing factories to create offers).
     * Safe to call multiple times; only runs once.
     */
    public static void initialize(Entity entity) {
        if (initialized) return;
        initialized = true;
        // First pass: hardcoded known vanilla trades (reliable)
        registerKnownVanillaTrades();
        // Second pass: dynamic scan (catches anything we missed)
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

    // --- Hardcoded vanilla trades ---

    /**
     * Register all known vanilla villager trades as a reliable baseline.
     * This ensures trades work even when dynamic scanning fails.
     */
    private static void registerKnownVanillaTrades() {
        // ===== FARMER =====
        addSellPrice(Items.WHEAT, 1, 20);
        addSellPrice(Items.POTATO, 1, 26);
        addSellPrice(Items.CARROT, 1, 22);
        addSellPrice(Items.BEETROOT, 1, 15);
        addSellPrice(Items.PUMPKIN, 1, 6);
        addSellPrice(Items.MELON, 1, 4);
        addBuyPrice(Items.BREAD, 1, 6);
        addBuyPrice(Items.PUMPKIN_PIE, 1, 4);
        addBuyPrice(Items.APPLE, 1, 4);
        addBuyPrice(Items.COOKIE, 3, 18);
        addBuyPrice(Items.CAKE, 1, 1);
        addBuyPrice(Items.GOLDEN_CARROT, 3, 3);
        addBuyPrice(Items.GLISTERING_MELON_SLICE, 3, 3);

        // ===== FISHERMAN =====
        addSellPrice(Items.STRING, 1, 20);
        addSellPrice(Items.COAL, 1, 10);
        addSellPrice(Items.COD, 1, 15);
        addSellPrice(Items.SALMON, 1, 13);
        addSellPrice(Items.TROPICAL_FISH, 1, 6);
        addSellPrice(Items.PUFFERFISH, 1, 4);
        addBuyPrice(Items.COD_BUCKET, 3, 1);
        addBuyPrice(Items.COOKED_COD, 1, 6);
        addBuyPrice(Items.COOKED_SALMON, 1, 6);
        addBuyPrice(Items.CAMPFIRE, 2, 1);

        // ===== SHEPHERD =====
        addSellPrice(Items.WHITE_WOOL, 1, 18);
        addSellPrice(Items.BROWN_WOOL, 1, 18);
        addSellPrice(Items.BLACK_WOOL, 1, 18);
        addSellPrice(Items.GRAY_WOOL, 1, 18);
        addSellPrice(Items.WHITE_DYE, 1, 12);
        addSellPrice(Items.BLACK_DYE, 1, 12);
        addSellPrice(Items.BROWN_DYE, 1, 12);
        addSellPrice(Items.GRAY_DYE, 1, 12);
        addSellPrice(Items.LIGHT_GRAY_DYE, 1, 12);
        addSellPrice(Items.LIME_DYE, 1, 12);
        addSellPrice(Items.GREEN_DYE, 1, 12);
        addSellPrice(Items.RED_DYE, 1, 12);
        addSellPrice(Items.BLUE_DYE, 1, 12);
        addSellPrice(Items.YELLOW_DYE, 1, 12);
        addSellPrice(Items.ORANGE_DYE, 1, 12);
        addSellPrice(Items.PINK_DYE, 1, 12);
        addSellPrice(Items.PURPLE_DYE, 1, 12);
        addSellPrice(Items.CYAN_DYE, 1, 12);
        addSellPrice(Items.LIGHT_BLUE_DYE, 1, 12);
        addSellPrice(Items.MAGENTA_DYE, 1, 12);
        addBuyPrice(Items.SHEARS, 2, 1);

        // ===== BUTCHER =====
        addSellPrice(Items.CHICKEN, 1, 14);
        addSellPrice(Items.PORKCHOP, 1, 7);
        addSellPrice(Items.RABBIT, 1, 4);
        addSellPrice(Items.MUTTON, 1, 7);
        addSellPrice(Items.BEEF, 1, 10);
        addSellPrice(Items.DRIED_KELP_BLOCK, 1, 10);
        addSellPrice(Items.SWEET_BERRIES, 1, 10);
        addBuyPrice(Items.RABBIT_STEW, 1, 1);
        addBuyPrice(Items.COOKED_PORKCHOP, 1, 5);
        addBuyPrice(Items.COOKED_CHICKEN, 1, 8);

        // ===== LIBRARIAN =====
        addSellPrice(Items.PAPER, 1, 24);
        addSellPrice(Items.BOOK, 1, 4);
        addSellPrice(Items.INK_SAC, 1, 5);
        addBuyPrice(Items.BOOKSHELF, 9, 1);
        addBuyPrice(Items.LANTERN, 1, 1);
        addBuyPrice(Items.GLASS, 1, 4);
        addBuyPrice(Items.WRITABLE_BOOK, 1, 1);
        addBuyPrice(Items.CLOCK, 5, 1);
        addBuyPrice(Items.COMPASS, 4, 1);
        addBuyPrice(Items.NAME_TAG, 20, 1);

        // ===== CARTOGRAPHER =====
        addSellPrice(Items.GLASS_PANE, 1, 11);
        addSellPrice(Items.COMPASS, 1, 1);
        addBuyPrice(Items.MAP, 7, 1);
        addBuyPrice(Items.ITEM_FRAME, 7, 1);

        // ===== CLERIC =====
        addSellPrice(Items.ROTTEN_FLESH, 1, 32);
        addSellPrice(Items.GOLD_INGOT, 1, 3);
        addSellPrice(Items.RABBIT_FOOT, 1, 2);
        addSellPrice(Items.SCUTE, 1, 4);
        addSellPrice(Items.GLASS_BOTTLE, 1, 9);
        addSellPrice(Items.NETHER_WART, 1, 22);
        addBuyPrice(Items.REDSTONE, 1, 2);
        addBuyPrice(Items.LAPIS_LAZULI, 1, 1);
        addBuyPrice(Items.GLOWSTONE, 4, 1);
        addBuyPrice(Items.ENDER_PEARL, 5, 1);
        addBuyPrice(Items.EXPERIENCE_BOTTLE, 3, 1);

        // ===== ARMORER =====
        addSellPrice(Items.IRON_INGOT, 1, 4);
        addSellPrice(Items.DIAMOND, 1, 1);
        addBuyPrice(Items.IRON_HELMET, 5, 1);
        addBuyPrice(Items.IRON_CHESTPLATE, 9, 1);
        addBuyPrice(Items.IRON_LEGGINGS, 7, 1);
        addBuyPrice(Items.IRON_BOOTS, 4, 1);
        addBuyPrice(Items.BELL, 36, 1);
        addBuyPrice(Items.CHAINMAIL_HELMET, 1, 1);
        addBuyPrice(Items.CHAINMAIL_CHESTPLATE, 4, 1);
        addBuyPrice(Items.CHAINMAIL_LEGGINGS, 3, 1);
        addBuyPrice(Items.CHAINMAIL_BOOTS, 1, 1);
        addBuyPrice(Items.SHIELD, 5, 1);
        addBuyPrice(Items.DIAMOND_HELMET, 27, 1);
        addBuyPrice(Items.DIAMOND_CHESTPLATE, 35, 1);
        addBuyPrice(Items.DIAMOND_LEGGINGS, 31, 1);
        addBuyPrice(Items.DIAMOND_BOOTS, 27, 1);

        // ===== WEAPONSMITH =====
        addSellPrice(Items.FLINT, 1, 24);
        addBuyPrice(Items.IRON_AXE, 3, 1);
        addBuyPrice(Items.IRON_SWORD, 2, 1);
        addBuyPrice(Items.DIAMOND_AXE, 12, 1);
        addBuyPrice(Items.DIAMOND_SWORD, 8, 1);

        // ===== TOOLSMITH =====
        addBuyPrice(Items.STONE_AXE, 1, 1);
        addBuyPrice(Items.STONE_SHOVEL, 1, 1);
        addBuyPrice(Items.STONE_PICKAXE, 1, 1);
        addBuyPrice(Items.STONE_HOE, 1, 1);
        addBuyPrice(Items.IRON_SHOVEL, 2, 1);
        addBuyPrice(Items.IRON_PICKAXE, 3, 1);
        addBuyPrice(Items.DIAMOND_SHOVEL, 5, 1);
        addBuyPrice(Items.DIAMOND_PICKAXE, 13, 1);
        addBuyPrice(Items.DIAMOND_HOE, 4, 1);

        // ===== MASON =====
        addSellPrice(Items.CLAY_BALL, 1, 10);
        addSellPrice(Items.STONE, 1, 20);
        addSellPrice(Items.GRANITE, 1, 16);
        addSellPrice(Items.ANDESITE, 1, 16);
        addSellPrice(Items.DIORITE, 1, 16);
        addSellPrice(Items.NETHER_QUARTZ_ORE, 1, 12);
        addBuyPrice(Items.BRICK, 1, 10);
        addBuyPrice(Items.CHISELED_STONE_BRICKS, 1, 4);
        addBuyPrice(Items.POLISHED_ANDESITE, 1, 4);
        addBuyPrice(Items.POLISHED_DIORITE, 1, 4);
        addBuyPrice(Items.POLISHED_GRANITE, 1, 4);
        addBuyPrice(Items.DRIPSTONE_BLOCK, 1, 4);
        addBuyPrice(Items.QUARTZ_BLOCK, 1, 1);
        addBuyPrice(Items.QUARTZ_PILLAR, 1, 1);

        // ===== LEATHERWORKER =====
        addSellPrice(Items.LEATHER, 1, 6);
        addSellPrice(Items.RABBIT_HIDE, 1, 9);
        addBuyPrice(Items.LEATHER_HELMET, 5, 1);
        addBuyPrice(Items.LEATHER_CHESTPLATE, 7, 1);
        addBuyPrice(Items.LEATHER_LEGGINGS, 3, 1);
        addBuyPrice(Items.LEATHER_BOOTS, 4, 1);
        addBuyPrice(Items.LEATHER_HORSE_ARMOR, 6, 1);
        addBuyPrice(Items.SADDLE, 6, 1);

        // ===== FLETCHER =====
        addSellPrice(Items.STICK, 1, 32);
        addSellPrice(Items.FEATHER, 1, 24);
        addSellPrice(Items.TRIPWIRE_HOOK, 1, 8);
        addBuyPrice(Items.ARROW, 1, 16);
        addBuyPrice(Items.BOW, 2, 1);
        addBuyPrice(Items.CROSSBOW, 3, 1);

        // ===== WANDERING TRADER =====
        addBuyPrice(Items.SEA_PICKLE, 2, 1);
        addBuyPrice(Items.SLIME_BALL, 4, 1);
        addBuyPrice(Items.NAUTILUS_SHELL, 5, 1);
        addBuyPrice(Items.FERN, 1, 1);
        addBuyPrice(Items.SUGAR_CANE, 1, 1);
        addBuyPrice(Items.KELP, 3, 1);
        addBuyPrice(Items.CACTUS, 3, 1);
        addBuyPrice(Items.VINE, 1, 1);
        addBuyPrice(Items.BROWN_MUSHROOM, 1, 1);
        addBuyPrice(Items.RED_MUSHROOM, 1, 1);
        addBuyPrice(Items.LILY_PAD, 1, 1);
        addBuyPrice(Items.SAND, 1, 8);
        addBuyPrice(Items.RED_SAND, 1, 4);
        addBuyPrice(Items.POINTED_DRIPSTONE, 2, 1);
        addBuyPrice(Items.ROOTED_DIRT, 1, 2);
        addBuyPrice(Items.MOSS_BLOCK, 1, 2);
        addBuyPrice(Items.PODZOL, 3, 3);
        addBuyPrice(Items.PACKED_ICE, 3, 1);
        addBuyPrice(Items.GUNPOWDER, 1, 1);
    }

    private static void addSellPrice(Item item, int emeralds, int itemCount) {
        int newEncoded = encodeTrade(emeralds, itemCount);
        int current = SELL_PRICES.getOrDefault(item, -1);
        if (current == -1 || compareRatio(newEncoded, current) > 0) {
            SELL_PRICES.put(item, newEncoded);
        }
    }

    private static void addBuyPrice(Item item, int emeralds, int itemCount) {
        int newEncoded = encodeTrade(emeralds, itemCount);
        int current = BUY_PRICES.getOrDefault(item, -1);
        if (current == -1 || compareRatio(newEncoded, current) < 0) {
            BUY_PRICES.put(item, newEncoded);
        }
    }

    // --- Dynamic scanning ---

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
                MerchantOffer offer = listing.getOffer(entity, entity.level().getRandom());
                if (offer != null) {
                    processOffer(offer);
                }
            } catch (Exception ignored) {
                // Some listings may fail without proper world context (maps, etc.)
                // These are covered by the hardcoded table above
            }
        }
    }

    private static void processOffer(MerchantOffer offer) {
        ItemStack costA = offer.getCostA();
        ItemStack costB = offer.getCostB();
        ItemStack result = offer.getResult();

        // Pattern 1: Villager buys items from player → gives emeralds
        if (result.is(Items.EMERALD) && !costA.is(Items.EMERALD)) {
            addSellPrice(costA.getItem(), result.getCount(), costA.getCount());
        }

        // Pattern 2: Villager sells items to player for emeralds
        if (costA.is(Items.EMERALD) && !result.is(Items.EMERALD) && costB.isEmpty()) {
            addBuyPrice(result.getItem(), costA.getCount(), result.getCount());
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
