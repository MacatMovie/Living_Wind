package com.macatmovie.livingwind.fabric;

import com.macatmovie.livingwind.client.gui.FabricSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Only constructed by Mod Menu when it is installed. Not a hard runtime dependency. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FabricSettingsScreen::new;
    }
}
