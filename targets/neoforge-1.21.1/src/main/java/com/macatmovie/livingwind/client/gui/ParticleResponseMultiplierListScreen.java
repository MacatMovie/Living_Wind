package com.macatmovie.livingwind.client.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Order-independent particle response list. Each row is stored as modid:particle=multiplier.
 *
 * The multiplier editor is bound directly to cfgList instead of relying on NeoForge's generic
 * string-list editor to commit the focused text box when Done is pressed. This matters because
 * the generic list editor can leave the last focused string value uncommitted when closing the
 * nested screen, which made multiplier edits appear to revert.
 */
public final class ParticleResponseMultiplierListScreen extends ConfigurationScreen.ConfigurationListScreen<String> {
    public ParticleResponseMultiplierListScreen(
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
        remove.setTooltip(Tooltip.create(Component.translatable("livingwind.config.responseMultipliers.removeTooltip")));
        return remove;
    }

    @Override
    @Nullable
    protected Element createStringListValue(int idx, String value) {
        EditBox box = new EditBox(font, 0, 0, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT,
            Component.translatable("livingwind.config.responseMultipliers.title"));
        box.setMaxLength(256);
        box.setValue(value);

        // Capture the ID for this row. The UI is meant for changing the multiplier; IDs are added
        // through the particle picker. Invalid/intermediate text is allowed in the box while typing,
        // but cfgList is updated immediately whenever the complete entry is valid.
        int eq = value.lastIndexOf('=');
        String particleId = (eq > 0 ? value.substring(0, eq) : value).trim();
        box.setResponder(text -> {
            String normalized = normalizeEntry(particleId, text);
            if (normalized == null) return;
            if (idx < 0 || idx >= cfgList.size() || normalized.equals(cfgList.get(idx))) return;

            cfgList = new ArrayList<>(cfgList);
            cfgList.set(idx, normalized);
            onChanged(key);
        });

        return new Element(null, null, box, false);
    }

    /**
     * Accept both the normal full entry shown by the field (id=value) and just a number. Supporting
     * a number-only value makes this robust if NeoForge changes how it initializes list text fields.
     */
    @Nullable
    private static String normalizeEntry(String particleId, String text) {
        String trimmed = text.trim();
        String multiplierText;
        int eq = trimmed.lastIndexOf('=');
        if (eq >= 0) {
            String typedId = trimmed.substring(0, eq).trim();
            if (!typedId.equals(particleId)) return null;
            multiplierText = trimmed.substring(eq + 1).trim();
        } else {
            multiplierText = trimmed;
        }
        if (multiplierText.isEmpty()) return null;

        try {
            double multiplier = Double.parseDouble(multiplierText);
            if (!Double.isFinite(multiplier) || multiplier < 0.0 || multiplier > 10.0) return null;
            return particleId + "=" + multiplierText;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    protected void createAddElementButton() {
        list.addSmall(
            new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, Component.empty(), font),
            Button.builder(Component.translatable("livingwind.config.responseMultipliers.addMore"), button ->
                minecraft.setScreen(new ParticlePickerScreen(
                    this,
                    selectedIds(),
                    this::addParticleId,
                    Component.translatable("livingwind.config.responseMultipliers.pickerTitle")
                ))
            ).build()
        );
    }

    private Set<String> selectedIds() {
        Set<String> result = new HashSet<>();
        for (String entry : cfgList) {
            int eq = entry.lastIndexOf('=');
            result.add((eq > 0 ? entry.substring(0, eq) : entry).trim());
        }
        return result;
    }

    private void addParticleId(String id) {
        if (selectedIds().contains(id)) return;
        cfgList = new ArrayList<>(cfgList);
        cfgList.add(id + "=1.0");
        onChanged(key);
        rebuild();
    }
}
