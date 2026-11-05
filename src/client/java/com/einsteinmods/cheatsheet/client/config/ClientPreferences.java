package com.einsteinmods.cheatsheet.client.config;

import com.einsteinmods.cheatsheet.core.SheetScale;
import com.einsteinmods.cheatsheet.core.StartTab;

/**
 * Every player setting, saved in {@code config/cheatsheet.json}.
 *
 * @param pauseButton the book button next to "Back to Game" in the pause menu
 * @param startTab tab the cheat sheet opens on: {@link StartTab#LAST_OPENED} or a tab id
 * @param lastOpenedTab id of the tab open when the cheat sheet was last closed ("" before the first
 *     time); not shown in the settings screen
 * @param sheetScale how big the sheet is drawn: auto or window pixels per sheet pixel
 * @param confirmCommandDelete whether deleting a saved command asks first
 */
public record ClientPreferences(
    boolean pauseButton,
    String startTab,
    String lastOpenedTab,
    SheetScale sheetScale,
    boolean confirmCommandDelete) {
  public static final String NO_TAB = "";

  public static final ClientPreferences DEFAULTS =
      new ClientPreferences(true, StartTab.LAST_OPENED, NO_TAB, SheetScale.AUTO, true);

  public ClientPreferences withLastOpenedTab(String tabId) {
    return new ClientPreferences(pauseButton, startTab, tabId, sheetScale, confirmCommandDelete);
  }

  public ClientPreferences withConfirmCommandDelete(boolean confirm) {
    return new ClientPreferences(pauseButton, startTab, lastOpenedTab, sheetScale, confirm);
  }
}
