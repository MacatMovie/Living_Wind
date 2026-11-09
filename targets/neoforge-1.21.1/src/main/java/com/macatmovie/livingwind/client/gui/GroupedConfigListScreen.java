package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.DefaultEntryVisibility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/** Shared presentation logic for the four configurable particle/block/entity lists.
 * Collapsing, searching, and display filters are UI-only and never change gameplay.
 */
public abstract class GroupedConfigListScreen extends Screen {
    protected static final int TOP = 85, BOTTOM = 44, ROW = 24;
    protected final Screen parent;
    protected final int leftOffset = 230;
    protected boolean editDefaults;
    protected double scroll;
    protected record Entry(String id, boolean builtin, boolean customized) {}
    private record Row(String mod, Entry entry, int count) {}
    private final Set<String> expanded = new HashSet<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private List<Row> visible = List.of();
    private EditBox search;
    private boolean showDefaults = true, showCustom = true;
    private boolean draggingScrollbar;

    protected GroupedConfigListScreen(Screen parent, String title) {
        super(Component.literal(title));
        this.parent = parent;
    }
    protected abstract List<Entry> entries();
    protected abstract void addEntryWidgets(Entry entry, int left, int y);
    protected abstract void onAddEntry();
    protected abstract void onSave();
    protected abstract String helpText();
    protected void beforeRowsChange() {}
    protected void addExtraFooter(int left, int y) {}

