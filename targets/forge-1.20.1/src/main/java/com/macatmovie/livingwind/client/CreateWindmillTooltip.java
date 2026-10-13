package com.macatmovie.livingwind.client;

import com.macatmovie.livingwind.LivingWind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One optional cyan line for Create's Windmill Bearing; never links to Create classes. */
@Mod.EventBusSubscriber(modid = LivingWind.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CreateWindmillTooltip {
    private CreateWindmillTooltip() {}

    @SubscribeEvent
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
