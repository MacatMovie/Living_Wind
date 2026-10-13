package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.client.BuiltinParticleBlacklist;
import com.macatmovie.livingwind.config.ClientWindConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class ParticleFilterListScreen extends Screen {
    private static final int TOP=42, BOTTOM=42, ROW=23;
    private final Screen parent;
    private final List<String> values = new ArrayList<>();
    private double scroll;

    public ParticleFilterListScreen(Screen parent){ super(Component.literal("Living Wind Particle Filter")); this.parent=parent; for(String s:ClientWindConfig.PARTICLE_FILTER.get()) values.add(s); }
    @Override protected void init(){ rebuildButtons(); }
    private boolean showBuiltins() {
        return ClientWindConfig.PARTICLE_FILTER_MODE.get() == ClientWindConfig.FilterMode.BLACKLIST;
    }

    private List<String> visibleItems() {
        List<String> entries = new ArrayList<>();
        if (showBuiltins()) entries.addAll(BuiltinParticleBlacklist.installedIds());
        for (String id : values) {
            if (!showBuiltins() || !BuiltinParticleBlacklist.contains(id)) entries.add(id);
        }
        return entries;
    }

    private void rebuildButtons() {
        clearWidgets();
        List<String> items = visibleItems();
        int left = width / 2 - 210;
        int first = (int) (scroll / ROW);
        int visible = Math.max(1, (height - TOP - BOTTOM) / ROW + 2);
        for (int i = first; i < Math.min(items.size(), first + visible); i++) {
            String id = items.get(i);
            if (showBuiltins() && BuiltinParticleBlacklist.contains(id)) continue;
            int y = TOP + i * ROW - (int) scroll;
            if (y < TOP || y + 20 > height - BOTTOM) continue;
            addRenderableWidget(Button.builder(Component.literal("×"), b -> {
                values.remove(id);
                clamp();
                rebuildButtons();
            }).bounds(left + 390, y, 20, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Add More..."), b ->
            minecraft.setScreen(new ParticlePickerScreen(this, new HashSet<>(values), this::add))
        ).bounds(width / 2 - 154, height - 30, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> done())
            .bounds(width / 2 + 4, height - 30, 150, 20).build());
    }

    private void add(String id) {
        if (!values.contains(id)) values.add(id);
    }

    private void done() {
        ClientWindConfig.PARTICLE_FILTER.set(new ArrayList<>(values));
        ParticleWindFilter.refresh();
        minecraft.setScreen(parent);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override public boolean mouseScrolled(double x, double y, double d) {
        if (y >= TOP && y < height - BOTTOM) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - d * ROW * 3));
            rebuildButtons();
            return true;
        }
        return super.mouseScrolled(x, y, d);
    }

    private double maxScroll() {
        return Math.max(0, visibleItems().size() * ROW - (height - TOP - BOTTOM));
    }

    private void clamp() { scroll = Math.max(0, Math.min(maxScroll(), scroll)); }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        List<String> items = visibleItems();
        int left = width / 2 - 210;
        int first = (int) (scroll / ROW);
        int visible = Math.max(1, (height - TOP - BOTTOM) / ROW + 2);
        for (int i = first; i < Math.min(items.size(), first + visible); i++) {
            int y = TOP + i * ROW - (int) scroll;
            if (y < TOP || y + 20 > height - BOTTOM) continue;
            String id = items.get(i);
            boolean builtin = showBuiltins() && BuiltinParticleBlacklist.contains(id);
            g.fill(left, y, left + 385, y + 20, builtin ? 0x80605020 : 0x60303030);
            g.drawString(font, Component.literal("✓  " + id).withStyle(builtin ? ChatFormatting.YELLOW : ChatFormatting.GREEN),
                left + 6, y + 6, 0xFFFFFF, false);
        }
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, showBuiltins() ?
            "Yellow = built-in (read-only)    Green = custom" : "Green = your whitelist", width / 2, 28, 0xBBBBBB);
    }

}
