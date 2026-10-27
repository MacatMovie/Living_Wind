package com.macatmovie.livingwind.client.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Forge 1.20.1 needs an explicit background pass for the shared leaf editor. */
public final class ForgeLeafTypeMappingScreen extends LeafTypeMappingScreen {
    public ForgeLeafTypeMappingScreen(Screen parent, List<? extends String> values,
            Consumer<List<String>> save) {
        super(parent, values, save);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
