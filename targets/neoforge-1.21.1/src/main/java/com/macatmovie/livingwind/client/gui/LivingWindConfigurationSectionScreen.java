package com.macatmovie.livingwind.client.gui;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps NeoForge's normal config UI for all ordinary settings, but substitutes a purpose-built
 * particle-ID picker for Living Wind's blacklist. This avoids the generic list editor's
 * meaningless up/down ordering controls without adding another config-screen dependency.
 */
public final class LivingWindConfigurationSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {
    // UI-only grouping: keep the existing TOML paths untouched for existing worlds.
    private static final List<String> COMMON_COMPAT = List.of(
        "seasonalCompatibility", "createWindmills", "aeronautics", "highSeas", "smallShips", "treasureBalloons", "gliderWind"
    );
    private record SectionInfo(Map<String, Object> specs, Set<? extends Entry> entries, Component title) {}
    private final Map<String, SectionInfo> commonSections = new LinkedHashMap<>();

    public LivingWindConfigurationSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
        super(parent, type, modConfig, title);
    }

    private LivingWindConfigurationSectionScreen(
        Context parentContext,
        Screen parent,
        Map<String, Object> valueSpecs,
        String key,
        Set<? extends Entry> entrySet,
        Component title
    ) {
        super(parentContext, parent, valueSpecs, key, entrySet, title);
    }

    @Override
    @Nullable
    protected Element createSection(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
        if (subconfig.isEmpty()) return null;

        Component sectionName = getTranslationComponent(key);
        if (context.keylist().isEmpty() && "dimensions".equals(key)) {
            Component help = Component.literal("Configure wind behavior in each dimension.");
            Button open = Button.builder(Component.translatable("livingwind.config.configure"), button ->
                minecraft.setScreen(new DimensionSettingsScreen(this))
            ).tooltip(Tooltip.create(help)).width(Button.DEFAULT_WIDTH).build();
            return new Element(Component.translatable("livingwind.config.dimensionModes"), help, open, false);
        }
        if (context.keylist().isEmpty() && COMMON_COMPAT.contains(key)) {
            commonSections.put(key, new SectionInfo(subconfig.valueMap(), subsection.entrySet(), sectionName));
            return null; // Replaced by one Compatibility entry from createSyntheticValues().
        }
        Button open = Button.builder(
            Component.translatable("livingwind.config.configure"),
            button -> minecraft.setScreen(sectionCache.computeIfAbsent(key, ignored ->
                new LivingWindConfigurationSectionScreen(
                    context, this, subconfig.valueMap(), key, subsection.entrySet(), sectionName
                )
            ))
        ).tooltip(Tooltip.create(shortTooltip(key, null)))
         .width(Button.DEFAULT_WIDTH)
         .build();
        return new Element(sectionName, shortTooltip(key, null), open, false);
    }

    @Override
    protected Collection<? extends Element> createSyntheticValues() {
        if (commonSections.isEmpty()) return super.createSyntheticValues();
        Component label = Component.translatable("livingwind.config.section.compatibility");
        Component help = Component.literal("Configure integrations with other mods.");
        Button open = Button.builder(Component.translatable("livingwind.config.configure"), b ->
            minecraft.setScreen(new LivingWindCommonCompatibilityScreen(this,
                new ArrayList<>(commonSections.keySet()), this::openCommonSection))
        ).tooltip(Tooltip.create(help)).width(Button.DEFAULT_WIDTH).build();
        return List.of(new Element(label, help, open, false));
    }

    private void openCommonSection(String key, Screen backScreen) {
        SectionInfo info = commonSections.get(key);
        if (info != null) {
            minecraft.setScreen(new LivingWindConfigurationSectionScreen(
                context, backScreen, info.specs(), key, info.entries(), info.title()
            ));
        }
    }

    // Keep tooltips brief across all standard NeoForge controls as well as custom ones.
    @Override
    protected Component getTooltipComponent(String key, ModConfigSpec.Range<?> range) {
        if ("checkShelter".equals(key) && context.keylist().contains("entityWind")) {
            return Component.translatableWithFallback("livingwind.config.entityWindCheckShelter.tooltip",
                "ON: Keep entity wind limited to exposed areas. OFF: Skip per-entity shelter checks for better performance, allowing wind to move entities indoors and underground. Particle and sound shelter are unaffected.");
        }
        String description;
        if ("responseMultipliers".equals(key)) {
            description = "Adjust wind response for individual particles (0 = none, 1 = normal).";
        } else if ("filter".equals(key)) {
            description = "Edit additional IDs in the particle wind blacklist.";
        } else {
            description = getComment(key);
            if (description == null) description = "";
            description = description.replace('\n', ' ').trim();
            if (description.startsWith("Optional ")) description = description.substring(9);
            int end = description.indexOf(". ");
            if (end > 10) description = description.substring(0, end + 1);
            if (description.length() > 125) description = description.substring(0, 122).trim() + "...";
        }
        return Component.translatableWithFallback(getTranslationKey(key) + ".tooltip", description);
    }

    private Component shortTooltip(String key, ModConfigSpec.Range<?> range) {
        return getTooltipComponent(key, range);
    }

    /** Keep the legacy boolean in the TOML for compatibility, but hide it in the GUI. */
    @Override
    @Nullable
    protected Element createBooleanValue(
        String key,
        ModConfigSpec.ValueSpec spec,
        Supplier<Boolean> source,
        Consumer<Boolean> target
    ) {
        if ("streamsReflowingDefaultsMigrated".equals(key) && context.keylist().contains("particleWind")) return null;
        if ("showHud".equals(key) && context.keylist().contains("debug")) return null;
        return super.createBooleanValue(key, spec, source, target);
    }

    /** Show the effective migrated state and retire the old boolean on any user selection. */
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T extends Enum<T>> Element createEnumValue(
        String key,
        ModConfigSpec.ValueSpec spec,
        Supplier<T> source,
        Consumer<T> target
    ) {
        if (context.keylist().contains("dimensions") && ("netherWindMode".equals(key) || "endWindMode".equals(key) || "defaultDimensionMode".equals(key))) return null;
        if ("hudMode".equals(key) && context.keylist().contains("debug")) {
            return super.createEnumValue(key, spec,
                () -> (T) ClientWindConfig.debugHudMode(),
                next -> {
                    ClientWindConfig.DEBUG_HUD.set(false);
                    target.accept(next);
                });
        }
        return super.createEnumValue(key, spec, source, target);
    }

    @Override
    @Nullable
    protected Element createDoubleValue(
        String key,
        ModConfigSpec.ValueSpec spec,
        Supplier<Double> source,
        Consumer<Double> target
    ) {
        if ((("volume".equals(key) || "directionalWindVolume".equals(key)) && context.keylist().contains("windAudio"))
                || ("windImpact".equals(key) && context.keylist().contains("fancyWorldAnimations"))
                || ("forceMultiplier".equals(key) && context.keylist().contains("aeronautics"))
                || ("windMultiplier".equals(key) && context.keylist().contains("treasureBalloons"))
                || ("strengthFluctuation".equals(key) && context.keylist().contains("weather"))
                || (context.keylist().contains("entityWind")
                    && ("clearWeatherMultiplier".equals(key) || "rainMultiplier".equals(key)
                        || "thunderMultiplier".equals(key)))) {
            final boolean percent = "strengthFluctuation".equals(key);
            final double maxVolume = percent ? 1.0
                    : "windImpact".equals(key) ? 12.0
                    : ("forceMultiplier".equals(key) || "windMultiplier".equals(key)
                        || (context.keylist().contains("entityWind")
                            && ("clearWeatherMultiplier".equals(key) || "rainMultiplier".equals(key)
                                || "thunderMultiplier".equals(key)))) ? 5.0 : 3.0;
            double actualInitial = Math.max(0.0, Math.min(maxVolume, source.get()));
            double initial = actualInitial / maxVolume;
            AbstractSliderButton slider = new AbstractSliderButton(0, 0, 150, 20,
                Component.literal(percent ? String.format(Locale.ROOT, "%.0f%%", actualInitial * 100.0)
                    : String.format(Locale.ROOT, "%.2f", actualInitial)), initial) {
                private double lastApplied = actualInitial;

                @Override
                protected void updateMessage() {
                    double actual = Math.round(value * maxVolume * 100.0) / 100.0;
                    setMessage(Component.literal(percent ? String.format(Locale.ROOT, "%.0f%%", actual * 100.0)
                        : String.format(Locale.ROOT, "%.2f", actual)));
                }

                @Override
                protected void applyValue() {
                    double actual = Math.round(value * maxVolume * 100.0) / 100.0;
                    if (Double.compare(actual, lastApplied) != 0) {
                        lastApplied = actual;
                        target.accept(actual);
                        LivingWindConfigurationSectionScreen.this.onChanged(key);
                    }
                    updateMessage();
                }
            };
            return new Element(getTranslationComponent(key), shortTooltip(key, null), slider, false);
        }
        return super.createDoubleValue(key, spec, source, target);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> Element createList(
        String key,
        ModConfigSpec.ListValueSpec spec,
        ModConfigSpec.ConfigValue<List<T>> list
    ) {
        if ("projectileBlacklistExceptions".equals(key) && context.keylist().contains("entityWind")) return null;
        if ("projectileBlacklist".equals(key) && context.keylist().contains("entityWind")) {
            Component name = getTranslationComponent(key);
            Component help = Component.literal("Protect selected projectile types from wind deflection; only checked when projectile wind is enabled.");
            Button open = Button.builder(Component.translatable("livingwind.config.edit"), button ->
                minecraft.setScreen(new ProjectileBlacklistScreen(this)))
                .tooltip(Tooltip.create(help)).build();
            return new Element(name, help, open, false);
        }
        if ("dimensionModes".equals(key) && context.keylist().contains("dimensions")) {
            Component name = getTranslationComponent(key);
            Button open = Button.builder(Component.translatable("livingwind.config.edit"), button ->
                minecraft.setScreen(new DimensionSettingsScreen(this))
            ).tooltip(Tooltip.create(Component.literal("Set wind behavior for each dimension."))).build();
            return new Element(name, Component.literal("Choose a mode for each dimension."), open, false);
        }
        if ("dimensionBlacklist".equals(key) && context.keylist().contains("dimensions")) return null;
        if ("builtinBlacklistExceptions".equals(key) && context.keylist().contains("particleWind")) return null;
        if ("leafTypeOverrides".equals(key) && context.keylist().contains("windLeaves")) {
            Component name = getTranslationComponent(key);
            Button open = Button.builder(Component.translatable("livingwind.config.configure"), button ->
                minecraft.setScreen(new LeafTypeMappingScreen(this,
                    ClientWindConfig.LEAF_TYPE_OVERRIDES.get(), values -> {
                        ClientWindConfig.LEAF_TYPE_OVERRIDES.set(values);
                        ClientWindConfig.LEAF_TYPE_OVERRIDES.save();
                    }))
            ).tooltip(net.minecraft.client.gui.components.Tooltip.create(shortTooltip(key, null)))
             .build();
            return new Element(name, shortTooltip(key, null), open, false);
        }
        if ("filter".equals(key) && context.keylist().contains("particleWind")) {
            Component name = getTranslationComponent(key);
            Button open = Button.builder(
                Component.translatable("livingwind.config.edit"),
                button -> minecraft.setScreen(new ParticleFilterListScreen(this))
            ).tooltip(net.minecraft.client.gui.components.Tooltip.create(shortTooltip(key, null)))
             .build();
            return new Element(name, shortTooltip(key, null), open, false);
        }
        if ("blockEmitterOverrides".equals(key) && context.keylist().contains("ambientParticles")) {
            Component name = getTranslationComponent(key);
            Component help = Component.literal("Customize which blocks emit tiny windblown pixels, including mod compatibility presets.");
            Button open = Button.builder(Component.translatable("livingwind.config.edit"), button ->
                minecraft.setScreen(new AmbientEmitterListScreen(this))
            ).tooltip(Tooltip.create(help)).build();
            return new Element(name, help, open, false);
        }
        if ("responseMultipliers".equals(key) && context.keylist().contains("particleWind")) {
            Component name = getTranslationComponent(key);
            Button open = Button.builder(
                Component.translatable("livingwind.config.edit"),
                button -> minecraft.setScreen(new ParticleResponseMultiplierListScreen(this))
            ).tooltip(net.minecraft.client.gui.components.Tooltip.create(shortTooltip(key, null)))
             .build();
            return new Element(name, shortTooltip(key, null), open, false);
        }
        return super.createList(key, spec, list);
    }
}
