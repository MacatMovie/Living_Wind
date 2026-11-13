package com.macatmovie.livingwind.client;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import java.util.List;

/** One optional cyan line for Create's Windmill Bearing; never links to Create classes. */
public final class CreateWindmillTooltip {
    private CreateWindmillTooltip() {}

    public static void onTooltip(ItemStack stack, List<Component> tooltip) {
        if (!ClientWindState.createWindmillsEnabled()) return;
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"create".equals(itemId.getNamespace()) || !"windmill_bearing".equals(itemId.getPath())) return;
        tooltip.add(Component.translatable("tooltip.livingwind.create_windmill_bearing_header")
            .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.livingwind.create_windmill_bearing")
            .withStyle(ChatFormatting.AQUA));
    }
}
