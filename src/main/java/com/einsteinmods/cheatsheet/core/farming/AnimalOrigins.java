package com.einsteinmods.cheatsheet.core.farming;

import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin.Archaeology;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin.BeeNests;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin.NaturalSpawn;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin.Riding;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin.Source;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin.Special;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin.StructureSpawn;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin.StructureTemplates;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Where each breeding card's animal comes from, as the zoomed card shows it. Spawns are server data
 * (each biome's {@code gameplay/natural_mob_spawns} attribute, structure spawn overrides, structure
 * templates, loot tables, tree features), never sent to the client, so this is a copy: {@code
 * AnimalOriginGameTest} (part of {@code build}) fails on any difference with the game. Only the
 * {@link Special} sources and the texts are not checked there: they follow the vanilla code cited.
 */
public final class AnimalOrigins {
  private static final String NAMESPACE = "minecraft:";

  /** {@code place.<key>}: animal pens, stables and cats placed when a village generates. */
  public static final String VILLAGES = "villages";

  /** Keys of the obtaining texts ({@code get.<key>}). */
  private static final String LURE = "lure";

  private static final String TAME_BY_RIDING = "tame_by_riding";
  private static final String TAME_WITH_ITEMS = "tame_with_items";

  /** Cows, sheep, pigs and chickens: grassland, forest, taiga, savanna, jungle and badlands. */
  private static final List<String> FARM_BIOMES =
      List.of(
          "plains",
          "forest",
          "taiga",
          "savanna",
          "swamp",
          "jungle",
          "badlands",
          "sunflower_plains",
          "flower_forest",
          "birch_forest",
          "old_growth_birch_forest",
          "dark_forest",
          "dappled_forest",
          "old_growth_pine_taiga",
          "old_growth_spruce_taiga",
          "snowy_taiga",
          "savanna_plateau",
          "windswept_savanna",
          "windswept_hills",
          "windswept_forest",
          "windswept_gravelly_hills",
          "sparse_jungle",
          "bamboo_jungle",
          "eroded_badlands",
          "wooded_badlands");

  private static final List<String> SAVANNAS =
      List.of("savanna", "savanna_plateau", "windswept_savanna");

  private static final List<String> TAIGAS =
      List.of("taiga", "snowy_taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga");

  /** {@code CatSpawner}, the {@code village/common/cats} pool: one of ten coats. */
  private static final List<String> CAT_TEMPLATES =
      Stream.of(
              "black", "british", "calico", "jellie", "persian", "ragdoll", "red", "siamese",
              "tabby", "white")
          .map(coat -> "village/common/animals/cat_" + coat)
          .toList();

