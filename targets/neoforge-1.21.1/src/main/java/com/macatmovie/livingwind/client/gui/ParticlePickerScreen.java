package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.BuiltinParticleBlacklist;
import com.macatmovie.livingwind.config.ClientWindConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Lazy, scrollable particle-registry browser. The registry is enumerated only when the user opens
 * Add More; nothing here runs during boot, world load, or normal gameplay.
 */
public final class ParticlePickerScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int SCROLL_STEP_ROWS = 3;

    private final Screen parent;
    private final Set<String> selected;
    private final Consumer<String> onSelect;
    private final List<String> allParticles = new ArrayList<>();
    private final List<String> filtered = new ArrayList<>();

    private EditBox search;
    private int listLeft;
    private int listRight;
    private int listTop;
    private int listBottom;
    private double scrollAmount;
    private String completionCandidate;

    public ParticlePickerScreen(Screen parent, Set<String> selected, Consumer<String> onSelect) {
        this(parent, selected, onSelect, Component.translatable("livingwind.config.particlePicker.title"));
    }

    public ParticlePickerScreen(Screen parent, Set<String> selected, Consumer<String> onSelect, Component title) {
        super(title);
        this.parent = parent;
        this.selected = new HashSet<>(selected);
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        // Intentionally lazy: enumerate/sort the live registry only when this screen is opened.
        allParticles.clear();
        for (ResourceLocation id : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            allParticles.add(id.toString());
        }
        allParticles.sort(Comparator.naturalOrder());

        int panelWidth = Math.min(380, Math.max(220, width - 30));
        listLeft = (width - panelWidth) / 2;
        listRight = listLeft + panelWidth;
        listTop = 61;
        listBottom = Math.max(listTop + 40, height - 45);

        search = addRenderableWidget(new EditBox(font, listLeft, 34, panelWidth, 20,
            Component.translatable("livingwind.config.particlePicker.search")));
        search.setMaxLength(160);
        search.setHint(Component.translatable("livingwind.config.particlePicker.searchHint"));
        search.setResponder(ignored -> {
            scrollAmount = 0.0;
            refreshResults();
        });

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
            .bounds(width / 2 - 75, height - 27, 150, 20)
            .build());

        refreshResults();
        setInitialFocus(search);
    }

    private void refreshResults() {
        String query = normalizedQuery();
        filtered.clear();

        if (query.isEmpty()) {
            filtered.addAll(allParticles);
        } else {
            // Command-like search feel: exact prefixes first, then other substring matches.
            for (String id : allParticles) {
                if (id.toLowerCase(Locale.ROOT).startsWith(query)) filtered.add(id);
            }
            for (String id : allParticles) {
                String lowered = id.toLowerCase(Locale.ROOT);
                if (!lowered.startsWith(query) && lowered.contains(query)) filtered.add(id);
            }
        }

        updateCompletionSuggestion();
        clampScroll();
    }

    private String normalizedQuery() {
        return search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private void updateCompletionSuggestion() {
        completionCandidate = null;
        if (search == null) return;

        String raw = search.getValue();
        String query = raw.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            search.setSuggestion(null);
            return;
        }

        for (String id : filtered) {
            String lowered = id.toLowerCase(Locale.ROOT);
            if (lowered.startsWith(query) && id.length() > raw.length()) {
                completionCandidate = id;
                // EditBox draws this as command-style ghost completion after the current text.
                search.setSuggestion(id.substring(raw.length()));
                return;
            }
        }
        search.setSuggestion(null);
    }

    private void acceptCompletion() {
        if (completionCandidate == null || search == null) return;
        search.setValue(completionCandidate);
        search.setCursorPosition(completionCandidate.length());
        search.setHighlightPos(completionCandidate.length());
        refreshResults();
    }

    private double maxScroll() {
        int stride = ROW_HEIGHT + ROW_GAP;
        int contentHeight = filtered.size() * stride;
        return Math.max(0.0, contentHeight - (listBottom - listTop));
    }

    private void clampScroll() {
        scrollAmount = Mth.clamp(scrollAmount, 0.0, maxScroll());
    }

    private int indexAt(double mouseX, double mouseY) {
        if (mouseX < listLeft || mouseX >= listRight - 7 || mouseY < listTop || mouseY >= listBottom) return -1;
        int stride = ROW_HEIGHT + ROW_GAP;
        int index = (int) ((mouseY - listTop + scrollAmount) / stride);
        if (index < 0 || index >= filtered.size()) return -1;

        double rowYWithin = mouseY - listTop + scrollAmount - index * stride;
        return rowYWithin < ROW_HEIGHT ? index : -1;
    }

    private void selectIndex(int index) {
        if (index < 0 || index >= filtered.size()) return;
        String id = filtered.get(index);
        if (isBuiltinActive(id)) return; // Built-ins are read-only; they need no custom entry.
        if (selected.add(id)) {
            onSelect.accept(id);
        }
    }

    private static boolean isBuiltinActive(String id) {
        return ClientWindConfig.PARTICLE_FILTER_MODE.get() == ClientWindConfig.FilterMode.BLACKLIST
            && BuiltinParticleBlacklist.contains(id);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int index = indexAt(mouseX, mouseY);
            if (index >= 0) {
                selectIndex(index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
            scrollAmount -= scrollY * (ROW_HEIGHT + ROW_GAP) * SCROLL_STEP_ROWS;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB && search != null && search.isFocused() && completionCandidate != null) {
            acceptCompletion();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xD0101010);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(listLeft, listTop, listRight, listBottom);
        int stride = ROW_HEIGHT + ROW_GAP;
        int first = Math.max(0, (int) (scrollAmount / stride));
        int visibleRows = ((listBottom - listTop) / stride) + 2;
        int last = Math.min(filtered.size(), first + visibleRows);

        for (int i = first; i < last; i++) {
            int y = listTop + i * stride - (int) scrollAmount;
            boolean hovered = mouseX >= listLeft && mouseX < listRight - 7 && mouseY >= y && mouseY < y + ROW_HEIGHT;
            boolean builtin = isBuiltinActive(filtered.get(i));
            boolean alreadySelected = builtin || selected.contains(filtered.get(i));

            int bg = builtin ? 0x80605020 : (alreadySelected ? 0x80305030 : (hovered ? 0x80505050 : 0x60303030));
            graphics.fill(listLeft, y, listRight - 7, y + ROW_HEIGHT, bg);
            graphics.drawString(
                font,
                Component.literal((alreadySelected ? "✓  " : "+  ") + filtered.get(i)),
                listLeft + 6,
                y + 6,
                builtin ? 0xFFDD55 : (alreadySelected ? 0x88DD88 : 0xFFFFFF),
                false
            );
        }
        graphics.disableScissor();

        drawScrollbar(graphics);

        Component resultText = Component.translatable(
            "livingwind.config.particlePicker.resultsScrollable",
            filtered.size()
        );
        graphics.drawString(font, resultText, listLeft, listBottom + 5, 0xB0B0B0, false);
        if (completionCandidate != null && search != null && search.isFocused()) {
            Component tabHint = Component.translatable("livingwind.config.particlePicker.tabHint");
            graphics.drawString(font, tabHint, listRight - font.width(tabHint), listBottom + 5, 0x808080, false);
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        double max = maxScroll();
        if (max <= 0.0) return;

        int trackLeft = listRight - 5;
        int trackRight = listRight - 1;
        int trackHeight = listBottom - listTop;
        int stride = ROW_HEIGHT + ROW_GAP;
        int contentHeight = Math.max(trackHeight, filtered.size() * stride);
        int thumbHeight = Math.max(16, (int) ((double) trackHeight * trackHeight / contentHeight));
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = listTop + (int) (thumbTravel * (scrollAmount / max));

        graphics.fill(trackLeft, listTop, trackRight, listBottom, 0x60404040);
        graphics.fill(trackLeft, thumbY, trackRight, thumbY + thumbHeight, 0xFFC0C0C0);
    }
}
