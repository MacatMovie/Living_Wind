package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.BuiltinParticleBlacklist;
import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** The built-in blacklist remains protected unless explicitly unlocked.
 * User exemptions are saved separately so updating defaults never wipes custom choices.
 */
public final class ParticleFilterListScreen extends Screen {
    private static final int TOP = 63, BOTTOM = 45, ROW = 23;
    private final Screen parent;
    private final List<String> values = new ArrayList<>();
    private final Set<String> exceptions = new HashSet<>();
    private boolean editDefaults;
    private double scroll;

    public ParticleFilterListScreen(Screen parent) {
        super(Component.literal("Living Wind Particle Filter"));
        this.parent = parent;
        values.addAll(ClientWindConfig.PARTICLE_FILTER.get());
        exceptions.addAll(ClientWindConfig.BUILTIN_BLACKLIST_EXCEPTIONS.get());
    }

    @Override protected void init() { rebuildButtons(); }

    private boolean showBuiltins() {
        return ClientWindConfig.PARTICLE_FILTER_MODE.get() == ClientWindConfig.FilterMode.BLACKLIST;
    }

    private List<String> visibleItems() {
        List<String> entries = new ArrayList<>();
        if (showBuiltins()) {
            for (String id : BuiltinParticleBlacklist.installedIds()) {
                if (!exceptions.contains(id)) entries.add(id);
            }
        }
        for (String id : values) {
            if (!entries.contains(id)) entries.add(id);
        }
        return entries;
    }

    private void rebuildButtons() {
        clearWidgets();
        int left = width / 2 - 210;
        addRenderableWidget(Button.builder(Component.literal(editDefaults ? "Lock Defaults" : "Unlock Defaults"), b -> {
            editDefaults = !editDefaults;
            rebuildButtons();
        }).bounds(width / 2 + 90, 27, 120, 20).build());

        List<String> rows = visibleItems();
        int first = (int)(scroll / ROW);
        int visible = Math.max(1, (height - TOP - BOTTOM) / ROW + 2);
        for (int i = first; i < Math.min(rows.size(), first + visible); i++) {
            String id = rows.get(i);
            boolean builtin = showBuiltins() && BuiltinParticleBlacklist.contains(id);
            if (builtin && !editDefaults) continue;
            int y = TOP + i * ROW - (int)scroll;
            if (y < TOP || y + 20 > height - BOTTOM) continue;
            addRenderableWidget(Button.builder(Component.literal("×"), b -> {
                if (builtin) { exceptions.add(id); values.remove(id); }
                else values.remove(id);
                clamp();
                rebuildButtons();
            }).bounds(left + 390, y, 20, 20).build());
        }
        if (showBuiltins() && editDefaults && !exceptions.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("Restore Defaults"), b -> {
                exceptions.clear();
                clamp();
                rebuildButtons();
            }).bounds(width / 2 - 210, 27, 130, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Add More..."), b ->
            minecraft.setScreen(new ParticlePickerScreen(this, new HashSet<>(visibleItems()), this::add))
        ).bounds(width / 2 - 154, height - 30, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> done())
            .bounds(width / 2 + 4, height - 30, 150, 20).build());
    }

    private void add(String id) {
        if (showBuiltins() && BuiltinParticleBlacklist.contains(id)) {
            exceptions.remove(id); // Restore a removed default when explicitly re-added.
        } else if (!values.contains(id)) values.add(id);
        rebuildButtons();
    }

    private void done() {
        ClientWindConfig.PARTICLE_FILTER.set(new ArrayList<>(values));
        ClientWindConfig.BUILTIN_BLACKLIST_EXCEPTIONS.set(new ArrayList<>(exceptions));
        ClientWindConfig.PARTICLE_FILTER.save();
        ClientWindConfig.BUILTIN_BLACKLIST_EXCEPTIONS.save();
        ParticleWindFilter.refresh();
        minecraft.setScreen(parent);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (y >= TOP && y < height - BOTTOM) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * ROW * 3));
            rebuildButtons();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private double maxScroll() {
        return Math.max(0, visibleItems().size() * ROW - (height - TOP - BOTTOM));
    }

    private void clamp() { scroll = Math.max(0, Math.min(maxScroll(), scroll)); }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        // In 1.21.1 the base Screen draws its blurred/dimmed background during render.
        // Draw our custom rows *after* this, keeping labels crisp like standard config screens.
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        g.drawString(font, "Yellow = default  |  Green = custom", width / 2 - 210, 49, 0xAAAAAA, false);
        List<String> rows = visibleItems();
        int left = width / 2 - 210;
        int first = (int)(scroll / ROW);
        int visible = Math.max(1, (height - TOP - BOTTOM) / ROW + 2);
        for (int i = first; i < Math.min(rows.size(), first + visible); i++) {
            int y = TOP + i * ROW - (int)scroll;
            if (y < TOP || y + 20 > height - BOTTOM) continue;
            String id = rows.get(i);
            boolean builtin = showBuiltins() && BuiltinParticleBlacklist.contains(id);
            g.fill(left, y, left + 385, y + 20, builtin ? 0x80605020 : 0x60303030);
            g.drawString(font, "✓  " + id, left + 6, y + 6,
                builtin ? 0xFFDD55 : 0x88DD88, false);
        }
    }
}