  public static final List<AnimalOrigin> ORIGINS =
      List.of(
          origin(
              "cow",
              List.of(
                  natural("cow", FARM_BIOMES),
                  natural("mooshroom", List.of("mushroom_fields")),
                  templates("cow", VILLAGES, "village/common/animals/cows_1")),
              "cow",
              Riding.NONE),
          origin(
              "sheep",
              List.of(
                  natural("sheep", plus(FARM_BIOMES, "meadow", "cherry_grove")),
                  templates(
                      "sheep",
                      VILLAGES,
                      "village/common/animals/sheep_1",
                      "village/common/animals/sheep_2")),
              LURE,
              Riding.NONE),
          origin(
              "pig",
              List.of(
                  natural("pig", plus(FARM_BIOMES, "cherry_grove")),
                  templates("pig", VILLAGES, "village/common/animals/pigs_1")),
              LURE,
              Riding.CARROT_ON_A_STICK),
          origin("chicken", List.of(natural("chicken", FARM_BIOMES)), "chicken", Riding.NONE),
          origin(
              "rabbit",
              List.of(
                  natural(
                      "rabbit",
                      plus(
                          List.of("desert", "snowy_plains", "ice_spikes", "snowy_slopes", "grove"),
                          plus(
                              TAIGAS,
                              "flower_forest",
                              "meadow",
                              "cherry_grove",
                              "dappled_forest")))),
              LURE,
              Riding.NONE),
          origin(
              "goat",
              List.of(natural("goat", List.of("snowy_slopes", "jagged_peaks", "frozen_peaks"))),
              LURE,
              Riding.NONE),
          origin(
              "horse",
              List.of(
                  natural("horse", plus(List.of("plains", "sunflower_plains"), SAVANNAS)),
                  templates(
                      "horse",
                      VILLAGES,
                      "village/common/animals/horses_1",
                      "village/common/animals/horses_2",
                      "village/common/animals/horses_3",
                      "village/common/animals/horses_4",
                      "village/common/animals/horses_5")),
              TAME_BY_RIDING,
              Riding.SADDLE),
          origin(
              "donkey",
              List.of(
                  natural(
                      "donkey",
                      plus(plus(List.of("plains", "sunflower_plains"), SAVANNAS), "meadow"))),
              TAME_BY_RIDING,
              Riding.SADDLE),
          origin(
              "llama",
              List.of(
                  natural(
                      "llama",
                      List.of(
                          "savanna_plateau",
                          "windswept_hills",
                          "windswept_forest",
                          "windswept_gravelly_hills")),
                  // WanderingTraderSpawner#tryToSpawnLlamaFor: two trader llamas with each trader.
                  new Special("wandering_trader")),
              "llama",
              Riding.NOT_STEERED),
          tamed(
              "wolf",
              List.of(
                  natural(
                      "wolf",
                      plus(
                          plus(List.of("forest"), TAIGAS),
                          "grove",
                          "savanna_plateau",
                          "sparse_jungle",
                          "wooded_badlands"))),
              // Wolf#mobInteract: only a bone tames it.
              new BreedingFood.FixedFood(List.of(NAMESPACE + "bone"))),
          tamed(
              "cat",
              List.of(
                  templates("cat", VILLAGES, CAT_TEMPLATES.toArray(new String[0])),
                  new StructureSpawn(NAMESPACE + "cat", "swamp_huts", NAMESPACE + "swamp_hut")),
              tag("cat_food")),
          new AnimalOrigin(
              NAMESPACE + "ocelot",
              List.of(natural("ocelot", List.of("jungle", "bamboo_jungle"))),
              "ocelot",
              Optional.of(tag("ocelot_food")),
              Riding.NONE),
          origin(
              "fox",
              List.of(natural("fox", plus(TAIGAS, "grove", "dappled_forest"))),
              "fox",
              Riding.NONE),
          origin(
              "panda",
              List.of(natural("panda", List.of("bamboo_jungle", "jungle"))),
              LURE,
              Riding.NONE),
          origin(
              "bee",
              List.of(
                  new BeeNests(
                      ids(
                          List.of(
                              "plains",
                              "sunflower_plains",
                              "flower_forest",
                              "meadow",
                              "cherry_grove",
                              "mangrove_swamp",
                              "forest",
                              "birch_forest",
                              "old_growth_birch_forest")))),
              "bee",
              Riding.NONE),
          origin("turtle", List.of(natural("turtle", List.of("beach"))), "turtle", Riding.NONE),
          origin(
              "frog",
              List.of(
                  natural("frog", List.of("swamp", "mangrove_swamp")),
                  // Tadpole#ageUp: a tadpole becomes a frog.
                  new Special("tadpoles")),
              "frog",
              Riding.NONE),
          origin(
              "axolotl",
              List.of(natural("axolotl", List.of("lush_caves"))),
              "axolotl",
              Riding.NONE),
          origin(
              "armadillo",
              List.of(
                  natural(
                      "armadillo",
                      plus(SAVANNAS, "badlands", "eroded_badlands", "wooded_badlands"))),
              LURE,
              Riding.NONE),
          origin(
              "camel",
              List.of(
                  natural("camel", List.of("desert")),
                  templates("camel", "desert_villages", "village/desert/camel_spawn")),
              "camel",
              Riding.SADDLE),
          origin(
              "sniffer",
              List.of(
                  new Archaeology(
                      "warm_ocean_ruins",
                      NAMESPACE + "archaeology/ocean_ruin_warm",
                      NAMESPACE + "sniffer_egg")),
              "sniffer",
              Riding.NONE),
          origin(
              "strider",
              List.of(
                  natural(
                      "strider",
                      List.of(
                          "nether_wastes",
                          "crimson_forest",
                          "warped_forest",
                          "soul_sand_valley",
                          "basalt_deltas"))),
              "strider",
              Riding.WARPED_FUNGUS_ON_A_STICK),
          origin(
              "hoglin",
              List.of(
                  natural("hoglin", List.of("crimson_forest")),
                  templates("hoglin", "bastions", "bastion/mobs/hoglin")),
              "hoglin",
              Riding.NONE),
          new AnimalOrigin(
              NAMESPACE + "nautilus",
              List.of(
                  natural(
                      "nautilus",
                      List.of(
                          "ocean",
                          "deep_ocean",
                          "warm_ocean",
                          "lukewarm_ocean",
                          "deep_lukewarm_ocean",
                          "cold_ocean",
                          "deep_cold_ocean",
                          "frozen_ocean",
                          "deep_frozen_ocean"))),
              TAME_WITH_ITEMS,
              Optional.of(tag("nautilus_taming_items")),
              Riding.SADDLE));

  private AnimalOrigins() {}

  /** The origin of the card whose first animal is {@code entityId}, if the table has one. */
  public static Optional<AnimalOrigin> of(String entityId) {
    for (AnimalOrigin origin : ORIGINS) {
      if (origin.entityId().equals(entityId)) {
        return Optional.of(origin);
      }
    }
    return Optional.empty();
  }

  private static AnimalOrigin origin(
      String path, List<Source> sources, String obtainKey, Riding riding) {
    return new AnimalOrigin(NAMESPACE + path, sources, obtainKey, Optional.empty(), riding);
  }

  private static AnimalOrigin tamed(
      String path, List<Source> sources, BreedingFood.Food tamingItems) {
    return new AnimalOrigin(
        NAMESPACE + path, sources, TAME_WITH_ITEMS, Optional.of(tamingItems), Riding.NONE);
  }

  private static NaturalSpawn natural(String path, List<String> biomePaths) {
    return new NaturalSpawn(NAMESPACE + path, ids(biomePaths));
  }

  private static StructureTemplates templates(
      String path, String placeKey, String... templatePaths) {
    return new StructureTemplates(NAMESPACE + path, placeKey, ids(List.of(templatePaths)));
  }

  private static BreedingFood.TagFood tag(String path) {
    return new BreedingFood.TagFood(NAMESPACE + path);
  }

  private static List<String> plus(List<String> first, String... more) {
    return plus(first, List.of(more));
  }

  private static List<String> plus(List<String> first, List<String> more) {
    List<String> all = new ArrayList<>(first);
    all.addAll(more);
    return List.copyOf(all);
  }

  private static List<String> ids(List<String> paths) {
    return paths.stream().map(path -> NAMESPACE + path).toList();
  }
}
