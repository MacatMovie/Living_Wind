package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.fabric.config.FabricConfigSpec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Exact numeric entry, including values smaller than the old 0.05 button step. */
public final class FabricNumberEditorScreen extends Screen {
    private final Screen parent;
    private final FabricConfigSpec.ConfigValue<?> setting;
    private EditBox input;
    private String error = "";

    public FabricNumberEditorScreen(Screen parent, FabricConfigSpec.ConfigValue<?> setting) {
        super(Component.literal("Living Wind - " + setting.path));
        this.parent = parent;
        this.setting = setting;
    }
    @Override protected void init() {
        int x = width / 2 - 145;
        int y = height / 2 - 20;
        input = new EditBox(font, x, y, 290, 20, Component.literal("Numeric value"));
        input.setMaxLength(40);
        input.setValue(String.valueOf(setting.get()));
        addRenderableWidget(input);
        setInitialFocus(input);
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
            .bounds(x, y + 26, 91, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            set(setting, setting.getDefault()); onClose();
        }).bounds(x + 98, y + 26, 91, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(x + 196, y + 26, 94, 20).build());
    }
    private void save() {
        try {
            Object parsed;
            if (setting.get() instanceof Integer) parsed = Integer.valueOf(input.getValue().trim());
            else parsed = Double.valueOf(input.getValue().trim());
            set(setting, parsed);
            if (!setting.get().equals(parsed)) {
                error = "Out of range (or invalid value). Original value retained.";
                return;
            }
            onClose();
        } catch (NumberFormatException ex) {
            error = "Enter a valid number.";
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void set(FabricConfigSpec.ConfigValue<?> setting, Object value) {
        ((FabricConfigSpec.ConfigValue) setting).set(value);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, setting.path, width / 2, height / 2 - 60, 0xE3F4FF);
        g.drawCenteredString(font, setting.description, width / 2, height / 2 - 44, 0xBBBBBB);
        if (!error.isEmpty()) g.drawCenteredString(font, error, width / 2, height / 2 + 57, 0xFF7777);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
