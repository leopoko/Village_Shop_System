package com.github.leopoko.village_shop_system.shopgroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manages all shop groups in the world.
 * Stored as SavedData on the Overworld level.
 */
public class ShopGroupManager extends SavedData {
    private static final String DATA_NAME = "village_shop_system_groups";

    private final Map<String, ShopGroup> groups = new HashMap<>();

    public ShopGroupManager() {}

    // --- Access ---

    @Nullable
    public ShopGroup getGroup(String name) {
        return groups.get(name);
    }

    public ShopGroup getOrCreateGroup(String name) {
        return groups.computeIfAbsent(name, ShopGroup::new);
    }

    public Collection<ShopGroup> getAllGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    public Set<String> getGroupNames() {
        return Collections.unmodifiableSet(groups.keySet());
    }

    // --- Selling Shelf B management ---

    public void addSellingShelfB(String groupName, BlockPos pos) {
        getOrCreateGroup(groupName).addSellingShelfB(pos);
        setDirty();
    }

    public void removeSellingShelfB(String groupName, BlockPos pos) {
        ShopGroup group = groups.get(groupName);
        if (group != null) {
            group.removeSellingShelfB(pos);
            cleanupEmptyGroup(groupName, group);
            setDirty();
        }
    }

    // --- Register management ---

    public void addRegister(String groupName, BlockPos pos) {
        getOrCreateGroup(groupName).addRegister(pos);
        setDirty();
    }

    public void removeRegister(String groupName, BlockPos pos) {
        ShopGroup group = groups.get(groupName);
        if (group != null) {
            group.removeRegister(pos);
            cleanupEmptyGroup(groupName, group);
            setDirty();
        }
    }

    // --- Chair management ---

    public void addChair(String groupName, BlockPos pos) {
        getOrCreateGroup(groupName).addChair(pos);
        setDirty();
    }

    public void removeChair(String groupName, BlockPos pos) {
        ShopGroup group = groups.get(groupName);
        if (group != null) {
            group.removeChair(pos);
            cleanupEmptyGroup(groupName, group);
            setDirty();
        }
    }

    // --- Standing position management ---

    public void addStandingPosition(String groupName, BlockPos pos) {
        getOrCreateGroup(groupName).addStandingPosition(pos);
        setDirty();
    }

    public void removeStandingPosition(String groupName, BlockPos pos) {
        ShopGroup group = groups.get(groupName);
        if (group != null) {
            group.removeStandingPosition(pos);
            cleanupEmptyGroup(groupName, group);
            setDirty();
        }
    }

    // --- Cleanup ---

    private void cleanupEmptyGroup(String name, ShopGroup group) {
        if (group.isEmpty()) {
            groups.remove(name);
        }
    }

    /**
     * Remove a block position from all groups (called when block is broken).
     */
    public void removeBlockFromAllGroups(BlockPos pos) {
        Iterator<Map.Entry<String, ShopGroup>> it = groups.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<String, ShopGroup> entry = it.next();
            ShopGroup group = entry.getValue();
            group.removeSellingShelfB(pos);
            group.removeRegister(pos);
            group.removeChair(pos);
            group.removeStandingPosition(pos);
            if (group.isEmpty()) {
                it.remove();
            }
            changed = true;
        }
        if (changed) setDirty();
    }

    // --- SavedData ---

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag groupList = new ListTag();
        for (ShopGroup group : groups.values()) {
            groupList.add(group.save());
        }
        tag.put("Groups", groupList);
        return tag;
    }

    private static ShopGroupManager load(CompoundTag tag, HolderLookup.Provider registries) {
        ShopGroupManager manager = new ShopGroupManager();
        ListTag groupList = tag.getList("Groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < groupList.size(); i++) {
            ShopGroup group = ShopGroup.load(groupList.getCompound(i));
            manager.groups.put(group.getName(), group);
        }
        return manager;
    }

    public static SavedData.Factory<ShopGroupManager> factory() {
        return new SavedData.Factory<>(
                ShopGroupManager::new,
                ShopGroupManager::load,
                DataFixTypes.LEVEL
        );
    }

    /**
     * Get the ShopGroupManager for the given server level.
     * Always retrieves from the Overworld data storage.
     */
    public static ShopGroupManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }
}
