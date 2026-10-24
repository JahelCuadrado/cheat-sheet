package com.einsteinmods.cheatsheet.core.farming;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * How the game picks a spawning animal's look: the {@code spawn_conditions} of the variant
 * registries ({@code cow_variant}, {@code wolf_variant}...) and the biome tags the animals' code
 * tests (white rabbits, snow foxes, warm and cold sheep). Both are server data: spawn conditions
 * are left out of the registries sent to the client ({@code WolfVariant.NETWORK_CODEC}), so this is
 * a copy. {@code AnimalVariantGameTest} (part of {@code build}) fails on any difference.
 *
 * <p>The game ({@code PriorityProvider#select}) keeps, of every variant's selectors whose condition
 * holds, those of the highest priority, and picks one of their variants at random.
 */
public final class VariantSpawnRules {
  private static final String NAMESPACE = "minecraft:";

  /** The priority of the fallback selectors, and of the specific ones that beat them. */
  private static final int PRIORITY_FALLBACK = 0;

  private static final int PRIORITY_SPECIFIC = 1;

  /** When a selector applies. */
  public sealed interface Condition permits Always, InBiomes, InStructures, MoonBrightness {}

  /** Always: the fallback. */
  public record Always() implements Condition {}

  /**
   * The spawn position is in one of these biomes.
   *
   * @param biomeIds the biomes, tags expanded
   */
  public record InBiomes(List<String> biomeIds) implements Condition {
    public InBiomes {
      biomeIds = List.copyOf(biomeIds);
    }
  }

  /**
   * The spawn position is inside one of these structures.
   *
   * @param structureIds the structures, tags expanded
   */
  public record InStructures(List<String> structureIds) implements Condition {
    public InStructures {
      structureIds = List.copyOf(structureIds);
    }
  }

  /**
   * The moon is at least this bright ({@code DimensionType.MOON_BRIGHTNESS_PER_PHASE}: 1 at full
   * moon, 0.75 at most otherwise).
   *
   * @param min the least brightness
   */
  public record MoonBrightness(double min) implements Condition {}

  /** One spawn condition of a variant and its priority. */
  public record Selector(Condition condition, int priority) {}

  /**
   * A variant of a registry.
   *
   * @param variantId its id
   * @param selectors its spawn conditions
   */
  public record Variant(String variantId, List<Selector> selectors) {
    public Variant {
      selectors = List.copyOf(selectors);
    }
  }

  /**
   * A variant registry.
   *
   * @param registryId the registry, e.g. {@code minecraft:cow_variant}
   * @param entityId the animal it gives looks to
   * @param variants every variant, in the order the card shows them
   */
  public record VariantRegistry(String registryId, String entityId, List<Variant> variants) {
    public VariantRegistry {
      variants = List.copyOf(variants);
    }

    /**
     * The variants an animal spawning in {@code biomeId} may get, outside any structure and not at
     * full moon: those whose matching selectors share the highest priority.
     */
    public List<String> winnersIn(String biomeId) {
      int best = Integer.MIN_VALUE;
      List<String> winners = new ArrayList<>();
      for (Variant variant : variants) {
        for (Selector selector : variant.selectors()) {
          if (!matches(selector.condition(), biomeId) || selector.priority() < best) {
            continue;
          }
          if (selector.priority() > best) {
            best = selector.priority();
            winners.clear();
          }
          if (!winners.contains(variant.variantId())) {
            winners.add(variant.variantId());
          }
        }
      }
      return List.copyOf(winners);
    }

    private static boolean matches(Condition condition, String biomeId) {
      return switch (condition) {
        case Always always -> true;
        case InBiomes biomes -> biomes.biomeIds().contains(biomeId);
        case InStructures structures -> false;
        case MoonBrightness moon -> false;
      };
    }
  }

  /**
   * A biome tag the code (or a variant) tests.
   *
   * @param tagId the tag
   * @param biomeIds its biomes, nested tags expanded, in the tag's order
   */
  public record BiomeTag(String tagId, List<String> biomeIds) {
    public BiomeTag {
      biomeIds = List.copyOf(biomeIds);
    }
  }

  private static final List<String> IS_END =
      ids("the_end", "end_highlands", "end_midlands", "small_end_islands", "end_barrens");

  private static final List<String> IS_NETHER =
      ids("nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas");

  public static final BiomeTag IS_JUNGLE =
      new BiomeTag(NAMESPACE + "is_jungle", ids("bamboo_jungle", "jungle", "sparse_jungle"));

  public static final BiomeTag IS_SAVANNA =
      new BiomeTag(
          NAMESPACE + "is_savanna", ids("savanna", "savanna_plateau", "windswept_savanna"));

  public static final BiomeTag IS_BADLANDS =
      new BiomeTag(
          NAMESPACE + "is_badlands", ids("badlands", "eroded_badlands", "wooded_badlands"));

  /** The snowy biomes the frogs' and farm animals' cold tags start with. */
  private static final List<String> SNOWY =
      concat(
          ids(
              "snowy_plains",
              "ice_spikes",
              "frozen_peaks",
              "jagged_peaks",
              "snowy_slopes",
              "frozen_ocean",
              "deep_frozen_ocean",
              "grove",
              "deep_dark",
              "frozen_river",
              "snowy_taiga",
              "snowy_beach"),
          IS_END);

  public static final BiomeTag COLD_FARM_ANIMALS =
      new BiomeTag(
          NAMESPACE + "spawns_cold_variant_farm_animals",
          concat(
              SNOWY,
              ids(
                  "cold_ocean",
                  "deep_cold_ocean",
                  "old_growth_pine_taiga",
                  "old_growth_spruce_taiga",
                  "taiga",
                  "windswept_forest",
                  "windswept_gravelly_hills",
                  "windswept_hills",
                  "stony_peaks",
                  "dappled_forest")));

  /** The warm biomes the frogs' and farm animals' warm tags start with. */
  private static final List<String> WARM =
      Stream.of(
              ids("desert", "warm_ocean"),
              IS_JUNGLE.biomeIds(),
              IS_SAVANNA.biomeIds(),
              IS_NETHER,
              IS_BADLANDS.biomeIds(),
              ids("mangrove_swamp"))
          .flatMap(List::stream)
          .toList();

  public static final BiomeTag WARM_FARM_ANIMALS =
      new BiomeTag(
          NAMESPACE + "spawns_warm_variant_farm_animals",
          concat(WARM, ids("deep_lukewarm_ocean", "lukewarm_ocean")));

  public static final BiomeTag COLD_FROGS =
      new BiomeTag(NAMESPACE + "spawns_cold_variant_frogs", SNOWY);

  public static final BiomeTag WARM_FROGS =
      new BiomeTag(NAMESPACE + "spawns_warm_variant_frogs", WARM);

  /** {@code Rabbit#getRandomRabbitVariant}: white or white splotched here. */
  public static final BiomeTag WHITE_RABBITS =
      new BiomeTag(
          NAMESPACE + "spawns_white_rabbits",
          ids(
              "snowy_plains",
              "ice_spikes",
              "frozen_ocean",
              "snowy_taiga",
              "frozen_river",
              "snowy_beach",
              "frozen_peaks",
              "jagged_peaks",
              "snowy_slopes",
              "grove"));

  /** {@code Rabbit#getRandomRabbitVariant}: gold here. */
  public static final BiomeTag GOLD_RABBITS =
      new BiomeTag(NAMESPACE + "spawns_gold_rabbits", ids("desert"));

  /** {@code Fox.Variant#byBiome}: snow foxes here. */
  public static final BiomeTag SNOW_FOXES =
      new BiomeTag(NAMESPACE + "spawns_snow_foxes", WHITE_RABBITS.biomeIds());

  /** Every tag copied here, for the GameTest. */
  public static final List<BiomeTag> TAGS =
      List.of(
          IS_JUNGLE,
          IS_SAVANNA,
          IS_BADLANDS,
          COLD_FARM_ANIMALS,
          WARM_FARM_ANIMALS,
          COLD_FROGS,
          WARM_FROGS,
          WHITE_RABBITS,
          GOLD_RABBITS,
          SNOW_FOXES);

  public static final String TEMPERATE = NAMESPACE + "temperate";
  public static final String WARM_VARIANT = NAMESPACE + "warm";
  public static final String COLD_VARIANT = NAMESPACE + "cold";

  public static final VariantRegistry COW = climates("cow", COLD_FARM_ANIMALS, WARM_FARM_ANIMALS);
  public static final VariantRegistry PIG = climates("pig", COLD_FARM_ANIMALS, WARM_FARM_ANIMALS);
  public static final VariantRegistry CHICKEN =
      climates("chicken", COLD_FARM_ANIMALS, WARM_FARM_ANIMALS);
  public static final VariantRegistry FROG = climates("frog", COLD_FROGS, WARM_FROGS);

  /** {@code wolf_variant}: pale anywhere else. */
  public static final VariantRegistry WOLF =
      new VariantRegistry(
          NAMESPACE + "wolf_variant",
          NAMESPACE + "wolf",
          List.of(
              fallback("pale"),
              inBiomes("woods", ids("forest")),
              inBiomes("ashen", ids("snowy_taiga")),
              inBiomes("black", ids("old_growth_pine_taiga")),
              inBiomes("chestnut", ids("old_growth_spruce_taiga")),
              inBiomes("snowy", ids("grove")),
              inBiomes("rusty", IS_JUNGLE.biomeIds()),
              inBiomes("spotted", IS_SAVANNA.biomeIds()),
              inBiomes("striped", IS_BADLANDS.biomeIds())));

  /** {@code cat_variant}: all black in swamp huts, and one more choice at full moon. */
  public static final String ALL_BLACK_CAT = NAMESPACE + "all_black";

  /** The least moon brightness of an all-black cat: full moon only. */
  public static final double FULL_MOON = 0.9;

  public static final VariantRegistry CAT =
      new VariantRegistry(
          NAMESPACE + "cat_variant",
          NAMESPACE + "cat",
          Stream.concat(
                  Stream.of(
                          "tabby",
                          "black",
                          "red",
                          "siamese",
                          "british_shorthair",
                          "calico",
                          "persian",
                          "ragdoll",
                          "white",
                          "jellie")
                      .map(VariantSpawnRules::fallback),
                  Stream.of(
                      new Variant(
                          ALL_BLACK_CAT,
                          List.of(
                              new Selector(new InStructures(ids("swamp_hut")), PRIORITY_SPECIFIC),
                              new Selector(new MoonBrightness(FULL_MOON), PRIORITY_FALLBACK)))))
              .toList());

  /** Every variant registry copied here, for the GameTest. */
  public static final List<VariantRegistry> REGISTRIES =
      List.of(COW, PIG, CHICKEN, FROG, WOLF, CAT);

  private VariantSpawnRules() {}

  /** Temperate anywhere else, cold and warm in their tags' biomes. */
  private static VariantRegistry climates(String animal, BiomeTag cold, BiomeTag warm) {
    return new VariantRegistry(
        NAMESPACE + animal + "_variant",
        NAMESPACE + animal,
        List.of(
            fallback("temperate"),
            inBiomes("warm", warm.biomeIds()),
            inBiomes("cold", cold.biomeIds())));
  }

  private static Variant fallback(String path) {
    return new Variant(NAMESPACE + path, List.of(new Selector(new Always(), PRIORITY_FALLBACK)));
  }

  private static Variant inBiomes(String path, List<String> biomeIds) {
    return new Variant(
        NAMESPACE + path, List.of(new Selector(new InBiomes(biomeIds), PRIORITY_SPECIFIC)));
  }

  /**
   * The biomes of {@code biomeIds} each variant wins, in that order; variants that win none left
   * out.
   */
  public static Map<String, List<String>> biomesByWinner(
      VariantRegistry registry, List<String> biomeIds) {
    Map<String, List<String>> byVariant = new LinkedHashMap<>();
    for (String biome : biomeIds) {
      for (String winner : registry.winnersIn(biome)) {
        byVariant.computeIfAbsent(winner, key -> new ArrayList<>()).add(biome);
      }
    }
    return byVariant;
  }

  private static List<String> ids(String... paths) {
    return Stream.of(paths).map(path -> NAMESPACE + path).toList();
  }

  private static List<String> concat(List<String> first, List<String> second) {
    return Stream.concat(first.stream(), second.stream()).toList();
  }
}
