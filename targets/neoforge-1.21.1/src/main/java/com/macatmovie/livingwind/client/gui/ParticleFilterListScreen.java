package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.BuiltinParticleBlacklist;
import com.macatmovie.livingwind.config.ClientWindConfig;

import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

/** Shows only the IDs currently in the filter. Ordering is intentionally irrelevant. */
public final class ParticleFilterListScreen extends ConfigurationScreen.ConfigurationListScreen<String> {
    public ParticleFilterListScreen(
        Context context,
        String key,
        Component title,
        ModConfigSpec.ListValueSpec spec,
        ModConfigSpec.ConfigValue<java.util.List<String>> valueList
    ) {
        super(context, key, title, spec, valueList);
    }

    @Override
    protected AbstractWidget createListLabel(int idx) {
        Button remove = Button.builder(Component.literal("×"), button -> del(idx, false))
            .bounds(0, 0, 20, 20)
            .build();
        remove.setTooltip(Tooltip.create(Component.literal(
            "Remove custom entry (built-in blacklist exclusions remain active)")));
        return remove;
    }

    @Override
    @Nullable
    protected Element createStringListValue(int idx, String value) {
        boolean builtin = ClientWindConfig.PARTICLE_FILTER_MODE.get() == ClientWindConfig.FilterMode.BLACKLIST
            && BuiltinParticleBlacklist.contains(value);
        StringWidget label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT,
            Component.literal("✓  " + value).withStyle(builtin ? ChatFormatting.YELLOW : ChatFormatting.GREEN), font);
        return new Element(null, null, label, false);
    }

    @Override
    protected void createAddElementButton() {
        // The picker displays the complete registry including read-only built-in exclusions.
        // Only user additions are saved in this configuration list.
        list.addSmall(
            new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT,
                Component.literal("Yellow: built-in  |  Green: custom").withStyle(ChatFormatting.GRAY), font),
            Button.builder(Component.translatable("livingwind.config.particleFilter.addMore"), button ->
                minecraft.setScreen(new ParticlePickerScreen(
                    this,
                    new HashSet<>(cfgList),
                    this::addParticleId
                ))
            ).build()
        );
    }

    private void addParticleId(String id) {
        if (cfgList.contains(id)) return;
        cfgList = new ArrayList<>(cfgList);
        cfgList.add(id);
        onChanged(key);
        rebuild();
    }
}
