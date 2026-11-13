package com.macatmovie.livingwind.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Fabric-side persistent config adapter for the v1.4.0 common/client settings.
 * This is intentionally in our namespace, NOT a fake NeoForge class. The main
 * config declarations are compiled from the original version with only an import
 * and a type-name change. We keep original keys, defaults and validators.
 */
public final class FabricConfigSpec {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<String, ConfigValue<?>> values = new LinkedHashMap<>();
    private Path path;

    private FabricConfigSpec() {}

    public synchronized void load(String fileName) {
        path = FabricLoader.getInstance().getConfigDir().resolve(fileName);
        if (!Files.isRegularFile(path)) { save(); return; }
        try {
            JsonObject object = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            for (ConfigValue<?> value : values.values()) {
                JsonElement element = object.get(value.path);
                if (element != null) value.loadJson(element);
            }
        } catch (Exception problem) {
            System.err.println("[Living Wind] Invalid config " + path + ": " + problem);
        }
    }

    public synchronized void save() {
        if (path == null) return;
        JsonObject object = new JsonObject();
        values.forEach((name, setting) -> object.add(name, GSON.toJsonTree(setting.get())));
        try {
            Files.createDirectories(path.getParent());
            // A truncated config file can silently reset every setting on next boot.
            // Write the new complete state to a sibling, then replace the old file.
            Path temporary = path.resolveSibling(path.getFileName().toString() + ".tmp");
            Files.writeString(temporary, GSON.toJson(object), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException problem) {
            System.err.println("[Living Wind] Could not save config " + path + ": " + problem);
        }
    }

    public Map<String, ConfigValue<?>> entries() { return Collections.unmodifiableMap(values); }

    public static class ConfigValue<T> {
        private final FabricConfigSpec owner;
        public final String path;
        public final String translationKey;
        public final String description;
        private final T defaultValue;
        private final Predicate<Object> validator;
        private T value;

        ConfigValue(FabricConfigSpec owner, String path, String translationKey,
                    String description, T initial, Predicate<Object> validator) {
            this.owner = owner;
            this.path = path;
            this.translationKey = translationKey;
            this.description = description;
            this.defaultValue = initial;
            this.value = initial;
            this.validator = validator;
        }
        public T get() { return value; }
        public T getDefault() { return defaultValue; }
        public boolean accepts(Object candidate) { return validator.test(candidate); }
        /** Compatibility with the per-setting save() convention used by the Forge configs. */
        public void save() { owner.save(); }
        public void set(T replacement) {
            if (setWithoutSaving(replacement)) owner.save();
        }
        /** Apply GUI slider changes in memory; persist once on screen close/navigation. */
        public boolean setWithoutSaving(T replacement) {
            if (!validator.test(replacement) || value.equals(replacement)) return false;
            if (replacement instanceof List<?> list) {
                // Prevent a caller from silently editing a previously validated list.
                @SuppressWarnings("unchecked") T frozen = (T) List.copyOf(list);
                value = frozen;
            } else {
                value = replacement;
            }
            return true;
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void loadJson(JsonElement source) {
            try {
                Object converted;
                if (defaultValue instanceof Boolean) converted = source.getAsBoolean();
                else if (defaultValue instanceof Integer) converted = source.getAsInt();
                else if (defaultValue instanceof Double) converted = source.getAsDouble();
                else if (defaultValue instanceof Enum<?> e) converted = Enum.valueOf(e.getDeclaringClass(), source.getAsString());
                else if (defaultValue instanceof List<?>) {
                    ArrayList<String> list = new ArrayList<>();
                    for (JsonElement entry : source.getAsJsonArray()) list.add(entry.getAsString());
                    converted = List.copyOf(list);
                } else converted = source.getAsString();
                if (validator.test(converted)) value = (T) converted;
            } catch (Exception ignored) { /* retain safe default */ }
        }
    }
    public static final class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(FabricConfigSpec spec,String key,String trans,String desc,boolean def) {
            super(spec,key,trans,desc,def,v -> v instanceof Boolean);
        }
    }
    public static final class IntValue extends ConfigValue<Integer> {
        private final int minimum, maximum;
        public int minimum() { return minimum; }
        public int maximum() { return maximum; }
        IntValue(FabricConfigSpec spec,String key,String trans,String desc,int def,int min,int max) {
            super(spec,key,trans,desc,def,v -> v instanceof Integer i && i >= min && i <= max);
            this.minimum = min; this.maximum = max;
        }
    }
    public static final class DoubleValue extends ConfigValue<Double> {
        private final double minimum, maximum;
        public double minimum() { return minimum; }
        public double maximum() { return maximum; }
        DoubleValue(FabricConfigSpec spec,String key,String trans,String desc,double def,double min,double max) {
            super(spec,key,trans,desc,def,v -> v instanceof Double d && Double.isFinite(d) && d >= min && d <= max);
            this.minimum = min; this.maximum = max;
        }
    }
    public static final class EnumValue<E extends Enum<E>> extends ConfigValue<E> {
        EnumValue(FabricConfigSpec spec,String key,String trans,String desc,E def) {
            super(spec,key,trans,desc,def,v -> def.getDeclaringClass().isInstance(v));
        }
    }
    public static final class Builder {
        private final FabricConfigSpec spec = new FabricConfigSpec();
        private final Deque<String> sections = new ArrayDeque<>();
        private String translationKey = "";
        private String description = "";
        public Builder comment(String... comments) { description = String.join(" ", comments); return this; }
        public Builder translation(String key) { translationKey = key; return this; }
        public Builder push(String group) { sections.addLast(group); return this; }
        public Builder pop() { if (!sections.isEmpty()) sections.removeLast(); return this; }
        private String name(String key) {
            return sections.isEmpty() ? key : String.join(".", sections) + "." + key;
        }
        private <T extends ConfigValue<?>> T add(T value) {
            spec.values.put(value.path, value); translationKey = ""; description = "";
            return value;
        }
        public BooleanValue define(String key, boolean initial) {
            return add(new BooleanValue(spec, name(key), translationKey, description, initial));
        }
        public IntValue defineInRange(String key,int initial,int min,int max) {
            return add(new IntValue(spec, name(key), translationKey, description, initial,min,max));
        }
        public DoubleValue defineInRange(String key,double initial,double min,double max) {
            return add(new DoubleValue(spec, name(key), translationKey, description, initial,min,max));
        }
        public <E extends Enum<E>> EnumValue<E> defineEnum(String key,E initial) {
            return add(new EnumValue<>(spec,name(key),translationKey,description,initial));
        }
        /** Legacy Forge 1.20.1 style: no prototype supplier. */
        public <T> ConfigValue<List<? extends T>> defineListAllowEmpty(String key, List<? extends T> initial,
                Predicate<Object> validator) {
            return defineListAllowEmpty(key, initial, () -> null, validator);
        }
        public <T> ConfigValue<List<? extends T>> defineListAllowEmpty(String key, List<? extends T> initial,
                Supplier<? extends T> prototype, Predicate<Object> validator) {
            // The unused prototype is a NightConfig display hint, not a real item.
            return add(new ConfigValue<>(spec,name(key),translationKey,description,List.copyOf(initial),
                v -> v instanceof List<?> list && list.stream().allMatch(validator)));
        }
        public FabricConfigSpec build() { return spec; }
    }
}
