package com.einsteinmods.cheatsheet.core.potions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BrewingTableTest {
  @Test
  void noMixIsListedTwice() {
    Set<String> seen = new HashSet<>();
    for (BrewingMix mix : BrewingTable.MIXES) {
      assertTrue(seen.add(mix.input() + "+" + mix.reagent()), mix.toString());
    }
  }

  @Test
  void awkwardPotionGivesTheBasePotions() {
    List<BrewingMix> mixes = BrewingTable.mixesFrom(BrewingTable.AWKWARD);
    assertEquals(15, mixes.size());
    assertEquals("night_vision", mixes.getFirst().output());
    assertEquals(Optional.of(BrewingTable.AWKWARD), BrewingTable.result("water", "nether_wart"));
  }

  @Test
  void upgradesAreFoundOnlyWhereTheyExist() {
    assertEquals(Optional.of("long_swiftness"), BrewingTable.result("swiftness", "redstone"));
    assertEquals(
        Optional.of("strong_swiftness"), BrewingTable.result("swiftness", "glowstone_dust"));
    assertEquals(Optional.empty(), BrewingTable.result("fire_resistance", "glowstone_dust"));
    assertEquals(Optional.empty(), BrewingTable.result("healing", "redstone"));
  }

  @Test
  void corruptionsStartFromBasePotionsAndGroupByResult() {
    assertEquals(
        List.of(
            new Corruption(List.of("night_vision"), "invisibility"),
            new Corruption(List.of("healing", "poison"), "harming"),
            new Corruption(List.of("swiftness", "leaping"), "slowness")),
        BrewingTable.corruptions());
  }

  @Test
  void potionsIncludeWaterAndEveryResult() {
    Set<String> potions = BrewingTable.potions();
    assertTrue(potions.contains(BrewingTable.WATER));
    assertTrue(potions.contains("strong_turtle_master"));
    assertTrue(potions.contains("mundane"));
    assertEquals(45, potions.size());
  }
}
