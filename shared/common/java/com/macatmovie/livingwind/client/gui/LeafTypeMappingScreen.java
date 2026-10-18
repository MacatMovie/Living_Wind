package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.LeafParticleTypes;
import com.macatmovie.livingwind.client.WindBlockTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

/** A lazy browser for leaf blocks in the installed modpack; no renderer or per-tick registry scanning.
 * Click a row to cycle Auto -> Conifer -> Broadleaf -> Auto. User entries win over datapack tags.
 */
public final class LeafTypeMappingScreen extends Screen {
    private static final int TOP = 69;
    private static final int ROW = 21;
    private static final int SCROLLBAR_WIDTH = 10;
    private static final int MIN_THUMB_HEIGHT = 18;
    private final Screen parent;
    private final Consumer<List<String>> save;
    private final Map<String, LeafParticleTypes.Type> entries = new LinkedHashMap<>();
    private final List<String> available = new ArrayList<>();
    private final List<String> displayed = new ArrayList<>();
    private EditBox search;
    private EditBox customId;
    private double scroll;
    private int left;
    private int widthPanel;
    private boolean draggingScrollbar;
    private double scrollbarDragOffset;

    public LeafTypeMappingScreen(Screen parent, List<? extends String> current,
                                  Consumer<List<String>> onSave) {
        super(Component.literal("Leaf Particle Types"));
        this.parent = parent;
        this.save = onSave;
        for (String entry : current) {
            if (!LeafParticleTypes.validEntry(entry)) continue;
            int eq = entry.lastIndexOf('=');
            entries.put(entry.substring(0, eq).trim(),
                LeafParticleTypes.Type.valueOf(entry.substring(eq + 1).trim().toUpperCase(Locale.ROOT)));
        }
    }

    @Override protected void init() {
        available.clear();
        // Block tags can be unloaded in the title menu; suffix-based discovery remains useful there.
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            Block block = BuiltInRegistries.BLOCK.get(id);
            String path = id.getPath();
            if (block.defaultBlockState().is(BlockTags.LEAVES)
                    || block.defaultBlockState().is(WindBlockTags.LEAF_EMITTERS)
                    || path.endsWith("_leaves") || path.contains("_leaves_")
                    || entries.containsKey(id.toString())) {
                available.add(id.toString());
            }
        }
        for (String id : entries.keySet()) if (!available.contains(id)) available.add(id);
        available.sort(Comparator.naturalOrder());
        left = Math.max(12, width / 2 - 215);
        widthPanel = Math.min(width - 24, 430);

        search = addRenderableWidget(new EditBox(font, left, 38, widthPanel, 20,
                Component.literal("Search leaf blocks")));
        search.setMaxLength(160);
        search.setHint(Component.literal("Search modid or leaf name..."));
        search.setResponder(v -> { scroll = 0; updateDisplayed(); });

        customId = addRenderableWidget(new EditBox(font, left, height - 75, widthPanel, 20,
                Component.literal("Other leaf block ID")));
        customId.setMaxLength(160);
        customId.setHint(Component.literal("Add a custom modid:leaf_block below"));

        addRenderableWidget(Button.builder(Component.literal("Add Conifer"), b -> addCustom(LeafParticleTypes.Type.CONIFER))
                .bounds(left, height - 49, widthPanel / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Add Broadleaf"), b -> addCustom(LeafParticleTypes.Type.BROADLEAF))
                .bounds(left + widthPanel / 2 + 2, height - 49, widthPanel / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> done())
                .bounds(width / 2 - 75, height - 25, 150, 20).build());
        updateDisplayed();
        setInitialFocus(search);
    }

