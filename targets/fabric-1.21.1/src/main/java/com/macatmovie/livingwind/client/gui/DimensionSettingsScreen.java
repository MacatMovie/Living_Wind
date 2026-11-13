package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.wind.WindDimensionBlacklist;
import com.macatmovie.livingwind.wind.WindDimensionSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

/** Dimension wind editor: never persists built-in defaults as user overrides.
 * Displays vanilla dimensions first, then installed optional dimension presets.
 */
public final class DimensionSettingsScreen extends Screen {
    private final Screen parent;
    private final Map<String, WindDimensionSettings.Mode> overrides = new LinkedHashMap<>();
    private final List<String> known = new ArrayList<>();
    private final List<String> matches = new ArrayList<>();
    private final List<Button> dynamicButtons = new ArrayList<>();
    private WindDimensionSettings.Mode fallback;
    private EditBox input;
    private Button fallbackButton;
    private Button installedButton;
    private int left, panelWidth;
    private double scroll;
    private boolean installedOnly = true;
    private boolean draggingScrollbar;
    private String completion;

    public DimensionSettingsScreen(Screen parent) {
        super(Component.literal("Dimension Settings"));
        this.parent = parent;
        fallback = WindConfig.DEFAULT_DIMENSION_MODE.get();
        // Import old blacklist first, then newer explicit mode overrides.
        for (String raw : WindConfig.DIMENSION_BLACKLIST.get()) {
            String id = raw.trim().toLowerCase(Locale.ROOT);
            if (WindDimensionBlacklist.validId(id)) overrides.put(id, WindDimensionSettings.Mode.DISABLED);
        }
        for (String entry : WindConfig.DIMENSION_MODES.get()) {
            if (!WindDimensionSettings.validEntry(entry)) continue;
            int eq = entry.lastIndexOf('=');
            overrides.put(entry.substring(0, eq).trim().toLowerCase(Locale.ROOT),
                WindDimensionSettings.Mode.valueOf(entry.substring(eq + 1).trim().toUpperCase(Locale.ROOT)));
        }
    }

    private Map<String, WindDimensionSettings.Mode> defaults() {
        Map<String, WindDimensionSettings.Mode> builtins = new LinkedHashMap<>();
        builtins.put("minecraft:overworld", fallback);
        builtins.put("minecraft:the_nether", WindDimensionSettings.legacyNetherMode());
        builtins.put("minecraft:the_end", WindDimensionSettings.legacyEndMode());
        WindDimensionSettings.BUILTIN_MOD_MODES.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(e -> builtins.put(e.getKey(), e.getValue()));
        return builtins;
    }

    private boolean isInstalled(String id) {
        // A dimension namespace need not match its owning Fabric mod ID. If the
        // server advertises a dimension, it is installed even for datapacks or
        // mods whose loader ID differs from the dimension's registry namespace.
        if (known.contains(id)) return true;
        String namespace = id.substring(0, id.indexOf(':'));
        return "minecraft".equals(namespace) || FabricLoader.getInstance().isModLoaded(namespace);
    }

