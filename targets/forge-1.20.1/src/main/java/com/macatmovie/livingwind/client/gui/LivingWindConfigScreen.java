package com.macatmovie.livingwind.client.gui;

import com.macatmovie.livingwind.client.ParticleWindFilter;
import com.macatmovie.livingwind.config.ClientWindConfig;
import com.macatmovie.livingwind.config.WindConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Forge 1.20.1 in-game configuration screen with no external config-screen dependency. */
public final class LivingWindConfigScreen extends Screen {
    private static final int TOP = 32, BOTTOM = 36, ROW_H = 26;
    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private double scroll;
    private int contentHeight;

    public LivingWindConfigScreen(Screen parent) {
        super(Component.literal("Living Wind Configuration"));
        this.parent = parent;
    }

    @Override protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets(); rows.clear();
        header("Wind Simulation");
        number("Minimum Base Strength", WindConfig.MIN_BASE_STRENGTH.get(), v -> WindConfig.MIN_BASE_STRENGTH.set(v), 0, 1.5);
        number("Maximum Base Strength", WindConfig.MAX_BASE_STRENGTH.get(), v -> WindConfig.MAX_BASE_STRENGTH.set(v), 0, 1.5);
        number("Maximum Simulated Strength", WindConfig.MAX_EFFECTIVE_STRENGTH.get(), v -> WindConfig.MAX_EFFECTIVE_STRENGTH.set(v), .25, 2.5);
        number("Global Strength Multiplier", WindConfig.GLOBAL_STRENGTH_MULTIPLIER.get(), v -> WindConfig.GLOBAL_STRENGTH_MULTIPLIER.set(v), .25, 2.5);

        header("Weather & Events");
        number("Rain Strength Bonus", WindConfig.RAIN_STRENGTH_BONUS.get(), v -> WindConfig.RAIN_STRENGTH_BONUS.set(v), 0, 1.5);
        number("Thunder Strength Bonus", WindConfig.THUNDER_STRENGTH_BONUS.get(), v -> WindConfig.THUNDER_STRENGTH_BONUS.set(v), 0, 1.5);
        number("Pre-Storm Front Chance", WindConfig.PRE_STORM_FRONT_CHANCE.get(), v -> WindConfig.PRE_STORM_FRONT_CHANCE.set(v), 0, 1);
        integer("Pre-Storm Lead Ticks", WindConfig.PRE_STORM_LEAD_TICKS.get(), v -> WindConfig.PRE_STORM_LEAD_TICKS.set(v), 200, 12000);
        number("Calm Day Chance", WindConfig.CALM_DAY_CHANCE.get(), v -> WindConfig.CALM_DAY_CHANCE.set(v), 0, 1);
        number("Breezy Day Chance", WindConfig.BREEZY_DAY_CHANCE.get(), v -> WindConfig.BREEZY_DAY_CHANCE.set(v), 0, 1);
        number("Windy Period Chance", WindConfig.WINDY_PERIOD_CHANCE.get(), v -> WindConfig.WINDY_PERIOD_CHANCE.set(v), 0, 1);
        integer("Windy Period Min Ticks", WindConfig.WINDY_PERIOD_MIN_TICKS.get(), v -> WindConfig.WINDY_PERIOD_MIN_TICKS.set(v), 200, 24000);
        integer("Windy Period Max Ticks", WindConfig.WINDY_PERIOD_MAX_TICKS.get(), v -> WindConfig.WINDY_PERIOD_MAX_TICKS.set(v), 200, 24000);
        bool("Gust Clusters", WindConfig.GUST_CLUSTERS_ENABLED.get(), WindConfig.GUST_CLUSTERS_ENABLED::set);
        number("Gust Cluster Chance Multiplier", WindConfig.GUST_CLUSTER_CHANCE_MULTIPLIER.get(), WindConfig.GUST_CLUSTER_CHANCE_MULTIPLIER::set, 0, 3);

