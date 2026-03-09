package com.github.leopoko.village_shop_system.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Config screen builder using Cloth Config API.
 * Creates a categorized GUI for all mod settings.
 */
public final class ModConfigScreen {

    private ModConfigScreen() {}

    /**
     * Create the config screen with all entries.
     * @param parent the parent screen to return to
     * @return the config screen
     */
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.village_shop_system.title"))
                .setSavingRunnable(() -> ModConfig.save());

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // === Villager AI Category ===
        ConfigCategory villagerAI = builder.getOrCreateCategory(
                Component.translatable("config.village_shop_system.category.villager_ai"));

        villagerAI.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.villager_search_range"),
                        ModConfig.villagerSearchRange)
                .setDefaultValue(32)
                .setMin(8).setMax(128)
                .setTooltip(Component.translatable("config.village_shop_system.villager_search_range.tooltip"))
                .setSaveConsumer(val -> ModConfig.villagerSearchRange = val)
                .build());

        villagerAI.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.villager_shopping_cooldown"),
                        ModConfig.villagerShoppingCooldownTicks)
                .setDefaultValue(2400)
                .setMin(200).setMax(12000)
                .setTooltip(Component.translatable("config.village_shop_system.villager_shopping_cooldown.tooltip"))
                .setSaveConsumer(val -> ModConfig.villagerShoppingCooldownTicks = val)
                .build());

        villagerAI.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.min_shelves_to_visit"),
                        ModConfig.minShelvesToVisit)
                .setDefaultValue(1)
                .setMin(1).setMax(10)
                .setTooltip(Component.translatable("config.village_shop_system.min_shelves_to_visit.tooltip"))
                .setSaveConsumer(val -> ModConfig.minShelvesToVisit = val)
                .build());

        villagerAI.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.max_shelves_to_visit"),
                        ModConfig.maxShelvesToVisit)
                .setDefaultValue(3)
                .setMin(1).setMax(10)
                .setTooltip(Component.translatable("config.village_shop_system.max_shelves_to_visit.tooltip"))
                .setSaveConsumer(val -> ModConfig.maxShelvesToVisit = val)
                .build());

        villagerAI.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.villager_budget"),
                        ModConfig.villagerBudgetPerTrade)
                .setDefaultValue(20)
                .setMin(1).setMax(128)
                .setTooltip(Component.translatable("config.village_shop_system.villager_budget.tooltip"))
                .setSaveConsumer(val -> ModConfig.villagerBudgetPerTrade = val)
                .build());

        villagerAI.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.villager_trade_xp"),
                        ModConfig.villagerTradeXp)
                .setDefaultValue(1)
                .setMin(0).setMax(50)
                .setTooltip(Component.translatable("config.village_shop_system.villager_trade_xp.tooltip"))
                .setSaveConsumer(val -> ModConfig.villagerTradeXp = val)
                .build());

        // === Trade Rates Category ===
        ConfigCategory tradeRates = builder.getOrCreateCategory(
                Component.translatable("config.village_shop_system.category.trade_rates"));

        tradeRates.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.selling_emerald_penalty"),
                        ModConfig.sellingEmeraldPenalty)
                .setDefaultValue(1)
                .setMin(0).setMax(10)
                .setTooltip(Component.translatable("config.village_shop_system.selling_emerald_penalty.tooltip"))
                .setSaveConsumer(val -> ModConfig.sellingEmeraldPenalty = val)
                .build());

        tradeRates.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.village_shop_system.purchase_price_multiplier"),
                        ModConfig.purchasePriceMultiplier)
                .setDefaultValue(1.2)
                .setMin(0.5).setMax(5.0)
                .setTooltip(Component.translatable("config.village_shop_system.purchase_price_multiplier.tooltip"))
                .setSaveConsumer(val -> ModConfig.purchasePriceMultiplier = val)
                .build());

        tradeRates.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.village_shop_system.food_price_per_hunger"),
                        ModConfig.foodPricePerHunger)
                .setDefaultValue(0.25)
                .setMin(0.05).setMax(2.0)
                .setTooltip(Component.translatable("config.village_shop_system.food_price_per_hunger.tooltip"))
                .setSaveConsumer(val -> ModConfig.foodPricePerHunger = val)
                .build());

        tradeRates.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.potion_base_price"),
                        ModConfig.potionBasePrice)
                .setDefaultValue(3)
                .setMin(1).setMax(64)
                .setTooltip(Component.translatable("config.village_shop_system.potion_base_price.tooltip"))
                .setSaveConsumer(val -> ModConfig.potionBasePrice = val)
                .build());

        tradeRates.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.village_shop_system.tool_durability_factor"),
                        ModConfig.toolDurabilityFactor)
                .setDefaultValue(0.005)
                .setMin(0.001).setMax(0.1)
                .setTooltip(Component.translatable("config.village_shop_system.tool_durability_factor.tooltip"))
                .setSaveConsumer(val -> ModConfig.toolDurabilityFactor = val)
                .build());

        tradeRates.addEntry(entryBuilder.startDoubleField(
                        Component.translatable("config.village_shop_system.tool_speed_factor"),
                        ModConfig.toolSpeedFactor)
                .setDefaultValue(0.5)
                .setMin(0.1).setMax(5.0)
                .setTooltip(Component.translatable("config.village_shop_system.tool_speed_factor.tooltip"))
                .setSaveConsumer(val -> ModConfig.toolSpeedFactor = val)
                .build());

        // === Behavior Category ===
        ConfigCategory behavior = builder.getOrCreateCategory(
                Component.translatable("config.village_shop_system.category.behavior"));

        behavior.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.min_rest_ticks"),
                        ModConfig.minRestTicks)
                .setDefaultValue(100)
                .setMin(20).setMax(1200)
                .setTooltip(Component.translatable("config.village_shop_system.min_rest_ticks.tooltip"))
                .setSaveConsumer(val -> ModConfig.minRestTicks = val)
                .build());

        behavior.addEntry(entryBuilder.startIntField(
                        Component.translatable("config.village_shop_system.max_rest_ticks"),
                        ModConfig.maxRestTicks)
                .setDefaultValue(200)
                .setMin(40).setMax(2400)
                .setTooltip(Component.translatable("config.village_shop_system.max_rest_ticks.tooltip"))
                .setSaveConsumer(val -> ModConfig.maxRestTicks = val)
                .build());

        behavior.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.village_shop_system.pause_cooldown_while_sleeping"),
                        ModConfig.pauseCooldownWhileSleeping)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.village_shop_system.pause_cooldown_while_sleeping.tooltip"))
                .setSaveConsumer(val -> ModConfig.pauseCooldownWhileSleeping = val)
                .build());

        return builder.build();
    }
}
