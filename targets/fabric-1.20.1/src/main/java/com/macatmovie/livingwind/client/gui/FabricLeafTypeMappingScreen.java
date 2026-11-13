package com.macatmovie.livingwind.client.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** 1.20.1-only background wrapper for the otherwise shared leaf mapping editor. */
public final class FabricLeafTypeMappingScreen extends LeafTypeMappingScreen {
    public FabricLeafTypeMappingScreen(Screen parent, List<? extends String> current,
                                       Consumer<List<String>> onSave) {
        super(parent, current, onSave);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