        header("Create Windmills (Optional)");
        bool("Wind-Driven Create Windmills", WindConfig.CREATE_WINDMILLS_ENABLED.get(), WindConfig.CREATE_WINDMILLS_ENABLED::set);
        number("Create Minimum Speed Multiplier", WindConfig.CREATE_WINDMILL_MIN_MULTIPLIER.get(), WindConfig.CREATE_WINDMILL_MIN_MULTIPLIER::set, 0.01, 2.0);
        number("Create Maximum Speed Multiplier", WindConfig.CREATE_WINDMILL_MAX_MULTIPLIER.get(), WindConfig.CREATE_WINDMILL_MAX_MULTIPLIER::set, 0.50, 4.0);
        number("Create RPM Step", WindConfig.CREATE_WINDMILL_RPM_STEP.get(), WindConfig.CREATE_WINDMILL_RPM_STEP::set, 0.25, 4.0);
        number("Create Min RPM Difference", WindConfig.CREATE_WINDMILL_MIN_RPM_CHANGE.get(), WindConfig.CREATE_WINDMILL_MIN_RPM_CHANGE::set, 0.25, 8.0);
        number("Create Max RPM Change per Update", WindConfig.CREATE_WINDMILL_MAX_CHANGE_PER_UPDATE.get(), WindConfig.CREATE_WINDMILL_MAX_CHANGE_PER_UPDATE::set, 0.50, 32.0);
        integer("Create Refresh Interval (Ticks)", WindConfig.CREATE_WINDMILL_UPDATE_TICKS.get(), WindConfig.CREATE_WINDMILL_UPDATE_TICKS::set, 20, 200);

        header("Dimension Wind");
        enumNetherWindMode();
        enumEndWindMode();

        header("Altitude & Networking");
        bool("Altitude Scaling", WindConfig.ALTITUDE_SCALING_ENABLED.get(), WindConfig.ALTITUDE_SCALING_ENABLED::set);
        integer("Altitude Minimum Y", WindConfig.ALTITUDE_MIN_Y.get(), WindConfig.ALTITUDE_MIN_Y::set, -64, 320);
        integer("Altitude Maximum Y", WindConfig.ALTITUDE_MAX_Y.get(), WindConfig.ALTITUDE_MAX_Y::set, -64, 512);
        number("Altitude Minimum Multiplier", WindConfig.ALTITUDE_MIN_MULTIPLIER.get(), WindConfig.ALTITUDE_MIN_MULTIPLIER::set, 0, 3);
        number("Altitude Maximum Multiplier", WindConfig.ALTITUDE_MAX_MULTIPLIER.get(), WindConfig.ALTITUDE_MAX_MULTIPLIER::set, 0, 3);
        integer("Sync Interval Ticks", WindConfig.SYNC_INTERVAL_TICKS.get(), WindConfig.SYNC_INTERVAL_TICKS::set, 10, 200);

        header("Particle Wind");
        bool("Affect Existing Particles", ClientWindConfig.AFFECT_EXISTING_PARTICLES.get(), ClientWindConfig.AFFECT_EXISTING_PARTICLES::set);
        number("Particle Wind Force", ClientWindConfig.PARTICLE_WIND_FORCE.get(), ClientWindConfig.PARTICLE_WIND_FORCE::set, 0, .25);
        enumFilterMode();
        nav("Particle Blacklist / Whitelist", () -> minecraft.setScreen(new ParticleFilterListScreen(this)));
        nav("Particle Response Multipliers", () -> minecraft.setScreen(new ParticleResponseMultiplierListScreen(this)));

