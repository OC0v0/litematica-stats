package dev.litematicastats.gui;

import fi.dy.masa.malilib.util.StringUtils;

public enum StatsTab {
    BLOCKS("litematica-stats.gui.tab.blocks"),
    ENTITIES("litematica-stats.gui.tab.entities"),
    CONTENTS("litematica-stats.gui.tab.contents");

    private final String labelKey;

    StatsTab(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabel() {
        return StringUtils.translate(this.labelKey);
    }
}