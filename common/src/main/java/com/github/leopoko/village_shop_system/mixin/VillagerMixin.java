package com.github.leopoko.village_shop_system.mixin;

import com.github.leopoko.village_shop_system.villager.ShopBehaviorAccessor;
import com.github.leopoko.village_shop_system.villager.VillagerShopBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into Villager to inject custom shop interaction behavior.
 * Adds VillagerShopBehavior field and ticks it during customServerAiStep.
 */
@Mixin(Villager.class)
public abstract class VillagerMixin implements ShopBehaviorAccessor {

    @Shadow
    protected abstract void updateTrades();

    @Unique
    private final VillagerShopBehavior village_shop_system$shopBehavior = new VillagerShopBehavior();

    @Override
    public VillagerShopBehavior village_shop_system$getShopBehavior() {
        return village_shop_system$shopBehavior;
    }

    @Override
    public void village_shop_system$updateTrades() {
        this.updateTrades();
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void village_shop_system$onCustomServerAiStep(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (self.isDeadOrDying() || self.isRemoved()) {
            village_shop_system$shopBehavior.cleanup(self);
        } else {
            village_shop_system$shopBehavior.tick(self);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void village_shop_system$onSaveData(CompoundTag tag, CallbackInfo ci) {
        tag.put("VillageShopSystem", village_shop_system$shopBehavior.save());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void village_shop_system$onLoadData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("VillageShopSystem")) {
            village_shop_system$shopBehavior.load(tag.getCompound("VillageShopSystem"));
        }
    }
}
