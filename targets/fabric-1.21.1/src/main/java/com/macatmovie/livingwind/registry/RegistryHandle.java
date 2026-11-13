package com.macatmovie.livingwind.registry;

import java.util.Objects;
import java.util.function.Supplier;

/** Fabric registry reference exposing the same .get() shape as DeferredHolder. */
public final class RegistryHandle<T> implements Supplier<T> {
    private T value;
    public void set(T value) { this.value = Objects.requireNonNull(value); }
    @Override public T get() { return Objects.requireNonNull(value, "Living Wind registry object used before initialization"); }
}
