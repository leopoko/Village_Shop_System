package com.github.leopoko.village_shop_system.shopgroup;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data structure representing a shop group.
 * Contains positions of all linked blocks (selling shelves B, registers, chairs, standing positions).
 */
public class ShopGroup {
    private final String name;
    private final List<BlockPos> sellingShelfBPositions = new ArrayList<>();
    private final List<BlockPos> registerPositions = new ArrayList<>();
    private final List<BlockPos> chairPositions = new ArrayList<>();
    private final List<BlockPos> standingPositions = new ArrayList<>();

    public ShopGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // --- Selling Shelf B ---
    public List<BlockPos> getSellingShelfBPositions() {
        return Collections.unmodifiableList(sellingShelfBPositions);
    }

    public void addSellingShelfB(BlockPos pos) {
        if (!sellingShelfBPositions.contains(pos)) {
            sellingShelfBPositions.add(pos);
        }
    }

    public void removeSellingShelfB(BlockPos pos) {
        sellingShelfBPositions.remove(pos);
    }

    // --- Registers ---
    public List<BlockPos> getRegisterPositions() {
        return Collections.unmodifiableList(registerPositions);
    }

    public void addRegister(BlockPos pos) {
        if (!registerPositions.contains(pos)) {
            registerPositions.add(pos);
        }
    }

    public void removeRegister(BlockPos pos) {
        registerPositions.remove(pos);
    }

    // --- Chairs ---
    public List<BlockPos> getChairPositions() {
        return Collections.unmodifiableList(chairPositions);
    }

    public void addChair(BlockPos pos) {
        if (!chairPositions.contains(pos)) {
            chairPositions.add(pos);
        }
    }

    public void removeChair(BlockPos pos) {
        chairPositions.remove(pos);
    }

    // --- Standing Positions ---
    public List<BlockPos> getStandingPositions() {
        return Collections.unmodifiableList(standingPositions);
    }

    public void addStandingPosition(BlockPos pos) {
        if (!standingPositions.contains(pos)) {
            standingPositions.add(pos);
        }
    }

    public void removeStandingPosition(BlockPos pos) {
        standingPositions.remove(pos);
    }

    // --- NBT ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.put("SellingShelvesB", savePositions(sellingShelfBPositions));
        tag.put("Registers", savePositions(registerPositions));
        tag.put("Chairs", savePositions(chairPositions));
        tag.put("StandingPositions", savePositions(standingPositions));
        return tag;
    }

    public static ShopGroup load(CompoundTag tag) {
        ShopGroup group = new ShopGroup(tag.getString("Name"));
        loadPositions(tag, "SellingShelvesB", group.sellingShelfBPositions);
        loadPositions(tag, "Registers", group.registerPositions);
        loadPositions(tag, "Chairs", group.chairPositions);
        loadPositions(tag, "StandingPositions", group.standingPositions);
        return group;
    }

    private static ListTag savePositions(List<BlockPos> positions) {
        ListTag list = new ListTag();
        for (BlockPos pos : positions) {
            CompoundTag posTag = new CompoundTag();
            posTag.putLong("Pos", pos.asLong());
            list.add(posTag);
        }
        return list;
    }

    private static void loadPositions(CompoundTag parent, String key, List<BlockPos> target) {
        ListTag list = parent.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            target.add(BlockPos.of(list.getCompound(i).getLong("Pos")));
        }
    }

    public boolean isEmpty() {
        return sellingShelfBPositions.isEmpty()
                && registerPositions.isEmpty()
                && chairPositions.isEmpty()
                && standingPositions.isEmpty();
    }
}
