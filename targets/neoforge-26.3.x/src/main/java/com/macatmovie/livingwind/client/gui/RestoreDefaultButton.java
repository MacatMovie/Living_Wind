package com.macatmovie.livingwind.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** A real pixel-art restore button, rather than a font-dependent Unicode glyph. */
public final class RestoreDefaultButton extends Button.Plain {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("livingwind", "textures/gui/restore_default.png");
    private static final int TEXTURE_SIZE = 16;
    private static final int ICON_RENDER_SIZE = 10;

    public RestoreDefaultButton(int x, int y, int width, int height, OnPress action, String hint) {
        super(x, y, width, height, Component.empty(), action, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.literal(hint)));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        // Use the icon's actual texture dimensions. The shorter blit overload
        // assumes a 256x256 atlas and only samples this 16x16 PNG's transparent corner.
        int x = getX() + (getWidth() - ICON_RENDER_SIZE) / 2;
        int y = getY() + (getHeight() - ICON_RENDER_SIZE) / 2;
        graphics.blit(ICON, x, y, x + ICON_RENDER_SIZE, y + ICON_RENDER_SIZE, 0f, 1f, 0f, 1f);
    }
}
