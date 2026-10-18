package com.macatmovie.livingwind.client.gui;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps NeoForge's normal config UI for all ordinary settings, but substitutes a purpose-built
 * particle-ID picker for Living Wind's blacklist/whitelist. This avoids the generic list editor's
 * meaningless up/down ordering controls without adding another config-screen dependency.
 */
public final class LivingWindConfigurationSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {
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
        Button open = Button.builder(
            Component.translatable("livingwind.config.openSection", sectionName),
            button -> minecraft.setScreen(sectionCache.computeIfAbsent(key, ignored ->
                new LivingWindConfigurationSectionScreen(
                    context,
                    this,
                    subconfig.valueMap(),
                    key,
                    subsection.entrySet(),
                    sectionName
                )
            ))
        ).tooltip(net.minecraft.client.gui.components.Tooltip.create(getTooltipComponent(key, null)))
         .width(Button.DEFAULT_WIDTH)
         .build();

        return new Element(sectionName, getTooltipComponent(key, null), open, false);
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
        if (("volume".equals(key) && context.keylist().contains("windAudio"))
                || ("windImpact".equals(key) && context.keylist().contains("fancyWorldAnimations"))
                || ("forceMultiplier".equals(key) && context.keylist().contains("aeronautics"))) {
            final double maxVolume = "windImpact".equals(key) ? 12.0
                    : "forceMultiplier".equals(key) ? 5.0 : 3.0;
            double actualInitial = Math.max(0.0, Math.min(maxVolume, source.get()));
            double initial = actualInitial / maxVolume;
            AbstractSliderButton slider = new AbstractSliderButton(0, 0, 150, 20, Component.literal(String.format(Locale.ROOT, "%.2f", actualInitial)), initial) {
                private double lastApplied = actualInitial;

                @Override
                protected void updateMessage() {
                    double actual = Math.round(value * maxVolume * 100.0) / 100.0;
                    setMessage(Component.literal(String.format(Locale.ROOT, "%.2f", actual)));
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
            return new Element(getTranslationComponent(key), getTooltipComponent(key, null), slider, false);
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
        if ("leafTypeOverrides".equals(key) && context.keylist().contains("windLeaves")) {
            Component name = getTranslationComponent(key);
            Button open = Button.builder(Component.literal("Edit Leaf Particle Types..."), button ->
                minecraft.setScreen(new LeafTypeMappingScreen(this,
                    ClientWindConfig.LEAF_TYPE_OVERRIDES.get(), values -> {
                        ClientWindConfig.LEAF_TYPE_OVERRIDES.set(values);
                        ClientWindConfig.LEAF_TYPE_OVERRIDES.save();
                    }))
            ).tooltip(net.minecraft.client.gui.components.Tooltip.create(getTooltipComponent(key, null)))
             .build();
            return new Element(name, getTooltipComponent(key, null), open, false);
        }
        if ("filter".equals(key) && context.keylist().contains("particleWind")) {
            Component name = getTranslationComponent(key);
            Button open = Button.builder(
                Component.translatable("livingwind.config.particleFilter.edit"),
                button -> minecraft.setScreen(new ParticleFilterListScreen(
                    ConfigurationScreen.ConfigurationSectionScreen.Context.list(context, this),
                    key,
                    Component.translatable("livingwind.config.particleFilter.title"),
                    spec,
                    (ModConfigSpec.ConfigValue<List<String>>) (ModConfigSpec.ConfigValue<?>) list
                ))
            ).tooltip(net.minecraft.client.gui.components.Tooltip.create(getTooltipComponent(key, null)))
             .build();
            return new Element(name, getTooltipComponent(key, null), open, false);
        }
        if ("responseMultipliers".equals(key) && context.keylist().contains("particleWind")) {
            Component name = getTranslationComponent(key);
            Button open = Button.builder(
                Component.translatable("livingwind.config.responseMultipliers.edit"),
                button -> minecraft.setScreen(new ParticleResponseMultiplierListScreen(
                    ConfigurationScreen.ConfigurationSectionScreen.Context.list(context, this),
                    key,
                    Component.translatable("livingwind.config.responseMultipliers.title"),
                    spec,
                    (ModConfigSpec.ConfigValue<List<String>>) (ModConfigSpec.ConfigValue<?>) list
                ))
            ).tooltip(net.minecraft.client.gui.components.Tooltip.create(getTooltipComponent(key, null)))
             .build();
            return new Element(name, getTooltipComponent(key, null), open, false);
        }
        return super.createList(key, spec, list);
    }
}
