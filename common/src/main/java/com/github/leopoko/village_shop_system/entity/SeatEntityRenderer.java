package com.github.leopoko.village_shop_system.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * No-op renderer for SeatEntity. Renders nothing.
 * Replaces NoopRenderer which doesn't exist in 1.20.1.
 */
public class SeatEntityRenderer extends EntityRenderer<SeatEntity> {

    public SeatEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SeatEntity entity) {
        return new ResourceLocation("missingno");
    }
}
