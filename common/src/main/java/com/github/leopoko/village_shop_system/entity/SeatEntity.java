package com.github.leopoko.village_shop_system.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Invisible, zero-size entity used solely as a seat for villagers resting in chairs.
 * Has no hitbox, no physics, no persistence — purely transient.
 */
public class SeatEntity extends Entity {

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
        this.setNoGravity(true);
        this.setSilent(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        // No synched data needed
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Not persisted
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Not persisted
    }

    @Override
    public void tick() {
        super.tick();
        // Auto-discard if no passenger (server-side only).
        // Must NOT run on client: the spawn packet may arrive before the
        // passenger packet, causing a false-positive discard.
        if (!this.level().isClientSide() && !this.isVehicle()) {
            this.discard();
        }
    }
}
