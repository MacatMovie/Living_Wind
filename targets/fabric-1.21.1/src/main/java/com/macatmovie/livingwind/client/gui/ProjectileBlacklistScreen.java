package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.DefaultEntryVisibility;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.BuiltinProjectileBlacklist;
import com.macatmovie.livingwind.wind.ProjectileWindBlacklist;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Protected projectile wind exclusions, with per-entry persistent exceptions. */
public final class ProjectileBlacklistScreen extends GroupedConfigListScreen {
    private final List<String> custom = new ArrayList<>();
    private final Set<String> exceptions = new LinkedHashSet<>();
    public ProjectileBlacklistScreen(Screen parent) {
        super(parent, "Entity Wind - Projectile Blacklist");
        custom.addAll(WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST.get());
        exceptions.addAll(WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST_EXCEPTIONS.get());
    }
    @Override protected String helpText() { return "Listed projectile types ignore entity wind when projectile wind is enabled."; }
    @Override protected List<Entry> entries() {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (String id : BuiltinProjectileBlacklist.IDS) {
            if (!exceptions.contains(id) && (!DefaultEntryVisibility.installedOnly() || DefaultEntryVisibility.entityInstalled(id)))
                map.put(id, new Entry(id, true, false));
        }
        for (String id : custom) map.put(id, new Entry(id, BuiltinProjectileBlacklist.contains(id), true));
        return new ArrayList<>(map.values());
    }
    @Override protected void addEntryWidgets(Entry e, int left, int y) {
        if (e.builtin() && !editDefaults) return;
        addRowWidget(Button.builder(Component.literal("×"), b -> {
            if (e.builtin()) { exceptions.add(e.id()); custom.remove(e.id()); }
            else custom.remove(e.id());
            refreshRows();
        }).tooltip(Tooltip.create(Component.literal("Remove exclusion"))).bounds(left + 413, y, 25, 20).build());
    }
    @Override protected void addExtraFooter(int left, int y) {
        addRenderableWidget(Button.builder(Component.literal("Restore Defaults"), b -> {
            exceptions.clear();
            refreshRows();
        }).tooltip(Tooltip.create(Component.literal("Restore all removed built-in projectile exclusions")))
          .bounds(left, y, 107, 20).build());
    }
    private void add(String id) {
        if (BuiltinProjectileBlacklist.contains(id)) { exceptions.remove(id); custom.remove(id); }
        else if (!custom.contains(id)) custom.add(id);
        refreshRows();
    }
    @Override protected void onAddEntry() {
        Set<String> ids = new HashSet<>(); for (Entry e : entries()) ids.add(e.id());
        minecraft.setScreen(new ProjectileTypePickerScreen(this, ids, this::add,
            Component.literal("Add Projectile Exclusion"), id -> BuiltinProjectileBlacklist.contains(id) && !exceptions.contains(id)));
    }
    @Override protected void onSave() {
        WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST.set(new ArrayList<>(custom));
        WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST_EXCEPTIONS.set(new ArrayList<>(exceptions));
        WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST.save();
        WindConfig.ENTITY_WIND_PROJECTILE_BLACKLIST_EXCEPTIONS.save();
        ProjectileWindBlacklist.refresh();
        minecraft.setScreen(parent);
    }
}
