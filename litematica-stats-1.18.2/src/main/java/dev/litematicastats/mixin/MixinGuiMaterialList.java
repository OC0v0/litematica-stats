package dev.litematicastats.mixin;

import dev.litematicastats.gui.StatsEntry;
import dev.litematicastats.gui.StatsHost;
import dev.litematicastats.gui.StatsSession;
import dev.litematicastats.gui.StatsTab;
import dev.litematicastats.gui.WidgetListStats;
import dev.litematicastats.gui.WidgetStatsEntry;
import dev.litematicastats.data.ContainerInfo;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.materials.MaterialListSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(value = GuiMaterialList.class, remap = false)
public abstract class MixinGuiMaterialList extends GuiListBase<MaterialListEntry, WidgetMaterialListEntry, WidgetListMaterialList> implements StatsHost {
    private MixinGuiMaterialList() {
        super(0, 0);
    }

    @Shadow
    public abstract MaterialListBase getMaterialList();

    @Unique
    private StatsTab litematicastats$tab = StatsTab.BLOCKS;
    @Unique
    @Nullable
    private StatsSession litematicastats$session;

    @Inject(method = "initGui", at = @At("HEAD"), remap = false)
    private void litematicastats$moveListDown(CallbackInfo ci) {
        this.setListPosition(10, 66);
    }

    @Inject(method = "getBrowserHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void litematicastats$browserHeight(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.height - 102);
    }

    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    private void litematicastats$addTabs(CallbackInfo ci) {
        int x = 12;
        int y = 44;
        for (StatsTab t : StatsTab.values()) {
            String label = t.getLabel();
            int width = this.getStringWidth(label) + 16;
            ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label, new String[0]);
            button.setEnabled(t != this.litematicastats$tab);
            this.addButton(button, (btn, mouseButton) -> this.setStatsTab(t));
            x += width + 2;
        }

