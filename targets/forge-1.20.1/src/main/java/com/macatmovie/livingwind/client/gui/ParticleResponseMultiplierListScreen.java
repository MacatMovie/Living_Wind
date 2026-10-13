package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class ParticleResponseMultiplierListScreen extends Screen {
    private static final int TOP = 42, BOTTOM = 42, ROW = 25;
    private final Screen parent;
    private final List<String> values = new ArrayList<>();

    // Keep the real list index for every visible field. The previous implementation stored only a
    // flat list of EditBoxes and reconstructed indexes from scroll position, which could commit a
    // field to the wrong/no row when clipping or scrolling was involved.
    private final Map<Integer, EditBox> boxes = new HashMap<>();
    private double scroll;

    public ParticleResponseMultiplierListScreen(Screen parent) {
        super(Component.literal("Particle Wind Response Multipliers"));
        this.parent = parent;
        for (String s : ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.get()) values.add(s);
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        boxes.clear();
        int left = width / 2 - 230;
        int first = (int) (scroll / ROW);
        int visible = Math.max(1, (height - TOP - BOTTOM) / ROW + 2);

        for (int i = first; i < Math.min(values.size(), first + visible); i++) {
            final int idx = i;
            int y = TOP + i * ROW - (int) scroll;
            if (y < TOP || y + 20 > height - BOTTOM) continue;

            String e = values.get(i);
            int eq = e.lastIndexOf('=');
            String mult = eq > 0 ? e.substring(eq + 1) : "1.0";

            EditBox box = new EditBox(font, left + 300, y, 90, 20, Component.literal("Multiplier"));
            box.setValue(mult);
            box.setFilter(v -> v.isEmpty() || v.matches("\\d*(?:\\.\\d*)?"));
            // Save into our working list as soon as a complete valid multiplier is typed. This means
            // clicking Done while the field is still focused cannot lose the final edit.
            box.setResponder(v -> updateMultiplier(idx, v));
            boxes.put(idx, box);
            addRenderableWidget(box);

            addRenderableWidget(Button.builder(Component.literal("×"), b -> {
                commitVisible();
                values.remove(idx);
                clamp();
                rebuild();
            }).bounds(left + 400, y, 20, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Add More..."), b -> {
            commitVisible();
            minecraft.setScreen(new ParticlePickerScreen(this, selectedIds(), this::add, Component.literal("Add Particle Response")));
        }).bounds(width / 2 - 154, height - 30, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> done()).bounds(width / 2 + 4, height - 30, 150, 20).build());
    }

    private void updateMultiplier(int idx, String rawValue) {
        if (idx < 0 || idx >= values.size() || rawValue == null || rawValue.isBlank()) return;
        try {
            double parsed = Double.parseDouble(rawValue);
            if (!Double.isFinite(parsed) || parsed < 0.0 || parsed > 10.0) return;
        } catch (NumberFormatException ignored) {
            return;
        }

        String e = values.get(idx);
        int eq = e.lastIndexOf('=');
        String id = eq > 0 ? e.substring(0, eq) : e;
        values.set(idx, id + "=" + rawValue);
    }

    private void commitVisible() {
        for (Map.Entry<Integer, EditBox> entry : boxes.entrySet()) {
            updateMultiplier(entry.getKey(), entry.getValue().getValue());
        }
    }

    private Set<String> selectedIds() {
        Set<String> s = new HashSet<>();
        for (String e : values) {
            int eq = e.lastIndexOf('=');
            s.add((eq > 0 ? e.substring(0, eq) : e).trim());
        }
        return s;
    }

    private void add(String id) {
        if (!selectedIds().contains(id)) values.add(id + "=1.0");
    }

    private void done() {
        commitVisible();
        ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.set(new ArrayList<>(values));
        // Forge's ConfigValue#set updates the live value but does not persist it to disk. Explicitly
        // save here so returning to the screen/restarting the game keeps multiplier edits.
        ClientWindConfig.PARTICLE_RESPONSE_MULTIPLIERS.save();
        ParticleWindFilter.refresh();
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if (y >= TOP && y < height - BOTTOM) {
            commitVisible();
            scroll = Math.max(0, Math.min(maxScroll(), scroll - d * ROW * 3));
            rebuild();
            return true;
        }
        return super.mouseScrolled(x, y, d);
    }

    private double maxScroll() {
        return Math.max(0, values.size() * ROW - (height - TOP - BOTTOM));
    }

    private void clamp() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        int left = width / 2 - 230;
        int first = (int) (scroll / ROW);
        int visible = Math.max(1, (height - TOP - BOTTOM) / ROW + 2);
        for (int i = first; i < Math.min(values.size(), first + visible); i++) {
            int y = TOP + i * ROW - (int) scroll;
            if (y < TOP || y + 20 > height - BOTTOM) continue;
            String e = values.get(i);
            int eq = e.lastIndexOf('=');
            String id = eq > 0 ? e.substring(0, eq) : e;
            g.drawString(font, id, left + 5, y + 6, 0xFFFFFF, false);
        }
        super.render(g, mx, my, pt);
    }
}
