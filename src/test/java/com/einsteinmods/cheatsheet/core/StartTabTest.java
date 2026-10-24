package com.einsteinmods.cheatsheet.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StartTabTest {
  private static final List<String> TABS = List.of("ores", "potions", "food");

  @Test
  void fixedTabWinsOverTheLastOpened() {
    assertEquals(1, StartTab.initialIndex("potions", "food", TABS));
  }

  @Test
  void lastOpenedTabWhenAskedOrWhenTheFixedOneIsGone() {
    assertEquals(2, StartTab.initialIndex(StartTab.LAST_OPENED, "food", TABS));
    assertEquals(2, StartTab.initialIndex("removed", "food", TABS));
  }

  @Test
  void firstTabWhenNothingIsKnown() {
    assertEquals(0, StartTab.initialIndex(StartTab.LAST_OPENED, "", TABS));
    assertEquals(0, StartTab.initialIndex("removed", "also_removed", TABS));
  }

  @Test
  void sanitizeKeepsKnownTabsOnly() {
    assertEquals("food", StartTab.sanitize("food", TABS));
    assertEquals(StartTab.LAST_OPENED, StartTab.sanitize("removed", TABS));
    assertEquals(StartTab.LAST_OPENED, StartTab.sanitize(StartTab.LAST_OPENED, TABS));
  }
}