    @Override protected void init() {
        panelWidth = Math.min(460, width - 32);
        left = (width - panelWidth) / 2;
        known.clear();
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().levels().forEach(key -> known.add(key.location().toString()));
        }
        known.sort(Comparator.naturalOrder());
        input = addRenderableWidget(new EditBox(font, left, 37, panelWidth - 75, 20,
            Component.literal("Dimension ID")));
        input.setMaxLength(160);
        input.setHint(Component.literal("modid:dimension"));
        input.setResponder(v -> updateSuggestions());
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> addInput())
            .bounds(left + panelWidth - 70, 37, 70, 20).build());
        fallbackButton = addRenderableWidget(Button.builder(Component.literal(fallback.name()), b -> {
            fallback = next(fallback);
            fallbackButton.setMessage(Component.literal(fallback.name()));
            rebuildRows();
        }).bounds(left + panelWidth - 145, 175, 145, 20)
          .tooltip(Tooltip.create(Component.literal("Wind behavior for dimensions without a custom mode or built-in preset."))).build());
        installedButton = addRenderableWidget(Button.builder(Component.literal(installedOnly ? "Installed Only" : "All"), b -> {
            installedOnly = !installedOnly;
            b.setMessage(Component.literal(installedOnly ? "Installed Only" : "All"));
            b.setTooltip(installedTooltip());
            scroll = 0;
            rebuildRows();
        }).tooltip(installedTooltip()).bounds(left + panelWidth - 118, 201, 118, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> save())
            .bounds(width / 2 - 75, height - 28, 150, 20).build());
        updateSuggestions();
        setInitialFocus(input);
    }

    private Tooltip installedTooltip() {
        return Tooltip.create(Component.literal(installedOnly ?
            "Showing only installed built-in presets; custom dimensions stay visible" :
            "Showing every dimension entry, including presets for uninstalled mods"));
    }

    private static WindDimensionSettings.Mode next(WindDimensionSettings.Mode old) {
        WindDimensionSettings.Mode[] modes = WindDimensionSettings.Mode.values();
        return modes[(old.ordinal() + 1) % modes.length];
    }

    private void addInput() {
        String id = input.getValue().trim().toLowerCase(Locale.ROOT);
        if (!WindDimensionBlacklist.validId(id)) return;
        add(id);
    }
    private void add(String id) {
        if (!defaults().containsKey(id)) overrides.putIfAbsent(id, fallback);
        input.setValue("");
        scroll = 0;
        updateSuggestions();
    }

    private void updateSuggestions() {
        if (input == null) return;
        matches.clear();
        String query = input.getValue().trim().toLowerCase(Locale.ROOT);
        for (String id : known)
            if (!defaults().containsKey(id) && !overrides.containsKey(id) &&
                    (query.isEmpty() || id.startsWith(query))) matches.add(id);
        for (String id : known)
            if (!defaults().containsKey(id) && !overrides.containsKey(id) &&
                    !id.startsWith(query) && id.contains(query)) matches.add(id);
        completion = null;
        for (String id : matches) {
            if (!query.isEmpty() && id.startsWith(query) && id.length() > query.length()) {
                completion = id;
                break;
            }
        }
        input.setSuggestion(completion == null ? null : completion.substring(Math.min(query.length(), completion.length())));
        rebuildRows();
    }

    private List<String> sortedIds() {
        Map<String, WindDimensionSettings.Mode> defaults = defaults();
        List<String> ids = new ArrayList<>();
        for (String id : defaults.keySet())
            if (!installedOnly || isInstalled(id) || overrides.containsKey(id)) ids.add(id);
        List<String> customs = new ArrayList<>(overrides.keySet());
        customs.removeAll(defaults.keySet());
        customs.sort(Comparator.naturalOrder());
        // Preserve player-added / datapack dimensions even when no mod with that
        // namespace is installed, just like custom particle list entries.
        ids.addAll(customs);
        return ids;
    }

    private WindDimensionSettings.Mode value(String id) {
        return overrides.getOrDefault(id, defaults().getOrDefault(id, fallback));
    }

    private void rebuildRows() {
        for (Button b : dynamicButtons) removeWidget(b);
        dynamicButtons.clear();
        for (int i = 0; i < Math.min(4, matches.size()); i++) {
            String id = matches.get(i);
            dynamicButtons.add(addRenderableWidget(Button.builder(Component.literal(id), b -> add(id))
                .bounds(left, 81 + i * 21, panelWidth, 20).build()));
        }
        List<String> ids = sortedIds();
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        int start = (int)(scroll / 24);
        int visible = Math.max(0, (height - 267) / 24);
        for (int i = start; i < Math.min(ids.size(), start + visible + 2); i++) {
            String id = ids.get(i);
            int y = 223 + i * 24 - (int)scroll;
            if (y < 221 || y + 20 > height - 38) continue;
            Button modeButton = Button.builder(Component.literal(value(id).name()), b -> {
                WindDimensionSettings.Mode changed = next(value(id));
                if (defaults().containsKey(id) && changed == defaults().get(id)) overrides.remove(id);
                else overrides.put(id, changed);
                b.setMessage(Component.literal(changed.name()));
            }).tooltip(Tooltip.create(Component.literal("Click to cycle wind mode; edited entries are saved as custom overrides.")))
              .bounds(left + panelWidth - 165, y, 134, 20).build();
            dynamicButtons.add(addRenderableWidget(modeButton));
            Button.OnPress reset = b -> {
                overrides.remove(id);
                scroll = Math.max(0, Math.min(maxScroll(), scroll));
                updateSuggestions();
            };
            dynamicButtons.add(addRenderableWidget(defaults().containsKey(id)
                ? new RestoreDefaultButton(left + panelWidth - 25, y, 23, 20, reset, "Restore built-in wind mode")
                : Button.builder(Component.literal("×"), reset)
                    .tooltip(Tooltip.create(Component.literal("Remove custom dimension")))
                    .bounds(left + panelWidth - 25, y, 23, 20).build()));
        }
    }

    private double maxScroll() {
        return Math.max(0, sortedIds().size() * 24 - Math.max(1, height - 267));
    }

    private void save() {
        List<String> encoded = new ArrayList<>();
        for (var e : overrides.entrySet()) {
            WindDimensionSettings.Mode builtin = defaults().get(e.getKey());
            if (builtin != e.getValue()) encoded.add(e.getKey() + "=" + e.getValue().name());
        }
        WindConfig.DIMENSION_MODES.set(encoded);
        WindConfig.DIMENSION_MODES.save();
        WindConfig.DEFAULT_DIMENSION_MODE.set(fallback);
        WindConfig.DEFAULT_DIMENSION_MODE.save();
        WindConfig.DIMENSION_BLACKLIST.set(List.<String>of());
        WindConfig.DIMENSION_BLACKLIST.save();
        minecraft.setScreen(parent);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_TAB && completion != null && input.isFocused()) {
            input.setValue(completion); return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER && input.isFocused()) { addInput(); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (y >= 221 && y < height - 38) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - vertical * 48));
            rebuildRows();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    private void scrollTo(double y) {
        int track = Math.max(1, height - 259);
        int thumb = Math.max(18, (int)(track * Math.min(1d, (double)track / Math.max(track, sortedIds().size() * 24))));
        scroll = Math.max(0, Math.min(maxScroll(), (y - 221 - thumb / 2d) / Math.max(1d, track - thumb) * maxScroll()));
        rebuildRows();
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        int bx = left + panelWidth + 5;
        if (button == 0 && x >= bx - 3 && x < bx + 10 && y >= 221 && y < height - 38 && maxScroll() > 0) {
            draggingScrollbar = true; scrollTo(y); return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) { scrollTo(y); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0) draggingScrollbar = false;
        return super.mouseReleased(x, y, button);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 13, 0xFFFFFF);
        g.drawString(font, "Search server dimensions or enter an ID", left, 65, 0xAAAAAA, false);
        if (matches.isEmpty()) g.drawString(font, "No matching dimensions; manual IDs are accepted.", left, 87, 0xAAAAAA, false);
        g.drawString(font, "Default Wind Mode:", left, 181, 0xE0E0E0, false);
        g.drawString(font, "Dimension modes (yellow = default, green = edited):", left, 207, 0xFFFFFF, false);
        List<String> ids = sortedIds();
        for (int i = (int)(scroll / 24); i < ids.size(); i++) {
            int y = 223 + i * 24 - (int) scroll;
            if (y < 221 || y + 20 > height - 38) continue;
            String id = ids.get(i);
            boolean modified = overrides.containsKey(id);
            g.fill(left, y, left + panelWidth - 170, y + 20, modified ? 0x60303030 : 0x80605020);
            g.drawString(font, font.plainSubstrByWidth(id, panelWidth - 184),
                left + 6, y + 6, modified ? 0x88DD88 : 0xFFDD55, false);
        }
        int track = Math.max(1, height - 259);
        int x = left + panelWidth + 5;
        g.fill(x, 221, x + 6, 221 + track, 0x60404040);
        int thumb = Math.max(18, (int)(track * Math.min(1d, (double)track / Math.max(track, ids.size() * 24))));
        int sy = 221 + (maxScroll() == 0 ? 0 : (int)((track - thumb) * scroll / maxScroll()));
        g.fill(x, sy, x + 6, Math.min(221 + track, sy + thumb), 0xFFC0C0C0);
    }
}
