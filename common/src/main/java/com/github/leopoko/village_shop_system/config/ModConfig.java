package com.github.leopoko.village_shop_system.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod configuration with JSON file persistence.
 * All fields are static for easy access throughout the codebase.
 * Load/save uses Gson and Architectury's Platform.getConfigFolder().
 */
public final class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("village_shop_system");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "village_shop_system.json";

    // === Villager AI ===
    public static int villagerSearchRange = 32;
    public static int villagerShoppingCooldownTicks = 2400; // 2 minutes
    public static int minShelvesToVisit = 1;
    public static int maxShelvesToVisit = 3;
    public static int villagerBudgetPerTrade = 20;
    public static int villagerTradeXp = 1;

    // === Trade Rates ===
    public static int sellingEmeraldPenalty = 1;
    public static double purchasePriceMultiplier = 1.2;
    public static double foodPricePerHunger = 0.25;
    public static int potionBasePrice = 3;
    public static double toolDurabilityFactor = 0.005;
    public static double toolSpeedFactor = 0.5;
    public static int enchantedBookBasePrice = 5;

    // === Behavior ===
    public static int minRestTicks = 100;
    public static int maxRestTicks = 200;
    public static boolean pauseCooldownWhileSleeping = true;
    public static boolean villagerEatingEffects = true;
    public static double shoppingRushChance = 0.05; // 5% chance to trigger shopping rush

    private ModConfig() {}

    /**
     * Internal data class for Gson serialization/deserialization.
     */
    private static class ConfigData {
        // Villager AI
        int villagerSearchRange = 32;
        int villagerShoppingCooldownTicks = 2400;
        int minShelvesToVisit = 1;
        int maxShelvesToVisit = 3;
        int villagerBudgetPerTrade = 20;
        int villagerTradeXp = 1;

        // Trade Rates
        int sellingEmeraldPenalty = 1;
        double purchasePriceMultiplier = 1.2;
        double foodPricePerHunger = 0.25;
        int potionBasePrice = 3;
        double toolDurabilityFactor = 0.005;
        double toolSpeedFactor = 0.5;
        int enchantedBookBasePrice = 5;

        // Behavior
        int minRestTicks = 100;
        int maxRestTicks = 200;
        boolean pauseCooldownWhileSleeping = true;
        boolean villagerEatingEffects = true;
        double shoppingRushChance = 0.05;
    }

    /**
     * Load config from JSON file. Creates defaults if file doesn't exist.
     */
    public static void load() {
        Path configPath = Platform.getConfigFolder().resolve(CONFIG_FILE_NAME);
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                if (data != null) {
                    applyFromData(data);
                    LOGGER.info("Loaded config from {}", configPath);
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                LOGGER.error("Failed to load config, using defaults", e);
            }
        } else {
            // Save defaults on first run
            save();
            LOGGER.info("Created default config at {}", configPath);
        }
    }

    /**
     * Save current config values to JSON file.
     */
    public static void save() {
        Path configPath = Platform.getConfigFolder().resolve(CONFIG_FILE_NAME);
        ConfigData data = new ConfigData();
        data.villagerSearchRange = villagerSearchRange;
        data.villagerShoppingCooldownTicks = villagerShoppingCooldownTicks;
        data.minShelvesToVisit = minShelvesToVisit;
        data.maxShelvesToVisit = maxShelvesToVisit;
        data.villagerBudgetPerTrade = villagerBudgetPerTrade;
        data.villagerTradeXp = villagerTradeXp;
        data.sellingEmeraldPenalty = sellingEmeraldPenalty;
        data.purchasePriceMultiplier = purchasePriceMultiplier;
        data.foodPricePerHunger = foodPricePerHunger;
        data.potionBasePrice = potionBasePrice;
        data.toolDurabilityFactor = toolDurabilityFactor;
        data.toolSpeedFactor = toolSpeedFactor;
        data.enchantedBookBasePrice = enchantedBookBasePrice;
        data.minRestTicks = minRestTicks;
        data.maxRestTicks = maxRestTicks;
        data.pauseCooldownWhileSleeping = pauseCooldownWhileSleeping;
        data.villagerEatingEffects = villagerEatingEffects;
        data.shoppingRushChance = shoppingRushChance;

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * Apply values from deserialized ConfigData to static fields.
     */
    private static void applyFromData(ConfigData data) {
        villagerSearchRange = data.villagerSearchRange;
        villagerShoppingCooldownTicks = data.villagerShoppingCooldownTicks;
        minShelvesToVisit = data.minShelvesToVisit;
        maxShelvesToVisit = data.maxShelvesToVisit;
        villagerBudgetPerTrade = data.villagerBudgetPerTrade;
        villagerTradeXp = data.villagerTradeXp;
        sellingEmeraldPenalty = data.sellingEmeraldPenalty;
        purchasePriceMultiplier = data.purchasePriceMultiplier;
        foodPricePerHunger = data.foodPricePerHunger;
        potionBasePrice = data.potionBasePrice;
        toolDurabilityFactor = data.toolDurabilityFactor;
        toolSpeedFactor = data.toolSpeedFactor;
        enchantedBookBasePrice = data.enchantedBookBasePrice;
        minRestTicks = data.minRestTicks;
        maxRestTicks = data.maxRestTicks;
        pauseCooldownWhileSleeping = data.pauseCooldownWhileSleeping;
        villagerEatingEffects = data.villagerEatingEffects;
        shoppingRushChance = data.shoppingRushChance;
    }
}
