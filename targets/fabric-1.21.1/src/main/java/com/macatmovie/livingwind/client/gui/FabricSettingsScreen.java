package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.config.WindConfig;
import com.macatmovie.livingwind.fabric.config.FabricConfigSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Living Wind's native Fabric config, with the same category hierarchy, compact
 * two-column controls and specialized editors used by the Forge / NeoForge GUIs.
 * The configuration schema remains the authoritative source of keys and values;
 * navigation is presentation-only and does not change existing JSON config files.
 * Neither YACL nor Mod Menu is required; both may coexist with this screen.
 */
public final class FabricSettingsScreen extends Screen {
    private static final int TOP = 36, BOTTOM = 39, ROW_HEIGHT = 26;
    private static final List<String> COMMON_COMPAT = List.of(
        "seasonalCompatibility", "createWindmills", "valkyrienSkies", "aeronautics",
        "highSeas", "smallShips", "alekiShips", "treasureBalloons", "gliderWind");

    // Forge 1.20.1 introduced these client integrations before they were
    // reorganized under compatibility in NeoForge 1.21.1. Keep their original
    // persisted JSON paths, but show them under Compatibility in both UIs.
    private static final List<String> LEGACY_CLIENT_COMPAT_ROOTS = List.of(
        "fancyWorldAnimations", "chimes", "swingingSigns", "swingingLanterns",
        "windblown", "interactiveFoliage", "supplementaries", "shaderBridge");
    private static final List<String> CLIENT_COMPAT_ORDER = List.of(
        "seasonalCompatibility", "prettyRain", "particleRain", "particular",
        "advancedChimneys", "supplementaries", "swingingSigns", "swingingLanterns",
        "chimes", "fancyWorldAnimations", "windblown", "interactiveFoliage", "shaderBridge");

    private final Screen parent;
    // Root, or a category path with a root section (common / client).
    private final List<String> path;
    private final List<Row> rows = new ArrayList<>();
    private int contentHeight;
    private double scroll;
    private int labelLeft;
    private boolean canEditCommon;

    public FabricSettingsScreen(Screen parent) {
        this(parent, List.of());
    }

    private FabricSettingsScreen(Screen parent, List<String> path) {
        super(Component.literal("Living Wind Configuration"));
        this.parent = parent;
        this.path = List.copyOf(path);
    }

    private boolean isRemoteServer() {
        return minecraft != null && minecraft.level != null && !minecraft.hasSingleplayerServer();
    }

    private Map<String, FabricConfigSpec.ConfigValue<?>> activeSettings() {
        return !path.isEmpty() && "common".equals(path.get(0))
            ? WindConfig.SPEC.entries() : ClientWindConfig.SPEC.entries();
    }

    private static class Node {
        final Map<String, Node> children = new LinkedHashMap<>();
        final List<FabricConfigSpec.ConfigValue<?>> settings = new ArrayList<>();
    }

    private Node buildTree() {
        Node root = new Node();
        boolean common = "common".equals(path.get(0));
        for (FabricConfigSpec.ConfigValue<?> setting : activeSettings().values()) {
            String[] pieces = setting.path.split("\\.");
            if (hidden(setting.path)) continue;
            List<String> groups = new ArrayList<>(Arrays.asList(pieces).subList(0, pieces.length - 1));
            if (common && !groups.isEmpty() && COMMON_COMPAT.contains(groups.get(0))) {
                groups.add(0, "compatibility");
            } else if (!common && !groups.isEmpty()) {
                // Presentation-only migration: never change setting.path or its
                // on-disk JSON key, or old Forge-derived Fabric configs would reset.
                if (LEGACY_CLIENT_COMPAT_ROOTS.contains(groups.get(0))) {
                    groups.add(0, "compatibility");
                } else if (groups.size() == 1 && "compatibility".equals(groups.get(0))) {
                    String section = legacyClientCompatSection(pieces[pieces.length - 1]);
                    if (section != null) groups.add(section);
                }
            }
            Node n = root;
            for (String group : groups) n = n.children.computeIfAbsent(group, x -> new Node());
            n.settings.add(setting);
        }
        if (common) root.children.putIfAbsent("dimensions", new Node());
        if (!common && root.children.containsKey("compatibility")) {
            reorderSections(root.children.get("compatibility"), CLIENT_COMPAT_ORDER);
        }
        // Keep category order aligned with the original loader screens, independently
        // of the declaration order in the TOML/JSON schema.
        List<String> preferred = common
            ? List.of("wind", "weather", "events", "altitude", "dimensions", "entityWind", "network", "compatibility")
            : List.of("particleWind", "ambientParticles", "windStreaks", "windLeaves", "windAudio", "exposure", "debug", "compatibility");
        Map<String, Node> sorted = new LinkedHashMap<>();
        for (String key : preferred) if (root.children.containsKey(key)) sorted.put(key, root.children.get(key));
        root.children.forEach(sorted::putIfAbsent);
        root.children.clear();
        root.children.putAll(sorted);
        return root;
    }

