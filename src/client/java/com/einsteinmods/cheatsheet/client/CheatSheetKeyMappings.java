package com.einsteinmods.cheatsheet.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Mod key mappings, unbound by default so they never clash with vanilla or other mods. */
public final class CheatSheetKeyMappings {
  private static final KeyMapping.Category CATEGORY =
      KeyMapping.Category.register(CheatSheetClient.id("main"));

  private final KeyMapping openCheatSheet = register("key.cheatsheet.open");

  /** Mappings shown in the settings screen, in display order. */
  public List<KeyMapping> configurable() {
    return List.of(openCheatSheet);
  }

  /**
   * Per client tick: one click counter read while the key is not pressed. Opens the cheat sheet
   * only while playing (no screen open), so a burst of presses never stacks screens.
   */
  public void onClientTick(Minecraft minecraft) {
    while (openCheatSheet.consumeClick()) {
      if (minecraft.gui.screen() == null && minecraft.player != null) {
        CheatSheetClient.openCheatSheet(minecraft, null);
      }
    }
  }

  private static KeyMapping register(String name) {
    return KeyMappingHelper.registerKeyMapping(
        new KeyMapping(name, InputConstants.UNKNOWN.getValue(), CATEGORY));
  }
}
