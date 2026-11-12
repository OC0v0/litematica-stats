package dev.litematicastats.mixin;

import dev.litematicastats.gui.StatsHost;
import dev.litematicastats.gui.StatsTab;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "fi.dy.masa.litematica.gui.GuiMaterialList$MultiplierListener", remap = false)
public abstract class MixinMultiplierListener {
    @Accessor("gui")
    public abstract GuiMaterialList litematicastats$getGui();

    @Inject(method = "onTextChange", at = @At("HEAD"), cancellable = true, remap = false)
    private void litematicastats$guardMultiplier(CallbackInfoReturnable<Boolean> cir) {
        if (((StatsHost) this.litematicastats$getGui()).getStatsTab() != StatsTab.BLOCKS) {
            cir.setReturnValue(false);
        }
    }
}