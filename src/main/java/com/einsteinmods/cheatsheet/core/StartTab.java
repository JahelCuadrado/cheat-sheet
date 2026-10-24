package com.einsteinmods.cheatsheet.core;

import java.util.List;

/**
 * Which tab the cheat sheet opens on: the one open when it was last closed ({@link #LAST_OPENED})
 * or a fixed tab, named by its id. Ids come from the tab list, so they are plain strings here.
 */
public final class StartTab {
  /** Setting value meaning "the tab open when the cheat sheet was last closed". */
  public static final String LAST_OPENED = "last_opened";

  private StartTab() {}

  /**
   * Index of the tab to open: the fixed tab, else the last opened one, else the first. Unknown ids
   * (a tab removed by an update, a hand-edited file) fall through to the next choice.
   */
  public static int initialIndex(String startTab, String lastOpenedTab, List<String> tabIds) {
    if (!LAST_OPENED.equals(startTab)) {
      int fixed = tabIds.indexOf(startTab);
      if (fixed >= 0) {
        return fixed;
      }
    }
    return Math.max(0, tabIds.indexOf(lastOpenedTab));
  }

  /**
   * {@code startTab} when it is {@link #LAST_OPENED} or a known tab id, else {@link #LAST_OPENED}.
   */
  public static String sanitize(String startTab, List<String> tabIds) {
    return tabIds.contains(startTab) ? startTab : LAST_OPENED;
  }
}
