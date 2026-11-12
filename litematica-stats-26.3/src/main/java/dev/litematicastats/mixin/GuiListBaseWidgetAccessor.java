package dev.litematicastats.mixin;

import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GuiListBase.class, remap = false)
public interface GuiListBaseWidgetAccessor {
    @Accessor("widget")
    void litematicastats$setWidget(WidgetListBase<?, ?> widget);
}