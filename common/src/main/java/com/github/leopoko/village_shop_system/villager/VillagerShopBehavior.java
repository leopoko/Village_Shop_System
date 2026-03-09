package com.github.leopoko.village_shop_system.villager;

import com.github.leopoko.village_shop_system.blockentity.PurchaseShelfBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBlockEntity;
import com.github.leopoko.village_shop_system.config.ModConfig;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroup;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroupManager;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static final int NAV_TIMEOUT = 600;

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

    // Shopping state
    private String currentShopGroup = null;
    private int shelvesVisited = 0;
    private int shelvesToVisit = 0;
    private List<BlockPos> pendingShelves = null;

    // Resting state
    private int restTimer = 0;
    private ArmorStand seatEntity = null;

    /**
     * Called every tick from the villager's customServerAiStep.
     */
    public void tick(Villager villager) {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return;

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
            case SHOPPING_GOING_TO_REGISTER -> tickNavigatingToTarget(villager, serverLevel, this::arriveAtRegister);
            case SHOPPING_GOING_TO_REST -> tickNavigatingToTarget(villager, serverLevel, this::arriveAtRestSpot);
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
            resetAll();
            cooldown = ModConfig.villagerShoppingCooldownTicks
                    + villager.getRandom().nextInt(ModConfig.villagerShoppingCooldownTicks / 2);
            return;
        }

        if (navTimer > NAV_TIMEOUT) {
            resetAll();
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
            resetAll();
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

    // --- SHOPPING: go to register ---

    private void goToRegister(Villager villager, ServerLevel level) {
        if (currentShopGroup == null) {
            goToRest(villager, level);
            return;
        }

        ShopGroupManager manager = ShopGroupManager.get(level);
        ShopGroup group = manager.getGroup(currentShopGroup);
        if (group == null || group.getRegisterPositions().isEmpty()) {
            goToRest(villager, level);
            return;
        }

        // Pick closest register
        target = findClosest(villager.blockPosition(), group.getRegisterPositions());
        if (target == null) {
            goToRest(villager, level);
            return;
        }

        state = State.SHOPPING_GOING_TO_REGISTER;
        navTimer = 0;
        villager.getNavigation().moveTo(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
    }

    private void arriveAtRegister(Villager villager, ServerLevel level) {
        level.playSound(null, target, SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.NEUTRAL, 0.5f, 1.2f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5,
                3, 0.3, 0.3, 0.3, 0.02);
        goToRest(villager, level);
    }

    // --- SHOPPING: go to rest spot ---

    private void goToRest(Villager villager, ServerLevel level) {
        if (currentShopGroup == null) {
            resetAll();
            cooldown = ModConfig.villagerShoppingCooldownTicks;
            return;
        }

        ShopGroupManager manager = ShopGroupManager.get(level);
        ShopGroup group = manager.getGroup(currentShopGroup);
        if (group == null) {
            resetAll();
            cooldown = ModConfig.villagerShoppingCooldownTicks;
            return;
        }

        List<BlockPos> restSpots = new ArrayList<>();
        restSpots.addAll(group.getChairPositions());
        restSpots.addAll(group.getStandingPositions());

        if (restSpots.isEmpty()) {
            resetAll();
            cooldown = ModConfig.villagerShoppingCooldownTicks;
            return;
        }

        target = findClosest(villager.blockPosition(), restSpots);
        if (target == null) {
            resetAll();
            cooldown = ModConfig.villagerShoppingCooldownTicks;
            return;
        }

        state = State.SHOPPING_GOING_TO_REST;
        navTimer = 0;
        villager.getNavigation().moveTo(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
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
            sitDown(villager, level, target);
        }

        state = State.RESTING;
        restTimer = ModConfig.minRestTicks
                + villager.getRandom().nextInt(ModConfig.maxRestTicks - ModConfig.minRestTicks + 1);
    }

    // --- RESTING ---

    private void tickResting(Villager villager, ServerLevel level) {
        restTimer--;
        if (restTimer <= 0) {
            standUp(villager);
            resetAll();
            cooldown = ModConfig.villagerShoppingCooldownTicks
                    + villager.getRandom().nextInt(ModConfig.villagerShoppingCooldownTicks / 2);
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

    // --- GENERIC NAVIGATION HELPER ---

    @FunctionalInterface
    private interface ArrivalHandler {
        void onArrive(Villager villager, ServerLevel level);
    }

    private void tickNavigatingToTarget(Villager villager, ServerLevel level, ArrivalHandler onArrive) {
        navTimer++;

        if (target == null) {
            resetAll();
            cooldown = SEARCH_INTERVAL;
            return;
        }

        double distSq = villager.distanceToSqr(
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        if (distSq <= INTERACTION_RANGE_SQ) {
            onArrive.onArrive(villager, level);
            return;
        }

        if (navTimer > NAV_TIMEOUT) {
            resetAll();
            cooldown = SEARCH_INTERVAL;
            return;
        }

        if (navTimer % 40 == 0) {
            villager.getNavigation().moveTo(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
        }
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

        villager.getNavigation().moveTo(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.5);
    }

    // --- SHELF INTERACTION ---

    private int interactWithShelf(Villager villager, ServerLevel level, BlockPos shelfPos) {
        BlockEntity be = level.getBlockEntity(shelfPos);
        int result = 0;

        if (be instanceof SellingShelfBlockEntity shelf) {
            result = shelf.processTrades();
        } else if (be instanceof SellingShelfBBlockEntity shelfB) {
            result = shelfB.processTrades();
        } else if (be instanceof PurchaseShelfBlockEntity purchase) {
            result = purchase.processPurchases();
        }

        if (result > 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + 1.5, villager.getZ(),
                    5, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, shelfPos, SoundEvents.VILLAGER_TRADE,
                    SoundSource.NEUTRAL, 1.0f, 1.0f);
            villager.setVillagerXp(villager.getVillagerXp() + result);
        }

        return result;
    }

    // --- UTILITIES ---

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

    private void resetAll() {
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
        restTimer = 0;
    }

    /**
     * Clean up when villager is removed (e.g., death, despawn).
     */
    public void cleanup(Villager villager) {
        standUp(villager);
    }
}