    private static void reorderSections(Node node, List<String> preferred) {
        Map<String, Node> ordered = new LinkedHashMap<>();
        for (String name : preferred) {
            Node section = node.children.get(name);
            if (section != null) ordered.put(name, section);
        }
        node.children.forEach(ordered::putIfAbsent);
        node.children.clear();
        node.children.putAll(ordered);
    }

    /** Groups early 1.20.1 flat compatibility settings in the UI only. */
    private static String legacyClientCompatSection(String key) {
        return switch (key) {
            case "prettyRainWind", "prettyRainWindMultiplier" -> "prettyRain";
            case "particleRainWind", "particleRainWindMultiplier" -> "particleRain";
            case "advancedChimneysOutdoorSmokeWind" -> "advancedChimneys";
            case "fallingLeavesSource", "particularFallingLeavesWind",
                 "particularLeavesMinDensity", "particularLeavesMaxDensity",
                 "particularLeavesFullWindStrength" -> "particular";
            default -> null;
        };
    }

    private static boolean hidden(String key) {
        // These are migrations/exception backing lists. Their working editors below
        // expose the correct effective values, just as the NeoForge GUI does.
        return key.equals("particleWind.streamsReflowingDefaultsMigrated")
            || key.equals("particleWind.builtinBlacklistExceptions")
            || key.equals("debug.showHud")
            || key.equals("dimensions.dimensionBlacklist")
            || key.equals("dimensions.dimensionModes")
            || key.equals("dimensions.netherWindMode")
            || key.equals("dimensions.endWindMode")
            || key.equals("dimensions.defaultDimensionMode")
            || key.equals("entityWind.projectileBlacklistExceptions");
    }

    private Node currentNode() {
        Node n = buildTree();
        for (int i = 1; i < path.size(); i++) {
            n = n.children.get(path.get(i));
            if (n == null) return new Node();
        }
        return n;
    }

    private String pageTitle() {
        if (path.isEmpty()) return "Living Wind Configuration";
        if (path.size() == 1) return "Living Wind " + ("common".equals(path.get(0)) ? "Common" : "Client") + " Configuration";
        return "Living Wind - " + categoryName(path.get(path.size() - 1));
    }

