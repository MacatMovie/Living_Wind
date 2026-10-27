package com.macatmovie.livingwind.client.gui;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Groups common compatibility pages without changing their saved config paths. */
public final class LivingWindCommonCompatibilityScreen extends Screen {
    private final Screen parent;
    private final List<String> sectionKeys;
    private final BiConsumer<String, Screen> openSection;
    private int scroll;

    public LivingWindCommonCompatibilityScreen(Screen parent, List<String> sectionKeys,
            BiConsumer<String, Screen> openSection) {
        super(Component.translatable("livingwind.config.section.compatibility"));
        this.parent = parent;
        this.sectionKeys = List.copyOf(sectionKeys);
        this.openSection = openSection;
    }

    @Override
    protected void init() {
        clearWidgets();
        int top = 46;
        int visible = Math.max(1, (height - 100) / 26);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, sectionKeys.size() - visible)));
        int end = Math.min(sectionKeys.size(), scroll + visible);
        for (int i = scroll; i < end; i++) {
            String key = sectionKeys.get(i);
            // Labels on the left, action button on the right.
            int y = top + (i - scroll) * 26;
            addRenderableWidget(Button.builder(Component.translatable("livingwind.config.configure"), b ->
                openSection.accept(key, this)).bounds(width / 2 + 15, y, 125, 20).build());
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
            .bounds(width / 2 - 75, height - 28, 150, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY != 0) {
            scroll -= (int) Math.signum(deltaY);
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Screen.render handles vanilla blur/dim before drawing its buttons. Draw
        // our custom labels afterward so they are not blurred with the background.
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 17, 0xFFFFFF);
        int visible = Math.max(1, (height - 100) / 26);
        int end = Math.min(sectionKeys.size(), scroll + visible);
        for (int i = scroll; i < end; i++) {
            Component name = Component.translatable("livingwind.config.section." + sectionKeys.get(i));
            g.drawString(font, name, width / 2 - 150, 52 + (i - scroll) * 26, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