        header("Ambient Particles");
        bool("Enabled", ClientWindConfig.AMBIENT_PARTICLES.get(), ClientWindConfig.AMBIENT_PARTICLES::set);
        integer("Samples Per Tick", ClientWindConfig.AMBIENT_SAMPLES_PER_TICK.get(), ClientWindConfig.AMBIENT_SAMPLES_PER_TICK::set, 0, 32);
        integer("Sample Radius", ClientWindConfig.AMBIENT_RADIUS.get(), ClientWindConfig.AMBIENT_RADIUS::set, 4, 48);
        number("Minimum Wind Strength", ClientWindConfig.AMBIENT_MIN_WIND.get(), ClientWindConfig.AMBIENT_MIN_WIND::set, 0, 1.5);
        number("Density Multiplier", ClientWindConfig.AMBIENT_DENSITY_MULTIPLIER.get(), ClientWindConfig.AMBIENT_DENSITY_MULTIPLIER::set, 0, 4);
        number("Wind Response", ClientWindConfig.AMBIENT_WIND_RESPONSE_SCALE.get(), ClientWindConfig.AMBIENT_WIND_RESPONSE_SCALE::set, 0, 4);
        number("Upward Lift", ClientWindConfig.AMBIENT_LIFT_MULTIPLIER.get(), ClientWindConfig.AMBIENT_LIFT_MULTIPLIER::set, 0, 4);
        number("Turbulence Sway", ClientWindConfig.AMBIENT_SWAY_MULTIPLIER.get(), ClientWindConfig.AMBIENT_SWAY_MULTIPLIER::set, 0, 4);
        integer("Minimum Lifetime Ticks", ClientWindConfig.AMBIENT_LIFETIME_MIN_TICKS.get(), ClientWindConfig.AMBIENT_LIFETIME_MIN_TICKS::set, 20, 600);
        integer("Maximum Lifetime Ticks", ClientWindConfig.AMBIENT_LIFETIME_MAX_TICKS.get(), ClientWindConfig.AMBIENT_LIFETIME_MAX_TICKS::set, 20, 600);

        header("Windblown Leaves");
        bool("Enable Windblown Leaves", ClientWindConfig.LEAF_PARTICLES_ENABLED.get(), ClientWindConfig.LEAF_PARTICLES_ENABLED::set);
        bool("Texture-Derived Colors", ClientWindConfig.LEAF_TEXTURE_COLORS.get(), ClientWindConfig.LEAF_TEXTURE_COLORS::set);
        number("Leaf Density", ClientWindConfig.LEAF_PARTICLE_DENSITY.get(), ClientWindConfig.LEAF_PARTICLE_DENSITY::set, 0, 4);
        integer("Leaf Sampling Radius", ClientWindConfig.LEAF_PARTICLE_RADIUS.get(), ClientWindConfig.LEAF_PARTICLE_RADIUS::set, 8, 48);
        number("Leaf Wind Response", ClientWindConfig.LEAF_PARTICLE_WIND_RESPONSE.get(), ClientWindConfig.LEAF_PARTICLE_WIND_RESPONSE::set, 0, 3);

        header("Wind Sounds");
        bool("Wind Audio Enabled", ClientWindConfig.WIND_AUDIO_ENABLED.get(), ClientWindConfig.WIND_AUDIO_ENABLED::set);
        slider("Wind Audio Volume", ClientWindConfig.WIND_AUDIO_VOLUME.get(), ClientWindConfig.WIND_AUDIO_VOLUME::set, 0, 3);
        bool("Gust Accents", ClientWindConfig.WIND_AUDIO_GUSTS.get(), ClientWindConfig.WIND_AUDIO_GUSTS::set);
        bool("Environmental Accents", ClientWindConfig.WIND_AUDIO_ENVIRONMENTAL.get(), ClientWindConfig.WIND_AUDIO_ENVIRONMENTAL::set);
        enumAudioMode();
        enumDynamicSurroundingsAudioMode();

        header("Compatibility");
        bool("Pretty Rain Wind", ClientWindConfig.PRETTY_RAIN_COMPAT.get(), ClientWindConfig.PRETTY_RAIN_COMPAT::set);
        number("Pretty Rain Wind Strength", ClientWindConfig.PRETTY_RAIN_WIND_MULTIPLIER.get(), ClientWindConfig.PRETTY_RAIN_WIND_MULTIPLIER::set, 0, 2);
        bool("Particle Rain v4 Wind", ClientWindConfig.PARTICLE_RAIN_COMPAT.get(), ClientWindConfig.PARTICLE_RAIN_COMPAT::set);
        number("Particle Rain v4 Strength", ClientWindConfig.PARTICLE_RAIN_WIND_MULTIPLIER.get(), ClientWindConfig.PARTICLE_RAIN_WIND_MULTIPLIER::set, 0, 2);
        enumFallingLeavesSource();
        bool("Particular Leaf Density Follows Wind", ClientWindConfig.PARTICULAR_FALLING_LEAVES_COMPAT.get(), ClientWindConfig.PARTICULAR_FALLING_LEAVES_COMPAT::set);
        number("Particular Min Leaf Density", ClientWindConfig.PARTICULAR_LEAF_MIN_DENSITY.get(), ClientWindConfig.PARTICULAR_LEAF_MIN_DENSITY::set, 0, 4);
        number("Particular Max Leaf Density", ClientWindConfig.PARTICULAR_LEAF_MAX_DENSITY.get(), ClientWindConfig.PARTICULAR_LEAF_MAX_DENSITY::set, 0, 8);
        number("Particular Full-Wind Strength", ClientWindConfig.PARTICULAR_LEAF_FULL_WIND_STRENGTH.get(), ClientWindConfig.PARTICULAR_LEAF_FULL_WIND_STRENGTH::set, .1, 3);

