package dev.litematicastats.gui;

import dev.litematicastats.data.ContainerInfo;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import com.mojang.blaze3d.vertex.PoseStack;
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
    private String displayText;
    private String countText;
    private String headerFirst;
    private String headerSecond;
    private List<CardRender> cards;

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
                boolean ignored = host.isIgnored(container);
                String label = ignored ? StatsSession.ignoredLabel() : StatsSession.ignoreLabel();
                int btnWidth = (ignored ? StatsSession.ignoredLabelWidth() : StatsSession.ignoreLabelWidth()) + 10;
                ButtonGeneric button = new ButtonGeneric(cx + cardWidth - btnWidth - StatsSession.CARD_PAD, y + 3, btnWidth, 14, label);
                this.addButton(button, (btn, mouseButton) -> {
                    host.toggleIgnored(container);
                    listWidget.refreshEntries();
                });
                cx += cardWidth + StatsSession.CARD_GAP;
            }
        } else if ((entry.kind == StatsEntry.Kind.CATEGORY && !StatsSession.IGNORED_KEY.equals(entry.categoryKey))
                || entry.kind == StatsEntry.Kind.SINGLE_GROUP) {
            boolean ignored = host.isCategoryIgnored(entry.categoryKey);
            String label = ignored ? StatsSession.ignoredLabel() : StatsSession.ignoreLabel();
            int btnWidth = (ignored ? StatsSession.ignoredLabelWidth() : StatsSession.ignoreLabelWidth()) + 10;
            ButtonGeneric button = new ButtonGeneric(x + width - btnWidth - 4, y + 4, btnWidth, 14, label);
            this.addButton(button, (btn, mouseButton) -> host.toggleCategoryIgnored(entry.categoryKey));
        }

        // 文本与测宽只在构建时计算一次，渲染时直接复用，避免每帧翻译与字体测量。
        switch (entry.kind) {
            case ENTITY -> {
                String name = entry.entity.name().getString();
                if (entry.entity.hasInventory()) {
                    name += GuiBase.TXT_GOLD + " *" + GuiBase.TXT_RST;
                }
                this.displayText = name;
                this.countText = String.valueOf(entry.entity.count());
            }
            case ITEM -> {
                boolean aggregate = this.isAggregateItemView();
                int textX = aggregate ? this.getColumnPosX(0) + 20 : this.x + 44;
                int countX = aggregate ? this.getColumnPosX(1) : this.x + this.width - 60;
                this.displayText = this.truncate(this.itemDisplayName(entry.stack), countX - textX - 8);
                this.countText = aggregate ? String.valueOf(entry.count) : "x" + entry.count;
            }
            case CATEGORY, SINGLE_GROUP -> {
                String color = entry.kind == StatsEntry.Kind.SINGLE_GROUP ? GuiBase.TXT_LIGHT_PURPLE : GuiBase.TXT_AQUA;
                int tx = this.x + (entry.holderIcon != null ? 24 : 4);
                int titleMax = this.x + this.width - tx - 24 - StatsSession.ignoredLabelWidth();
                this.displayText = color + GuiBase.TXT_BOLD + this.truncate(entry.title, Math.max(20, titleMax)) + GuiBase.TXT_RST;
            }
            case HEADER -> {
                String firstColumn = this.isAggregateItemView() ? "litematica-stats.gui.label.column.item" : "litematica-stats.gui.label.column.name";
                this.headerFirst = GuiBase.TXT_BOLD + StringUtils.translate(firstColumn) + GuiBase.TXT_RST;
                this.headerSecond = GuiBase.TXT_BOLD + StringUtils.translate("litematica-stats.gui.label.column.count") + GuiBase.TXT_RST;
            }
            case STRIP -> {
                this.cards = new ArrayList<>(entry.stripContainers.size());
                for (ContainerInfo container : entry.stripContainers) {
                    int cardWidth = StatsSession.cardWidth(container);
                    int cardHeight = StatsSession.cardHeight(container);
                    String title = container.typeName;
                    if (container.customName != null) {
                        title += GuiBase.TXT_AQUA + GuiBase.TXT_ITALIC + " \"" + container.customName.getString() + "\"" + GuiBase.TXT_RST;
                    }
                    if (host.isIgnored(container)) {
                        title += GuiBase.TXT_RED + " [" + StatsSession.ignoredLabel() + "]" + GuiBase.TXT_RST;
                    }
                    String badge = container.mergedCount > 1 ? "x" + container.mergedCount : null;
                    int badgeTextWidth = badge != null ? this.getStringWidth(badge) : 0;
                    int badgeX = cardWidth - StatsSession.ignoredButtonWidth() - 8 - badgeTextWidth;
                    int titleMax = badgeX - 4 - StatsSession.CARD_PAD;
                    this.cards.add(new CardRender(cardWidth, cardHeight, badgeX,
                            this.truncate(title, Math.max(titleMax, 20)), badge));
                }
            }
            default -> {
            }
        }
    }

    private static final class CardRender {
        final int width;
        final int height;
        final int badgeX;
        final String title;
        @Nullable
        final String badge;

        CardRender(int width, int height, int badgeX, String title, @Nullable String badge) {
            this.width = width;
            this.height = height;
            this.badgeX = badgeX;
            this.title = title;
            this.badge = badge;
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
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton) {
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
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.listWidget.isWithinBrowser(mouseY)) {
            return false;
        }
        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton) {
        switch (this.entry.kind) {
            case HEADER -> {
                int column = this.getMouseOverColumn(mouseX, mouseY);
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
    public void render(int mouseX, int mouseY, boolean selected, PoseStack ctx) {
        int bgColor = this.isMouseOver(mouseX, mouseY) ? COLOR_HOVER : (this.isOdd ? COLOR_ODD : COLOR_EVEN);
        RenderUtils.drawRect(this.x, this.y, this.width, this.height, bgColor);

        switch (this.entry.kind) {
            case HEADER -> this.renderHeader(ctx, mouseX, mouseY);
            case ENTITY -> this.renderEntity(ctx);
            case CATEGORY, SINGLE_GROUP -> this.renderCategory(ctx);
            case STRIP -> this.renderStrip(ctx);
            case ITEM -> this.renderItem(ctx);
            default -> {
            }
        }

        super.render(mouseX, mouseY, selected, ctx);
    }

    private void renderHeader(PoseStack ctx, int mouseX, int mouseY) {
        int y = this.y + 7;
        this.drawString(this.getColumnPosX(0), y, -1, this.headerFirst, ctx);
        this.drawString(this.getColumnPosX(1), y, -1, this.headerSecond, ctx);
        if (!this.listWidget.getSearchBarWidget().isSearchOpen()) {
            this.renderColumnHeader(mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);
        }
    }

    private void renderEntity(PoseStack ctx) {
        int x1 = this.getColumnPosX(0);
        int y = this.y + 3;
        RenderUtils.drawRect(x1, y, 16, 16, COLOR_ICON_BG);
        this.renderGuiItem(ctx, this.entry.entity.icon(), x1, y);
        this.drawString(x1 + 20, this.y + 7, -1, this.displayText, ctx);
        this.drawString(this.getColumnPosX(1), this.y + 7, -1, this.countText, ctx);
    }

    private void renderCategory(PoseStack ctx) {
        boolean collapsed = this.host.isCategoryCollapsed(this.entry.categoryKey);
        int tx = this.x + 4;
        if (this.entry.holderIcon != null) {
            int y = this.y + 3;
            RenderUtils.drawRect(this.x + 4, y, 16, 16, COLOR_ICON_BG);
            this.renderGuiItem(ctx, this.entry.holderIcon, this.x + 4, y);
            tx = this.x + 24;
        }
        this.renderTriangle(tx, this.y + 7, collapsed);
        this.drawString(tx + 12, this.y + 7, -1, this.displayText, ctx);
    }

    private void renderTriangle(int x, int y, boolean collapsed) {
        int color = 0xFFE0E0E0;
        for (int i = 0; i < 5; i++) {
            int len = 9 - 2 * i;
            if (collapsed) {
                RenderUtils.drawRect(x + i, y + i, 1, len, color);
            } else {
                RenderUtils.drawRect(x + i, y + i, len, 1, color);
            }
        }
    }

    private void renderStrip(PoseStack ctx) {
        int cx = this.x;
        for (int i = 0; i < this.entry.stripContainers.size(); i++) {
            ContainerInfo container = this.entry.stripContainers.get(i);
            CardRender card = this.cards.get(i);
            boolean ignored = this.host.isIgnored(container);
            RenderUtils.drawRect(cx, this.y, card.width, card.height, ignored ? 0x60402020 : 0x60000000);
            RenderUtils.drawOutline(cx, this.y, card.width, card.height, ignored ? 0xFF804040 : 0xFF606060);
            this.drawString(cx + StatsSession.CARD_PAD, this.y + 6, -1, card.title, ctx);
            if (card.badge != null) {
                this.drawString(cx + card.badgeX, this.y + 6, -1, GuiBase.TXT_GOLD + card.badge + GuiBase.TXT_RST, ctx);
            }
            this.drawSlotGrid(ctx, cx + StatsSession.CARD_PAD, this.y + StatsSession.CARD_GRID_TOP, container);
            cx += card.width + StatsSession.CARD_GAP;
        }
    }

    private void renderGuiItem(PoseStack ps, ItemStack stack, int x, int y) {
        ps.pushPose();
        RenderUtils.enableDiffuseLightingGui3D();
        this.mc.getItemRenderer().renderGuiItem(ps, stack, x, y);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        RenderUtils.disableDiffuseLighting();
        ps.popPose();
    }

    private void renderGuiItemDecorations(PoseStack ps, ItemStack stack, int x, int y) {
        ps.pushPose();
        this.mc.getItemRenderer().renderGuiItemDecorations(ps, this.textRenderer, stack, x, y);
        ps.popPose();
    }

    private void drawSlotGrid(PoseStack ctx, int x, int y, ContainerInfo container) {
        for (int i = 0; i < container.gridSlots; i++) {
            int cx = x + (i % container.gridColumns) * StatsSession.CELL_SIZE;
            int cy = y + (i / container.gridColumns) * StatsSession.CELL_SIZE;
            RenderUtils.drawRect(cx, cy, StatsSession.CELL_SIZE, StatsSession.CELL_SIZE, 0xFF8B8B8B);
            RenderUtils.drawRect(cx + 1, cy + 1, StatsSession.CELL_SIZE - 2, StatsSession.CELL_SIZE - 2, 0xFF373737);
            ItemStack stack = container.getStackAt(i);
            if (stack != null) {
                this.renderGuiItem(ctx, stack, cx + 1, cy + 1);
                this.renderGuiItemDecorations(ctx, stack, cx + 1, cy + 1);
            }
        }
    }

    private void renderItem(PoseStack ctx) {
        ItemStack stack = this.entry.stack;
        int y = this.y + 3;
        boolean aggregate = this.isAggregateItemView();
        int iconX = aggregate ? this.getColumnPosX(0) : this.x + 24;
        int textX = aggregate ? iconX + 20 : this.x + 44;
        RenderUtils.drawRect(iconX, y, 16, 16, COLOR_ICON_BG);
        this.renderGuiItem(ctx, stack, iconX, y);
        int countX = aggregate ? this.getColumnPosX(1) : this.x + this.width - 60;
        this.drawString(textX, this.y + 7, -1, this.displayText, ctx);
        this.drawString(countX, this.y + 7, -1, this.countText, ctx);
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
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, PoseStack ctx) {
        switch (this.entry.kind) {
            case ENTITY -> this.renderEntityHover(ctx, mouseX, mouseY);
            case CATEGORY -> RenderUtils.drawHoverText(mouseX + 10, mouseY,
                    List.of(StringUtils.translate("litematica-stats.gui.hover.category_hint")), ctx);
            case SINGLE_GROUP -> RenderUtils.drawHoverText(mouseX + 10, mouseY,
                    List.of(StringUtils.translate("litematica-stats.gui.hover.single_group_hint")), ctx);
            case STRIP -> {
                if (this.isOverSubWidget(mouseX, mouseY) || !this.listWidget.isWithinBrowser(mouseY)) {
                    return;
                }
                ContainerInfo hovered = this.getCardAt(mouseX, mouseY);
                if (hovered != null) {
                    ContainerInfo.SlotStack slot = this.getSlotAt(hovered, mouseX, mouseY);
                    if (slot != null) {
                        this.host.renderItemTooltip(ctx, slot.stack(), mouseX, mouseY);
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

    private void renderEntityHover(PoseStack ctx, int mouseX, int mouseY) {
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
            RenderUtils.drawHoverText(mouseX + 10, mouseY, lines, ctx);
            return;
        }
        this.renderEntityGridTooltip(ctx, mouseX, mouseY, lines, entityContainers);
    }

    private String itemDisplayName(ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            return GuiBase.TXT_AQUA + GuiBase.TXT_ITALIC + stack.getHoverName().getString() + GuiBase.TXT_RST
                    + GuiBase.TXT_DARK_GRAY + " (" + stack.getItem().getName(stack).getString() + ")" + GuiBase.TXT_RST;
        }
        return stack.getHoverName().getString();
    }

    private void renderEntityGridTooltip(PoseStack ctx, int mouseX, int mouseY, List<String> lines, List<ContainerInfo> containers) {
        final int padding = 6;
        final int lineHeight = 10;
        final int cellSize = 18;
        final int maxGrids = 5;
        int screenWidth = GuiUtils.getScaledWindowWidth();
        int screenHeight = GuiUtils.getScaledWindowHeight();

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

        RenderUtils.drawOutlinedBox(x, y, width, height, 0xF0100010, 0xFF5000FF);
        int textX = x + padding;
        int textY = y + padding;
        for (String line : lines) {
            this.drawString(textX, textY, -1, this.truncate(line, maxContentWidth), ctx);
            textY += lineHeight;
        }

        for (int g = 0; g < shown; g++) {
            ContainerInfo container = containers.get(g);
            this.drawString(textX, textY, -6250336, this.truncate(labels.get(g), maxContentWidth), ctx);
            textY += lineHeight;

            this.drawSlotGrid(ctx, textX, textY, container);
            int rows = (container.gridSlots + container.gridColumns - 1) / container.gridColumns;
            textY += rows * cellSize + 4;
        }

        if (footer != null) {
            this.drawString(textX, textY, -6250336, this.truncate(footer, maxContentWidth), ctx);
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

    private void renderContainerHover(PoseStack ctx, int mouseX, int mouseY, ContainerInfo container) {
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
                String line;
                if (row.stack.hasCustomHoverName()) {
                    line = GuiBase.TXT_AQUA + GuiBase.TXT_ITALIC + row.stack.getHoverName().getString() + GuiBase.TXT_RST
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
        RenderUtils.drawHoverText(mouseX + 10, mouseY, lines, ctx);
    }

    private void renderItemHover(PoseStack ctx, int mouseX, int mouseY) {
        this.host.renderItemTooltip(ctx, this.entry.stack, mouseX, mouseY);
    }
}