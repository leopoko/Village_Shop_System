package com.github.leopoko.village_shop_system.trade;

import com.github.leopoko.village_shop_system.config.ModConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;
import java.util.Map;

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

    public static int calculateSellPrice(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.EMERALD)) return 0;

        Item item = stack.getItem();
        int count = stack.getCount();
        int penalty = ModConfig.sellingEmeraldPenalty;

        int[] customSell = getCustomSellPrice(item);
        if (customSell != null) {
            return (customSell[0] * count) / customSell[1];
        }

        if (ModConfig.enableEnchantedBookTrading && stack.is(Items.ENCHANTED_BOOK)) {
            int bookPrice = calculateEnchantedBookSellPrice(stack);
            if (bookPrice > 0) {
                return Math.max(0, bookPrice - penalty);
            }
        }

        int vanillaTrade = TradeRegistry.getSellTrade(item);
        if (vanillaTrade != -1) {
            int emeralds = TradeRegistry.decodeEmeralds(vanillaTrade);
            int tradeItemCount = TradeRegistry.decodeItemCount(vanillaTrade);
            int adjustedEmeralds = emeralds - penalty;
            if (adjustedEmeralds > 0) {
                return (adjustedEmeralds * count) / tradeItemCount;
            } else if (emeralds > 0) {
                int newItemCount = (int) Math.ceil((double) tradeItemCount / emeralds * (emeralds + penalty));
                newItemCount = Math.min(newItemCount, 64);
                if (newItemCount <= 0) return 0;
                return count / newItemCount;
            }
            return 0;
        }

        int foodPricePerItem = calculateFoodPricePerItem(item);
        if (foodPricePerItem > 0) {
            int adjusted = foodPricePerItem - penalty;
            if (adjusted > 0) {
                return adjusted * count;
            } else {
                return count / 2;
            }
        }

        int toolPrice = calculateToolPrice(stack);
        if (toolPrice > 0) {
            return Math.max(0, toolPrice - penalty);
        }

        if (ModConfig.enablePotionSelling) {
            int potionPrice = calculatePotionSellPrice(stack);
            if (potionPrice > 0) return potionPrice;
        }

        return 0;
    }

    public static int calculateBuyPrice(Item item, int count) {
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        Integer customPrice = ModConfig.customBuyPrices.get(itemId);
        if (customPrice != null) {
            return customPrice * count;
        }

        int vanillaTrade = TradeRegistry.getBuyTrade(item);
        if (vanillaTrade != -1) {
            int emeralds = TradeRegistry.decodeEmeralds(vanillaTrade);
            int tradeItemCount = TradeRegistry.decodeItemCount(vanillaTrade);
            int baseEmeralds = (emeralds * count + tradeItemCount - 1) / tradeItemCount;
            return Math.max(1, (int) Math.ceil(baseEmeralds * ModConfig.purchasePriceMultiplier));
        }

        int sellTrade = TradeRegistry.getSellTrade(item);
        if (sellTrade != -1) {
            int emeralds = TradeRegistry.decodeEmeralds(sellTrade);
            int tradeItemCount = TradeRegistry.decodeItemCount(sellTrade);
            int baseEmeralds = (emeralds * count + tradeItemCount - 1) / tradeItemCount;
            return Math.max(1, (int) Math.ceil(baseEmeralds * ModConfig.purchasePriceMultiplier * 1.5));
        }

        return 0;
    }

    public static int calculateBuyPrice(ItemStack stack, int count) {
        if (ModConfig.enableEnchantedBookTrading && stack.is(Items.ENCHANTED_BOOK)) {
            int perItem = calculateEnchantedBookPrice(stack);
            if (perItem > 0) return perItem * count;
        }
        return calculateBuyPrice(stack.getItem(), count);
    }

    private static int calculateEnchantedBookPrice(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) return 0;

        int configTotal = 0;
        int baseTotal = 0;
        boolean hasConfig = false;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            String enchId = BuiltInRegistries.ENCHANTMENT.getKey(enchantment) != null
                    ? BuiltInRegistries.ENCHANTMENT.getKey(enchantment).toString()
                    : "";
            Integer configPrice = ModConfig.enchantedBookBuyPrices.get(enchId);
            if (configPrice != null) {
                configTotal += configPrice;
                hasConfig = true;
            } else {
                baseTotal += ModConfig.enchantedBookBasePrice * level;
            }
        }
        if (hasConfig && baseTotal == 0) {
            return Math.max(1, configTotal);
        }
        return Math.max(1, configTotal + (int) Math.ceil(baseTotal * ModConfig.purchasePriceMultiplier));
    }

    public static boolean isBuyableStack(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            if (!ModConfig.enableEnchantedBookTrading) return false;
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
            return !enchantments.isEmpty();
        }
        return isTradeable(stack.getItem());
    }

    public static int[] getSellTradeRatio(Item item) {
        int[] customSell = getCustomSellPrice(item);
        if (customSell != null) {
            return new int[]{customSell[0], customSell[1]};
        }

        int vanillaTrade = TradeRegistry.getSellTrade(item);
        if (vanillaTrade != -1) {
            int emeralds = TradeRegistry.decodeEmeralds(vanillaTrade);
            int itemCount = TradeRegistry.decodeItemCount(vanillaTrade);
            int adjustedEmeralds = Math.max(0, emeralds - ModConfig.sellingEmeraldPenalty);
            if (adjustedEmeralds <= 0) {
                if (emeralds > 0) {
                    int newItemCount = (int) Math.ceil((double) itemCount / emeralds * (emeralds + ModConfig.sellingEmeraldPenalty));
                    return new int[]{1, Math.min(newItemCount, 64)};
                }
                return null;
            }
            return new int[]{adjustedEmeralds, itemCount};
        }

        int foodPrice = calculateFoodPricePerItem(item);
        if (foodPrice > 0) {
            int adjusted = Math.max(0, foodPrice - ModConfig.sellingEmeraldPenalty);
            if (adjusted > 0) return new int[]{adjusted, 1};
            return new int[]{1, 2};
        }

        int toolPrice = calculateToolPricePerItem(item);
        if (toolPrice > 0) {
            int adjusted = Math.max(0, toolPrice - ModConfig.sellingEmeraldPenalty);
            if (adjusted > 0) return new int[]{adjusted, 1};
            return null;
        }

        return null;
    }

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
        FoodProperties food = item.getFoodProperties();
        return food != null && food.getNutrition() > 0;
    }

    private static int calculateFoodPricePerItem(Item item) {
        FoodProperties food = item.getFoodProperties();
        if (food == null) return 0;
        int nutrition = food.getNutrition();
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

    private static int[] getCustomSellPrice(Item item) {
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        return ModConfig.customSellPrices.get(itemId);
    }

    // --- Potion pricing ---

    private static boolean isPotionItem(Item item) {
        return item == Items.POTION || item == Items.SPLASH_POTION
                || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW;
    }

    private static int calculatePotionSellPrice(ItemStack stack) {
        if (!isPotionItem(stack.getItem())) return 0;
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        if (effects.isEmpty()) return 0;

        int pricePerItem = 0;
        for (MobEffectInstance effect : effects) {
            MobEffect mobEffect = effect.getEffect();
            if (mobEffect.getCategory() == MobEffectCategory.HARMFUL) continue;

            int amplifier = effect.getAmplifier() + 1;
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

    private static int calculateEnchantedBookSellPrice(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) return 0;

        int totalPrice = 0;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            int level = entry.getValue();
            totalPrice += ModConfig.enchantedBookBasePrice * level;
        }
        return totalPrice;
    }
}
