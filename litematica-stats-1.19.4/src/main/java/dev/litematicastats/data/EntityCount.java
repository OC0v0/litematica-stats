package dev.litematicastats.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public record EntityCount(String id, @Nullable EntityType<?> type, int count, ItemStack icon, Component name, boolean hasInventory) {
}