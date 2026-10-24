package com.einsteinmods.cheatsheet.core.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnimalOriginsTest {
  @Test
  void everyBreedingCardHasOneOriginInTheSameOrder() {
    List<String> cards =
        BreedingTable.ANIMALS.stream().map(animal -> animal.entityIds().getFirst()).toList();
    List<String> origins = AnimalOrigins.ORIGINS.stream().map(AnimalOrigin::entityId).toList();
    assertEquals(cards, origins);
  }

  @Test
  void sourcesNameOnlyTheAnimalsOfTheirCardAndNoBiomeTwice() {
    for (BreedingFood animal : BreedingTable.ANIMALS) {
      AnimalOrigin origin = AnimalOrigins.of(animal.entityIds().getFirst()).orElseThrow();
      assertFalse(origin.obtainKey().isBlank());
      for (AnimalOrigin.Source source : origin.sources()) {
        switch (source) {
          case AnimalOrigin.NaturalSpawn spawn -> {
            assertTrue(animal.entityIds().contains(spawn.entityId()), spawn.entityId());
            assertUnique(spawn.biomeIds());
          }
          case AnimalOrigin.StructureTemplates templates -> {
            assertTrue(animal.entityIds().contains(templates.entityId()), templates.entityId());
            assertUnique(templates.templateIds());
          }
          case AnimalOrigin.StructureSpawn spawn ->
              assertTrue(animal.entityIds().contains(spawn.entityId()), spawn.entityId());
          case AnimalOrigin.BeeNests nests -> assertUnique(nests.biomeIds());
          case AnimalOrigin.Archaeology archaeology -> assertFalse(archaeology.itemId().isBlank());
          case AnimalOrigin.Special special -> assertFalse(special.placeKey().isBlank());
        }
      }
    }
  }

  @Test
  void sticksSteerSaddledAnimalsAndLlamasTakeNoSaddle() {
    assertTrue(AnimalOrigin.Riding.CARROT_ON_A_STICK.saddled());
    assertTrue(AnimalOrigin.Riding.WARPED_FUNGUS_ON_A_STICK.saddled());
    assertTrue(AnimalOrigin.Riding.SADDLE.saddled());
    assertFalse(AnimalOrigin.Riding.NOT_STEERED.saddled());
    assertFalse(AnimalOrigin.Riding.NONE.saddled());
  }

  private static void assertUnique(List<String> ids) {
    assertEquals(ids.size(), new HashSet<>(ids).size(), "repeated in " + ids);
  }
}
