package com.github.leopoko.village_shop_system.villager;

import com.github.leopoko.village_shop_system.blockentity.BaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.PurchaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBlockEntity;
import com.github.leopoko.village_shop_system.config.ModConfig;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroup;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroupManager;
import com.github.leopoko.village_shop_system.trade.TradePriceCalculator;
import com.github.leopoko.village_shop_system.trade.TradeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles villager shop interaction behavior.
 * Called from VillagerMixin during customServerAiStep.
 *
 * Flow:
 * 1. Every N ticks, search for nearby shelves
 * 2. Pick a random shelf with tradeable items
 * 3. Navigate to it
 * 4. When close enough, process the trade
 * 5. If shelf is part of a shop group (SellingShelfB), enter shopping mode:
 *    a. Visit 1-3 more shelves in the group
 *    b. Go to a register to "pay"
 *    c. Sit on a chair or stand at a position to rest
 * 6. Enter cooldown
 */
public class VillagerShopBehavior {
    private static final int SEARCH_INTERVAL = 200;
    private static final double INTERACTION_RANGE_SQ = 4.0;
    private static final double REST_SPOT_RANGE_SQ = 2.25; // 1.5 block horizontal radius for rest spots
    private static final int NAV_TIMEOUT = 600;
    private static final int REGISTER_AUTO_PAY_TIMEOUT = 200; // 10 seconds - auto-pay if stuck

    /** Items that villagers keep in inventory when purchased (farmer food sharing items). */
    private static final Set<Item> FARMER_FOOD_ITEMS = Set.of(
            Items.BREAD, Items.CARROT, Items.POTATO, Items.BEETROOT
    );

    /** Vanilla villager XP thresholds for level-up. Index = current level (1-based). */
    private static final int[] LEVEL_XP_THRESHOLDS = {0, 10, 70, 150, 250};

    private enum State {
        IDLE,
        NAVIGATING_TO_SHELF,
        SHOPPING_VISITING_SHELVES,
        SHOPPING_GOING_TO_REGISTER,
        SHOPPING_GOING_TO_REST,
        RESTING
    }

    private int cooldown = 0;
    private int navTimer = 0;
    private BlockPos target = null;
    private State state = State.IDLE;

    // Budget tracking: random budget for each trade session
    private int tradeBudget = 0;
    private int budgetSpent = 0;

    // Shopping state
    private String currentShopGroup = null;
    private int shelvesVisited = 0;
    private int shelvesToVisit = 0;
    private List<BlockPos> pendingShelves = null;

    // Register state
    private BlockPos registerTarget = null; // Actual register BlockPos (target is set to below)
    private int registerWaitTimer = 0;

    // Resting state
    private int restTimer = 0;
    private ArmorStand seatEntity = null;

    // Item holding state: villagers visually hold purchased items
    private ItemStack heldItem = ItemStack.EMPTY;
    private ItemStack lastPurchasedFood = ItemStack.EMPTY;

    /**
     * Called every tick from the villager's customServerAiStep.
     */
    public void tick(Villager villager) {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return;

        // Baby villagers don't use shops
        if (villager.isBaby()) return;

        // Sleeping villagers don't shop
        if (villager.isSleeping()) {
            // Optionally still decrement cooldown during sleep (when pause is disabled)
            if (!ModConfig.pauseCooldownWhileSleeping && cooldown > 0) {
                cooldown--;
            }
            return;
        }

        // Initialize trade registry lazily
        if (!TradeRegistry.isInitialized()) {
            TradeRegistry.initialize(villager);
        }

        // Cooldown
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        switch (state) {
            case IDLE -> tickIdle(villager, serverLevel);
            case NAVIGATING_TO_SHELF -> tickNavigating(villager, serverLevel);
            case SHOPPING_VISITING_SHELVES -> tickShoppingVisitShelves(villager, serverLevel);
            case SHOPPING_GOING_TO_REGISTER -> tickRegisterNavigation(villager, serverLevel);
            case SHOPPING_GOING_TO_REST -> tickNavigatingToRestSpot(villager, serverLevel);
            case RESTING -> tickResting(villager, serverLevel);
        }
    }

    // --- IDLE: search for shelves ---

    private void tickIdle(Villager villager, ServerLevel level) {
        if (villager.tickCount % SEARCH_INTERVAL == 0) {
            findAndNavigateToShelf(villager, level);
        }
    }

    // --- NAVIGATING TO FIRST SHELF ---

