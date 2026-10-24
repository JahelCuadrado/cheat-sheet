package com.einsteinmods.cheatsheet.core.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.einsteinmods.cheatsheet.core.farming.AnimalVariant.Chance;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariant.InBiomes;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariant.Origin;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariants.SheepClimate;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariants.VariantSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnimalVariantsTest {
  private static final double EPSILON = 1e-9;
  private static final double WHOLE = 100.0;

  @Test
  void setsFollowTheBreedingCardsOrder() {
    List<String> cards =
        BreedingTable.ANIMALS.stream().map(animal -> animal.entityIds().getFirst()).toList();
    int last = -1;
    for (VariantSet set : AnimalVariants.SETS) {
      int index = cards.indexOf(set.cardId());
      assertTrue(index > last, set.cardId() + " out of order or not a card");
      last = index;
      BreedingFood card = BreedingTable.ANIMALS.get(index);
      for (AnimalVariant variant : set.variants()) {
        assertTrue(card.entityIds().contains(variant.entityId()), variant.entityId());
      }
      assertFalse(set.noteKeys().isEmpty(), set.cardId());
    }
  }

  @Test
  void biomeOriginsAreUniqueAndSpawnOnlyWhereTheAnimalDoes() {
    for (VariantSet set : AnimalVariants.SETS) {
      for (AnimalVariant variant : set.variants()) {
        for (Origin origin : variant.origins()) {
          if (origin instanceof InBiomes biomes) {
            assertEquals(
                biomes.biomeIds().size(),
                new HashSet<>(biomes.biomeIds()).size(),
                variant.variantId() + " repeats a biome");
          }
        }
      }
    }
  }

  @Test
  void wolvesTakeTheCoatOfTheirBiome() {
    VariantSet wolves = AnimalVariants.of("minecraft:wolf").orElseThrow();
    assertEquals(9, wolves.variants().size());
    assertEquals(List.of("minecraft:taiga"), spawnBiomes(wolves, "minecraft:pale"));
    assertEquals(List.of("minecraft:forest"), spawnBiomes(wolves, "minecraft:woods"));
    assertEquals(List.of("minecraft:sparse_jungle"), spawnBiomes(wolves, "minecraft:rusty"));
    assertEquals(List.of("minecraft:savanna_plateau"), spawnBiomes(wolves, "minecraft:spotted"));
    assertEquals(List.of("minecraft:wooded_badlands"), spawnBiomes(wolves, "minecraft:striped"));
    assertEquals(List.of("minecraft:grove"), spawnBiomes(wolves, "minecraft:snowy"));
  }

  @Test
  void farmAnimalsSplitTheirBiomesByClimate() {
    VariantSet cows = AnimalVariants.of("minecraft:cow").orElseThrow();
    List<String> cold = spawnBiomes(cows, "minecraft:cold");
    assertTrue(cold.contains("minecraft:taiga"));
    assertTrue(cold.contains("minecraft:dappled_forest"));
    assertTrue(spawnBiomes(cows, "minecraft:warm").contains("minecraft:savanna"));
    assertTrue(spawnBiomes(cows, "minecraft:temperate").contains("minecraft:plains"));
    Set<String> all = new HashSet<>();
    for (String climate : List.of("temperate", "warm", "cold")) {
      all.addAll(spawnBiomes(cows, "minecraft:" + climate));
    }
    AnimalOrigin.NaturalSpawn natural =
        (AnimalOrigin.NaturalSpawn)
            AnimalOrigins.of("minecraft:cow").orElseThrow().sources().getFirst();
    assertEquals(new HashSet<>(natural.biomeIds()), all);
  }

  @Test
  void frogsSpawnInSwampsAndColdOnesOnlyGrowUp() {
    VariantSet frogs = AnimalVariants.of("minecraft:frog").orElseThrow();
    assertEquals(List.of("minecraft:swamp"), spawnBiomes(frogs, "minecraft:temperate"));
    assertEquals(List.of("minecraft:mangrove_swamp"), spawnBiomes(frogs, "minecraft:warm"));
    assertEquals(List.of(), spawnBiomes(frogs, "minecraft:cold"));
  }

  @Test
  void sheepColoursAddUpInEveryClimate() {
    for (SheepClimate climate : AnimalVariants.SHEEP_CLIMATES) {
      double sum = 0;
      for (String color : List.of("white", "black", "gray", "light_gray", "brown", "pink")) {
        sum += climate.chanceOf(color).percent();
      }
      assertEquals(WHOLE, sum, EPSILON, climate.climate());
    }
    assertEquals(new Chance(41, 25000), AnimalVariants.SHEEP_CLIMATES.getFirst().chanceOf("pink"));
  }

  @Test
  void pandaPersonalitiesMatchTheirGenes() {
    double sum = 0;
    for (AnimalVariants.Weighted gene : AnimalVariants.PANDA_GENES) {
      sum += AnimalVariants.pandaChance(gene.id()).percent();
    }
    assertEquals(WHOLE, sum, EPSILON);
    assertEquals(new Chance(1, 64), AnimalVariants.pandaChance("brown"));
    assertEquals(new Chance(25, 256), AnimalVariants.pandaChance("weak"));
    assertEquals(new Chance(163, 256), AnimalVariants.pandaChance("normal"));
    assertEquals(new Chance(1, 16), AnimalVariants.pandaChance("lazy"));
  }

  @Test
  void rabbitsOfAGroupAddUpAndToastIsANameTag() {
    VariantSet rabbits = AnimalVariants.of("minecraft:rabbit").orElseThrow();
    double other = 0;
    for (AnimalVariant rabbit : rabbits.variants()) {
      if (rabbit.origins().getFirst() instanceof InBiomes biomes
          && biomes.labelKey().orElse("").equals(AnimalVariants.OTHER)) {
        other += biomes.chance().orElseThrow().percent();
      }
    }
    assertEquals(WHOLE, other, EPSILON);
    assertTrue(
        rabbits.variants().stream()
            .anyMatch(rabbit -> rabbit.customName().orElse("").equals(AnimalVariants.TOAST_NAME)));
  }

  @Test
  void catsAreElevenAndTheBlackOneJoinsAtFullMoon() {
    VariantSet cats = AnimalVariants.of("minecraft:cat").orElseThrow();
    assertEquals(11, cats.variants().size());
    AnimalVariant black =
        cats.variants().stream()
            .filter(cat -> cat.variantId().equals(VariantSpawnRules.ALL_BLACK_CAT))
            .findFirst()
            .orElseThrow();
    assertEquals(new Chance(1, 11), black.origins().get(1).chance().orElseThrow());
  }

  @Test
  void equalPrioritiesShareTheBiome() {
    assertEquals(10, VariantSpawnRules.CAT.winnersIn("minecraft:plains").size());
    assertEquals(
        List.of("minecraft:cold"), VariantSpawnRules.COW.winnersIn("minecraft:snowy_plains"));
  }

  private static List<String> spawnBiomes(VariantSet set, String variantId) {
    for (AnimalVariant variant : set.variants()) {
      if (variant.variantId().equals(variantId)) {
        for (Origin origin : variant.origins()) {
          if (origin instanceof InBiomes biomes && biomes.way() == AnimalVariant.BiomeWay.SPAWNS) {
            return biomes.biomeIds();
          }
        }
        return List.of();
      }
    }
    return List.of();
  }
}
