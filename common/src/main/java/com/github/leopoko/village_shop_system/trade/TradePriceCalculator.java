package com.github.leopoko.village_shop_system.trade;

import com.github.leopoko.village_shop_system.config.ModConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;

/**
 * Calculates trade prices for items.
 *
 * Price sources (in priority order):
 * 1. Vanilla trade registry (from VillagerTrades scan)
 * 2. Food pricing (based on hunger restoration)
 * 3. Tool pricing (based on tier durability and speed)
 * 4. Not tradeable
 *
 * Selling price = base price - emerald penalty (configurable, default 1)
 * Purchase price = base price * multiplier (configurable, default 1.2)
 */
public final class TradePriceCalculator {

    private TradePriceCalculator() {}

    /**
     * Calculate how many emeralds a villager will pay for items in a selling shelf.
     * Returns the number of emeralds earned, or 0 if not tradeable.
     *
     * @param stack the item stack being sold
     * @return emeralds earned (0 if not tradeable)
     */
    public static int calculateSellPrice(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.EMERALD)) return 0;

        Item item = stack.getItem();
        int count = stack.getCount();

        // Check vanilla trades first
        int vanillaTrade = TradeRegistry.getSellTrade(item);
        if (vanillaTrade != -1) {
            int emeralds = TradeRegistry.decodeEmeralds(vanillaTrade);
            int tradeItemCount = TradeRegistry.decodeItemCount(vanillaTrade);
            // Calculate emeralds for the given count
            int baseEmeralds = (emeralds * count) / tradeItemCount;
            // Apply penalty
            int penalty = ModConfig.sellingEmeraldPenalty;
            return Math.max(0, baseEmeralds - penalty);
        }

        // Try food pricing
        int foodPrice = calculateFoodPrice(stack);
        if (foodPrice > 0) {
            int penalty = ModConfig.sellingEmeraldPenalty;
            return Math.max(0, foodPrice - penalty);
        }

        // Try tool pricing
        int toolPrice = calculateToolPrice(stack);
        if (toolPrice > 0) {
            int penalty = ModConfig.sellingEmeraldPenalty;
            return Math.max(0, toolPrice - penalty);
        }

        return 0; // Not tradeable
    }

    /**
     * Calculate how many emeralds a villager charges to sell items in a purchase shelf.
     * Returns the emerald cost, or 0 if not tradeable.
     *
     * @param item the item being purchased
     * @param count how many items to buy
     * @return emerald cost (0 if not tradeable)
     */
    public static int calculateBuyPrice(Item item, int count) {
        // Check vanilla trades first
        int vanillaTrade = TradeRegistry.getBuyTrade(item);
        if (vanillaTrade != -1) {
            int emeralds = TradeRegistry.decodeEmeralds(vanillaTrade);
            int tradeItemCount = TradeRegistry.decodeItemCount(vanillaTrade);
            int baseEmeralds = (emeralds * count + tradeItemCount - 1) / tradeItemCount; // Round up
            return Math.max(1, (int) Math.ceil(baseEmeralds * ModConfig.purchasePriceMultiplier));
        }

        // For items only in sell table, derive a buy price from sell price
        int sellTrade = TradeRegistry.getSellTrade(item);
        if (sellTrade != -1) {
            int emeralds = TradeRegistry.decodeEmeralds(sellTrade);
            int tradeItemCount = TradeRegistry.decodeItemCount(sellTrade);
            int baseEmeralds = (emeralds * count + tradeItemCount - 1) / tradeItemCount;
            // Buy price is higher than sell price
            return Math.max(1, (int) Math.ceil(baseEmeralds * ModConfig.purchasePriceMultiplier * 1.5));
        }

        return 0;
    }

    /**
     * Calculate how many items are needed per emerald for the selling shelf display.
     * Returns: [emeralds, itemsNeeded] or null if not tradeable.
     */
    public static int[] getSellTradeRatio(Item item) {
        // Check vanilla trades
        int vanillaTrade = TradeRegistry.getSellTrade(item);
        if (vanillaTrade != -1) {
            int emeralds = TradeRegistry.decodeEmeralds(vanillaTrade);
            int itemCount = TradeRegistry.decodeItemCount(vanillaTrade);
            int adjustedEmeralds = Math.max(0, emeralds - ModConfig.sellingEmeraldPenalty);
            if (adjustedEmeralds <= 0) {
                // With penalty, need more items per emerald
                // e.g., 1 emerald for 20 wheat → with -1 penalty, need 40 wheat for 1 emerald
                if (emeralds > 0) {
                    int newItemCount = (int) Math.ceil((double) itemCount / emeralds * (emeralds + ModConfig.sellingEmeraldPenalty));
                    return new int[]{1, Math.min(newItemCount, 64)};
                }
                return null; // Can't trade
            }
            return new int[]{adjustedEmeralds, itemCount};
        }

        // Food
        int foodPrice = calculateFoodPricePerItem(item);
        if (foodPrice > 0) {
            int adjusted = Math.max(0, foodPrice - ModConfig.sellingEmeraldPenalty);
            if (adjusted > 0) return new int[]{adjusted, 1};
            return new int[]{1, 2}; // Need 2 items for 1 emerald with penalty
        }

        // Tool
        int toolPrice = calculateToolPricePerItem(item);
        if (toolPrice > 0) {
            int adjusted = Math.max(0, toolPrice - ModConfig.sellingEmeraldPenalty);
            if (adjusted > 0) return new int[]{adjusted, 1};
            return null;
        }

        return null; // Not tradeable
    }

    /**
     * Check if an item is tradeable (either sellable or buyable).
     */
    public static boolean isTradeable(Item item) {
        return TradeRegistry.isSellable(item)
                || TradeRegistry.isBuyable(item)
                || hasFoodPrice(item)
                || hasToolPrice(item);
    }

    /**
     * Check if an item is sellable (can be sold to villagers).
     */
    public static boolean isSellable(Item item) {
        return TradeRegistry.isSellable(item)
                || hasFoodPrice(item)
                || hasToolPrice(item);
    }

    // --- Food pricing ---

    private static boolean hasFoodPrice(Item item) {
        ItemStack testStack = new ItemStack(item);
        FoodProperties food = testStack.get(DataComponents.FOOD);
        return food != null && food.nutrition() > 0;
    }

    private static int calculateFoodPrice(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return 0;
        int nutrition = food.nutrition();
        if (nutrition <= 0) return 0;
        return (int) (nutrition * stack.getCount() * ModConfig.foodPricePerHunger);
    }

    private static int calculateFoodPricePerItem(Item item) {
        ItemStack testStack = new ItemStack(item);
        FoodProperties food = testStack.get(DataComponents.FOOD);
        if (food == null) return 0;
        int nutrition = food.nutrition();
        if (nutrition <= 0) return 0;
        return (int) (nutrition * ModConfig.foodPricePerHunger);
    }

    // --- Tool pricing ---

    private static boolean hasToolPrice(Item item) {
        return item instanceof TieredItem;
    }

    private static int calculateToolPrice(ItemStack stack) {
        if (!(stack.getItem() instanceof TieredItem tieredItem)) return 0;
        Tier tier = tieredItem.getTier();
        int durability = tier.getUses();
        float speed = tier.getSpeed();
        double price = durability * ModConfig.toolDurabilityFactor + speed * ModConfig.toolSpeedFactor;
        return Math.max(1, (int) Math.round(price));
    }

    private static int calculateToolPricePerItem(Item item) {
        return calculateToolPrice(new ItemStack(item));
    }
}
