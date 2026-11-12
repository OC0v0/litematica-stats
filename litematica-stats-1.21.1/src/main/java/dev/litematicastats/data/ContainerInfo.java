package dev.litematicastats.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class ContainerInfo {
    public final String region;
    public final BlockPos pos;
    public final boolean entity;
    @Nullable
    public final String entityId;
    @Nullable
    public final BlockState state;
    public final String typeKey;
    public final String typeName;
    public final ItemStack icon;
    @Nullable
    public final Component customName;
    public final boolean doubleChest;
    public final List<SlotStack> items;
    public final int totalItems;
    public final int gridColumns;
    public final int gridSlots;
    public int mergedCount = 1;
    @Nullable
    public List<ContainerInfo> mergedMembers;

    private final ItemStack[] slotsArray;

    public ContainerInfo(String region, BlockPos pos, boolean entity, @Nullable String entityId,
                         @Nullable BlockState state, String typeKey, String typeName, ItemStack icon,
                         @Nullable Component customName, boolean doubleChest, List<SlotStack> items,
                         int gridColumns, int gridSlots) {
        this.region = region;
        this.pos = pos;
        this.entity = entity;
        this.entityId = entityId;
        this.state = state;
        this.typeKey = typeKey;
        this.typeName = typeName;
        this.icon = icon;
        this.customName = customName;
        this.doubleChest = doubleChest;
        this.items = items;
        this.gridColumns = gridColumns;
        this.gridSlots = gridSlots;

        int total = 0;
        this.slotsArray = new ItemStack[gridSlots];
        for (SlotStack slot : items) {
            if (slot.slot() >= 0 && slot.slot() < gridSlots) {
                this.slotsArray[slot.slot()] = slot.stack();
            }
            total += slot.stack().getCount();
        }
        this.totalItems = total;
    }

    @Nullable
    public ItemStack getStackAt(int slot) {
        return slot >= 0 && slot < this.slotsArray.length ? this.slotsArray[slot] : null;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public String positionString() {
        return String.format("(%d, %d, %d)", this.pos.getX(), this.pos.getY(), this.pos.getZ());
    }

    public record SlotStack(int slot, ItemStack stack) {
    }
}