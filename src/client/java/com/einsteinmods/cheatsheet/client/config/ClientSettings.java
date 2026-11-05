package com.einsteinmods.cheatsheet.client.config;

import com.einsteinmods.cheatsheet.config.JsonConfigFile;
import com.einsteinmods.cheatsheet.core.StartTab;
import java.util.List;

/**
 * The player's saved preferences. The file is read once at startup and written only when a value
 * changes. Client thread only.
 */
public final class ClientSettings {
  private final JsonConfigFile<ClientSettingsFile> file;
  private final List<String> tabIds;
  private ClientPreferences preferences;

  /**
   * @param tabIds ids of the tabs that exist, in order; a saved start tab that is not one of them
   *     falls back to the last opened tab
   */
  public ClientSettings(JsonConfigFile<ClientSettingsFile> file, List<String> tabIds) {
    this.file = file;
    this.tabIds = List.copyOf(tabIds);
    ClientPreferences loaded = file.load().toPreferences();
    this.preferences =
        new ClientPreferences(
            loaded.pauseButton(),
            StartTab.sanitize(loaded.startTab(), this.tabIds),
            loaded.lastOpenedTab(),
            loaded.sheetScale(),
            loaded.confirmCommandDelete());
  }

  public ClientPreferences preferences() {
    return preferences;
  }

  public List<String> tabIds() {
    return tabIds;
  }

  public void update(ClientPreferences newPreferences) {
    if (!newPreferences.equals(preferences)) {
      preferences = newPreferences;
      file.save(ClientSettingsFile.from(preferences));
    }
  }

  /** Index of the tab the cheat sheet opens on. */
  public int startTabIndex() {
    return startTabIndex(tabIds);
  }

  /**
   * Index among {@code shownTabIds} (the tabs shown now, in order) of the tab the cheat sheet opens
   * on: a hidden start or last tab falls through like an unknown one.
   */
  public int startTabIndex(List<String> shownTabIds) {
    return StartTab.initialIndex(preferences.startTab(), preferences.lastOpenedTab(), shownTabIds);
  }

  /** Remembers the tab open when the cheat sheet closes; writes the file only if it changed. */
  public void rememberLastOpenedTab(String tabId) {
    update(preferences.withLastOpenedTab(tabId));
  }
}
