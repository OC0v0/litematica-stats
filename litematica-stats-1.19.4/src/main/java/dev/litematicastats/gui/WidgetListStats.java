package dev.litematicastats.gui;

import dev.litematicastats.data.ContainerInfo;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WidgetListStats extends WidgetListBase<StatsEntry, WidgetStatsEntry> {
    private final StatsHost host;
    private boolean hasHeader;
    private int headerHeight;

    public WidgetListStats(int x, int y, int width, int height, StatsHost host) {
        super(x, y, width, height, null);
        this.host = host;
        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 8, width - 16, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.RIGHT);
        this.widgetSearchBar.setZLevel(1);
        this.shouldSortList = false;
    }

    @Override
    protected Collection<StatsEntry> getAllEntries() {
        return this.host.buildStatsRows();
    }

    public boolean isWithinBrowser(int mouseY) {
        return mouseY >= this.posY && mouseY < this.posY + this.browserHeight;
    }

    private void applyScissor(int x1, int y1, int x2, int y2) {
        double scale = this.mc.getWindow().getGuiScale();
        int windowHeight = this.mc.getWindow().getHeight();
        RenderSystem.enableScissor((int) (x1 * scale), (int) (windowHeight - y2 * scale),
                (int) ((x2 - x1) * scale), (int) ((y2 - y1) * scale));
    }

    private void removeScissor() {
        RenderSystem.disableScissor();
    }

    @Override
    protected void reCreateListEntryWidgets() {
        // Pixel-based scrolling: the scrollbar value is a content offset in pixels,
        // so variable-height strips can always be scrolled fully into view.
        this.listWidgets.clear();
        this.maxVisibleBrowserEntries = 0;
        int usable = this.browserHeight - this.browserPaddingY - this.browserEntriesOffsetY;
        int totalHeight = 0;
        for (StatsEntry entry : this.listContents) {
            totalHeight += this.getBrowserEntryHeightFor(entry);
        }

        int x = this.browserEntriesStartX;
        int y = this.browserEntriesStartY;
        WidgetStatsEntry header = this.createHeaderWidget(x, y, 0, usable, 0);
        this.hasHeader = header != null;
        this.headerHeight = header != null ? header.getHeight() : 0;
        if (header != null) {
            this.listWidgets.add(header);
            y += this.headerHeight;
            usable -= this.headerHeight;
        }

        int maxScroll = Math.max(0, totalHeight - usable);
        this.scrollBar.setMaxValue(maxScroll);
        int offset = Math.min(Math.max(this.scrollBar.getValue(), 0), maxScroll);

        int index = 0;
        int entryStart = 0;
        while (index < this.listContents.size()) {
            int height = this.getBrowserEntryHeightFor(this.listContents.get(index));
            if (entryStart + height > offset) {
                break;
            }
            entryStart += height;
            index++;
        }
        int screenY = y + (entryStart - offset);
        int bottom = y + usable;
        while (index < this.listContents.size() && screenY < bottom) {
            StatsEntry entry = this.listContents.get(index);
            int height = this.getBrowserEntryHeightFor(entry);
            this.listWidgets.add(this.createListEntryWidget(x, screenY, index, (index & 1) != 0, entry));
            screenY += height;
            index++;
            this.maxVisibleBrowserEntries++;
        }
    }

    @Override
    protected void offsetSelectionOrScrollbar(int amount, boolean changeSelection) {
        this.scrollBar.offsetValue(amount * this.browserEntryHeight);
    }

    @Override
    public void drawContents(PoseStack ctx, int mouseX, int mouseY, float partialTicks) {
        WidgetBase hovered = null;
        int scrollbarHeight = this.browserHeight - this.browserEntriesOffsetY - 8;
        int totalHeight = 0;
        for (StatsEntry entry : this.listContents) {
            totalHeight += this.getBrowserEntryHeightFor(entry);
        }
        totalHeight = Math.max(totalHeight, scrollbarHeight);
        this.scrollBar.render(mouseX, mouseY, partialTicks,
                this.posX + this.browserWidth - 9, this.browserEntriesStartY + this.browserEntriesOffsetY,
                8, scrollbarHeight, totalHeight);
        if (this.scrollBar.getValue() != this.lastScrollbarPosition) {
            this.lastScrollbarPosition = this.scrollBar.getValue();
            this.reCreateListEntryWidgets();
        }

        int contentTop = this.browserEntriesStartY + (this.hasHeader ? this.headerHeight : 0);
        int contentBottom = Math.min(contentTop + this.browserHeight, this.posY + this.browserHeight);
        this.applyScissor(this.posX, contentTop, this.posX + this.browserWidth - 9, contentBottom);
        for (int i = this.hasHeader ? 1 : 0; i < this.listWidgets.size(); i++) {
            WidgetStatsEntry widget = this.listWidgets.get(i);
            widget.render(mouseX, mouseY, false, ctx);
            if (widget.isMouseOver(mouseX, mouseY)) {
                hovered = widget;
            }
        }
        this.removeScissor();

        if (this.hasHeader && !this.listWidgets.isEmpty()) {
            WidgetStatsEntry header = this.listWidgets.get(0);
            this.applyScissor(this.posX, header.getY(), this.posX + this.browserWidth - 9, header.getY() + header.getHeight());
            header.render(mouseX, mouseY, false, ctx);
            this.removeScissor();
            if (header.isMouseOver(mouseX, mouseY)) {
                hovered = header;
            }
        }

        if (this.widgetSearchBar != null) {
            this.widgetSearchBar.render(mouseX, mouseY, false, ctx);
        }
        if (hovered == null && this.widgetSearchBar != null && this.widgetSearchBar.isMouseOver(mouseX, mouseY)) {
            hovered = this.widgetSearchBar;
        }
        this.hoveredWidget = hovered;
    }

    @Override
    protected void refreshBrowserEntries() {
        this.host.setContentsViewWidth(this.browserEntryWidth);
        this.host.setContentsFilter(this.getFilterText());
        super.refreshBrowserEntries();
    }

    @Override
    protected int getBrowserEntryHeightFor(StatsEntry entry) {
        if (entry != null && entry.kind == StatsEntry.Kind.STRIP) {
            return entry.stripHeight;
        }
        return this.browserEntryHeight;
    }

    @Override
    protected List<String> getEntryStringsForFilter(StatsEntry entry) {
        List<String> strings = new ArrayList<>();
        switch (entry.kind) {
            case ENTITY -> {
                strings.add(entry.entity.name().getString().toLowerCase());
                strings.add(entry.entity.id().toLowerCase());
            }
            case CATEGORY, SINGLE_GROUP -> strings.add(entry.title.toLowerCase());
            case STRIP -> {
                for (ContainerInfo container : entry.stripContainers) {
                    strings.add(container.typeName.toLowerCase());
                    strings.add(container.positionString());
                    strings.add(container.region.toLowerCase());
                    if (container.customName != null) {
                        strings.add(container.customName.getString().toLowerCase());
                    }
                    if (container.entityId != null) {
                        strings.add(container.entityId.toLowerCase());
                    }
                }
            }
            case ITEM -> {
                strings.add(entry.stack.getHoverName().getString().toLowerCase());
                ResourceLocation rl = BuiltInRegistries.ITEM.getKey(entry.stack.getItem());
                if (rl != null) {
                    strings.add(rl.toString().toLowerCase());
                }
            }
            default -> {
            }
        }
        return strings;
    }

    @Override
    protected WidgetStatsEntry createHeaderWidget(int x, int y, int listIndexStart, int usableHeight, int usedHeight) {
        boolean wantsHeader = this.host.getStatsTab() == StatsTab.ENTITIES
                || (this.host.getStatsTab() == StatsTab.CONTENTS && this.host.isAggregateItems());
        if (!wantsHeader) {
            return null;
        }
        if (usedHeight + this.browserEntryHeight > usableHeight) {
            return null;
        }
        return this.createListEntryWidget(x, y, listIndexStart, true, StatsEntry.header());
    }

    @Override
    protected WidgetStatsEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, StatsEntry entry) {
        return new WidgetStatsEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), isOdd, entry, listIndex, this, this.host);
    }
}