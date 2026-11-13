package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.fabric.config.FabricConfigSpec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Editable ordered string list. Entries are added/modified individually to preserve IDs and punctuation. */
public final class FabricListEditorScreen extends Screen {
    private static final int ROWS = 7;
    private final Screen parent;
    private final FabricConfigSpec.ConfigValue<?> option;
    private final ArrayList<String> items = new ArrayList<>();
    private int page;
    private int selected = -1;
    private EditBox input;
    private String message = "Select an entry to edit, or type a new one and press Add.";

    public FabricListEditorScreen(Screen parent, FabricConfigSpec.ConfigValue<?> option) {
        super(Component.literal("Edit " + option.path));
        this.parent = parent;
        this.option = option;
        if (option.get() instanceof List<?> list) {
            for (Object item : list) items.add(String.valueOf(item));
        }
    }

    @Override protected void init() {
        int left = width / 2 - 175;
        int top = Math.max(45, height / 2 - 124);
        int pages = Math.max(1, (items.size() + ROWS - 1) / ROWS);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int row = 0; row < ROWS; ++row) {
            int index = page * ROWS + row;
            if (index >= items.size()) break;
            String value = items.get(index);
            String label = value.length() > 56 ? value.substring(0, 53) + "..." : value;
            addRenderableWidget(Button.builder(Component.literal((index + 1) + ". " + label), b -> {
                selected = index;
                input.setValue(items.get(index));
                setInitialFocus(input);
                message = "Editing entry #" + (index + 1) + ". Press Update to save.";
            }).bounds(left, top + row * 23, 350, 20)
                .tooltip(Tooltip.create(Component.literal(value))).build());
        }
        int fieldsY = top + ROWS * 23 + 3;
        input = new EditBox(font, left, fieldsY, 350, 20, Component.literal("String entry"));
        input.setMaxLength(4096);
        input.setValue(selected >= 0 && selected < items.size() ? items.get(selected) : "");
        addRenderableWidget(input);
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> add()).bounds(left, fieldsY + 24, 65, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Update"), b -> update()).bounds(left + 70, fieldsY + 24, 75, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> delete()).bounds(left + 150, fieldsY + 24, 75, 20).build())
            .active = selected >= 0 && selected < items.size();
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
            selected = -1;
            input.setValue("");
            message = "Ready for a new entry.";
            rebuild();
        }).bounds(left + 230, fieldsY + 24, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuild(); })
            .bounds(left, fieldsY + 49, 45, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuild(); })
            .bounds(left + 49, fieldsY + 49, 45, 20).build()).active = page < pages - 1;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(left + 255, fieldsY + 49, 95, 20).build());
    }

    private void add() {
        String text = input.getValue().trim();
        if (text.isEmpty()) { message = "Cannot add an empty entry."; return; }
        ArrayList<String> next = new ArrayList<>(items);
        next.add(text);
        if (commit(next)) {
            selected = next.size() - 1;
            page = selected / ROWS;
            message = "Entry added.";
            rebuild();
        }
    }

    private void update() {
        if (selected < 0 || selected >= items.size()) { message = "Select an existing entry first."; return; }
        String text = input.getValue().trim();
        if (text.isEmpty()) { message = "Entry cannot be empty. Use Delete instead."; return; }
        ArrayList<String> next = new ArrayList<>(items);
        next.set(selected, text);
        if (commit(next)) { message = "Entry updated."; rebuild(); }
    }

    private void delete() {
        if (selected < 0 || selected >= items.size()) return;
        ArrayList<String> next = new ArrayList<>(items);
        next.remove(selected);
        if (commit(next)) {
            selected = -1;
            message = "Entry removed.";
            rebuild();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean commit(ArrayList<String> proposed) {
        // The setting's existing validator enforces the same formats as the original config.
        ((FabricConfigSpec.ConfigValue) option).set(List.copyOf(proposed));
        if (!option.get().equals(proposed)) {
            message = "Invalid entry; check the expected format in the tooltip or JSON file.";
            return false;
        }
        items.clear();
        items.addAll(proposed);
        return true;
    }

    private void rebuild() { clearWidgets(); init(); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        int top = Math.max(45, height / 2 - 124);
        graphics.drawCenteredString(font, "Living Wind - " + option.path, width / 2, top - 38, 0xE3F4FF);
        graphics.drawCenteredString(font, "Entries " + items.size() + " | Page " + (page + 1) + "/"
            + Math.max(1, (items.size() + ROWS - 1) / ROWS), width / 2, top - 25, 0xBBBBBB);
        graphics.drawCenteredString(font, message, width / 2, top + ROWS * 23 + 104, 0xD8D8D8);
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
