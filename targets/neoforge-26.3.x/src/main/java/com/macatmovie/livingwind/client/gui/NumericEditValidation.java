package com.macatmovie.livingwind.client.gui;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.gui.components.EditBox;

/** Reject invalid edits on vanilla EditBox, where NeoForge's setFilter is unavailable. */
final class NumericEditValidation {
    private NumericEditValidation() {}

    static void install(EditBox box, Predicate<String> accepts, Consumer<String> changed) {
        box.setResponder(new Consumer<String>() {
            private String last = box.getValue();
            private boolean restoring;
            @Override public void accept(String value) {
                if (restoring) return;
                if (accepts.test(value)) {
                    last = value;
                    changed.accept(value);
                } else {
                    restoring = true;
                    try { box.setValue(last); }
                    finally { restoring = false; }
                }
            }
        });
    }
}
