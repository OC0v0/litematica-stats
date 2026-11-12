package dev.litematicastats.gui;

import dev.litematicastats.data.ContainerInfo;
import dev.litematicastats.data.EntityCount;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public class StatsEntry {
    public enum Kind {HEADER, ENTITY, CATEGORY, SINGLE_GROUP, ITEM, STRIP}

    public final Kind kind;
    @Nullable
    public final EntityCount entity;
    @Nullable
    public final ItemStack stack;
    public final int count;
    @Nullable
    public final String categoryKey;
    @Nullable
    public final String title;
    @Nullable
    public final List<ContainerInfo> stripContainers;
    public final int stripHeight;
    @Nullable
    public final ItemStack holderIcon;

    private StatsEntry(Kind kind, @Nullable EntityCount entity, @Nullable ItemStack stack, int count,
                       @Nullable String categoryKey, @Nullable String title,
                       @Nullable List<ContainerInfo> stripContainers, int stripHeight, @Nullable ItemStack holderIcon) {
        this.kind = kind;
        this.entity = entity;
        this.stack = stack;
        this.count = count;
        this.categoryKey = categoryKey;
        this.title = title;
        this.stripContainers = stripContainers;
        this.stripHeight = stripHeight;
        this.holderIcon = holderIcon;
    }

    public static StatsEntry header() {
        return new StatsEntry(Kind.HEADER, null, null, 0, null, null, null, 0, null);
    }

    public static StatsEntry entity(EntityCount entity) {
        return new StatsEntry(Kind.ENTITY, entity, null, 0, null, null, null, 0, null);
    }

    public static StatsEntry category(String key, String title, @Nullable ItemStack holderIcon) {
        return new StatsEntry(Kind.CATEGORY, null, null, 0, key, title, null, 0, holderIcon);
    }

    public static StatsEntry singleGroup(String key, String title, ItemStack holderIcon) {
        return new StatsEntry(Kind.SINGLE_GROUP, null, null, 0, key, title, null, 0, holderIcon);
    }

    public static StatsEntry item(ItemStack stack, int count) {
        return new StatsEntry(Kind.ITEM, null, stack, count, null, null, null, 0, null);
    }

    public static StatsEntry strip(List<ContainerInfo> containers, int height) {
        return new StatsEntry(Kind.STRIP, null, null, 0, null, null, containers, height, null);
    }
}