    private static String categoryName(String key) {
        String tr = "livingwind.config.section." + key;
        if (I18n.exists(tr)) return I18n.get(tr);
        if (key.equals("valkyrienSkies")) return "Valkyrien Skies / Eureka / Clockwork";
        if (key.equals("alekiShips")) return "Aleki's Nifty Ships";
        if (key.equals("fancyWorldAnimations")) return "Fancy World Animations";
        if (key.equals("seasonalCompatibility")) return "Seasonal Compatibility";
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private void open(String category) {
        commitValues();
        ArrayList<String> next = new ArrayList<>(path);
        next.add(category);
        minecraft.setScreen(new FabricSettingsScreen(this, next));
    }

    @Override protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        rows.clear();
        canEditCommon = !isRemoteServer();
        if (path.isEmpty()) {
            navigation("Common Options", () -> open("common"), canEditCommon,
                canEditCommon ? "Server gameplay, simulation, entities and dimension settings"
                    : "The connected server owns these values. Edit its config on the server.");
            navigation("Client Options", () -> open("client"), true,
                "Particles, leaves, effects, wind sounds and presentation settings");
        } else {
            Node node = currentNode();
            for (String category : node.children.keySet()) {
                if ("common".equals(path.get(0)) && "dimensions".equals(category) && path.size() == 1) {
                    navigation("Dimension Settings", () -> minecraft.setScreen(new DimensionSettingsScreen(this)), true,
                        "Configure wind behavior per dimension, with default presets and overrides");
                } else {
                    navigation(categoryName(category), () -> open(category), true, null);
                }
            }
            for (FabricConfigSpec.ConfigValue<?> entry : node.settings) settingRow(entry);
        }
        contentHeight = rows.size() * ROW_HEIGHT;
        scroll = clamp(scroll);
        int w = Math.min(150, Math.max(80, width / 2 - 14));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
            .bounds(width / 2 - w / 2, height - 29, w, 20).build());
        updatePositions();
    }

    private void navigation(String name, Runnable action, boolean enabled, String help) {
        Button.Builder builder = Button.builder(Component.translatable("livingwind.config.configure"), b -> action.run())
            .bounds(0, 0, 150, 20);
        if (help != null) builder.tooltip(Tooltip.create(Component.literal(help)));
        Button button = addRenderableWidget(builder.build());
        button.active = enabled;
        rows.add(new Row(Component.literal(name), button, null, null, null, enabled));
    }

    private void settingRow(FabricConfigSpec.ConfigValue<?> setting) {
        String name = labelOf(setting);
        Component label = Component.literal(name);
        Object current = setting.get();
        String tooltipText = setting.description == null ? "" : setting.description;
        if (tooltipText.isBlank()) tooltipText = setting.path;
        Tooltip tooltip = Tooltip.create(Component.literal(tooltipText));
        AbstractWidget control;
        Runnable commit = null;
        if (current instanceof Boolean) {
            control = Button.builder(boolName((Boolean) current), b -> {
                set(setting, !(Boolean) setting.get());
                b.setMessage(boolName((Boolean) setting.get()));
                updateResetButtons();
            }).bounds(0, 0, 150, 20).tooltip(tooltip).build();
        } else if (current instanceof Enum<?>) {
            control = Button.builder(Component.literal(enumName(setting)), b -> {
                Enum<?> e = (Enum<?>) setting.get();
                Enum<?>[] candidates = (Enum<?>[]) e.getDeclaringClass().getEnumConstants();
                Object choice = candidates[(e.ordinal() + 1) % candidates.length];
                if (setting == ClientWindConfig.DEBUG_HUD_MODE) {
                    ClientWindConfig.setDebugHudMode((ClientWindConfig.DebugHudMode) choice);
                } else {
                    set(setting, choice);
                }
                b.setMessage(Component.literal(enumName(setting)));
                updateResetButtons();
            }).bounds(0, 0, 150, 20).tooltip(tooltip).build();
        } else if (current instanceof Number && isSlider(setting)) {
            double min = min(setting), max = max(setting);
            boolean percent = setting.path.endsWith("strengthFluctuation");
            control = new ValueSlider(setting, min, max, percent);
            control.setTooltip(tooltip);
        } else if (current instanceof Number) {
            EditBox input = new EditBox(font, 0, 0, 150, 20, label);
            input.setValue(numberText(current));
            input.setFilter(v -> validPartialNumber(v, current instanceof Integer));
            input.setMaxLength(40);
            input.setTooltip(tooltip);
            control = input;
            commit = () -> {
                try {
                    Object parsed;
                    if (current instanceof Integer) parsed = Integer.valueOf(input.getValue());
                    else parsed = Double.valueOf(input.getValue());
                    if (setting.accepts(parsed)) set(setting, parsed);
                } catch (NumberFormatException ignored) { /* keep prior safe value */ }
            };
        } else if (current instanceof List<?>) {
            control = Button.builder(Component.translatable("livingwind.config.edit"), b -> openList(setting))
                .bounds(0, 0, 150, 20).tooltip(tooltip).build();
        } else {
            return;
        }
        addRenderableWidget(control);
        Button reset = addRenderableWidget(new RestoreDefaultButton(0, 0, 22, 20, b -> {
            commitValues();
            if (setting == ClientWindConfig.DEBUG_HUD_MODE) {
                ClientWindConfig.setDebugHudMode((ClientWindConfig.DebugHudMode) setting.getDefault());
            } else {
                set(setting, setting.getDefault());
            }
            rebuild();
        }, "Reset " + name + " to default: " + setting.getDefault()));
        reset.active = !current.equals(setting.getDefault());
        rows.add(new Row(label, control, reset, setting, commit, true));
    }

    private void openList(FabricConfigSpec.ConfigValue<?> setting) {
        commitValues();
        String key = setting.path;
        if (key.equals("particleWind.filter")) minecraft.setScreen(new ParticleFilterListScreen(this));
        else if (key.equals("particleWind.responseMultipliers")) minecraft.setScreen(new ParticleResponseMultiplierListScreen(this));
        else if (key.equals("ambientParticles.blockEmitterOverrides")) minecraft.setScreen(new AmbientEmitterListScreen(this));
        else if (key.equals("entityWind.projectileBlacklist")) minecraft.setScreen(new ProjectileBlacklistScreen(this));
        else if (key.equals("windLeaves.leafTypeOverrides")) {
            minecraft.setScreen(new com.macatmovie.livingwind.client.gui.LeafTypeMappingScreen(this,
                ClientWindConfig.LEAF_TYPE_OVERRIDES.get(), values -> {
                    ClientWindConfig.LEAF_TYPE_OVERRIDES.set(values);
                    ClientWindConfig.LEAF_TYPE_OVERRIDES.save();
                }));
        } else if (key.equals("dimensions.dimensionModes")) minecraft.setScreen(new DimensionSettingsScreen(this));
        else minecraft.setScreen(new FabricListEditorScreen(this, setting));
    }

    private static String enumName(FabricConfigSpec.ConfigValue<?> setting) {
        Enum<?> v = (Enum<?>) setting.get();
        if (v instanceof ClientWindConfig.EclipticLeavesSource || v instanceof ClientWindConfig.FallingLeavesSource) {
            return switch (v.name()) {
                case "LIVING_WIND" -> "Living Wind Only";
                case "ECLIPTIC_SEASONS" -> "Ecliptic Seasons Only";
                case "PARTICULAR" -> "Particular Only";
                default -> "Both";
            };
        }
        if (v instanceof ClientWindConfig.WindAudioDebugMode) {
            return switch (v.name()) {
                case "STEREO_ONLY" -> "Stereo Only";
                case "DIRECTIONAL_ONLY" -> "Directional Only";
                default -> "Normal";
            };
        }
        return v.name();
    }

    private static String labelOf(FabricConfigSpec.ConfigValue<?> setting) {
        if (setting == ClientWindConfig.DEBUG_HUD_MODE) return "Debug HUD";
        if (setting == ClientWindConfig.WIND_AUDIO_DEBUG_MODE) return "Wind Audio Test Mode";
        if (!setting.translationKey.isBlank() && I18n.exists(setting.translationKey)) return I18n.get(setting.translationKey);
        return categoryName(setting.path.substring(setting.path.lastIndexOf('.') + 1));
    }

    private static Component boolName(boolean enabled) {
        return Component.translatable(enabled ? "options.on" : "options.off");
    }

    private static String numberText(Object n) {
        return n instanceof Integer ? String.valueOf(n) : Double.toString(((Number) n).doubleValue());
    }

    private static boolean validPartialNumber(String v, boolean integer) {
        if (v.isEmpty() || v.equals("-")) return true;
        return integer ? v.matches("-?\\d+")
            : v.equals(".") || v.equals("-.") || v.matches("-?(?:\\d+\\.?\\d*|\\.\\d*)");
    }

    private static boolean isSlider(FabricConfigSpec.ConfigValue<?> entry) {
        String key = entry.path;
        return key.endsWith("strengthFluctuation")
            || key.endsWith("windImpact")
            || key.endsWith("windMultiplier")
            || key.endsWith("forceMultiplier")
            || key.endsWith("windSpeedScale")
            || key.endsWith("windMaxSpeed")
            || key.equals("windAudio.volume")
            || key.equals("windAudio.directionalWindVolume")
            || (key.startsWith("entityWind.") && (
                key.endsWith("Multiplier") || key.endsWith("windInfluence")));
    }

    private static double min(FabricConfigSpec.ConfigValue<?> value) {
        if (value instanceof FabricConfigSpec.DoubleValue d) return d.minimum();
        if (value instanceof FabricConfigSpec.IntValue i) return i.minimum();
        return 0.0;
    }
    private static double max(FabricConfigSpec.ConfigValue<?> value) {
        if (value instanceof FabricConfigSpec.DoubleValue d) return d.maximum();
        if (value instanceof FabricConfigSpec.IntValue i) return i.maximum();
        return 1.0;
    }

    private final class ValueSlider extends AbstractSliderButton {
        private final FabricConfigSpec.ConfigValue<?> setting;
        private final double min, max;
        private final boolean percent;
        ValueSlider(FabricConfigSpec.ConfigValue<?> setting, double min, double max, boolean percent) {
            super(0, 0, 150, 20, Component.empty(), max <= min ? 0.0 :
                Math.max(0.0, Math.min(1.0, (((Number) setting.get()).doubleValue() - min) / (max - min))));
            this.setting = setting;
            this.min = min;
            this.max = max;
            this.percent = percent;
            updateMessage();
        }
        private double actual() { return Math.round((min + (max - min) * value) * 100.0) / 100.0; }
        @Override protected void updateMessage() {
            setMessage(Component.literal(percent ? String.format(Locale.ROOT, "%.0f%%", actual() * 100.0)
                : String.format(Locale.ROOT, "%.2f", actual())));
        }
        @Override protected void applyValue() {
            double n = actual();
            if (setting.accepts(n)) preview(setting, n);
            updateMessage();
            updateResetButtons();
        }
    }

    private record Row(Component label, AbstractWidget widget, Button reset,
                       FabricConfigSpec.ConfigValue<?> setting, Runnable commit, boolean enabled) {}

    private int controlX() { return width / 2 + 4; }
    private int controlWidth() { return Math.min(150, Math.max(78, width / 2 - 32)); }
    private int labelX() {
        int longest = 0;
        for (Row row : rows) longest = Math.max(longest, font.width(row.label));
        return Math.max(8, width / 2 - Math.max(157, longest + 14));
    }
    private int rowY(int row) { return TOP + row * ROW_HEIGHT - (int) Math.round(scroll); }
    private double maxScroll() { return Math.max(0, contentHeight - Math.max(1, height - TOP - BOTTOM)); }
    private double clamp(double value) { return Math.max(0, Math.min(maxScroll(), value)); }

    private void updatePositions() {
        labelLeft = labelX();
        int x = controlX(), w = controlWidth();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = rowY(i);
            boolean visible = y >= TOP && y + ROW_HEIGHT <= height - BOTTOM;
            row.widget.setX(x);
            row.widget.setY(y + 2);
            row.widget.setWidth(row.reset == null ? w : w - 27);
            row.widget.visible = visible;
            row.widget.active = visible && row.enabled;
            if (row.reset != null) {
                row.reset.setX(x + w - 22);
                row.reset.setY(y + 2);
                row.reset.visible = visible;
                row.reset.active = visible && row.setting != null && !row.setting.get().equals(row.setting.getDefault());
            }
        }
    }

    private void updateResetButtons() {
        for (Row row : rows) {
            if (row.reset != null) {
                row.reset.active = row.setting != null && !row.setting.get().equals(row.setting.getDefault());
            }
        }
        updatePositions();
    }

    private void commitValues() {
        for (Row row : rows) if (row.commit != null) row.commit.run();
        ClientWindConfig.SPEC.save();
        if (!isRemoteServer()) WindConfig.SPEC.save();
        ParticleWindFilter.refresh();
    }

    @Override public void onClose() {
        commitValues();
        minecraft.setScreen(parent);
    }

    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (my >= TOP && my <= height - BOTTOM) {
            scroll = clamp(scroll - dy * ROW_HEIGHT * 2.0);
            updatePositions();
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        for (int i = 0; i < rows.size(); i++) {
            int y = rowY(i);
            if (y >= TOP && y + ROW_HEIGHT <= height - BOTTOM) {
                graphics.drawString(font, rows.get(i).label, labelLeft, y + 8, 0xFFFFFF, false);
            }
        }
        graphics.drawCenteredString(font, pageTitle(), width / 2, 14, 0xFFFFFF);
        if (path.isEmpty() && !canEditCommon)
            graphics.drawCenteredString(font, "Common settings are managed by the server", width / 2, height - 45, 0xDDAD81);
        drawScrollbar(graphics);
    }

    private void drawScrollbar(GuiGraphics g) {
        double m = maxScroll();
        if (m <= 0) return;
        int heightOfTrack = height - TOP - BOTTOM;
        int thumb = Math.max(18, (int) ((double) heightOfTrack * heightOfTrack / contentHeight));
        int y = TOP + (int) ((scroll / m) * (heightOfTrack - thumb));
        int x = controlX() + controlWidth() + 9;
        g.fill(x, TOP, x + 4, height - BOTTOM, 0x40202020);
        g.fill(x, y, x + 4, y + thumb, 0xFFA0A0A0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void preview(FabricConfigSpec.ConfigValue<?> value, Object replacement) {
        ((FabricConfigSpec.ConfigValue) value).setWithoutSaving(replacement);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void set(FabricConfigSpec.ConfigValue<?> value, Object replacement) {
        ((FabricConfigSpec.ConfigValue) value).set(replacement);
    }
}
