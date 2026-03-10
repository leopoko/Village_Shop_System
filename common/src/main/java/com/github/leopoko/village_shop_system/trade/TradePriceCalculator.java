package com.github.leopoko.village_shop_system.trade;

import com.github.leopoko.village_shop_system.config.ModConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Calculates trade prices for items.
 *
 * Price sources (in priority order):
 * 1. Custom config prices (exact, no penalty/multiplier)
 * 2. Enchanted book pricing (if enabled, based on enchantment levels)
 * 3. Vanilla trade registry (from VillagerTrades scan)
 * 4. Food pricing (based on hunger restoration)
 * 5. Tool pricing (based on tier durability and speed)
 * 6. Potion pricing (if enabled, based on effect type, level, duration)
 * 7. Not tradeable
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
        int penalty = ModConfig.sellingEmeraldPenalty;

        // Check custom config prices first (no penalty applied)
        int[] customSell = getCustomSellPrice(item);
        if (customSell != null) {
            return (customSell[0] * count) / customSell[1];
        }

        // Enchanted book selling (if enabled)
        if (ModConfig.enableEnchantedBookTrading && stack.is(Items.ENCHANTED_BOOK)) {
            int bookPrice = calculateEnchantedBookSellPrice(stack);
            if (bookPrice > 0) {
                return Math.max(0, bookPrice - penalty);
            }
        }

        // Check vanilla trades
        int vanillaTrade = TradeRegistry.getSellTrade(item);
        if (vanillaTrade != -1) {
            int emeralds = TradeRegistry.decodeEmeralds(vanillaTrade);
            int tradeItemCount = TradeRegistry.decodeItemCount(vanillaTrade);
            // Apply penalty per trade unit, not to the total
            int adjustedEmeralds = emeralds - penalty;
            if (adjustedEmeralds > 0) {
                return (adjustedEmeralds * count) / tradeItemCount;
            } else if (emeralds > 0) {
                // Penalty exceeds emeralds per trade → need more items per emerald
                int newItemCount = (int) Math.ceil((double) tradeItemCount / emeralds * (emeralds + penalty));
                newItemCount = Math.min(newItemCount, 64);
                if (newItemCount <= 0) return 0;
                return count / newItemCount;
            }
            return 0;
        }

        // Try food pricing (penalty per item)
        int foodPricePerItem = calculateFoodPricePerItem(item);
        if (foodPricePerItem > 0) {
            int adjusted = foodPricePerItem - penalty;
            if (adjusted > 0) {
                return adjusted * count;
            } else {
                // Need 2 items per emerald with penalty
                return count / 2;
            }
        }

        // Try tool pricing (tools don't stack, penalty on single item is correct)
        int toolPrice = calculateToolPrice(stack);
        if (toolPrice > 0) {
            return Math.max(0, toolPrice - penalty);
        }

        // Try potion pricing (if enabled)
        if (ModConfig.enablePotionSelling) {
            int potionPrice = calculatePotionSellPrice(stack);
            if (potionPrice > 0) return potionPrice;
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
        // Check custom config prices first (no multiplier applied)
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        Integer customPrice = ModConfig.customBuyPrices.get(itemId);
        if (customPrice != null) {
            return customPrice * count;
        }

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
     * Calculate buy price from an ItemStack (supports enchanted books with component data).
     * Falls back to calculateBuyPrice(Item, int) for non-enchanted-book items.
     *
     * @param stack the item stack being purchased
     * @param count how many items to buy
     * @return emerald cost (0 if not tradeable)
     */
    public static int calculateBuyPrice(ItemStack stack, int count) {
        if (ModConfig.enableEnchantedBookTrading && stack.is(Items.ENCHANTED_BOOK)) {
            int perItem = calculateEnchantedBookPrice(stack);
            if (perItem > 0) return perItem * count;
        }
        return calculateBuyPrice(stack.getItem(), count);
    }

    /**
     * Calculate the buy price for an enchanted book based on its stored enchantments.
     * Price = sum of (enchantedBookBasePrice * level) for each enchantment, multiplied by purchasePriceMultiplier.
     */
    private static int calculateEnchantedBookPrice(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return 0;

        int configTotal = 0;
        int baseTotal = 0;
        boolean hasConfig = false;
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();
            // Check config override for this enchantment
            String enchId = holder.unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("");
            Integer configPrice = ModConfig.enchantedBookBuyPrices.get(enchId);
            if (configPrice != null) {
                configTotal += configPrice;
                hasConfig = true;
            } else {
                baseTotal += ModConfig.enchantedBookBasePrice * level;
            }
        }
        if (hasConfig && baseTotal == 0) {
            // All enchantments have config prices
            return Math.max(1, configTotal);
        }
        // Mix: config prices are final, base prices get multiplier
        return Math.max(1, configTotal + (int) Math.ceil(baseTotal * ModConfig.purchasePriceMultiplier));
    }

    /**
     * Check if an ItemStack is buyable (supports enchanted books with component data).
     */
    public static boolean isBuyableStack(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            if (!ModConfig.enableEnchantedBookTrading) return false;
            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            return !enchantments.isEmpty();
        }
        return isTradeable(stack.getItem());
    }

    /**
     * Calculate how many items are needed per emerald for the selling shelf display.
     * Returns: [emeralds, itemsNeeded] or null if not tradeable.
     */
    public static int[] getSellTradeRatio(Item item) {
        // Check custom config prices first
        int[] customSell = getCustomSellPrice(item);
        if (customSell != null) {
            return new int[]{customSell[0], customSell[1]};
        }

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
        return hasCustomSellPrice(item)
                || hasCustomBuyPrice(item)
                || (ModConfig.enableEnchantedBookTrading && item == Items.ENCHANTED_BOOK)
                || TradeRegistry.isSellable(item)
                || TradeRegistry.isBuyable(item)
                || hasFoodPrice(item)
                || hasToolPrice(item)
                || (ModConfig.enablePotionSelling && isPotionItem(item));
    }

    /**
     * Check if an item is sellable (can be sold to villagers).
     */
    public static boolean isSellable(Item item) {
        return hasCustomSellPrice(item)
                || (ModConfig.enableEnchantedBookTrading && item == Items.ENCHANTED_BOOK)
                || TradeRegistry.isSellable(item)
                || hasFoodPrice(item)
                || hasToolPrice(item)
                || (ModConfig.enablePotionSelling && isPotionItem(item));
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

    // --- Custom config price helpers ---

    private static boolean hasCustomSellPrice(Item item) {
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        return ModConfig.customSellPrices.containsKey(itemId);
    }

    private static boolean hasCustomBuyPrice(Item item) {
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        return ModConfig.customBuyPrices.containsKey(itemId);
    }

    /**
     * Get custom sell price from config. Returns [emeralds, itemCount] or null.
     */
    private static int[] getCustomSellPrice(Item item) {
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        return ModConfig.customSellPrices.get(itemId);
    }

    // --- Potion pricing ---

    /**
     * Check if an item is a potion-type item (potion, splash, lingering, or tipped arrow).
     */
    private static boolean isPotionItem(Item item) {
        return item == Items.POTION || item == Items.SPLASH_POTION
                || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW;
    }

    /**
     * Calculate sell price for potion items based on their effects.
     * Only non-harmful effects contribute to the price.
     * Price per effect = potionBasePrice * (amplifier+1) * durationFactor
     * Penalty is subtracted per item, then multiplied by count.
     */
    private static int calculatePotionSellPrice(ItemStack stack) {
        if (!isPotionItem(stack.getItem())) return 0;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return 0;

        int pricePerItem = 0;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            MobEffect mobEffect = effect.getEffect().value();
            // Only count non-harmful effects (beneficial + neutral)
            if (mobEffect.getCategory() == MobEffectCategory.HARMFUL) continue;

            int amplifier = effect.getAmplifier() + 1; // Level 1 = amplifier 0
            int duration = effect.getDuration();
            double durationFactor = duration <= 1 ? 1.0
                    : Math.max(1.0, (double) duration / ModConfig.potionDurationUnit);
            pricePerItem += (int) (ModConfig.potionBasePrice * amplifier * durationFactor);
        }

        if (pricePerItem <= 0) return 0;
        int adjustedPrice = pricePerItem - ModConfig.sellingEmeraldPenalty;
        if (adjustedPrice <= 0) return 0;
        return adjustedPrice * stack.getCount();
    }

    // --- Enchanted book sell pricing ---

    /**
     * Calculate sell price for an enchanted book based on its stored enchantments.
     * Uses base formula only (enchantedBookBasePrice * level per enchantment).
     * Config buy price overrides are NOT applied to selling (economic balance).
     * Penalty is applied by the caller.
     */
    private static int calculateEnchantedBookSellPrice(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return 0;

        int totalPrice = 0;
        for (var entry : enchantments.entrySet()) {
            int level = entry.getIntValue();
            totalPrice += ModConfig.enchantedBookBasePrice * level;
        }
        return totalPrice;
    }
}
