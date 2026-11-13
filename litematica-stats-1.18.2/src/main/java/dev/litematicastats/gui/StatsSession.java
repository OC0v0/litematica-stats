package dev.litematicastats.gui;

import dev.litematicastats.data.ContainerInfo;
import dev.litematicastats.data.EntityCount;
import dev.litematicastats.data.StatsCollector;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatsSession {
    public static final String IGNORED_KEY = "__litematicastats_ignored__";
    public static final int CARD_PAD = 4;
    public static final int CARD_GRID_TOP = 22;
    public static final int CELL_SIZE = 18;
    public static final int CARD_GAP = 4;

    private static String ignoreLabel;
    private static String ignoredLabel;
    private static int ignoreLabelWidth;
    private static int ignoredLabelWidth;

    public static String ignoreLabel() {
        ensureLabels();
        return ignoreLabel;
    }

    public static String ignoredLabel() {
        ensureLabels();
        return ignoredLabel;
    }

    public static int ignoreLabelWidth() {
        ensureLabels();
        return ignoreLabelWidth;
    }

    public static int ignoredLabelWidth() {
        ensureLabels();
        return ignoredLabelWidth;
    }

    public static int ignoredButtonWidth() {
        return ignoredLabelWidth() + 10;
    }

    private static void ensureLabels() {
        if (ignoredLabel == null) {
            ignoreLabel = StringUtils.translate("litematica-stats.gui.button.ignore");
            ignoredLabel = StringUtils.translate("litematica-stats.gui.button.ignored");
            ignoreLabelWidth = StringUtils.getStringWidth(ignoreLabel);
            ignoredLabelWidth = StringUtils.getStringWidth(ignoredLabel);
        }
    }

    @Nullable
    private final LitematicaSchematic schematic;
    private final List<EntityCount> entities;
    private final List<ContainerInfo> containers;
    private static final Logger LOGGER = LoggerFactory.getLogger("litematica-stats");
    private final Set<String> collapsedCategories = new HashSet<>();
    private final Set<ContainerInfo> ignoredContainers = new HashSet<>();
    private final Map<ContainerInfo, List<StatsEntry>> itemRowsCache = new IdentityHashMap<>();
    private final Map<ContainerInfo, String> contentKeys = new IdentityHashMap<>();
    private boolean hideEmpty = true;
    private boolean aggregateItems = false;
    private int sortColumn = 0;
    private boolean sortReverse = false;
    private int contentsViewWidth = 400;
    private String contentsFilter = "";

    public final int maxEntityNameWidth;
    public final int maxEntityCountWidth;
    public final int maxAggregateItemNameWidth;
    public final int aggregatedTypeCount;

    public StatsSession(@Nullable LitematicaSchematic schematic) {
        long t0 = System.nanoTime();
        this.schematic = schematic;
        this.entities = schematic != null ? StatsCollector.collectEntities(schematic) : List.of();
        long t1 = System.nanoTime();
        this.containers = schematic != null ? StatsCollector.collectContainers(schematic) : List.of();
        long t2 = System.nanoTime();

        int nameWidth = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica-stats.gui.label.column.name") + GuiBase.TXT_RST);
        int countWidth = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica-stats.gui.label.column.count") + GuiBase.TXT_RST);
        for (EntityCount entry : this.entities) {
            nameWidth = Math.max(nameWidth, StringUtils.getStringWidth(entry.name().getString()) + 12);
            countWidth = Math.max(countWidth, StringUtils.getStringWidth(String.valueOf(entry.count())));
        }
        this.maxEntityNameWidth = nameWidth;
        this.maxEntityCountWidth = countWidth;

        // 名称列宽只需按不同物品类型测量，避免对每个物品栈都做一次字体测量。
        int itemNameWidth = StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate("litematica-stats.gui.label.column.item") + GuiBase.TXT_RST);
        Map<ItemKey, ItemStack> uniqueItems = new LinkedHashMap<>();
        for (ContainerInfo container : this.containers) {
            for (ContainerInfo.SlotStack slot : container.items) {
                uniqueItems.putIfAbsent(new ItemKey(slot.stack().getItem(), slot.stack().getTag()), slot.stack());
            }
        }
        for (ItemStack stack : uniqueItems.values()) {
            itemNameWidth = Math.max(itemNameWidth, StringUtils.getStringWidth(stack.getHoverName().getString()) + 24);
        }
        this.maxAggregateItemNameWidth = itemNameWidth;
        this.aggregatedTypeCount = uniqueItems.size();

        long t3 = System.nanoTime();
        LOGGER.info("Stats scan: entities {} ms, containers {} ms ({} containers, {} item types), layout {} ms",
                (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, this.containers.size(), uniqueItems.size(),
                (t3 - t2) / 1_000_000);
    }

    public boolean hasSchematic() {
        return this.schematic != null;
    }

    public boolean shouldShowRegion() {
        return this.schematic != null && this.schematic.getSubRegionCount() > 1;
    }

    public boolean isHideEmpty() {
        return this.hideEmpty;
    }

    public void toggleHideEmpty() {
        this.hideEmpty = !this.hideEmpty;
    }

    public boolean isAggregateItems() {
        return this.aggregateItems;
    }

    public void toggleAggregateItems() {
        this.aggregateItems = !this.aggregateItems;
    }

    public int getSortColumn() {
        return this.sortColumn;
    }

    public boolean isSortReverse() {
        return this.sortReverse;
    }

    public void setSort(int column) {
        if (this.sortColumn == column) {
            this.sortReverse = !this.sortReverse;
        } else {
            this.sortColumn = column;
            this.sortReverse = false;
        }
    }

    public boolean isCategoryCollapsed(String key) {
        return this.collapsedCategories.contains(key);
    }

    public void toggleCategory(String key) {
        if (!this.collapsedCategories.remove(key)) {
            this.collapsedCategories.add(key);
        }
    }

    public boolean isIgnored(ContainerInfo container) {
        return this.ignoredContainers.contains(container);
    }

    public void toggleIgnored(ContainerInfo container) {
        boolean target = !this.ignoredContainers.contains(container);
        this.setIgnored(container, target);
        if (container.mergedMembers != null) {
            for (ContainerInfo member : container.mergedMembers) {
                this.setIgnored(member, target);
            }
        }
    }

    private void setIgnored(ContainerInfo container, boolean ignored) {
        if (ignored) {
            this.ignoredContainers.add(container);
        } else {
            this.ignoredContainers.remove(container);
        }
    }

    private List<ContainerInfo> containersOfType(String typeKey) {
        List<ContainerInfo> list = new ArrayList<>();
        for (ContainerInfo container : this.containers) {
            if (container.typeKey.equals(typeKey)) {
                list.add(container);
            }
        }
        return list;
    }

    public boolean isCategoryIgnored(String typeKey) {
        List<ContainerInfo> list = this.containersOfType(typeKey);
        if (list.isEmpty()) {
            return false;
        }
        for (ContainerInfo container : list) {
            if (!this.ignoredContainers.contains(container)) {
                return false;
            }
        }
        return true;
    }

    public void toggleCategoryIgnored(String typeKey) {
        boolean target = !this.isCategoryIgnored(typeKey);
        for (ContainerInfo container : this.containersOfType(typeKey)) {
            this.setIgnored(container, target);
        }
    }

    private String contentKey(ContainerInfo container) {
        return this.contentKeys.computeIfAbsent(container, StatsSession::buildContentKey);
    }

    private static String buildContentKey(ContainerInfo container) {
        StringBuilder key = new StringBuilder(container.typeKey);
        for (ContainerInfo.SlotStack slot : container.items) {
            key.append('|').append(slot.slot()).append(':')
                    .append(Registry.ITEM.getKey(slot.stack().getItem())).append(':')
                    .append(slot.stack().getCount()).append(':')
                    .append(java.util.Objects.hashCode(slot.stack().getTag()));
        }
        return key.toString();
    }

    private List<ContainerInfo> mergeIdentical(List<ContainerInfo> containers) {
        Map<String, ContainerInfo> byContent = new LinkedHashMap<>();
        for (ContainerInfo container : containers) {
            container.mergedCount = 1;
            container.mergedMembers = null;
            String key = this.contentKey(container);
            ContainerInfo rep = byContent.get(key);
            if (rep == null) {
                byContent.put(key, container);
            } else {
                if (rep.mergedMembers == null) {
                    rep.mergedMembers = new ArrayList<>();
                    rep.mergedMembers.add(rep);
                }
                rep.mergedMembers.add(container);
                rep.mergedCount++;
            }
        }
        return new ArrayList<>(byContent.values());
    }

    public void setContentsViewWidth(int width) {
        this.contentsViewWidth = Math.max(width, 120);
    }

    public void setContentsFilter(String filter) {
        this.contentsFilter = filter == null ? "" : filter.toLowerCase();
    }

    public static int cardWidth(ContainerInfo container) {
        int gridWidth = container.gridColumns * CELL_SIZE + CARD_PAD * 2;
        int buttonWidth = ignoredButtonWidth();
        int badgeWidth = container.mergedCount > 1 ? StringUtils.getStringWidth("x" + container.mergedCount) + 8 : 0;
        int titleWidth = StringUtils.getStringWidth(container.typeName) + buttonWidth + badgeWidth + CARD_PAD * 2 + 10;
        return Math.max(gridWidth, Math.min(titleWidth, 240));
    }

    public static int cardHeight(ContainerInfo container) {
        int rows = (container.gridSlots + container.gridColumns - 1) / container.gridColumns;
        return CARD_GRID_TOP + rows * CELL_SIZE + CARD_PAD;
    }

    public long getEntityTotal() {
        long total = 0;
        for (EntityCount entry : this.entities) {
            total += entry.count();
        }
        return total;
    }

    public int getVisibleContainerCount() {
        int count = 0;
        for (ContainerInfo container : this.containers) {
            if (this.hideEmpty && container.isEmpty()) {
                continue;
            }
            if (this.ignoredContainers.contains(container)) {
                continue;
            }
            count++;
        }
        return count;
    }

    public long getVisibleItemTotal() {
        long total = 0;
        for (ContainerInfo container : this.containers) {
            if (this.hideEmpty && container.isEmpty()) {
                continue;
            }
            if (this.ignoredContainers.contains(container)) {
                continue;
            }
            total += container.totalItems;
        }
        return total;
    }

    private boolean containerMatchesFilter(ContainerInfo container) {
        if (this.contentsFilter.isEmpty()) {
            return true;
        }
        if (container.typeNameLower.contains(this.contentsFilter)
                || container.positionString().contains(this.contentsFilter)
                || container.region.toLowerCase().contains(this.contentsFilter)) {
            return true;
        }
        if (container.customName != null && container.customName.getString().toLowerCase().contains(this.contentsFilter)) {
            return true;
        }
        if (container.entityId != null && container.entityId.toLowerCase().contains(this.contentsFilter)) {
            return true;
        }
        for (ContainerInfo.SlotStack slot : container.items) {
            if (slot.stack().getHoverName().getString().toLowerCase().contains(this.contentsFilter)) {
                return true;
            }
        }
        return false;
    }

    public List<StatsEntry> buildRows(StatsTab tab) {
        List<StatsEntry> rows = new ArrayList<>();
        if (tab == StatsTab.ENTITIES) {
            List<EntityCount> sorted = new ArrayList<>(this.entities);
            Map<Item, Integer> inventory = this.inventoryItemTotals();
            Map<EntityCount, Integer> availableMap = new IdentityHashMap<>();
            Map<EntityCount, Integer> missingMap = new IdentityHashMap<>();
            for (EntityCount entry : sorted) {
                int available = entry.icon().getItem() == Items.BARRIER
                        ? 0 : inventory.getOrDefault(entry.icon().getItem(), 0);
                availableMap.put(entry, available);
                missingMap.put(entry, Math.max(0, entry.count() - available));
            }
            Comparator<EntityCount> comparator;
            if (this.sortColumn == 1) {
                comparator = Comparator.comparingInt(EntityCount::count);
            } else if (this.sortColumn == 2) {
                comparator = Comparator.comparingInt(missingMap::get);
            } else if (this.sortColumn == 3) {
                comparator = Comparator.comparingInt(availableMap::get);
            } else {
                Map<EntityCount, String> sortNames = new IdentityHashMap<>();
                for (EntityCount entry : sorted) {
                    sortNames.put(entry, entry.name().getString().toLowerCase());
                }
                comparator = Comparator.comparing(sortNames::get);
            }
            if (this.sortReverse) {
                comparator = comparator.reversed();
            }
            sorted.sort(comparator);
            for (EntityCount entry : sorted) {
                StatsEntry row = StatsEntry.entity(entry);
                row.available = availableMap.get(entry);
                row.missing = missingMap.get(entry);
                rows.add(row);
            }
        } else if (tab == StatsTab.CONTENTS) {
            if (this.aggregateItems) {
                List<ContainerInfo> active = new ArrayList<>();
                List<ContainerInfo> ignored = new ArrayList<>();
                for (ContainerInfo container : this.containers) {
                    if (this.ignoredContainers.contains(container)) {
                        ignored.add(container);
                    } else {
                        active.add(container);
                    }
                }
                List<StatsEntry> aggregateRows = new ArrayList<>(this.buildAggregateRows(active));
                if (!ignored.isEmpty()) {
                    long ignoredItems = 0;
                    for (ContainerInfo container : ignored) {
                        ignoredItems += container.totalItems;
                    }
                    String title = StringUtils.translate("litematica-stats.gui.category.ignored", ignored.size(), ignoredItems);
                    aggregateRows.add(StatsEntry.category(IGNORED_KEY, title, null));
                    if (!this.collapsedCategories.contains(IGNORED_KEY)) {
                        aggregateRows.addAll(this.buildAggregateRows(ignored));
                    }
                }
                return aggregateRows;
            }
            Map<String, List<ContainerInfo>> groups = new LinkedHashMap<>();
            for (ContainerInfo container : this.containers) {
                if (this.hideEmpty && container.isEmpty()) {
                    continue;
                }
                groups.computeIfAbsent(container.typeKey, k -> new ArrayList<>()).add(container);
            }
            for (Map.Entry<String, List<ContainerInfo>> group : groups.entrySet()) {
                List<ContainerInfo> groupContainers = group.getValue();
                long itemCount = 0;
                for (ContainerInfo container : groupContainers) {
                    itemCount += container.totalItems;
                }
                String title = StringUtils.translate("litematica-stats.gui.category.containers",
                        groupContainers.get(0).typeName, groupContainers.size(), itemCount);
                if (groupContainers.get(0).gridSlots == 1) {
                    // Single-slot holders (item frames, decorated pots, ...): one aggregated group per type
                    ItemStack holderIcon = groupContainers.get(0).icon;
                    rows.add(StatsEntry.singleGroup(group.getKey(), title, holderIcon));
                    if (!this.collapsedCategories.contains(group.getKey())) {
                        rows.addAll(this.buildAggregateRows(groupContainers));
                    }
                    continue;
                }
                List<ContainerInfo> matching = new ArrayList<>();
                for (ContainerInfo container : groupContainers) {
                    if (this.containerMatchesFilter(container)) {
                        matching.add(container);
                    }
                }
                if (matching.isEmpty()) {
                    continue;
                }
                rows.add(StatsEntry.category(group.getKey(), title, groupContainers.get(0).icon));
                if (this.collapsedCategories.contains(group.getKey())) {
                    continue;
                }
                rows.addAll(this.buildStrips(matching));
            }
        }
        return rows;
    }

    private List<StatsEntry> buildStrips(List<ContainerInfo> containers) {
        List<StatsEntry> strips = new ArrayList<>();
        List<ContainerInfo> current = new ArrayList<>();
        int currentWidth = 0;
        int currentHeight = 0;
        for (ContainerInfo container : this.mergeIdentical(containers)) {
            int width = cardWidth(container);
            int height = cardHeight(container);
            int needed = current.isEmpty() ? width : currentWidth + CARD_GAP + width;
            if (!current.isEmpty() && needed > this.contentsViewWidth) {
                strips.add(StatsEntry.strip(current, currentHeight));
                current = new ArrayList<>();
                currentWidth = 0;
                currentHeight = 0;
                needed = width;
            }
            current.add(container);
            currentWidth = needed;
            currentHeight = Math.max(currentHeight, height);
        }
        if (!current.isEmpty()) {
            strips.add(StatsEntry.strip(current, currentHeight));
        }
        return strips;
    }

    private List<StatsEntry> buildAggregateRows(List<ContainerInfo> scope) {
        Map<ItemKey, ItemAggregate> aggregated = new LinkedHashMap<>();
        for (ContainerInfo container : scope) {
            for (ContainerInfo.SlotStack slot : container.items) {
                ItemKey key = new ItemKey(slot.stack().getItem(), slot.stack().getTag());
                ItemAggregate aggregate = aggregated.computeIfAbsent(key, k -> new ItemAggregate(slot.stack().copy()));
                aggregate.count += slot.stack().getCount();
            }
        }

        Map<ItemKey, Integer> inventory = this.inventoryItemCounts();
        for (Map.Entry<ItemKey, ItemAggregate> entry : aggregated.entrySet()) {
            int available = inventory.getOrDefault(entry.getKey(), 0);
            entry.getValue().available = available;
            entry.getValue().missing = Math.max(0, entry.getValue().count - available);
        }

        List<ItemAggregate> sorted = new ArrayList<>(aggregated.values());
        Comparator<ItemAggregate> comparator;
        if (this.sortColumn == 1) {
            comparator = Comparator.comparingInt(a -> a.count);
        } else if (this.sortColumn == 2) {
            comparator = Comparator.comparingInt(a -> a.missing);
        } else if (this.sortColumn == 3) {
            comparator = Comparator.comparingInt(a -> a.available);
        } else {
            comparator = Comparator.comparing(a -> a.nameLower);
        }
        if (this.sortReverse) {
            comparator = comparator.reversed();
        }
        sorted.sort(comparator);

        List<StatsEntry> rows = new ArrayList<>();
        for (ItemAggregate aggregate : sorted) {
            ItemStack display = aggregate.stack.copy();
            display.setCount(aggregate.count);
            StatsEntry row = StatsEntry.item(display, aggregate.count);
            row.available = aggregate.available;
            row.missing = aggregate.missing;
            rows.add(row);
        }
        return rows;
    }

    private Map<ItemKey, Integer> inventoryItemCounts() {
        Map<ItemKey, Integer> counts = new HashMap<>();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return counts;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                counts.merge(new ItemKey(stack.getItem(), stack.getTag()), stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    private Map<Item, Integer> inventoryItemTotals() {
        Map<Item, Integer> counts = new HashMap<>();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return counts;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    public List<ContainerInfo> getEntityContainers(String entityId) {
        List<ContainerInfo> result = new ArrayList<>();
        String baseId = baseBoatId(entityId);
        for (ContainerInfo container : this.containers) {
            if (container.entity && (entityId.equals(container.entityId) || baseId.equals(container.entityId))) {
                result.add(container);
            }
        }
        return result;
    }

    private static String baseBoatId(String id) {
        if (id.endsWith("_chest_boat")) {
            return "minecraft:chest_boat";
        }
        if (id.endsWith("_boat") && !id.equals("minecraft:boat")) {
            return "minecraft:boat";
        }
        return id;
    }

    public List<StatsEntry> buildItemRows(ContainerInfo container) {
        return this.itemRowsCache.computeIfAbsent(container, this::computeItemRows);
    }

    private List<StatsEntry> computeItemRows(ContainerInfo container) {
        Map<ItemKey, ItemAggregate> aggregated = new LinkedHashMap<>();
        for (ContainerInfo.SlotStack slot : container.items) {
            ItemKey key = new ItemKey(slot.stack().getItem(), slot.stack().getTag());
            ItemAggregate aggregate = aggregated.computeIfAbsent(key, k -> new ItemAggregate(slot.stack().copy()));
            aggregate.count += slot.stack().getCount();
        }

        List<StatsEntry> rows = new ArrayList<>();
        for (ItemAggregate aggregate : aggregated.values()) {
            ItemStack display = aggregate.stack.copy();
            display.setCount(aggregate.count);
            rows.add(StatsEntry.item(display, aggregate.count));
        }
        return rows;
    }

    private record ItemKey(Item item, @Nullable CompoundTag tag) {
    }

    private static class ItemAggregate {
        final ItemStack stack;
        final String nameLower;
        int count;
        int available;
        int missing;

        ItemAggregate(ItemStack stack) {
            this.stack = stack;
            this.nameLower = stack.getHoverName().getString().toLowerCase();
        }
    }
}