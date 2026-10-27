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
import org.lwjgl.glfw.GLFW;

/** Dimension-first, server-config editor. Uses connected-server IDs for autocomplete. */
public final class DimensionSettingsScreen extends Screen {
    private final Screen parent;
    private final Map<String, WindDimensionSettings.Mode> selections = new LinkedHashMap<>();
    private final List<String> known = new ArrayList<>();
    private final List<String> matches = new ArrayList<>();
    private final List<Button> dynamicButtons = new ArrayList<>();
    private WindDimensionSettings.Mode fallback;
    private EditBox input;
    private Button fallbackButton;
    private int left;
    private int panelWidth;
    private double scroll;
    private String completion;

    public DimensionSettingsScreen(Screen parent) {
        super(Component.literal("Dimension Settings"));
        this.parent = parent;
        fallback = WindConfig.DEFAULT_DIMENSION_MODE.get();
        // Old config entries are imported before explicit overrides, which take priority.
        selections.put("minecraft:overworld", fallback);
        selections.put("minecraft:the_nether", WindDimensionSettings.legacyNetherMode());
        selections.put("minecraft:the_end", WindDimensionSettings.legacyEndMode());
        for (String id : WindConfig.DIMENSION_BLACKLIST.get()) {
            String normalized = id.trim().toLowerCase(Locale.ROOT);
            if (WindDimensionBlacklist.validId(normalized)) selections.put(normalized, WindDimensionSettings.Mode.DISABLED);
        }
        for (String entry : WindConfig.DIMENSION_MODES.get()) {
            if (!WindDimensionSettings.validEntry(entry)) continue;
            int split = entry.lastIndexOf('=');
            selections.put(entry.substring(0, split).trim().toLowerCase(Locale.ROOT),
                WindDimensionSettings.Mode.valueOf(entry.substring(split + 1).trim().toUpperCase(Locale.ROOT)));
        }
    }

    @Override protected void init() {
        panelWidth = Math.min(420, width - 32);
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
        input.setResponder(value -> updateSuggestions());
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> addInput())
            .bounds(left + panelWidth - 70, 37, 70, 20).build());
        fallbackButton = addRenderableWidget(Button.builder(Component.literal(fallback.name()), b -> {
            fallback = next(fallback);
            fallbackButton.setMessage(Component.literal(fallback.name()));
        }).bounds(left + panelWidth - 145, 175, 145, 20)
            .tooltip(Tooltip.create(Component.literal("Wind behavior for dimensions without custom settings.")))
            .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> save())
            .bounds(width / 2 - 75, height - 28, 150, 20).build());
        updateSuggestions();
        setInitialFocus(input);
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
        selections.putIfAbsent(id, fallback);
        input.setValue("");
        scroll = 0;
        updateSuggestions();
    }

    private void updateSuggestions() {
        if (input == null) return;
        matches.clear();
        String query = input.getValue().trim().toLowerCase(Locale.ROOT);
        for (String id : known) {
            if (!selections.containsKey(id) && (query.isEmpty() || id.startsWith(query))) matches.add(id);
        }
        for (String id : known) {
            if (!selections.containsKey(id) && !id.startsWith(query) && id.contains(query)) matches.add(id);
        }
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
        List<String> ids = new ArrayList<>(selections.keySet());
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    private void rebuildRows() {
        for (Button button : dynamicButtons) removeWidget(button);
        dynamicButtons.clear();
        for (int i = 0; i < Math.min(4, matches.size()); i++) {
            String id = matches.get(i);
            Button button = Button.builder(Component.literal(id), b -> add(id))
                .bounds(left, 81 + i * 21, panelWidth, 20).build();
            dynamicButtons.add(addRenderableWidget(button));
        }
        List<String> ids = sortedIds();
        int start = (int)(scroll / 24);
        int visible = Math.max(0, (height - 267) / 24);
        for (int i = start; i < Math.min(ids.size(), start + visible + 1); i++) {
            String id = ids.get(i);
            int y = 223 + i * 24 - (int) scroll;
            if (y < 221 || y + 20 > height - 38) continue;
            Button modeButton = Button.builder(Component.literal(selections.get(id).name()), b -> {
                WindDimensionSettings.Mode changed = next(selections.get(id));
                selections.put(id, changed);
                b.setMessage(Component.literal(changed.name()));
            }).bounds(left + panelWidth - 158, y, 132, 20).build();
            dynamicButtons.add(addRenderableWidget(modeButton));
            boolean vanilla = id.equals("minecraft:overworld") || id.equals("minecraft:the_nether")
                || id.equals("minecraft:the_end");
            Button removeButton = Button.builder(Component.literal(vanilla ? "↺" : "×"), b -> {
                if (vanilla) {
                    selections.put(id, id.equals("minecraft:the_nether") ? WindDimensionSettings.legacyNetherMode()
                        : id.equals("minecraft:the_end") ? WindDimensionSettings.legacyEndMode() : fallback);
                } else {
                    selections.remove(id);
                }
                scroll = Math.max(0, Math.min(maxScroll(), scroll));
                updateSuggestions();
            }).bounds(left + panelWidth - 23, y, 23, 20).build();
            dynamicButtons.add(addRenderableWidget(removeButton));
        }
    }

    private double maxScroll() {
        return Math.max(0, sortedIds().size() * 24 - Math.max(0, height - 267));
    }

    private void save() {
        List<String> encoded = new ArrayList<>();
        for (String id : sortedIds()) encoded.add(id + "=" + selections.get(id).name());
        WindConfig.DIMENSION_MODES.set(encoded);
        WindConfig.DIMENSION_MODES.save();
        WindConfig.DEFAULT_DIMENSION_MODE.set(fallback);
        WindConfig.DEFAULT_DIMENSION_MODE.save();
        // Old blacklist settings are now represented by explicit DISABLED entries.
        WindConfig.DIMENSION_BLACKLIST.set(List.<String>of());
        WindConfig.DIMENSION_BLACKLIST.save();
        minecraft.setScreen(parent);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_TAB && completion != null && input.isFocused()) {
            input.setValue(completion);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER && input.isFocused()) {
            addInput();
            return true;
        }
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

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        g.drawCenteredString(font, title, width / 2, 13, 0xFFFFFF);
        g.drawString(font, "Search server dimensions or enter an ID", left, 65, 0xAAAAAA, false);
        if (matches.isEmpty()) g.drawString(font, "No matching dimensions found. You can still enter an ID manually.", left, 87, 0xAAAAAA, false);
        g.drawString(font, "Default Wind Mode:", left, 181, 0xE0E0E0, false);
        g.drawString(font, "Dimension wind modes (click to cycle):", left, 207, 0xFFFFFF, false);
        List<String> ids = sortedIds();
        for (int i = (int)(scroll / 24); i < ids.size(); i++) {
            int y = 223 + i * 24 - (int) scroll;
            if (y < 221 || y + 20 > height - 38) continue;
            g.fill(left, y, left + panelWidth - 161, y + 20, 0x60303030);
            g.drawString(font, font.plainSubstrByWidth(ids.get(i), panelWidth - 174),
                left + 6, y + 6, 0xE0E0E0, false);
        }
        
    }
}
