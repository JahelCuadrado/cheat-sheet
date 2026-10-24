package com.einsteinmods.cheatsheet.client;

import com.einsteinmods.cheatsheet.client.config.ClientSettings;
import com.einsteinmods.cheatsheet.client.config.ClientSettingsFile;
import com.einsteinmods.cheatsheet.client.picture.PictureBakeRenderer;
import com.einsteinmods.cheatsheet.client.picture.ScaledItemIcons;
import com.einsteinmods.cheatsheet.client.tabs.CheatSheetTabs;
import com.einsteinmods.cheatsheet.config.JsonConfigFile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Client entrypoint: loads the settings, registers the key, the pause menu button and the renderer
 * that bakes the sheets' pictures (idle unless a sheet asks for a bake). Costs nothing while the
 * cheat sheet is closed beyond one key counter read per tick and one check per screen opened.
 */
public final class CheatSheetClient implements ClientModInitializer {
  public static final String MOD_ID = "cheatsheet";

  private static @Nullable ClientSettings settings;
  private static @Nullable CheatSheetKeyMappings keyMappings;
  private static @Nullable ScaledItemIcons scaledItemIcons;

  public static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(MOD_ID, path);
  }

  public static ClientSettings settings() {
    ClientSettings loaded = settings;
    if (loaded == null) {
      throw new IllegalStateException("Cheat Sheet is not initialized");
    }
    return loaded;
  }

  public static CheatSheetKeyMappings keyMappings() {
    CheatSheetKeyMappings registered = keyMappings;
    if (registered == null) {
      throw new IllegalStateException("Cheat Sheet is not initialized");
    }
    return registered;
  }

  /**
   * The cheat sheet's icons drawn at other sizes than 1x, created the first time one is drawn:
   * client-wide because the elements that draw them hold no screen, like vanilla's own item atlas.
   * The screen frees its textures when it closes.
   */
  public static ScaledItemIcons scaledItemIcons() {
    ScaledItemIcons icons = scaledItemIcons;
    if (icons == null) {
      icons = new ScaledItemIcons();
      scaledItemIcons = icons;
    }
    return icons;
  }

  /** Opens the cheat sheet; closing it returns to {@code parent}. */
  public static void openCheatSheet(Minecraft minecraft, @Nullable Screen parent) {
    minecraft.gui.setScreen(
        new CheatSheetScreen(parent, settings(), keyMappings(), CheatSheetTabs.ALL));
  }

  @Override
  public void onInitializeClient() {
    ClientSettings loaded =
        new ClientSettings(
            new JsonConfigFile<>(
                FabricLoader.getInstance().getConfigDir().resolve(ClientSettingsFile.FILE_NAME),
                ClientSettingsFile.class,
                ClientSettingsFile.DEFAULTS),
            CheatSheetTabs.ids());
    settings = loaded;
    CheatSheetKeyMappings mappings = new CheatSheetKeyMappings();
    keyMappings = mappings;
    ClientTickEvents.END_CLIENT_TICK.register(mappings::onClientTick);
    ScreenEvents.AFTER_INIT.register(PauseMenuButton::addTo);
    PictureInPictureRendererRegistry.register(context -> new PictureBakeRenderer());
  }
}
