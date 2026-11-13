package com.macatmovie.livingwind.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A real pixel-art restore button, rather than a font-dependent Unicode glyph. */
public final class RestoreDefaultButton extends Button {
    private static final ResourceLocation ICON = new ResourceLocation("livingwind", "textures/gui/restore_default.png");
    private static final int TEXTURE_SIZE = 16;
    private static final int ICON_RENDER_SIZE = 10;

    public RestoreDefaultButton(int x, int y, int width, int height, OnPress action, String hint) {
        super(x, y, width, height, Component.empty(), action, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.literal(hint)));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        // Use the icon's actual texture dimensions. The shorter blit overload
        // assumes a 256x256 atlas and only samples this 16x16 PNG's transparent corner.
        graphics.blit(ICON, getX() + (getWidth() - ICON_RENDER_SIZE) / 2,
            getY() + (getHeight() - ICON_RENDER_SIZE) / 2,
            0, 0, ICON_RENDER_SIZE, ICON_RENDER_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
