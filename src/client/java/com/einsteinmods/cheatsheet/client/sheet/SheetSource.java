package com.einsteinmods.cheatsheet.client.sheet;

import com.einsteinmods.cheatsheet.core.ZoomSize;
import net.minecraft.world.item.ItemStack;

/**
 * One tab of the cheat sheet. To add a tab: write one implementation, add one line to {@code
 * CheatSheetTabs}, and add its {@code cheatsheet.tab.<id>} lang keys to every lang file.
 */
public interface SheetSource {
  /**
   * Stable lower-case id: names the tab ({@code cheatsheet.tab.<id>} lang key, its keys start with
   * that prefix) and is saved in the settings file as the start or last opened tab.
   */
  String id();

  /** Item shown on the tab. */
  ItemStack icon();

  /**
   * Builds the sheet. Called at most once per cheat sheet screen, the first time the tab is shown;
   * do every lookup here (registries, translations, player state), never while drawing.
   */
  Sheet build(SheetContext context);

  /**
   * Whether a card lights up under the mouse and zooms when clicked; false for a tab whose content
   * is already big enough to read (the ores chart).
   */
  default boolean zoomsCards() {
    return true;
  }

  /**
   * How big this tab's cards get when zoomed, unless a card says otherwise ({@link
   * ZoomCard#size()}): small for cards that give little new information bigger.
   */
  default ZoomSize zoomSize() {
    return ZoomSize.MEDIUM;
  }
}
