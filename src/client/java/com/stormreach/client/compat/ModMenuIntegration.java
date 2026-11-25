package com.stormreach.client.compat;

import com.stormreach.client.config.StormreachConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Adds the "Configure" button for Stormreach in Mod Menu. Only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return StormreachConfigScreen::new;
    }
}
