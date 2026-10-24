package com.einsteinmods.cheatsheet.core.discs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiscTableTest {
  @Test
  void everyDiscIsListedOnceWithoutRepeatedSources() {
    Set<String> ids = new HashSet<>();
    for (Disc disc : DiscTable.DISCS) {
      assertTrue(ids.add(disc.itemId()), "repeated " + disc.itemId());
      assertEquals(
          disc.sources().size(),
          new HashSet<>(disc.sources()).size(),
          "repeated source in " + disc.itemId());
    }
  }

  @Test
  void everySourceGivesADisc() {
    for (DiscSource source : DiscSource.values()) {
      assertFalse(DiscTable.discsFrom(source).isEmpty(), source + " has no disc");
    }
  }

  @Test
  void onlyCraftingHasNoLootTable() {
    for (DiscSource source : DiscSource.values()) {
      assertEquals(source != DiscSource.CRAFTED, source.lootTable().isPresent(), source.name());
    }
  }

  @Test
  void discsFromKeepsTableOrder() {
    List<Disc> dungeon = DiscTable.discsFrom(DiscSource.DUNGEON);
    assertEquals(
        List.of("music_disc_13", "music_disc_cat", "music_disc_otherside"),
        dungeon.stream().map(Disc::itemId).toList());
    assertEquals(12, DiscTable.discsFrom(DiscSource.CREEPER_KILLED_BY_SKELETON).size());
  }
}
