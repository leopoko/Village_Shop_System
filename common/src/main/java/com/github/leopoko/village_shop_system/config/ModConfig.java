package com.github.leopoko.village_shop_system.config;

/**
 * Mod configuration. Currently uses static defaults.
 * TODO: Integrate with a proper config library (e.g., Cloth Config via Architectury)
 */
public final class ModConfig {
    // Villager AI
    public static int villagerSearchRange = 32;
    public static int villagerShoppingCooldownTicks = 2400; // 2 minutes

    // Trade rates
    public static int sellingEmeraldPenalty = 1;        // Emerald -1 for non-matching trades
    public static double purchasePriceMultiplier = 1.2;  // Buy price × 1.2

    // Food pricing: emeralds per hunger points
    public static double foodPricePerHunger = 0.25;      // 4 hunger = 1 emerald

    // Potion pricing base
    public static int potionBasePrice = 3;

    // Tool pricing factors
    public static double toolDurabilityFactor = 0.005;
    public static double toolSpeedFactor = 0.5;

    // Chair/rest timing
    public static int minRestTicks = 100;
    public static int maxRestTicks = 200;

    // Shopping behavior
    public static int minShelvesToVisit = 1;
    public static int maxShelvesToVisit = 3;

    private ModConfig() {}
}