        header("Fancy World Animations (Optional)");
        bool("Supplementaries Wind Vane", ClientWindConfig.SUPPLEMENTARIES_WIND_VANE_COMPAT.get(), ClientWindConfig.SUPPLEMENTARIES_WIND_VANE_COMPAT::set);
        bool("Swinging Lanterns Wind", ClientWindConfig.SWINGING_LANTERNS_COMPAT.get(), ClientWindConfig.SWINGING_LANTERNS_COMPAT::set);
        number("Swinging Lanterns Wind Strength", ClientWindConfig.SWINGING_LANTERNS_WIND_MULTIPLIER.get(), ClientWindConfig.SWINGING_LANTERNS_WIND_MULTIPLIER::set, 0, 3);
        bool("FWA Wind-Driven Swings", ClientWindConfig.FWA_WIND_COMPAT.get(), ClientWindConfig.FWA_WIND_COMPAT::set);
        slider("FWA Wind Impact", ClientWindConfig.FWA_WIND_IMPACT.get(), ClientWindConfig.FWA_WIND_IMPACT::set, 0, 12);
        number("FWA Chain Wind", ClientWindConfig.FWA_CHAIN_INTENSITY.get(), ClientWindConfig.FWA_CHAIN_INTENSITY::set, 0, 3);
        number("FWA Lantern Wind", ClientWindConfig.FWA_LANTERN_INTENSITY.get(), ClientWindConfig.FWA_LANTERN_INTENSITY::set, 0, 3);
        number("FWA Bell Wind", ClientWindConfig.FWA_BELL_INTENSITY.get(), ClientWindConfig.FWA_BELL_INTENSITY::set, 0, 3);

        header("Exposure & Debug");
        bool("Reduce Wind Indoors", ClientWindConfig.REDUCE_WIND_INDOORS.get(), ClientWindConfig.REDUCE_WIND_INDOORS::set);
        number("Shelter Effectiveness", ClientWindConfig.SHELTER_EFFECTIVENESS.get(), ClientWindConfig.SHELTER_EFFECTIVENESS::set, 0, 1);
        number("Indoor Sound Attenuation", ClientWindConfig.INDOOR_AUDIO_ATTENUATION.get(), ClientWindConfig.INDOOR_AUDIO_ATTENUATION::set, 0, 1);
        bool("Low-Cost Shelter Checks", ClientWindConfig.SHELTER_LOW_COST.get(), ClientWindConfig.SHELTER_LOW_COST::set);
        enumDebugHudMode();