    private void addCustom(LeafParticleTypes.Type type) {
        String raw = customId.getValue().trim().toLowerCase(Locale.ROOT);
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            customId.setHint(Component.literal("Unknown block ID — must be installed"));
            return;
        }
        entries.put(id.toString(), type);
        if (!available.contains(id.toString())) { available.add(id.toString()); available.sort(Comparator.naturalOrder()); }
        search.setValue(id.toString());
        customId.setValue("");
        updateDisplayed();
    }

    private void updateDisplayed() {
        displayed.clear();
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (String id : available) if (id.toLowerCase(Locale.ROOT).contains(q)) displayed.add(id);
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int listBottom() { return Math.max(TOP + 21, height - 86); }
    private double maxScroll() { return Math.max(0, displayed.size() * ROW - (listBottom() - TOP)); }
    private int scrollbarX() { return left + widthPanel - SCROLLBAR_WIDTH; }
    private int trackHeight() { return listBottom() - TOP; }
    private int thumbHeight() {
        int track = trackHeight();
        if (maxScroll() <= 0) return track;
        return Math.min(track, Math.max(MIN_THUMB_HEIGHT,
                (int)Math.round((double)track * track / (displayed.size() * ROW))));
    }
    private int thumbTop() {
        if (maxScroll() <= 0) return TOP;
        return TOP + (int)Math.round(scroll / maxScroll() * (trackHeight() - thumbHeight()));
    }
    private void dragScrollbar(double mouseY) {
        int travel = trackHeight() - thumbHeight();
        scroll = travel <= 0 ? 0 : Math.max(0, Math.min(maxScroll(),
                (mouseY - TOP - scrollbarDragOffset) / travel * maxScroll()));
    }

    // Minecraft 1.20.1 dispatches the three-argument method, while 1.21.1
    // dispatches the four-argument method (horizontal and vertical scroll).
    // Both signatures live here to keep this shared screen compatible with both.
    public boolean mouseScrolled(double x, double y, double amount) {
        return scrollList(x, y, amount);
    }

    public boolean mouseScrolled(double x, double y, double horizontalAmount, double verticalAmount) {
        return scrollList(x, y, verticalAmount);
    }

    private boolean scrollList(double x, double y, double amount) {
        if (x >= left && x < left + widthPanel && y >= TOP && y < listBottom()) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - amount * ROW * 3));
            return true;
        }
        return false;
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= scrollbarX() && x < left + widthPanel
                && y >= TOP && y < listBottom()) {
            // The thumb is draggable; clicking the track jumps to that part of the list.
            if (maxScroll() > 0) {
                int top = thumbTop();
                scrollbarDragOffset = y >= top && y < top + thumbHeight()
                        ? y - top : thumbHeight() / 2.0;
                draggingScrollbar = true;
                dragScrollbar(y);
            }
            return true;
        }
        if (button == 0 && x >= left && x < scrollbarX() - 2 && y >= TOP && y < listBottom()) {
            int i = (int)((y - TOP + scroll) / ROW);
            if (i >= 0 && i < displayed.size()) {
                String id = displayed.get(i);
                LeafParticleTypes.Type current = entries.get(id);
                if (current == null) entries.put(id, LeafParticleTypes.Type.CONIFER);
                else if (current == LeafParticleTypes.Type.CONIFER) entries.put(id, LeafParticleTypes.Type.BROADLEAF);
                else entries.remove(id);
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingScrollbar) {
            dragScrollbar(y);
            return true;
        }
        return super.mouseDragged(x, y, button, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    private void done() {
        List<String> lines = new ArrayList<>();
        entries.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> lines.add(e.getKey() + "=" + e.getValue().name()));
        save.accept(lines);
        minecraft.setScreen(parent);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        g.drawCenteredString(font, "Click a block to cycle: Auto / Conifer / Broadleaf", width / 2, 26, 0xBBBBBB);
        int first = (int)(scroll / ROW);
        int visible = Math.max(1, (listBottom() - TOP) / ROW + 2);
        int rowRight = scrollbarX() - 2;
        // Clip partial rows rather than hiding them while the wheel/thumb scrolls.
        g.enableScissor(left, TOP, rowRight, listBottom());
        for (int i = first; i < Math.min(displayed.size(), first + visible); i++) {
            int y = TOP + i * ROW - (int)scroll;
            String id = displayed.get(i);
            LeafParticleTypes.Type selected = entries.get(id);
            // Do not consult saved overrides here: removing an override in this
            // screen should immediately reveal the real automatic classification.
            ResourceLocation blockId = ResourceLocation.tryParse(id);
            LeafParticleTypes.Type automatic = blockId != null && BuiltInRegistries.BLOCK.containsKey(blockId)
                    ? LeafParticleTypes.resolveAutomatic(BuiltInRegistries.BLOCK.get(blockId).defaultBlockState())
                    : null; // A saved override may refer to a mod that is no longer installed.
            String status = selected == null ? "Auto (" + (automatic == null ? "Unknown"
                    : automatic == LeafParticleTypes.Type.CONIFER ? "Conifer" : "Broadleaf") + ")"
                    : selected == LeafParticleTypes.Type.CONIFER ? "Conifer*" : "Broadleaf*";
            int bg = selected == null ? 0x60303030 : 0x80333C40;
            g.fill(left, y, rowRight, y + ROW - 2, bg);
            int statusX = rowRight - 8 - font.width(status);
            int nameSpace = Math.max(0, statusX - left - 15);
            g.drawString(font, font.plainSubstrByWidth(id, nameSpace), left + 6, y + 6, 0xFFFFFF, false);
            g.drawString(font, status, statusX, y + 6,
                    selected == null ? 0xAAAAAA : selected == LeafParticleTypes.Type.CONIFER ? 0xA6EAF0 : 0xB5E6A3, false);
        }
        g.disableScissor();

        // Always show a track so the list's scrolling behavior is discoverable.
        // A full-height thumb signals that every matching block is visible.
        int barX = scrollbarX();
        g.fill(barX, TOP, barX + SCROLLBAR_WIDTH, listBottom(), 0xB0202020);
        g.fill(barX + 1, TOP + 1, barX + SCROLLBAR_WIDTH - 1, listBottom() - 1, 0xFF393939);
        int thumbY = thumbTop();
        int thumbH = thumbHeight();
        int thumbColor = maxScroll() <= 0 ? 0xFF656565
                : draggingScrollbar || (mx >= barX && mx < barX + SCROLLBAR_WIDTH
                    && my >= thumbY && my < thumbY + thumbH) ? 0xFFE0E0E0 : 0xFFAAAAAA;
        g.fill(barX + 2, thumbY + 1, barX + SCROLLBAR_WIDTH - 2, thumbY + thumbH - 1, thumbColor);
        g.drawCenteredString(font, displayed.size() + " blocks  |  * = manual override", width / 2, listBottom() + 2, 0xAAAAAA);
    }
}