        if (this.litematicastats$tab != StatsTab.BLOCKS && this.litematicastats$session != null) {
            if (this.litematicastats$tab == StatsTab.CONTENTS) {
                String hideLabel = this.litematicastats$onOffLabel("litematica-stats.gui.button.hide_empty",
                        this.litematicastats$session.isHideEmpty());
                ButtonGeneric hideEmptyButton = new ButtonGeneric(x, y, this.getStringWidth(hideLabel) + 10, 20, hideLabel, new String[0]);
                this.addButton(hideEmptyButton, (btn, mouseButton) -> {
                    this.litematicastats$session.toggleHideEmpty();
                    this.initGui();
                });
                x += hideEmptyButton.getWidth() + 2;

                String aggregateLabel = this.litematicastats$onOffLabel("litematica-stats.gui.button.aggregate_items",
                        this.litematicastats$session.isAggregateItems());
                ButtonGeneric aggregateButton = new ButtonGeneric(x, y, this.getStringWidth(aggregateLabel) + 10, 20, aggregateLabel, new String[0]);
                this.addButton(aggregateButton, (btn, mouseButton) -> this.toggleAggregateItems());
                x += aggregateButton.getWidth() + 8;
            }
            String summary = this.litematicastats$summary();
            this.addLabel(x, y + 6, this.getStringWidth(summary) + 4, 12, -1, summary);
        }
    }

    @Unique
    private String litematicastats$onOffLabel(String labelKey, boolean isOn) {
        String state = isOn
                ? TXT_GREEN + StringUtils.translate("litematica-stats.gui.label.on")
                : TXT_RED + StringUtils.translate("litematica-stats.gui.label.off");
        return StringUtils.translate(labelKey) + "：" + state + TXT_RST;
    }

    @Unique
    private String litematicastats$summary() {
        if (this.litematicastats$session == null || !this.litematicastats$session.hasSchematic()) {
            return StringUtils.translate("litematica-stats.gui.label.no_schematic_data");
        }
        if (this.litematicastats$tab == StatsTab.ENTITIES) {
            return StringUtils.translate("litematica-stats.gui.label.total_entities", this.litematicastats$session.getEntityTotal());
        }
        if (this.litematicastats$session.isAggregateItems()) {
            return StringUtils.translate("litematica-stats.gui.label.total_items_aggregated",
                    this.litematicastats$session.aggregatedTypeCount, this.litematicastats$session.getVisibleItemTotal());
        }
        return StringUtils.translate("litematica-stats.gui.label.total_containers",
                this.litematicastats$session.getVisibleContainerCount(), this.litematicastats$session.getVisibleItemTotal());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @Unique
    public void setStatsTab(StatsTab tab) {
        this.litematicastats$tab = tab;
        if (tab != StatsTab.BLOCKS && this.litematicastats$session == null) {
            this.litematicastats$session = new StatsSession(this.litematicastats$resolveSchematic());
        }

        WidgetListBase widget;
        if (tab == StatsTab.BLOCKS) {
            widget = new WidgetListMaterialList(this.getListX(), this.getListY(), this.getBrowserWidth(), this.getBrowserHeight(), (GuiMaterialList) (Object) this);
        } else {
            widget = new WidgetListStats(this.getListX(), this.getListY(), this.getBrowserWidth(), this.getBrowserHeight(), this);
        }
        ((GuiListBaseWidgetAccessor) this).litematicastats$setWidget(widget);
        this.initGui();
    }

    @Unique
    @Nullable
    private LitematicaSchematic litematicastats$resolveSchematic() {
        MaterialListBase materialList = this.getMaterialList();
        if (materialList instanceof MaterialListPlacement) {
            SchematicPlacement placement = ((MaterialListPlacementAccessor) materialList).litematicastats$getPlacement();
            return placement != null ? placement.getSchematic() : null;
        }
        if (materialList instanceof MaterialListSchematic) {
            return ((MaterialListSchematicAccessor) materialList).litematicastats$getSchematic();
        }
        return null;
    }

    @Override
    @Unique
    public StatsTab getStatsTab() {
        return this.litematicastats$tab;
    }

    @Override
    @Unique
    public List<StatsEntry> buildStatsRows() {
        return this.litematicastats$session != null ? this.litematicastats$session.buildRows(this.litematicastats$tab) : List.of();
    }

    @Override
    @Unique
    public List<StatsEntry> buildItemRows(ContainerInfo container) {
        return this.litematicastats$session != null ? this.litematicastats$session.buildItemRows(container) : List.of();
    }

    @Override
    @Unique
    public List<ContainerInfo> getEntityContainers(String entityId) {
        return this.litematicastats$session != null ? this.litematicastats$session.getEntityContainers(entityId) : List.of();
    }

    @Override
    @Unique
    public void renderItemTooltip(com.mojang.blaze3d.vertex.PoseStack ctx, net.minecraft.world.item.ItemStack stack, int mouseX, int mouseY) {
        this.renderTooltip(ctx, stack, mouseX, mouseY);
    }

    @Override
    @Unique
    public void setHoveredEntryWidget(WidgetStatsEntry entry) {
        // 1.18.2 的 GuiListBase 不会把 drawHoveredWidget 转发给列表控件，
        // 只能把悬停项放到屏幕级字段上，供 GuiBase.drawHoveredWidget 调用。
        this.hoveredWidget = entry;
    }

    @Override
    @Unique
    public int getStatsSortColumn() {
        return this.litematicastats$session != null ? this.litematicastats$session.getSortColumn() : 0;
    }

    @Override
    @Unique
    public boolean isStatsSortReverse() {
        return this.litematicastats$session != null && this.litematicastats$session.isSortReverse();
    }

    @Override
    @Unique
    public void setStatsSort(int column) {
        if (this.litematicastats$session != null) {
            this.litematicastats$session.setSort(column);
        }
    }

    @Override
    @Unique
    public boolean isCategoryCollapsed(String key) {
        return this.litematicastats$session != null && this.litematicastats$session.isCategoryCollapsed(key);
    }

    @Override
    @Unique
    public void toggleCategory(String key) {
        if (this.litematicastats$session != null) {
            this.litematicastats$session.toggleCategory(key);
        }
    }

    @Override
    @Unique
    public boolean isIgnored(ContainerInfo container) {
        return this.litematicastats$session != null && this.litematicastats$session.isIgnored(container);
    }

    @Override
    @Unique
    public void toggleIgnored(ContainerInfo container) {
        if (this.litematicastats$session != null) {
            this.litematicastats$session.toggleIgnored(container);
            this.initGui();
        }
    }

    @Override
    @Unique
    public boolean isCategoryIgnored(String typeKey) {
        return this.litematicastats$session != null && this.litematicastats$session.isCategoryIgnored(typeKey);
    }

    @Override
    @Unique
    public void toggleCategoryIgnored(String typeKey) {
        if (this.litematicastats$session != null) {
            this.litematicastats$session.toggleCategoryIgnored(typeKey);
            this.initGui();
        }
    }

    @Override
    @Unique
    public void setContentsViewWidth(int width) {
        if (this.litematicastats$session != null) {
            this.litematicastats$session.setContentsViewWidth(width);
        }
    }

    @Override
    @Unique
    public void setContentsFilter(String filter) {
        if (this.litematicastats$session != null) {
            this.litematicastats$session.setContentsFilter(filter);
        }
    }

    @Override
    @Unique
    public boolean shouldShowRegion() {
        return this.litematicastats$session != null && this.litematicastats$session.shouldShowRegion();
    }

    @Override
    @Unique
    public boolean isAggregateItems() {
        return this.litematicastats$session != null && this.litematicastats$session.isAggregateItems();
    }

    @Override
    @Unique
    public void toggleAggregateItems() {
        if (this.litematicastats$session != null) {
            this.litematicastats$session.toggleAggregateItems();
            this.initGui();
        }
    }

    @Override
    @Unique
    public int getMaxEntityNameWidth() {
        return this.litematicastats$session != null ? this.litematicastats$session.maxEntityNameWidth : 0;
    }

    @Override
    @Unique
    public int getMaxEntityCountWidth() {
        return this.litematicastats$session != null ? this.litematicastats$session.maxEntityCountWidth : 0;
    }

    @Override
    @Unique
    public int getMaxAggregateItemNameWidth() {
        return this.litematicastats$session != null ? this.litematicastats$session.maxAggregateItemNameWidth : 0;
    }
}