        contentHeight = rows.size() * ROW_H;
        scroll = clamp(scroll);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> saveAndClose()).bounds(width / 2 - 75, height - 28, 150, 20).build());
        updatePositions();
    }

    private void header(String s) { rows.add(Row.header(Component.literal(s).copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))); }
    private void bool(String label, boolean initial, Consumer<Boolean> setter) {
        final boolean[] v={initial};
        Button b=Button.builder(onOff(v[0]), x->{v[0]=!v[0]; setter.accept(v[0]); x.setMessage(onOff(v[0]));}).bounds(0,0,180,20).build();
        rows.add(Row.value(Component.literal(label), b, null)); addRenderableWidget(b);
    }
    private void number(String label, double initial, Consumer<Double> setter, double min, double max) {
        EditBox box=new EditBox(font,0,0,180,20,Component.literal(label)); box.setValue(Double.toString(initial));
        box.setFilter(LivingWindConfigScreen::partialDecimal);
        rows.add(Row.value(Component.literal(label), box, ()->{try{setter.accept(Math.max(min,Math.min(max,Double.parseDouble(box.getValue()))));}catch(Exception ignored){}})); addRenderableWidget(box);
    }
    private void integer(String label, int initial, Consumer<Integer> setter, int min, int max) {
        EditBox box=new EditBox(font,0,0,180,20,Component.literal(label)); box.setValue(Integer.toString(initial));
        box.setFilter(v->v.isEmpty()||v.equals("-")||v.matches("-?\\d+"));
        rows.add(Row.value(Component.literal(label), box, ()->{try{setter.accept(Math.max(min,Math.min(max,Integer.parseInt(box.getValue()))));}catch(Exception ignored){}})); addRenderableWidget(box);
    }
    private void slider(String label, double initial, Consumer<Double> setter, double min, double max) {
        double normalized = max <= min ? 0.0 : Math.max(0.0, Math.min(1.0, (initial - min) / (max - min)));
        // A named slider class avoids javac's duplicate synthetic-local error on
        // Forge 1.20.1 while preserving the same UI and update behavior.
        AbstractSliderButton slider = new ConfigSlider(initial, normalized, setter, min, max);
        rows.add(Row.value(Component.literal(label), slider, null));
        addRenderableWidget(slider);
    }

    private void enumDebugHudMode() {
        Button button = Button.builder(Component.literal(ClientWindConfig.debugHudMode().name()), b -> {
            ClientWindConfig.DebugHudMode[] modes = ClientWindConfig.DebugHudMode.values();
            ClientWindConfig.DebugHudMode current = ClientWindConfig.debugHudMode();
            ClientWindConfig.DebugHudMode next = modes[(current.ordinal() + 1) % modes.length];
            ClientWindConfig.setDebugHudMode(next);
            b.setMessage(Component.literal(next.name()));
        }).bounds(0, 0, 180, 20)
          .tooltip(Tooltip.create(Component.literal("OFF: hidden. SIMPLE: wind strength, direction and gusts. ALL: full diagnostic overlay.")))
          .build();
        rows.add(Row.value(Component.literal("Debug HUD"), button, null));
        addRenderableWidget(button);
    }

    private void enumFallingLeavesSource() {
        Button b = Button.builder(Component.literal(fallingLeavesLabel(ClientWindConfig.FALLING_LEAVES_SOURCE.get())), x -> {
            var values = ClientWindConfig.FallingLeavesSource.values();
            int next = (ClientWindConfig.FALLING_LEAVES_SOURCE.get().ordinal() + 1) % values.length;
            ClientWindConfig.FALLING_LEAVES_SOURCE.set(values[next]);
            x.setMessage(Component.literal(fallingLeavesLabel(values[next])));
        }).bounds(0, 0, 180, 20)
          .tooltip(Tooltip.create(Component.literal(
              "Living Wind Only (default): disables Particular leaves. Particular Only: disables Living Wind tree leaves. Both: both emit leaves. Flowers and grass stay enabled. If Particular is missing, Living Wind leaves remain active.")))
          .build();
        rows.add(Row.value(Component.literal("Falling Leaves Source"), b, null));
        addRenderableWidget(b);
    }

    private static String fallingLeavesLabel(ClientWindConfig.FallingLeavesSource source) {
        return switch (source) {
            case LIVING_WIND -> "Living Wind Only";
            case PARTICULAR -> "Particular Only";
            case BOTH -> "Both";
        };
    }

    private void enumAudioMode() {
        Button b=Button.builder(Component.literal(ClientWindConfig.WIND_AUDIO_MODE.get().name()), x->{
            var values=ClientWindConfig.WindAudioMode.values();
            int next=(ClientWindConfig.WIND_AUDIO_MODE.get().ordinal()+1)%values.length;
            ClientWindConfig.WIND_AUDIO_MODE.set(values[next]); x.setMessage(Component.literal(values[next].name()));
        }).bounds(0,0,180,20)
          .tooltip(Tooltip.create(Component.literal("Choose which mod supplies wind audio when Ambient Sounds is installed. LIVING_WIND - Plays only Living Wind's wind sounds. AMBIENT_SOUNDS - Plays only Ambient Sounds's wind sounds. BOTH - Plays both wind sound systems.")))
          .build(); rows.add(Row.value(Component.literal("Ambient Sounds Wind"),b,null)); addRenderableWidget(b);
    }
    private void enumDynamicSurroundingsAudioMode() {
        Button button = Button.builder(
            Component.literal(dynamicSurroundingsAudioLabel(ClientWindConfig.DYNAMIC_SURROUNDINGS_WIND_MODE.get())),
            widget -> {
                var modes = ClientWindConfig.DynamicSurroundingsWindMode.values();
                int next = (ClientWindConfig.DYNAMIC_SURROUNDINGS_WIND_MODE.get().ordinal() + 1) % modes.length;
                ClientWindConfig.DYNAMIC_SURROUNDINGS_WIND_MODE.set(modes[next]);
                widget.setMessage(Component.literal(dynamicSurroundingsAudioLabel(modes[next])));
            }
        ).bounds(0, 0, 180, 20)
          .tooltip(Tooltip.create(Component.literal(
              "Choose Living Wind only (default), Dynamic Surroundings only, or both wind soundtracks. " +
              "Other Dynamic Surroundings audio remains unchanged. Ambient Sounds is configured separately.")))
          .build();
        rows.add(Row.value(Component.literal("Dynamic Surroundings Wind"), button, null));
        addRenderableWidget(button);
    }

    private static String dynamicSurroundingsAudioLabel(ClientWindConfig.DynamicSurroundingsWindMode mode) {
        return switch (mode) {
            case LIVING_WIND -> "Living Wind Only";
            case DYNAMIC_SURROUNDINGS -> "Dynamic Surroundings Only";
            case BOTH -> "Both";
        };
    }

    private void enumNetherWindMode() {
        Button b=Button.builder(Component.literal(WindConfig.NETHER_WIND_MODE.get().name()), x->{
            var values=WindConfig.NetherWindMode.values();
            int next=(WindConfig.NETHER_WIND_MODE.get().ordinal()+1)%values.length;
            WindConfig.NETHER_WIND_MODE.set(values[next]); x.setMessage(Component.literal(values[next].name()));
        }).bounds(0,0,180,20).build(); rows.add(Row.value(Component.literal("Nether Wind Mode"),b,null)); addRenderableWidget(b);
    }
    private void enumEndWindMode() {
        Button b=Button.builder(Component.literal(WindConfig.END_WIND_MODE.get().name()), x->{
            var values=WindConfig.EndWindMode.values();
            int next=(WindConfig.END_WIND_MODE.get().ordinal()+1)%values.length;
            WindConfig.END_WIND_MODE.set(values[next]); x.setMessage(Component.literal(values[next].name()));
        }).bounds(0,0,180,20).build(); rows.add(Row.value(Component.literal("End Wind Mode"),b,null)); addRenderableWidget(b);
    }
    private void enumFilterMode() {
        Button b=Button.builder(Component.literal(ClientWindConfig.PARTICLE_FILTER_MODE.get().name()), x->{
            var next=ClientWindConfig.PARTICLE_FILTER_MODE.get()==ClientWindConfig.FilterMode.BLACKLIST?ClientWindConfig.FilterMode.WHITELIST:ClientWindConfig.FilterMode.BLACKLIST;
            ClientWindConfig.PARTICLE_FILTER_MODE.set(next); x.setMessage(Component.literal(next.name()));
        }).bounds(0,0,180,20).build(); rows.add(Row.value(Component.literal("Filter Mode"),b,null)); addRenderableWidget(b);
    }
    private void nav(String label, Runnable action) {
        Button b=Button.builder(Component.literal("Edit..."), x->action.run()).bounds(0,0,180,20).build(); rows.add(Row.value(Component.literal(label),b,null)); addRenderableWidget(b);
    }

    private void saveAndClose(){ for(Row r:rows) if(r.commit!=null) r.commit.run(); ParticleWindFilter.refresh(); ClientWindConfig.WIND_AUDIO_VOLUME.save(); WindConfig.NETHER_WIND_MODE.save(); minecraft.setScreen(parent); }
    @Override public void onClose(){ minecraft.setScreen(parent); }
    @Override public boolean mouseScrolled(double mx,double my,double delta){ if(my>=TOP&&my<height-BOTTOM&&maxScroll()>0){scroll=clamp(scroll-delta*ROW_H);updatePositions();return true;} return super.mouseScrolled(mx,my,delta); }
    @Override public void render(GuiGraphics g,int mx,int my,float pt){ renderBackground(g); for(int i=0;i<rows.size();i++){Row r=rows.get(i);int y=rowY(i);if(y+ROW_H<=TOP||y>=height-BOTTOM)continue;if(r.header)g.drawCenteredString(font,r.label,width/2,y+8,0xFFFF55);else g.drawString(font,r.label,width/2-rowWidth()/2+8,y+8,0xFFFFFF,false);} super.render(g,mx,my,pt); g.drawCenteredString(font,title,width/2,14,0xFFFFFF); drawBar(g); }
    private void updatePositions(){int rw=rowWidth(), left=width/2-rw/2, cw=Math.min(210,Math.max(130,rw/3)), cx=left+rw-cw-8, bottom=height-BOTTOM;for(int i=0;i<rows.size();i++){Row r=rows.get(i);if(r.widget==null)continue;int y=rowY(i);r.widget.setX(cx);r.widget.setY(y+2);r.widget.setWidth(cw);boolean vis=y>=TOP&&y+ROW_H<=bottom;r.widget.visible=vis;r.widget.active=vis;}}
    private void drawBar(GuiGraphics g){double m=maxScroll();if(m<=0)return;int top=TOP,bottom=height-BOTTOM,vis=bottom-top,thumb=Math.max(20,(int)((double)vis*vis/contentHeight)),travel=vis-thumb,y=top+(int)((scroll/m)*travel),x=width/2+rowWidth()/2+8;g.fill(x,top,x+4,bottom,0x40202020);g.fill(x,y,x+4,y+thumb,0xFFA0A0A0);}
    private int rowY(int i){return TOP+i*ROW_H-(int)Math.round(scroll);} private int rowWidth(){return Math.min(660,Math.max(280,width-50));} private double maxScroll(){return Math.max(0,contentHeight-(height-BOTTOM-TOP));} private double clamp(double v){return Math.max(0,Math.min(maxScroll(),v));}
    private static Component onOff(boolean v){return Component.translatable(v?"options.on":"options.off");}
    private static boolean partialDecimal(String v){return v.isEmpty()||v.equals("-")||v.equals(".")||v.equals("-.")||v.matches("-?(?:\\d+\\.?\\d*|\\.\\d*)");}
    private static final class ConfigSlider extends AbstractSliderButton {
        private final Consumer<Double> setter;
        private final double min;
        private final double max;

        private ConfigSlider(double initial, double normalized, Consumer<Double> setter, double min, double max) {
            super(0, 0, 180, 20, Component.literal(String.format(Locale.ROOT, "%.2f", initial)), normalized);
            this.setter = setter;
            this.min = min;
            this.max = max;
        }

        @Override protected void updateMessage() {
            double actual = min + (max - min) * value;
            setMessage(Component.literal(String.format(Locale.ROOT, "%.2f", actual)));
        }

        @Override protected void applyValue() {
            double actual = min + (max - min) * value;
            actual = Math.round(actual * 100.0) / 100.0;
            setter.accept(actual);
            updateMessage();
        }
    }

    private static final class Row{final Component label;final AbstractWidget widget;final Runnable commit;final boolean header;Row(Component l,AbstractWidget w,Runnable c,boolean h){label=l;widget=w;commit=c;header=h;}static Row header(Component l){return new Row(l,null,null,true);}static Row value(Component l,AbstractWidget w,Runnable c){return new Row(l,w,c,false);}}
}
