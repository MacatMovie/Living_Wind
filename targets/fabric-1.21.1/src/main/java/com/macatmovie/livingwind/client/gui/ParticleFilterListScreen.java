package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.BuiltinParticleBlacklist;
import com.macatmovie.livingwind.client.DefaultEntryVisibility;
import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Built-in particle exclusions are never modified without explicit user action. */
public final class ParticleFilterListScreen extends GroupedConfigListScreen {
    private final List<String> custom = new ArrayList<>();
    private final Set<String> exceptions = new HashSet<>();
    public ParticleFilterListScreen(Screen parent) {
        super(parent, "Living Wind Particle Blacklist");
        custom.addAll(ClientWindConfig.PARTICLE_FILTER.get());
        exceptions.addAll(ClientWindConfig.BUILTIN_BLACKLIST_EXCEPTIONS.get());
    }
    @Override protected String helpText() { return "Listed particles ignore generic wind. Unlock to exempt a built-in preset."; }
    @Override protected List<Entry> entries() {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (String id : BuiltinParticleBlacklist.installedIds()) {
            if (!exceptions.contains(id) && (!DefaultEntryVisibility.installedOnly() || DefaultEntryVisibility.particleInstalled(id)))
                map.put(id, new Entry(id, true, false));
        }
        for (String id : custom) map.put(id, new Entry(id, BuiltinParticleBlacklist.contains(id), true));
        return new ArrayList<>(map.values());
    }
    @Override protected void addEntryWidgets(Entry entry, int left, int y) {
        if (entry.builtin() && !editDefaults) return;
        addRowWidget(Button.builder(Component.literal("×"), b -> {
            if (entry.builtin()) { exceptions.add(entry.id()); custom.remove(entry.id()); }
            else custom.remove(entry.id());
            refreshRows();
        }).tooltip(Tooltip.create(Component.literal("Remove this entry (built-ins can be restored)")))
          .bounds(left + 413, y, 25, 20).build());
    }
    @Override protected void addExtraFooter(int left, int y) {
        addRenderableWidget(Button.builder(Component.literal("Restore Defaults"), b -> {
            exceptions.clear();
            refreshRows();
        }).tooltip(Tooltip.create(Component.literal("Restore all removed built-in particle blacklist entries")))
          .bounds(left, y, 107, 20).build());
    }
    private void add(String id) {
        if (BuiltinParticleBlacklist.contains(id)) { exceptions.remove(id); custom.remove(id); }
        else if (!custom.contains(id)) custom.add(id);
        refreshRows();
    }
    @Override protected void onAddEntry() {
        minecraft.setScreen(new ParticlePickerScreen(this, new HashSet<>(currentEntries()), this::add,
            Component.literal("Add Particles to Blacklist"), id -> BuiltinParticleBlacklist.contains(id) && !exceptions.contains(id)));
    }
    private List<String> currentEntries() { List<String> ids = new ArrayList<>(); for (Entry e: entries()) ids.add(e.id()); return ids; }
    @Override protected void onSave() {
        ClientWindConfig.PARTICLE_FILTER.set(new ArrayList<>(custom));
        ClientWindConfig.BUILTIN_BLACKLIST_EXCEPTIONS.set(new ArrayList<>(exceptions));
        ClientWindConfig.PARTICLE_FILTER.save();
        ClientWindConfig.BUILTIN_BLACKLIST_EXCEPTIONS.save();
        ParticleWindFilter.refresh();
        minecraft.setScreen(parent);
    }
}