    private void tickNavigating(Villager villager, ServerLevel level) {
        navTimer++;

        double distSq = villager.distanceToSqr(
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        if (distSq <= INTERACTION_RANGE_SQ) {
            interactWithShelf(villager, level, target);

            // Check if this shelf is part of a shop group -> start shopping flow
            BlockEntity be = level.getBlockEntity(target);
            if (be instanceof SellingShelfBBlockEntity shelfB && !shelfB.getShopGroup().isEmpty()) {
                startShoppingFlow(villager, level, shelfB.getShopGroup());
                return;
            }

            // Not a group shelf, just cooldown
            resetAll(villager);
            cooldown = getRandomCooldown(villager);
            return;
        }

        if (navTimer > NAV_TIMEOUT) {
            resetAll(villager);
            cooldown = SEARCH_INTERVAL;
            return;
        }

        if (navTimer % 40 == 0) {
            villager.getNavigation().moveTo(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
        }
    }

    // --- SHOPPING FLOW: visit shelves in group ---

    private void startShoppingFlow(Villager villager, ServerLevel level, String shopGroup) {
        this.currentShopGroup = shopGroup;
        this.shelvesVisited = 1;
        this.shelvesToVisit = ModConfig.minShelvesToVisit
                + villager.getRandom().nextInt(ModConfig.maxShelvesToVisit - ModConfig.minShelvesToVisit + 1);

        ShopGroupManager manager = ShopGroupManager.get(level);
        ShopGroup group = manager.getGroup(shopGroup);
        if (group == null) {
            resetAll(villager);
            cooldown = SEARCH_INTERVAL;
            return;
        }

        // Build list of shelves to visit (excluding current target)
        List<BlockPos> shelves = new ArrayList<>(group.getSellingShelfBPositions());
        shelves.remove(target);
        // RandomSource is not compatible with Collections.shuffle; use manual Fisher-Yates
        var rng = villager.getRandom();
        for (int i = shelves.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            BlockPos temp = shelves.get(i);
            shelves.set(i, shelves.get(j));
            shelves.set(j, temp);
        }
        this.pendingShelves = shelves;

        state = State.SHOPPING_VISITING_SHELVES;
        navTimer = 0;
        navigateToNextShoppingTarget(villager);
    }

    private void tickShoppingVisitShelves(Villager villager, ServerLevel level) {
        navTimer++;

        if (target == null) {
            goToRegister(villager, level);
            return;
        }

        double distSq = villager.distanceToSqr(
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        if (distSq <= INTERACTION_RANGE_SQ) {
            interactWithShelf(villager, level, target);
            shelvesVisited++;

            if (shelvesVisited >= shelvesToVisit || pendingShelves == null || pendingShelves.isEmpty()) {
                goToRegister(villager, level);
            } else {
                navigateToNextShoppingTarget(villager);
            }
            return;
        }

        if (navTimer > NAV_TIMEOUT) {
            goToRegister(villager, level);
            return;
        }

        if (navTimer % 40 == 0) {
            villager.getNavigation().moveTo(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
        }
    }

    private void navigateToNextShoppingTarget(Villager villager) {
        if (pendingShelves == null || pendingShelves.isEmpty()) {
            target = null;
            return;
        }

        target = pendingShelves.remove(0);
        navTimer = 0;
        villager.getNavigation().moveTo(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
    }

    // --- SHOPPING: go to register (navigate to one block below) ---

    private void goToRegister(Villager villager, ServerLevel level) {
        if (currentShopGroup == null) {
            goToRest(villager, level);
            return;
        }

        ShopGroupManager manager = ShopGroupManager.get(level);
        ShopGroup group = manager.getGroup(currentShopGroup);
        if (group == null || group.getRegisterPositions().isEmpty()) {
            // No register available - auto-pay and proceed
            flushEmeraldsToRegisters(level);
            goToRest(villager, level);
            return;
        }

        // Pick closest register
        registerTarget = findClosest(villager.blockPosition(), group.getRegisterPositions());
        if (registerTarget == null) {
            flushEmeraldsToRegisters(level);
            goToRest(villager, level);
            return;
        }

        // Navigate to one block BELOW the register (where the villager stands at the counter)
        target = registerTarget.below();
        state = State.SHOPPING_GOING_TO_REGISTER;
        navTimer = 0;
        registerWaitTimer = 0;
        villager.getNavigation().moveTo(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
    }

    /**
     * Register-specific navigation with auto-pay timeout.
     * If the villager can't reach the register within REGISTER_AUTO_PAY_TIMEOUT ticks
     * after pathfinding completes, emeralds are automatically transferred.
     */
    private void tickRegisterNavigation(Villager villager, ServerLevel level) {
        navTimer++;

        if (target == null) {
            autoPayAndProceed(villager, level);
            return;
        }

        double distSq = villager.distanceToSqr(
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        if (distSq <= INTERACTION_RANGE_SQ) {
            arriveAtRegister(villager, level);
            return;
        }

        if (navTimer > NAV_TIMEOUT) {
            autoPayAndProceed(villager, level);
            return;
        }

        PathNavigation nav = villager.getNavigation();
        if (nav.isDone()) {
            // Path complete but not close enough - force direct approach
            villager.getMoveControl().setWantedPosition(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.6);
            registerWaitTimer++;
            // If stuck for too long after path completes, auto-pay
            if (registerWaitTimer > REGISTER_AUTO_PAY_TIMEOUT) {
                autoPayAndProceed(villager, level);
            }
        } else if (navTimer % 40 == 0) {
            nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
        }
    }

    /**
     * Auto-pay: flush emeralds to registers and proceed to rest.
     * Called when the villager can't reach the register within the timeout.
     */
    private void autoPayAndProceed(Villager villager, ServerLevel level) {
        flushEmeraldsToRegisters(level);
        // Play effect at villager's position as feedback
        level.playSound(null, villager.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.NEUTRAL, 0.5f, 1.2f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                villager.getX(), villager.getY() + 1.5, villager.getZ(),
                3, 0.3, 0.3, 0.3, 0.02);
        // After checkout, prioritize holding food
        switchToFoodAfterCheckout(villager);
        goToRest(villager, level);
    }

    private void arriveAtRegister(Villager villager, ServerLevel level) {
        BlockPos effectPos = registerTarget != null ? registerTarget : target;
        level.playSound(null, effectPos, SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.NEUTRAL, 0.5f, 1.2f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                effectPos.getX() + 0.5, effectPos.getY() + 1.0, effectPos.getZ() + 0.5,
                3, 0.3, 0.3, 0.3, 0.02);
        // Flush any remaining local emeralds from shelves to registers
        flushEmeraldsToRegisters(level);
        // After checkout, prioritize holding food
        switchToFoodAfterCheckout(villager);
        goToRest(villager, level);
    }

    // --- SHOPPING: go to rest spot (filter occupied chairs) ---

    private void goToRest(Villager villager, ServerLevel level) {
        if (currentShopGroup == null) {
            resetAll(villager);
            cooldown = getRandomCooldown(villager);
            return;
        }

        ShopGroupManager manager = ShopGroupManager.get(level);
        ShopGroup group = manager.getGroup(currentShopGroup);
        if (group == null) {
            resetAll(villager);
            cooldown = getRandomCooldown(villager);
            return;
        }

        List<BlockPos> restSpots = new ArrayList<>();
        // Only add unoccupied chairs
        for (BlockPos chairPos : group.getChairPositions()) {
            if (!isChairOccupied(level, chairPos)) {
                restSpots.add(chairPos);
            }
        }
        // Add all standing positions
        restSpots.addAll(group.getStandingPositions());

        if (restSpots.isEmpty()) {
            resetAll(villager);
            cooldown = getRandomCooldown(villager);
            return;
        }

        target = findClosest(villager.blockPosition(), restSpots);
        if (target == null) {
            resetAll(villager);
            cooldown = getRandomCooldown(villager);
            return;
        }

        state = State.SHOPPING_GOING_TO_REST;
        navTimer = 0;
        villager.getNavigation().moveTo(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
    }

    /**
     * Navigation for rest spots using horizontal-only distance for arrival check.
     * Standing positions stored as pos.above() can have Y offset from the villager's actual Y,
     * so using only horizontal distance avoids issues with Y coordinate mismatches.
     */
    private void tickNavigatingToRestSpot(Villager villager, ServerLevel level) {
        navTimer++;

        if (target == null) {
            resetAll(villager);
            cooldown = SEARCH_INTERVAL;
            return;
        }

        // Use horizontal (XZ) distance for rest spot arrival
        double dx = villager.getX() - (target.getX() + 0.5);
        double dz = villager.getZ() - (target.getZ() + 0.5);
        double horizontalDistSq = dx * dx + dz * dz;

        if (horizontalDistSq <= REST_SPOT_RANGE_SQ) {
            arriveAtRestSpot(villager, level);
            return;
        }

        if (navTimer > NAV_TIMEOUT) {
            // Couldn't reach rest spot, just end the shopping session
            resetAll(villager);
            cooldown = getRandomCooldown(villager);
            return;
        }

        PathNavigation nav = villager.getNavigation();
        if (nav.isDone()) {
            // Path complete but not close enough - force direct approach
            villager.getMoveControl().setWantedPosition(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.6);
        } else if (navTimer % 40 == 0) {
            nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
        }
    }

    private void arriveAtRestSpot(Villager villager, ServerLevel level) {
        boolean isChair = false;
        if (currentShopGroup != null) {
            ShopGroupManager manager = ShopGroupManager.get(level);
            ShopGroup group = manager.getGroup(currentShopGroup);
            if (group != null) {
                isChair = group.getChairPositions().contains(target);
            }
        }

        if (isChair) {
            // Double-check the chair isn't occupied (another villager may have sat while we walked)
            if (isChairOccupied(level, target)) {
                // Chair was taken - find another rest spot
                goToRest(villager, level);
                return;
            }
            sitDown(villager, level, target);
        }

        state = State.RESTING;
        restTimer = ModConfig.minRestTicks
                + villager.getRandom().nextInt(ModConfig.maxRestTicks - ModConfig.minRestTicks + 1);
    }

    // --- RESTING ---

    private void tickResting(Villager villager, ServerLevel level) {
        restTimer--;

        // For standing positions (no seat entity), continuously stop navigation
        // to prevent vanilla AI from making the villager wander away
        if (seatEntity == null) {
            villager.getNavigation().stop();
        }

        if (restTimer <= 0) {
            standUp(villager);
            resetAll(villager);
            cooldown = getRandomCooldown(villager);
        }
    }

    // --- CHAIR SITTING ---

    private void sitDown(Villager villager, ServerLevel level, BlockPos chairPos) {
        ArmorStand seat = new ArmorStand(EntityType.ARMOR_STAND, level);
        seat.setPos(chairPos.getX() + 0.5, chairPos.getY() + 0.2, chairPos.getZ() + 0.5);
        seat.setInvisible(true);
        seat.setNoGravity(true);
        seat.setSilent(true);
        seat.setInvulnerable(true);
        // Set Marker flag via NBT to remove hitbox (setMarker is private)
        CompoundTag tag = new CompoundTag();
        seat.saveWithoutId(tag);
        tag.putBoolean("Marker", true);
        tag.putBoolean("Small", true);
        seat.load(tag);

        level.addFreshEntity(seat);
        villager.startRiding(seat, true);
        this.seatEntity = seat;
    }

    private void standUp(Villager villager) {
        if (villager != null) {
            villager.stopRiding();
        }
        if (seatEntity != null && seatEntity.isAlive()) {
            seatEntity.discard();
        }
        seatEntity = null;
    }

    // --- SHELF SEARCH ---

    private void findAndNavigateToShelf(Villager villager, ServerLevel level) {
        int range = ModConfig.villagerSearchRange;
        BlockPos villagerPos = villager.blockPosition();
        double rangeSq = range * range;

        List<BlockPos> candidates = new ArrayList<>();

        int chunkMinX = (villagerPos.getX() - range) >> 4;
        int chunkMaxX = (villagerPos.getX() + range) >> 4;
        int chunkMinZ = (villagerPos.getZ() - range) >> 4;
        int chunkMaxZ = (villagerPos.getZ() + range) >> 4;

        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                var chunk = level.getChunk(cx, cz);

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (pos.distSqr(villagerPos) > rangeSq) continue;

                    if (be instanceof SellingShelfBlockEntity shelf) {
                        if (shelf.hasTradeableItems() && shelf.hasOutputSpace()) {
                            candidates.add(pos);
                        }
                    } else if (be instanceof SellingShelfBBlockEntity shelfB) {
                        if (shelfB.hasTradeableItems() && shelfB.hasOutputSpace()) {
                            candidates.add(pos);
                        }
                    } else if (be instanceof PurchaseShelfBlockEntity purchase) {
                        if (purchase.canTrade()) {
                            candidates.add(pos);
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) return;

        target = candidates.get(villager.getRandom().nextInt(candidates.size()));
        state = State.NAVIGATING_TO_SHELF;
        navTimer = 0;

        // Set random budget for this trade session (50% to 150% of config value)
        int baseBudget = ModConfig.villagerBudgetPerTrade;
        tradeBudget = baseBudget / 2 + villager.getRandom().nextInt(baseBudget + 1);
        budgetSpent = 0;

        villager.getNavigation().moveTo(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
    }

    // --- SHELF INTERACTION ---

    private int interactWithShelf(Villager villager, ServerLevel level, BlockPos shelfPos) {
        BlockEntity be = level.getBlockEntity(shelfPos);
        int result = 0;
        int remainingBudget = Math.max(0, tradeBudget - budgetSpent);

        if (remainingBudget <= 0) return 0; // Budget exhausted

        // Determine display item and process trade
        ItemStack displayItem = ItemStack.EMPTY;

        if (be instanceof SellingShelfBlockEntity shelf) {
            // Snapshot farmer food before trade for inventory tracking
            Map<Item, Integer> farmerFoodBefore = snapshotFarmerFood(shelf);
            displayItem = getFirstTradeableItem(shelf);
            result = shelf.processTrades(remainingBudget);
            if (result > 0) {
                addConsumedFarmerFoodToInventory(villager, shelf, farmerFoodBefore);
            }
        } else if (be instanceof SellingShelfBBlockEntity shelfB) {
            Map<Item, Integer> farmerFoodBefore = snapshotFarmerFood(shelfB);
            displayItem = getFirstTradeableItem(shelfB);
            result = shelfB.processTrades(remainingBudget);
            if (result > 0) {
                addConsumedFarmerFoodToInventory(villager, shelfB, farmerFoodBefore);
            }
        } else if (be instanceof PurchaseShelfBlockEntity purchase) {
            displayItem = purchase.getConfiguredItem().copy();
            result = purchase.processPurchases();
            // For purchase shelves, add farmer food items to villager inventory
            if (result > 0 && !displayItem.isEmpty() && FARMER_FOOD_ITEMS.contains(displayItem.getItem())) {
                villager.getInventory().addItem(new ItemStack(displayItem.getItem(), result));
            }
        }

        if (result > 0) {
            budgetSpent += result;

            // Update held item (villager visually holds what they bought)
            if (!displayItem.isEmpty()) {
                setHeldItem(villager, new ItemStack(displayItem.getItem(), 1));
                // Track food items for post-checkout display
                if (isFood(displayItem.getItem())) {
                    lastPurchasedFood = new ItemStack(displayItem.getItem(), 1);
                }
            }

            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + 1.5, villager.getZ(),
                    5, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, shelfPos, SoundEvents.VILLAGER_TRADE,
                    SoundSource.NEUTRAL, 1.0f, 1.0f);
            // Add configurable XP and check for level-up
            villager.setVillagerXp(villager.getVillagerXp() + ModConfig.villagerTradeXp);
            tryLevelUp(villager, level);
        }

        return result;
    }

    // --- ITEM HOLDING HELPERS ---

    /**
     * Set the item the villager is visually holding in their main hand.
     */
    private void setHeldItem(Villager villager, ItemStack item) {
        this.heldItem = item.copy();
        villager.setItemSlot(EquipmentSlot.MAINHAND, item);
    }

    /**
     * Clear the held item from the villager's hand.
     */
    private void clearHeldItem(Villager villager) {
        this.heldItem = ItemStack.EMPTY;
        this.lastPurchasedFood = ItemStack.EMPTY;
        if (villager != null) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    /**
     * After checkout (register arrival or auto-pay), switch to holding food if any was purchased.
     */
    private void switchToFoodAfterCheckout(Villager villager) {
        if (!lastPurchasedFood.isEmpty()) {
            setHeldItem(villager, lastPurchasedFood.copy());
        }
    }

    /**
     * Check if an item is a food item.
     */
    private static boolean isFood(Item item) {
        ItemStack testStack = new ItemStack(item);
        return testStack.has(net.minecraft.core.component.DataComponents.FOOD);
    }

    /**
     * Get the first tradeable item in the shelf's input slots (for display purposes).
     */
    private static ItemStack getFirstTradeableItem(BaseShelfBlockEntity shelf) {
        for (int i = 0; i < BaseShelfBlockEntity.INPUT_SLOTS; i++) {
            ItemStack stack = shelf.getItem(i);
            if (!stack.isEmpty() && TradePriceCalculator.isSellable(stack.getItem())) {
                return new ItemStack(stack.getItem(), 1);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Snapshot farmer food item counts in the shelf before a trade.
     */
    private static Map<Item, Integer> snapshotFarmerFood(BaseShelfBlockEntity shelf) {
        Map<Item, Integer> counts = new HashMap<>();
        for (int i = 0; i < BaseShelfBlockEntity.INPUT_SLOTS; i++) {
            ItemStack stack = shelf.getItem(i);
            if (!stack.isEmpty() && FARMER_FOOD_ITEMS.contains(stack.getItem())) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    /**
     * After a trade, compare snapshot with current state and add consumed farmer food
     * to the villager's inventory.
     */
    private static void addConsumedFarmerFoodToInventory(Villager villager,
                                                          BaseShelfBlockEntity shelf,
                                                          Map<Item, Integer> before) {
        Map<Item, Integer> after = snapshotFarmerFood(shelf);
        for (Map.Entry<Item, Integer> entry : before.entrySet()) {
            int consumed = entry.getValue() - after.getOrDefault(entry.getKey(), 0);
            if (consumed > 0) {
                villager.getInventory().addItem(new ItemStack(entry.getKey(), consumed));
            }
        }
    }

    // --- LEVEL-UP ---

    /**
     * Check if the villager has enough XP to level up, and perform level-up if so.
     * Updates trade list and plays celebration effects.
     */
    private static void tryLevelUp(Villager villager, ServerLevel level) {
        int currentLevel = villager.getVillagerData().getLevel();
        if (currentLevel >= 5) return; // Already max level

        if (currentLevel < LEVEL_XP_THRESHOLDS.length
                && villager.getVillagerXp() >= LEVEL_XP_THRESHOLDS[currentLevel]) {
            // Level up
            villager.setVillagerData(villager.getVillagerData().setLevel(currentLevel + 1));
            // Refresh trades to add new level's offers
            ((ShopBehaviorAccessor) villager).village_shop_system$updateTrades();
            // Celebration effects
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + 1.5, villager.getZ(),
                    10, 0.5, 0.5, 0.5, 0.1);
            level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_CELEBRATE,
                    SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
    }

    // --- UTILITIES ---

    /**
     * Get a randomized cooldown based on config value.
     * Returns config value +/- up to 50% random variation.
     */
    private static int getRandomCooldown(Villager villager) {
        int base = ModConfig.villagerShoppingCooldownTicks;
        int half = Math.max(1, base / 2);
        return base + villager.getRandom().nextInt(half);
    }

    private static BlockPos findClosest(BlockPos origin, List<BlockPos> positions) {
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (BlockPos pos : positions) {
            double dist = pos.distSqr(origin);
            if (dist < closestDist) {
                closestDist = dist;
                closest = pos;
            }
        }
        return closest;
    }

    /**
     * Check if a chair position is already occupied by an ArmorStand seat entity.
     */
    private static boolean isChairOccupied(ServerLevel level, BlockPos pos) {
        AABB aabb = new AABB(
                pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5,
                pos.getX() + 1.5, pos.getY() + 2.0, pos.getZ() + 1.5);
        return !level.getEntitiesOfClass(ArmorStand.class, aabb).isEmpty();
    }

    /**
     * Flush remaining local emeralds from all SellingShelfB in the current shop group
     * to registers. This ensures emeralds don't get stuck in shelf output slots
     * when the villager can't physically reach the register.
     */
    private void flushEmeraldsToRegisters(ServerLevel level) {
        if (currentShopGroup == null || currentShopGroup.isEmpty()) return;
        ShopGroupManager manager = ShopGroupManager.get(level);
        ShopGroup group = manager.getGroup(currentShopGroup);
        if (group == null) return;

        for (BlockPos shelfPos : group.getSellingShelfBPositions()) {
            if (!level.isLoaded(shelfPos)) continue;
            BlockEntity be = level.getBlockEntity(shelfPos);
            if (be instanceof SellingShelfBBlockEntity shelfB) {
                shelfB.flushLocalEmeraldsToRegisters();
            }
        }
    }

    private void resetAll(Villager villager) {
        if (seatEntity != null && seatEntity.isAlive()) {
            seatEntity.discard();
        }
        seatEntity = null;
        target = null;
        state = State.IDLE;
        navTimer = 0;
        currentShopGroup = null;
        shelvesVisited = 0;
        shelvesToVisit = 0;
        pendingShelves = null;
        registerTarget = null;
        registerWaitTimer = 0;
        restTimer = 0;
        tradeBudget = 0;
        budgetSpent = 0;
        // Clear visual held item when shopping session ends
        clearHeldItem(villager);
    }

    /**
     * Clean up when villager is removed (e.g., death, despawn).
     */
    public void cleanup(Villager villager) {
        standUp(villager);
        clearHeldItem(villager);
    }
}
