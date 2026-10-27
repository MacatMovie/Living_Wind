package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.BuiltinParticleResponseMultipliers;
import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Read-only built-in response rows with opt-in editing and persistent user overrides. */
public final class ParticleResponseMultiplierListScreen extends Screen {
    private static final int TOP = 63, BOTTOM = 45, ROW = 25;
    private final Screen parent;
    // Only user-supplied or pre-existing config entries are serialized, never synthesized defaults.
    private final Map<String, String> overrides = new LinkedHashMap<>();
    private final Map<Integer, EditBox> boxes = new LinkedHashMap<>();
    private boolean editDefaults;
    private double scroll;

    public ParticleResponseMultiplierListScreen(Screen parent) {
        super(Component.literal("Particle Wind Response Multipliers"));
        this.parent = parent;
        ParticleWindFilter.migrateLegacyStreamsDefaults();
        for (String s : ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.get()) {
            int eq = s.lastIndexOf('=');
            if (eq > 0) overrides.put(s.substring(0, eq).trim(), s.substring(eq + 1).trim());
        }
    }

    @Override protected void init() { rebuild(); }

    private List<String> rows() {
        Set<String> ids = new LinkedHashSet<>(BuiltinParticleResponseMultipliers.installedDefaults());
        ids.addAll(overrides.keySet());
        return new ArrayList<>(ids);
    }

    private String valueFor(String id) {
        return overrides.getOrDefault(id, Float.toString(BuiltinParticleResponseMultipliers.defaultValue(id)));
    }

    private void rebuild() {
        clearWidgets();
        boxes.clear();
        int left = width / 2 - 230;
        addRenderableWidget(Button.builder(Component.literal(editDefaults ? "Lock Defaults" : "Unlock Defaults"), b -> {
            commitVisible();
            editDefaults = !editDefaults;
            rebuild();
        }).bounds(width / 2 + 100, 27, 130, 20).build());

        List<String> all = rows();
        int first = (int)(scroll / ROW);
        int visible = Math.max(1, (height - TOP - BOTTOM) / ROW + 2);
        for (int i = first; i < Math.min(all.size(), first + visible); i++) {
            final String id = all.get(i);
            boolean builtin = BuiltinParticleResponseMultipliers.isDefault(id);
            int y = TOP + i * ROW - (int)scroll;
            if (y < TOP || y + 20 > height - BOTTOM) continue;

            EditBox box = new EditBox(font, left + 305, y, 80, 20, Component.literal("Multiplier"));
            box.setValue(valueFor(id));
            box.setFilter(v -> v.isEmpty() || v.matches("\\d*(?:\\.\\d*)?"));
            box.setEditable(!builtin || editDefaults);
            box.setResponder(v -> updateMultiplier(id, v));
            boxes.put(i, box);
            addRenderableWidget(box);

            if (!builtin || editDefaults) {
                addRenderableWidget(Button.builder(Component.literal(builtin ? "↺" : "×"), b -> {
                    commitVisible();
                    overrides.remove(id);
                    clamp();
                    rebuild();
                }).bounds(left + 394, y, 20, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Add More..."), b -> {
            commitVisible();
            minecraft.setScreen(new ParticlePickerScreen(this, new HashSet<>(rows()), this::add,
                Component.literal("Add Particle Response")));
        }).bounds(width / 2 - 154, height - 30, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> done())
            .bounds(width / 2 + 4, height - 30, 150, 20).build());
    }

    private void updateMultiplier(String id, String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            double value = Double.parseDouble(raw);
            if (Double.isFinite(value) && value >= 0.0 && value <= 10.0)
                overrides.put(id, raw);
        } catch (NumberFormatException ignored) {}
    }

    private void commitVisible() {
        List<String> all = rows();
        for (Map.Entry<Integer, EditBox> e : boxes.entrySet()) {
            if (e.getKey() < 0 || e.getKey() >= all.size()) continue;
            String id = all.get(e.getKey());
            // Showing or scrolling past an unchanged built-in must not turn it
            // into a saved override; actual edits are recorded by its responder.
            if (BuiltinParticleResponseMultipliers.isDefault(id) && !overrides.containsKey(id)) continue;
            updateMultiplier(id, e.getValue().getValue());
        }
    }

    private void add(String id) {
        if (!overrides.containsKey(id)) overrides.put(id, "1.0");
        rebuild();
    }

    private void done() {
        commitVisible();
        List<String> serialized = new ArrayList<>();
        for (Map.Entry<String, String> entry : overrides.entrySet())
            serialized.add(entry.getKey() + "=" + entry.getValue());
        ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.set(serialized);
        ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.save();
        ParticleWindFilter.refresh();
        minecraft.setScreen(parent);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (y >= TOP && y < height - BOTTOM) {
            commitVisible();
            scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * ROW * 3));
            rebuild();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private double maxScroll() { return Math.max(0, rows().size() * ROW - (height - TOP - BOTTOM)); }
    private void clamp() { scroll = Math.max(0, Math.min(maxScroll(), scroll)); }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        // In 1.21.1 the base Screen draws its blurred/dimmed background during render.
        // Draw our custom rows *after* this, keeping labels crisp like standard config screens.
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        g.drawString(font, "Yellow = default  |  Green = custom  |  ↺ resets", width / 2 - 230, 49, 0xAAAAAA, false);
        int left = width / 2 - 230;
        List<String> all = rows();
        int first = (int)(scroll / ROW);
        int visible = Math.max(1, (height - TOP - BOTTOM) / ROW + 2);
        for (int i = first; i < Math.min(all.size(), first + visible); i++) {
            int y = TOP + i * ROW - (int)scroll;
            if (y < TOP || y + 20 > height - BOTTOM) continue;
            String id = all.get(i);
            boolean builtin = BuiltinParticleResponseMultipliers.isDefault(id);
            g.fill(left, y, left + 291, y + 20, builtin ? 0x80605020 : 0x60303030);
            g.drawString(font, id, left + 5, y + 6, builtin ? 0xFFDD55 : 0x88DD88, false);
        }
    }
}
