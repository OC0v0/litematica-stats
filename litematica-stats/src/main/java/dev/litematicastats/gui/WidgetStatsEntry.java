package dev.litematicastats.gui;

import dev.litematicastats.data.ContainerInfo;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class WidgetStatsEntry extends WidgetListEntrySortable<StatsEntry> {
    private static final int COLOR_ODD = -1609560048;
    private static final int COLOR_EVEN = -1607454672;
    private static final int COLOR_HOVER = -1603243920;
    private static final int COLOR_ICON_BG = 553648127;

    private final StatsHost host;
    private final WidgetListStats listWidget;
    private final StatsEntry entry;
    private final boolean isOdd;

    public WidgetStatsEntry(int x, int y, int width, int height, boolean isOdd, StatsEntry entry,
                            int listIndex, WidgetListStats listWidget, StatsHost host) {
        super(x, y, width, height, entry, listIndex);
        this.host = host;
        this.listWidget = listWidget;
        this.entry = entry;
        this.isOdd = isOdd;
        this.columnCount = this.hasCountColumn() ? 2 : 1;

        if (entry.kind == StatsEntry.Kind.STRIP) {
            int cx = x;
            for (ContainerInfo container : entry.stripContainers) {
                int cardWidth = StatsSession.cardWidth(container);
                String label = StringUtils.translate(host.isIgnored(container)
                        ? "litematica-stats.gui.button.ignored" : "litematica-stats.gui.button.ignore");
                int btnWidth = this.getStringWidth(label) + 10;
                ButtonGeneric button = new ButtonGeneric(cx + cardWidth - btnWidth - StatsSession.CARD_PAD, y + 3, btnWidth, 14, label);
                this.addButton(button, (btn, mouseButton) -> {
                    host.toggleIgnored(container);
                    listWidget.refreshEntries();
                });
                cx += cardWidth + StatsSession.CARD_GAP;
            }
        } else if ((entry.kind == StatsEntry.Kind.CATEGORY && !StatsSession.IGNORED_KEY.equals(entry.categoryKey))
                || entry.kind == StatsEntry.Kind.SINGLE_GROUP) {
            String label = StringUtils.translate(host.isCategoryIgnored(entry.categoryKey)
                    ? "litematica-stats.gui.button.ignored" : "litematica-stats.gui.button.ignore");
            int btnWidth = this.getStringWidth(label) + 10;
            ButtonGeneric button = new ButtonGeneric(x + width - btnWidth - 4, y + 4, btnWidth, 14, label);
            this.addButton(button, (btn, mouseButton) -> host.toggleCategoryIgnored(entry.categoryKey));
        }
    }

    private boolean hasCountColumn() {
        return this.host.getStatsTab() == StatsTab.ENTITIES
                || (this.host.getStatsTab() == StatsTab.CONTENTS && this.host.isAggregateItems());
    }

    private boolean isAggregateItemView() {
        return this.host.getStatsTab() == StatsTab.CONTENTS && this.host.isAggregateItems();
    }

    @Override
    public boolean canSelectAt(MouseButtonEvent click) {
        return false;
    }

    @Override
    protected int getCurrentSortColumn() {
        return this.host.getStatsSortColumn();
    }

    @Override
    protected boolean getSortInReverse() {
        return this.host.isStatsSortReverse();
    }

    @Override
    protected int getColumnPosX(int column) {
        int nameWidth = this.isAggregateItemView() ? this.host.getMaxAggregateItemNameWidth() : this.host.getMaxEntityNameWidth();
        int x1 = this.x + 4;
        int x2 = x1 + nameWidth + 40;
        return switch (column) {
            case 0 -> x1;
            case 1 -> x2;
            default -> x2 + this.host.getMaxEntityCountWidth() + 20;
        };
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubleClick) {
        if (!this.listWidget.isWithinBrowser((int) click.y())) {
            return false;
        }
        return super.onMouseClicked(click, doubleClick);
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick) {
        switch (this.entry.kind) {
            case HEADER -> {
                int column = this.getMouseOverColumn((int) click.x(), (int) click.y());
                if (column >= 0 && column < 2) {
                    this.host.setStatsSort(column);
                    this.listWidget.refreshEntries();
                    return true;
                }
                return false;
            }
            case CATEGORY, SINGLE_GROUP -> {
                this.host.toggleCategory(this.entry.categoryKey);
                this.listWidget.refreshEntries();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
        int bgColor = this.isMouseOver(mouseX, mouseY) ? COLOR_HOVER : (this.isOdd ? COLOR_ODD : COLOR_EVEN);
        RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, bgColor);

        switch (this.entry.kind) {
            case HEADER -> this.renderHeader(ctx, mouseX, mouseY);
            case ENTITY -> this.renderEntity(ctx);
            case CATEGORY, SINGLE_GROUP -> this.renderCategory(ctx);
            case STRIP -> this.renderStrip(ctx);
            case ITEM -> this.renderItem(ctx);
            default -> {
            }
        }

        super.render(ctx, mouseX, mouseY, selected);
    }

    private void renderHeader(GuiContext ctx, int mouseX, int mouseY) {
        int y = this.y + 7;
        String firstColumn = this.isAggregateItemView() ? "litematica-stats.gui.label.column.item" : "litematica-stats.gui.label.column.name";
        this.drawString(ctx, this.getColumnPosX(0), y, -1,
                GuiBase.TXT_BOLD + StringUtils.translate(firstColumn) + GuiBase.TXT_RST);
        this.drawString(ctx, this.getColumnPosX(1), y, -1,
                GuiBase.TXT_BOLD + StringUtils.translate("litematica-stats.gui.label.column.count") + GuiBase.TXT_RST);
        if (!this.listWidget.getSearchBarWidget().isSearchOpen()) {
            this.renderColumnHeader(ctx, mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);
        }
    }

    private void renderEntity(GuiContext ctx) {
        int x1 = this.getColumnPosX(0);
        int y = this.y + 3;
        RenderUtils.drawRect(ctx, x1, y, 16, 16, COLOR_ICON_BG);
        ctx.renderItem(this.entry.entity.icon(), x1, y);
        String name = this.entry.entity.name().getString();
        if (this.entry.entity.hasInventory()) {
            name = name + GuiBase.TXT_GOLD + " *" + GuiBase.TXT_RST;
        }
        this.drawString(ctx, x1 + 20, this.y + 7, -1, name);
        this.drawString(ctx, this.getColumnPosX(1), this.y + 7, -1, String.valueOf(this.entry.entity.count()));
    }

    private void renderCategory(GuiContext ctx) {
        boolean collapsed = this.host.isCategoryCollapsed(this.entry.categoryKey);
        String color = this.entry.kind == StatsEntry.Kind.SINGLE_GROUP ? GuiBase.TXT_LIGHT_PURPLE : GuiBase.TXT_AQUA;
        int tx = this.x + 4;
        if (this.entry.holderIcon != null) {
            int y = this.y + 3;
            RenderUtils.drawRect(ctx, this.x + 4, y, 16, 16, COLOR_ICON_BG);
            ctx.renderItem(this.entry.holderIcon, this.x + 4, y);
            tx = this.x + 24;
        }
        this.renderTriangle(ctx, tx, this.y + 7, collapsed);
        int titleMax = this.x + this.width - tx - 12 - this.getStringWidth(StringUtils.translate("litematica-stats.gui.button.ignored")) - 12;
        this.drawString(ctx, tx + 12, this.y + 7, -1,
                color + GuiBase.TXT_BOLD + this.truncate(this.entry.title, Math.max(20, titleMax)) + GuiBase.TXT_RST);
    }

    private void renderTriangle(GuiContext ctx, int x, int y, boolean collapsed) {
        int color = 0xFFE0E0E0;
        for (int i = 0; i < 5; i++) {
            int len = 9 - 2 * i;
            if (collapsed) {
                RenderUtils.drawRect(ctx, x + i, y + i, 1, len, color);
            } else {
                RenderUtils.drawRect(ctx, x + i, y + i, len, 1, color);
            }
        }
    }

    private void renderStrip(GuiContext ctx) {
        int cx = this.x;
        for (ContainerInfo container : this.entry.stripContainers) {
            int cardWidth = StatsSession.cardWidth(container);
            int cardHeight = StatsSession.cardHeight(container);
            boolean ignored = this.host.isIgnored(container);
            RenderUtils.drawRect(ctx, cx, this.y, cardWidth, cardHeight, ignored ? 0x60402020 : 0x60000000);
            RenderUtils.drawOutline(ctx, cx, this.y, cardWidth, cardHeight, ignored ? 0xFF804040 : 0xFF606060);

            String title = container.typeName;
            if (container.customName != null) {
                title += GuiBase.TXT_AQUA + GuiBase.TXT_ITALIC + " \"" + container.customName.getString() + "\"" + GuiBase.TXT_RST;
            }
            if (ignored) {
                title += GuiBase.TXT_RED + " [" + StringUtils.translate("litematica-stats.gui.button.ignored") + "]" + GuiBase.TXT_RST;
            }
            int buttonWidth = this.getStringWidth(StringUtils.translate("litematica-stats.gui.button.ignored")) + 10;
            String badge = container.mergedCount > 1 ? "x" + container.mergedCount : null;
            int badgeTextWidth = badge != null ? this.getStringWidth(badge) : 0;
            int badgeX = cx + cardWidth - buttonWidth - 8 - badgeTextWidth;
            int titleMax = badgeX - 4 - (cx + StatsSession.CARD_PAD);
            this.drawString(ctx, cx + StatsSession.CARD_PAD, this.y + 6, -1, this.truncate(title, Math.max(titleMax, 20)));
            if (badge != null) {
                this.drawString(ctx, badgeX, this.y + 6, -1, GuiBase.TXT_GOLD + badge + GuiBase.TXT_RST);
            }

            this.drawSlotGrid(ctx, cx + StatsSession.CARD_PAD, this.y + StatsSession.CARD_GRID_TOP, container);
            cx += cardWidth + StatsSession.CARD_GAP;
        }
    }

    private void drawSlotGrid(GuiContext ctx, int x, int y, ContainerInfo container) {
        for (int i = 0; i < container.gridSlots; i++) {
            int cx = x + (i % container.gridColumns) * StatsSession.CELL_SIZE;
            int cy = y + (i / container.gridColumns) * StatsSession.CELL_SIZE;
            RenderUtils.drawRect(ctx, cx, cy, StatsSession.CELL_SIZE, StatsSession.CELL_SIZE, 0xFF8B8B8B);
            RenderUtils.drawRect(ctx, cx + 1, cy + 1, StatsSession.CELL_SIZE - 2, StatsSession.CELL_SIZE - 2, 0xFF373737);
            ItemStack stack = container.getStackAt(i);
            if (stack != null) {
                ctx.renderItem(stack, cx + 1, cy + 1);
                ctx.renderItemDecorations(this.textRenderer, stack, cx + 1, cy + 1);
            }
        }
    }

    private void renderItem(GuiContext ctx) {
        ItemStack stack = this.entry.stack;
        int y = this.y + 3;
        boolean aggregate = this.isAggregateItemView();
        int iconX = aggregate ? this.getColumnPosX(0) : this.x + 24;
        int textX = aggregate ? iconX + 20 : this.x + 44;
        RenderUtils.drawRect(ctx, iconX, y, 16, 16, COLOR_ICON_BG);
        ctx.renderItem(stack, iconX, y);

        String name = this.itemDisplayName(stack);

        if (aggregate) {
            int countX = this.getColumnPosX(1);
            this.drawString(ctx, textX, this.y + 7, -1, this.truncate(name, countX - textX - 8));
            this.drawString(ctx, countX, this.y + 7, -1, String.valueOf(this.entry.count));
        } else {
            int countX = this.x + this.width - 60;
            this.drawString(ctx, textX, this.y + 7, -1, this.truncate(name, countX - textX - 8));
            this.drawString(ctx, countX, this.y + 7, -1, "x" + this.entry.count);
        }
    }

    private String truncate(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (this.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return this.textRenderer.plainSubstrByWidth(text, maxWidth - this.getStringWidth("...")) + "...";
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
        switch (this.entry.kind) {
            case ENTITY -> this.renderEntityHover(ctx, mouseX, mouseY);
            case CATEGORY -> RenderUtils.drawHoverText(ctx, mouseX + 10, mouseY,
                    List.of(StringUtils.translate("litematica-stats.gui.hover.category_hint")));
            case SINGLE_GROUP -> RenderUtils.drawHoverText(ctx, mouseX + 10, mouseY,
                    List.of(StringUtils.translate("litematica-stats.gui.hover.single_group_hint")));
            case STRIP -> {
                if (this.isOverSubWidget(mouseX, mouseY) || !this.listWidget.isWithinBrowser(mouseY)) {
                    return;
                }
                ContainerInfo hovered = this.getCardAt(mouseX, mouseY);
                if (hovered != null) {
                    ContainerInfo.SlotStack slot = this.getSlotAt(hovered, mouseX, mouseY);
                    if (slot != null) {
                        InventoryOverlay.renderStackToolTipStyled(ctx, mouseX + 10, mouseY, slot.stack());
                    } else {
                        this.renderContainerHover(ctx, mouseX, mouseY, hovered);
                    }
                }
            }
            case ITEM -> this.renderItemHover(ctx, mouseX, mouseY);
            default -> {
            }
        }
    }

    private void renderEntityHover(GuiContext ctx, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        lines.add(GuiBase.TXT_BOLD + this.entry.entity.name().getString());
        lines.add(GuiBase.TXT_DARK_GRAY + this.entry.entity.id());
        lines.add(StringUtils.translate("litematica-stats.gui.label.column.count") + ": " + this.entry.entity.count());
        lines.add(GuiBase.TXT_GRAY + StringUtils.translate("litematica-stats.gui.label.entity_note"));
        if (this.entry.entity.icon().getItem() == Items.BARRIER) {
            lines.add(GuiBase.TXT_GRAY + StringUtils.translate("litematica-stats.gui.label.no_item"));
        }

        List<ContainerInfo> entityContainers = this.entry.entity.hasInventory()
                ? this.host.getEntityContainers(this.entry.entity.id()) : List.of();
        boolean allSingleSlot = !entityContainers.isEmpty() && entityContainers.stream().allMatch(c -> c.gridSlots == 1);
        if (entityContainers.isEmpty() || allSingleSlot) {
            if (this.entry.entity.hasInventory()) {
                lines.add(GuiBase.TXT_GOLD + StringUtils.translate("litematica-stats.gui.label.has_inventory"));
            }
            RenderUtils.drawHoverText(ctx, mouseX + 10, mouseY, lines);
            return;
        }
        this.renderEntityGridTooltip(ctx, mouseX, mouseY, lines, entityContainers);
    }

    private String itemDisplayName(ItemStack stack) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            return GuiBase.TXT_AQUA + GuiBase.TXT_ITALIC + customName.getString() + GuiBase.TXT_RST
                    + GuiBase.TXT_DARK_GRAY + " (" + stack.getItem().getName(stack).getString() + ")" + GuiBase.TXT_RST;
        }
        return stack.getHoverName().getString();
    }

    private void renderEntityGridTooltip(GuiContext ctx, int mouseX, int mouseY, List<String> lines, List<ContainerInfo> containers) {
        final int padding = 6;
        final int lineHeight = 10;
        final int cellSize = 18;
        final int maxGrids = 5;
        int screenWidth = GuiUtils.getCurrentScreenWidth();
        int screenHeight = GuiUtils.getCurrentScreenHeight();

        List<String> labels = new ArrayList<>();
        int contentWidth = 0;
        for (String line : lines) {
            contentWidth = Math.max(contentWidth, this.getStringWidth(line));
        }

        int shown = 0;
        int gridsHeight = 0;
        int textHeight = lines.size() * lineHeight;
        for (ContainerInfo container : containers) {
            if (shown >= maxGrids) {
                break;
            }
            String label = container.positionString();
            if (this.host.shouldShowRegion()) {
                label += " " + container.region;
            }
            if (container.customName != null) {
                label += " \"" + container.customName.getString() + "\"";
            }
            int rows = (container.gridSlots + container.gridColumns - 1) / container.gridColumns;
            int gridHeight = lineHeight + rows * cellSize + 4;
            boolean hasFooter = shown + 1 < containers.size();
            int footerHeight = hasFooter ? lineHeight : 0;
            if (padding * 2 + textHeight + gridsHeight + gridHeight + footerHeight > screenHeight - 8 && shown > 0) {
                break;
            }
            labels.add(label);
            contentWidth = Math.max(contentWidth, Math.max(this.getStringWidth(label), container.gridColumns * cellSize));
            gridsHeight += gridHeight;
            shown++;
        }

        int remaining = containers.size() - shown;
        String footer = remaining > 0 ? StringUtils.translate("litematica-stats.gui.label.more_entities", remaining) : null;
        if (footer != null) {
            contentWidth = Math.max(contentWidth, this.getStringWidth(footer));
        }

        int maxContentWidth = screenWidth - 4 - padding * 2;
        contentWidth = Math.min(contentWidth, maxContentWidth);
        int width = contentWidth + padding * 2;
        int height = padding * 2 + textHeight + gridsHeight + (footer != null ? lineHeight : 0);
        int x = mouseX + 12;
        if (x + width > screenWidth - 2) {
            x = mouseX - width - 12;
        }
        x = Math.max(2, x);
        int y = mouseY - height - 4;
        if (y < 2) {
            y = Math.min(mouseY + 16, screenHeight - height - 2);
        }
        y = Math.max(2, y);

        RenderUtils.drawOutlinedBox(ctx, x, y, width, height, 0xF0100010, 0xFF5000FF);
        int textX = x + padding;
        int textY = y + padding;
        for (String line : lines) {
            this.drawString(ctx, textX, textY, -1, this.truncate(line, maxContentWidth));
            textY += lineHeight;
        }

        for (int g = 0; g < shown; g++) {
            ContainerInfo container = containers.get(g);
            this.drawString(ctx, textX, textY, -6250336, this.truncate(labels.get(g), maxContentWidth));
            textY += lineHeight;

            this.drawSlotGrid(ctx, textX, textY, container);
            int rows = (container.gridSlots + container.gridColumns - 1) / container.gridColumns;
            textY += rows * cellSize + 4;
        }

        if (footer != null) {
            this.drawString(ctx, textX, textY, -6250336, this.truncate(footer, maxContentWidth));
        }
    }

    private boolean isOverSubWidget(int mouseX, int mouseY) {
        for (WidgetBase widget : this.subWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private ContainerInfo getCardAt(int mouseX, int mouseY) {
        if (this.entry.kind != StatsEntry.Kind.STRIP || mouseY < this.y || mouseY > this.y + this.height) {
            return null;
        }
        int cx = this.x;
        for (ContainerInfo container : this.entry.stripContainers) {
            int cardWidth = StatsSession.cardWidth(container);
            if (mouseX >= cx && mouseX < cx + cardWidth) {
                return container;
            }
            cx += cardWidth + StatsSession.CARD_GAP;
        }
        return null;
    }

    private int getCardX(ContainerInfo target) {
        int cx = this.x;
        for (ContainerInfo container : this.entry.stripContainers) {
            if (container == target) {
                return cx;
            }
            cx += StatsSession.cardWidth(container) + StatsSession.CARD_GAP;
        }
        return this.x;
    }

    @Nullable
    private ContainerInfo.SlotStack getSlotAt(ContainerInfo container, int mouseX, int mouseY) {
        int gx = this.getCardX(container) + StatsSession.CARD_PAD;
        int gy = this.y + StatsSession.CARD_GRID_TOP;
        if (mouseX < gx || mouseY < gy) {
            return null;
        }
        int col = (mouseX - gx) / StatsSession.CELL_SIZE;
        int row = (mouseY - gy) / StatsSession.CELL_SIZE;
        if (col >= container.gridColumns) {
            return null;
        }
        int slotIndex = row * container.gridColumns + col;
        if (slotIndex >= container.gridSlots) {
            return null;
        }
        ItemStack stack = container.getStackAt(slotIndex);
        return stack != null ? new ContainerInfo.SlotStack(slotIndex, stack) : null;
    }

    private void renderContainerHover(GuiContext ctx, int mouseX, int mouseY, ContainerInfo container) {
        List<String> lines = new ArrayList<>();
        String header = GuiBase.TXT_BOLD + container.typeName;
        if (container.doubleChest) {
            header += " [" + StringUtils.translate("litematica-stats.gui.label.double_chest") + "]";
        }
        lines.add(header);
        if (container.customName != null) {
            lines.add(GuiBase.TXT_AQUA + GuiBase.TXT_ITALIC + container.customName.getString());
        }
        if (container.mergedCount > 1) {
            lines.add(GuiBase.TXT_GOLD + StringUtils.translate("litematica-stats.gui.label.merged", container.mergedCount));
        }

        if (container.isEmpty()) {
            lines.add(GuiBase.TXT_RED + StringUtils.translate("litematica-stats.gui.label.empty"));
        } else {
            List<StatsEntry> itemRows = this.host.buildItemRows(container);
            int maxLines = 15;
            for (int i = 0; i < Math.min(itemRows.size(), maxLines); i++) {
                StatsEntry row = itemRows.get(i);
                Component customName = row.stack.get(DataComponents.CUSTOM_NAME);
                String line;
                if (customName != null) {
                    line = GuiBase.TXT_AQUA + GuiBase.TXT_ITALIC + customName.getString() + GuiBase.TXT_RST
                            + GuiBase.TXT_DARK_GRAY + " (" + row.stack.getItem().getName(row.stack).getString() + ")" + GuiBase.TXT_RST
                            + " x" + row.count;
                } else {
                    line = row.stack.getHoverName().getString() + " x" + row.count;
                }
                lines.add(line);
            }
            if (itemRows.size() > maxLines) {
                lines.add(GuiBase.TXT_GRAY + "... +" + (itemRows.size() - maxLines));
            }
        }
        RenderUtils.drawHoverText(ctx, mouseX + 10, mouseY, lines);
    }

    private void renderItemHover(GuiContext ctx, int mouseX, int mouseY) {
        InventoryOverlay.renderStackToolTipStyled(ctx, mouseX + 10, mouseY, this.entry.stack);
    }
}