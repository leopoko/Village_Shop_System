package com.github.leopoko.village_shop_system.registry;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.entity.SeatEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Village_shop_system.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<SeatEntity>> SEAT =
            ENTITY_TYPES.register("seat", () ->
                    EntityType.Builder.of(SeatEntity::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .noSave()
                            .noSummon()
                            .fireImmune()
                            .build("seat"));

    public static void register() {
        ENTITY_TYPES.register();
    }

    private ModEntityTypes() {}
}
