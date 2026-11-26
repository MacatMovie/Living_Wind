package com.macatmovie.livingwind.client;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** One optional cyan line for Create's Windmill Bearing; never links to Create classes. */
public final class CreateWindmillTooltip {
    private CreateWindmillTooltip() {}

    public static void onTooltip(ItemTooltipEvent event) {
        if (!ClientWindState.createWindmillsEnabled()) return;
        var itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!"create".equals(itemId.getNamespace()) || !"windmill_bearing".equals(itemId.getPath())) return;
        event.getToolTip().add(Component.translatable("tooltip.livingwind.create_windmill_bearing_header")
            .withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable("tooltip.livingwind.create_windmill_bearing")
            .withStyle(ChatFormatting.AQUA));
    }
}
