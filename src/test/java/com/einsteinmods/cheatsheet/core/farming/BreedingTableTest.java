package com.einsteinmods.cheatsheet.core.farming;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BreedingTableTest {
  @Test
  void everyAnimalIsListedOnce() {
    Set<String> seen = new HashSet<>();
    for (BreedingFood animal : BreedingTable.ANIMALS) {
      for (String id : animal.entityIds()) {
        assertTrue(seen.add(id), id + " listed twice");
        assertFalse(BreedingTable.NOT_BRED_BY_FEEDING.contains(id), id + " also left out");
      }
    }
  }
}