    protected final <T extends AbstractWidget> T addRowWidget(T widget) {
        rowWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    private static String nameOf(String ns) {
        if ("minecraft".equals(ns)) return "Minecraft";
        if ("aether".equals(ns)) return "The Aether";
        if ("aether_ii".equals(ns)) return "The Aether II";
        if ("twilightforest".equals(ns)) return "The Twilight Forest";
        return ModList.get().getModContainerById(ns).map(c -> c.getModInfo().getDisplayName())
            .orElseGet(() -> {
                StringBuilder s = new StringBuilder();
                for (String part : ns.split("[_-]")) {
                    if (part.isEmpty()) continue;
                    if (s.length() > 0) s.append(' ');
                    s.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
                return s.toString();
            });
    }

    @Override protected void init() {
        clearWidgets();
        rowWidgets.clear();
        int left = width / 2 - leftOffset;
        String previous = search == null ? "" : search.getValue();
        search = addRenderableWidget(new EditBox(font, left, 29, 194, 20, Component.literal("Search entries")));
        search.setMaxLength(160);
        search.setHint(Component.literal("Search mods or IDs..."));
        search.setValue(previous);
        search.setResponder(v -> { scroll = 0; rebuildRows(); });

        addRenderableWidget(Button.builder(Component.literal(DefaultEntryVisibility.installedOnly() ? "X" : " "), b -> {
            beforeRowsChange();
            DefaultEntryVisibility.toggle();
            b.setMessage(Component.literal(DefaultEntryVisibility.installedOnly() ? "X" : " "));
            b.setTooltip(tooltip(DefaultEntryVisibility.installedOnly() ? "Showing installed mods only" : "Showing all mods, including uninstalled"));
            scroll = 0;
            rebuildRows();
        }).tooltip(tooltip(DefaultEntryVisibility.installedOnly() ? "Showing installed mods only" : "Showing all mods, including uninstalled"))
          .bounds(left + 214, 29, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(showDefaults ? "X" : " "), b -> {
            beforeRowsChange();
            showDefaults = !showDefaults;
            b.setMessage(Component.literal(showDefaults ? "X" : " "));
            b.setTooltip(tooltip(showDefaults ? "Showing built-in defaults" : "Hiding unmodified built-in defaults"));
            scroll = 0;
            rebuildRows();
        }).tooltip(tooltip(showDefaults ? "Showing built-in defaults" : "Hiding unmodified built-in defaults"))
          .bounds(left + 255, 29, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(showCustom ? "X" : " "), b -> {
            beforeRowsChange();
            showCustom = !showCustom;
            b.setMessage(Component.literal(showCustom ? "X" : " "));
            b.setTooltip(tooltip(showCustom ? "Showing custom entries and overrides" : "Showing defaults only"));
            scroll = 0;
            rebuildRows();
        }).tooltip(tooltip(showCustom ? "Showing custom entries and overrides" : "Showing defaults only"))
          .bounds(left + 296, 29, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(editDefaults ? "Lock Defaults" : "Unlock Defaults"), b -> {
            beforeRowsChange();
            editDefaults = !editDefaults;
            b.setMessage(Component.literal(editDefaults ? "Lock Defaults" : "Unlock Defaults"));
            rebuildRows();
        }).tooltip(tooltip("Unlock built-in rows to remove or override presets. Your changes can be reset."))
          .bounds(left + 326, 29, 134, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Add More..."), b -> { beforeRowsChange(); onAddEntry(); })
            .bounds(left + 116, height - 29, 134, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> { beforeRowsChange(); onSave(); })
            .bounds(left + 267, height - 29, 134, 20).build());
        addExtraFooter(left, height - 29);
        rebuildRows();
        setInitialFocus(search);
    }

    private static Tooltip tooltip(String text) { return Tooltip.create(Component.literal(text)); }

    private List<Row> filteredRows() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        Map<String, List<Entry>> groups = new LinkedHashMap<>();
        List<Entry> source = new ArrayList<>(entries());
        source.sort(Comparator.comparing(Entry::id));
        for (Entry entry : source) {
            if (entry.customized() ? !showCustom : !showDefaults) continue;
            String ns = entry.id().contains(":") ? entry.id().substring(0, entry.id().indexOf(':')) : "minecraft";
            if (!q.isEmpty() && !entry.id().toLowerCase(Locale.ROOT).contains(q)
                    && !nameOf(ns).toLowerCase(Locale.ROOT).contains(q)) continue;
            groups.computeIfAbsent(ns, ignored -> new ArrayList<>()).add(entry);
        }
        List<Row> result = new ArrayList<>();
        List<String> mods = new ArrayList<>(groups.keySet());
        mods.sort((a, b) -> a.equals("minecraft") ? -1 : b.equals("minecraft") ? 1
            : nameOf(a).compareToIgnoreCase(nameOf(b)));
        for (String ns : mods) {
            result.add(new Row(ns, null, groups.get(ns).size()));
            if (expanded.contains(ns) || !q.isEmpty())
                for (Entry e : groups.get(ns)) result.add(new Row(ns, e, groups.get(ns).size()));
        }
        return result;
    }

    protected final void refreshRows() { rebuildRows(); }
    private void rebuildRows() {
        for (AbstractWidget widget : rowWidgets) removeWidget(widget);
        rowWidgets.clear();
        visible = filteredRows();
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        int start = (int)(scroll / ROW);
        int end = Math.min(visible.size(), start + Math.max(1, (height - TOP - BOTTOM) / ROW + 2));
        int left = width / 2 - leftOffset;
        for (int i = start; i < end; i++) {
            Row row = visible.get(i);
            int y = TOP + i * ROW - (int)scroll;
            if (y < TOP || y + 20 > height - BOTTOM) continue;
            if (row.entry() == null) {
                String ns = row.mod();
                final String symbol = (expanded.contains(ns) || (search != null && !search.getValue().isBlank())) ? "▼ " : "▲ ";
                addRowWidget(Button.builder(Component.literal(symbol + nameOf(ns) + " (" + row.count() + ")"), b -> {
                    beforeRowsChange();
                    if (!expanded.add(ns)) expanded.remove(ns);
                    rebuildRows();
                }).tooltip(tooltip("Expand/collapse " + nameOf(ns))).bounds(left, y, 438, 20).build());
            } else addEntryWidgets(row.entry(), left, y);
        }
    }

    private double maxScroll() { return Math.max(0, visible.size() * ROW - Math.max(1, height - TOP - BOTTOM)); }
    private void updateScrollFromMouse(double y) {
        int track = Math.max(1, height - TOP - BOTTOM);
        int thumb = Math.max(18, (int)(track * Math.min(1d, (double)track / Math.max(track, visible.size() * ROW))));
        scroll = Math.max(0, Math.min(maxScroll(), (y - TOP - thumb / 2d) / Math.max(1d, track - thumb) * maxScroll()));
        beforeRowsChange();
        rebuildRows();
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        int x = width / 2 - leftOffset + 447;
        if (button == 0 && mx >= x - 3 && mx <= x + 9 && my >= TOP && my <= height - BOTTOM && maxScroll() > 0) {
            draggingScrollbar = true;
            updateScrollFromMouse(my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) { updateScrollFromMouse(my); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) draggingScrollbar = false;
        return super.mouseReleased(mx, my, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (y >= TOP && y < height - BOTTOM) {
            beforeRowsChange();
            scroll = Math.max(0, Math.min(maxScroll(), scroll - dy * ROW * 3));
            rebuildRows();
            return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int left = width / 2 - leftOffset;
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        g.drawString(font, "I", left + 203, 35, 0xBBBBBB, false);
        g.drawString(font, "D", left + 244, 35, 0xBBBBBB, false);
        g.drawString(font, "C", left + 285, 35, 0xBBBBBB, false);
        g.drawString(font, "I: installed   D: defaults   C: custom   |   Yellow: default   Green: customized", left, 54, 0xAAAAAA, false);
        g.drawString(font, helpText(), left, 67, 0xAAAAAA, false);
        int start = (int)(scroll / ROW);
        int end = Math.min(visible.size(), start + Math.max(1, (height - TOP - BOTTOM) / ROW + 2));
        for (int i = start; i < end; i++) {
            Row row = visible.get(i);
            int y = TOP + i * ROW - (int)scroll;
            if (y < TOP || y + 20 > height - BOTTOM || row.entry() == null) continue;
            Entry e = row.entry();
            g.fill(left, y, left + 297, y + 20, e.customized() ? 0x60303030 : 0x80605020);
            g.drawString(font, font.plainSubstrByWidth(e.id(), 276), left + 7, y + 6,
                e.customized() ? 0x88DD88 : 0xFFDD55, false);
        }
        int track = Math.max(1, height - TOP - BOTTOM);
        int x = left + 447;
        g.fill(x, TOP, x + 6, TOP + track, 0x60404040);
        int thumb = Math.max(18, (int)(track * Math.min(1d, (double)track / Math.max(track, visible.size() * ROW))));
        int y = TOP + (maxScroll() <= 0 ? 0 : (int)((track - thumb) * scroll / maxScroll()));
        g.fill(x, y, x + 6, Math.min(TOP + track, y + thumb), 0xFFC0C0C0);
        if (visible.isEmpty()) g.drawCenteredString(font, "No matching entries", width / 2, TOP + 10, 0xAAAAAA);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
