package com.github.leopoko.village_shop_system.network;

import com.github.leopoko.village_shop_system.Village_shop_system;
import com.github.leopoko.village_shop_system.blockentity.RegisterBlockEntity;
import com.github.leopoko.village_shop_system.blockentity.SellingShelfBBlockEntity;
import com.github.leopoko.village_shop_system.item.ChairSettingStick;
import com.github.leopoko.village_shop_system.registry.ModItems;
import com.github.leopoko.village_shop_system.shopgroup.ShopGroupManager;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Network packet registration and handling.
 * Uses Architectury NetworkManager for cross-loader compatibility.
 */
public final class ModPackets {
    /** C2S: Client sends shop group name update for a block entity */
    public static final ResourceLocation SHOP_GROUP_UPDATE = ResourceLocation.fromNamespaceAndPath(
            Village_shop_system.MOD_ID, "shop_group_update");

    /** C2S: Client sends shop group name update for the chair setting stick */
    public static final ResourceLocation STICK_GROUP_UPDATE = ResourceLocation.fromNamespaceAndPath(
            Village_shop_system.MOD_ID, "stick_group_update");

    private ModPackets() {}

    public static void register() {
        // C2S: Shop group name update
        NetworkManager.registerReceiver(NetworkManager.c2s(), SHOP_GROUP_UPDATE, (buf, context) -> {
            BlockPos pos = buf.readBlockPos();
            String groupName = buf.readUtf(64);

            context.queue(() -> {
                ServerPlayer player = (ServerPlayer) context.getPlayer();
                ServerLevel level = player.serverLevel();

                // Validate player is close enough
                if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                    return;
                }

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof SellingShelfBBlockEntity shelfB) {
                    String oldGroup = shelfB.getShopGroup();
                    if (!oldGroup.isEmpty()) {
                        ShopGroupManager.get(level).removeSellingShelfB(oldGroup, pos);
                    }
                    shelfB.setShopGroup(groupName);
                    if (!groupName.isEmpty()) {
                        ShopGroupManager.get(level).addSellingShelfB(groupName, pos);
                    }
                } else if (be instanceof RegisterBlockEntity register) {
                    String oldGroup = register.getShopGroup();
                    if (!oldGroup.isEmpty()) {
                        ShopGroupManager.get(level).removeRegister(oldGroup, pos);
                    }
                    register.setShopGroup(groupName);
                    if (!groupName.isEmpty()) {
                        ShopGroupManager.get(level).addRegister(groupName, pos);
                    }
                }
            });
        });

        // C2S: Stick shop group name update
        NetworkManager.registerReceiver(NetworkManager.c2s(), STICK_GROUP_UPDATE, (buf, context) -> {
            String groupName = buf.readUtf(64);

            context.queue(() -> {
                ServerPlayer player = (ServerPlayer) context.getPlayer();
                // Find the chair setting stick in player's hands
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();
                ItemStack stick = null;
                if (mainHand.is(ModItems.CHAIR_SETTING_STICK.get())) {
                    stick = mainHand;
                } else if (offHand.is(ModItems.CHAIR_SETTING_STICK.get())) {
                    stick = offHand;
                }
                if (stick != null) {
                    ChairSettingStick.setShopGroup(stick, groupName);
                }
            });
        });
    }

    /**
     * Send shop group update from client to server for a block entity.
     */
    public static void sendShopGroupUpdate(RegistryAccess registryAccess, BlockPos pos, String groupName) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        buf.writeBlockPos(pos);
        buf.writeUtf(groupName, 64);
        NetworkManager.sendToServer(SHOP_GROUP_UPDATE, buf);
    }

    /**
     * Send shop group name update for the chair setting stick from client to server.
     */
    public static void sendStickGroupUpdate(RegistryAccess registryAccess, String groupName) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        buf.writeUtf(groupName, 64);
        NetworkManager.sendToServer(STICK_GROUP_UPDATE, buf);
    }
}
