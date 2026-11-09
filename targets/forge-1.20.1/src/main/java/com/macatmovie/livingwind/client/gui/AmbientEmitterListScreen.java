package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.AmbientEmitterBlocks;
import com.macatmovie.livingwind.client.DefaultEntryVisibility;
import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Natural blocks that emit ambient pixels; built-in and user override values stay separate. */
public final class AmbientEmitterListScreen extends GroupedConfigListScreen {
    private final Map<String, AmbientEmitterBlocks.Kind> overrides = new LinkedHashMap<>();
    public AmbientEmitterListScreen(Screen parent) {
        super(parent, "Ambient Pixel Block Emitters");
        for (String raw : ClientWindConfig.AMBIENT_BLOCK_EMITTER_OVERRIDES.get()) {
            if (!AmbientEmitterBlocks.validEntry(raw)) continue;
            int eq = raw.lastIndexOf('=');
            overrides.put(raw.substring(0, eq).trim().toLowerCase(Locale.ROOT),
                AmbientEmitterBlocks.Kind.valueOf(raw.substring(eq + 1).trim().toUpperCase(Locale.ROOT)));
        }
    }
    @Override protected String helpText() { return "Click emitter types to cycle; NONE disables pixels for the selected block."; }
    @Override protected List<Entry> entries() {
        Set<String> ids = new LinkedHashSet<>(DefaultEntryVisibility.installedOnly()
            ? AmbientEmitterBlocks.installedDefaults() : AmbientEmitterBlocks.allDefaults());
        ids.addAll(overrides.keySet());
        List<Entry> result = new ArrayList<>();
        for (String id : ids) result.add(new Entry(id, AmbientEmitterBlocks.isDefault(id), overrides.containsKey(id)));
        return result;
    }
    private AmbientEmitterBlocks.Kind value(String id) {
        return overrides.getOrDefault(id, AmbientEmitterBlocks.defaultKind(id));
    }
    @Override protected void addEntryWidgets(Entry e, int left, int y) {
        Button b = Button.builder(Component.literal(value(e.id()).name()), button -> {
            AmbientEmitterBlocks.Kind[] kinds = AmbientEmitterBlocks.Kind.values();
            AmbientEmitterBlocks.Kind next = kinds[(value(e.id()).ordinal() + 1) % kinds.length];
            if (e.builtin() && next == AmbientEmitterBlocks.defaultKind(e.id())) overrides.remove(e.id());
            else overrides.put(e.id(), next);
            button.setMessage(Component.literal(next.name()));
        }).tooltip(Tooltip.create(Component.literal("Cycle emitter category; NONE disables this block")))
          .bounds(left + 301, y, 101, 20).build();
        b.active = !e.builtin() || editDefaults;
        addRowWidget(b);
        if (!e.builtin() || editDefaults) {
            Button.OnPress reset = button -> {
                overrides.remove(e.id());
                refreshRows();
            };
            addRowWidget(e.builtin()
                ? new RestoreDefaultButton(left + 413, y, 25, 20, reset, "Reset to built-in type")
                : Button.builder(Component.literal("×"), reset)
                    .tooltip(Tooltip.create(Component.literal("Remove custom entry")))
                    .bounds(left + 413, y, 25, 20).build());
        }
    }
    private void add(String id) { overrides.putIfAbsent(id, AmbientEmitterBlocks.Kind.DUST); refreshRows(); }
    @Override protected void onAddEntry() {
        Set<String> ids = new HashSet<>(); for (Entry e : entries()) ids.add(e.id());
        minecraft.setScreen(new BlockPickerScreen(this, ids, this::add));
    }
    @Override protected void onSave() {
        List<String> encoded = new ArrayList<>();
        for (var e : overrides.entrySet()) encoded.add(e.getKey() + "=" + e.getValue().name());
        ClientWindConfig.AMBIENT_BLOCK_EMITTER_OVERRIDES.set(encoded);
        ClientWindConfig.AMBIENT_BLOCK_EMITTER_OVERRIDES.save();
        minecraft.setScreen(parent);
    }
}
