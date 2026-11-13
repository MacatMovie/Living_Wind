package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.BuiltinParticleResponseMultipliers;
import com.macatmovie.livingwind.client.DefaultEntryVisibility;
import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Per-particle wind strength, preserving only actual user overrides. */
public final class ParticleResponseMultiplierListScreen extends GroupedConfigListScreen {
    private final Map<String, String> overrides = new LinkedHashMap<>();
    private final Map<String, EditBox> boxes = new LinkedHashMap<>();
    public ParticleResponseMultiplierListScreen(Screen parent) {
        super(parent, "Particle Wind Response Multipliers");
        ParticleWindFilter.migrateLegacyStreamsDefaults();
        for (String raw : ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.get()) {
            int eq = raw.lastIndexOf('=');
            if (eq > 0) {
                String id = raw.substring(0, eq).trim();
                String value = raw.substring(eq + 1).trim();
                // Previous config versions pre-filled stock preset values into the user's
                // override list. Treat an exactly matching default as an unmodified preset.
                try {
                    if (BuiltinParticleResponseMultipliers.isDefault(id)
                            && Float.parseFloat(value) == BuiltinParticleResponseMultipliers.defaultValue(id)) continue;
                } catch (NumberFormatException ignored) {}
                overrides.put(id, value);
            }
        }
    }
    @Override protected String helpText() { return "1.0 = normal response, 0 = stationary, 10 = maximum response."; }
    @Override protected List<Entry> entries() {
        Set<String> all = new LinkedHashSet<>(DefaultEntryVisibility.installedOnly()
            ? BuiltinParticleResponseMultipliers.installedDefaults() : BuiltinParticleResponseMultipliers.allDefaults());
        all.addAll(overrides.keySet());
        List<Entry> result = new ArrayList<>();
        for (String id : all) result.add(new Entry(id, BuiltinParticleResponseMultipliers.isDefault(id), overrides.containsKey(id)));
        return result;
    }
    private String value(String id) { return overrides.getOrDefault(id, Float.toString(BuiltinParticleResponseMultipliers.defaultValue(id))); }
    @Override protected void addEntryWidgets(Entry e, int left, int y) {
        EditBox box = new EditBox(font, left + 301, y, 80, 20, Component.literal("Multiplier"));
        box.setFilter(v -> v.isEmpty() || v.matches("\\d*(?:\\.\\d*)?"));
        box.setValue(value(e.id()));
        box.setEditable(!e.builtin() || editDefaults);
        box.setResponder(v -> update(e.id(), v));
        boxes.put(e.id(), addRowWidget(box));
        if (!e.builtin() || editDefaults) {
            Button.OnPress reset = b -> {
                commitVisible();
                overrides.remove(e.id());
                refreshRows();
            };
            addRowWidget(e.builtin()
                ? new RestoreDefaultButton(left + 413, y, 25, 20, reset, "Reset to default multiplier")
                : Button.builder(Component.literal("×"), reset)
                    .tooltip(Tooltip.create(Component.literal("Remove custom multiplier")))
                    .bounds(left + 413, y, 25, 20).build());
        }
    }
    private void update(String id, String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            double n = Double.parseDouble(raw);
            if (Double.isFinite(n) && n >= 0 && n <= 10) overrides.put(id, raw);
        } catch (NumberFormatException ignored) {}
    }
    @Override protected void beforeRowsChange() { commitVisible(); }
    private void commitVisible() {
        for (var e : boxes.entrySet()) {
            if (BuiltinParticleResponseMultipliers.isDefault(e.getKey()) && !overrides.containsKey(e.getKey())) continue;
            update(e.getKey(), e.getValue().getValue());
        }
        boxes.clear();
    }
    private void add(String id) { overrides.putIfAbsent(id, "1.0"); refreshRows(); }
    @Override protected void onAddEntry() {
        minecraft.setScreen(new ParticlePickerScreen(this, new HashSet<>(allIds()), this::add,
            Component.literal("Add Particle Response"), id -> false));
    }
    private List<String> allIds() { List<String> ids = new ArrayList<>(); for (Entry e : entries()) ids.add(e.id()); return ids; }
    @Override protected void onSave() {
        List<String> encoded = new ArrayList<>();
        for (var e : overrides.entrySet()) encoded.add(e.getKey() + "=" + e.getValue());
        ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.set(encoded);
        ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.save();
        ParticleWindFilter.refresh();
        minecraft.setScreen(parent);
    }
}
