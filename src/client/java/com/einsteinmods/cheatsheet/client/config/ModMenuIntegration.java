package com.einsteinmods.cheatsheet.client.config;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu "Configure" button; only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent ->
        new CheatSheetSettingsScreen(
            parent, CheatSheetClient.settings(), CheatSheetClient.keyMappings());
  }
}
