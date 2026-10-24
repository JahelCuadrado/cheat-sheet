package com.einsteinmods.cheatsheet.core.farming;

import com.einsteinmods.cheatsheet.core.farming.AnimalVariant.BiomeWay;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariant.Chance;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariant.InBiomes;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariant.Origin;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariant.Told;
import com.einsteinmods.cheatsheet.core.farming.VariantSpawnRules.BiomeTag;
import com.einsteinmods.cheatsheet.core.farming.VariantSpawnRules.VariantRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The looks of each breeding card's animals and where each comes from, as the zoomed card's variant
 * grid shows them. Looks picked by spawn conditions or biome tags follow {@link VariantSpawnRules}
 * over the animal's natural spawn biomes ({@link AnimalOrigins}), both checked against the game;
 * chances written in the animals' code are copied from the code cited, and {@code
 * AnimalVariantGameTest} samples the ones the game exposes (sheep colours, panda genes).
 */
public final class AnimalVariants {
  private static final String NAMESPACE = "minecraft:";

  /** Prefix of the mod's variant names; sheep use the game's colour names. */
  private static final String NAME_PREFIX = "cheatsheet.tab.farming.variant.name.";

  private static final String DYE_PREFIX = "color.minecraft.";

  /** {@code Rabbit.Variant.EVIL}: the game names it. */
  private static final String KILLER_BUNNY = "entity.minecraft.killer_bunny";

  /** {@code RabbitRenderer#extractRenderState}: a rabbit named exactly this looks like toast. */
  public static final String TOAST_NAME = "Toast";

  /** Climate labels ({@code climate.<key>}). */
  public static final String TEMPERATE = "temperate";

  public static final String WARM = "warm";
  public static final String COLD = "cold";
  public static final String SNOWY = "snowy";
  public static final String OTHER = "other";
  public static final String ALL = "all";

  /** Notes under the grid ({@code note.<key>}). */
  private static final String INHERITED = "inherited";

  /** {@code Panda.Gene#getRandom}: sixteenths of each gene at spawn or mutation. */
  public static final List<Weighted> PANDA_GENES =
      List.of(
          new Weighted("normal", 5),
          new Weighted("lazy", 1),
          new Weighted("worried", 1),
          new Weighted("playful", 1),
          new Weighted("aggressive", 1),
          new Weighted("weak", 5),
          new Weighted("brown", 2));

  /** {@code Panda.Gene}: shown only when both genes are this one. */
  public static final List<String> RECESSIVE_GENES = List.of("weak", "brown");

  /** {@code SheepColorSpawnRules}: a climate's five colours, weighted out of 100. */
  public static final int SHEEP_COMMON_WEIGHT = 82;

  public static final int SHEEP_UNCOMMON_WEIGHT = 5;
  public static final int SHEEP_RARE_WEIGHT = 3;
  public static final int SHEEP_WEIGHTS = 100;

  /** {@code SheepColorSpawnRules#commonColors}: one common sheep in 500 is pink instead. */
  public static final int SHEEP_PINK_IN = 500;

  /**
   * {@code SheepColorSpawnRules}: per climate, the common colour, the three uncommon ones and the
   * rare one.
   */
  public static final List<SheepClimate> SHEEP_CLIMATES =
      List.of(
          new SheepClimate(TEMPERATE, "white", List.of("black", "gray", "light_gray"), "brown"),
          new SheepClimate(WARM, "brown", List.of("gray", "light_gray", "white"), "black"),
          new SheepClimate(COLD, "black", List.of("light_gray", "gray", "white"), "brown"));

  public static final String SHEEP_PINK = "pink";

  /** {@code Rabbit#getRandomRabbitVariant}: percent of each look per biome group. */
  private static final int RABBIT_WHITE = 80;

  private static final int RABBIT_WHITE_SPLOTCHED = 20;
  private static final int RABBIT_BROWN = 50;
  private static final int RABBIT_SALT = 40;
  private static final int RABBIT_BLACK = 10;
  private static final int PERCENT = 100;

  /** {@code Goat#finalizeSpawn}: one goat in fifty screams. */
  public static final Chance SCREAMING_GOAT = new Chance(2, PERCENT);

  /** {@code Axolotl#useRareVariant}: one bred axolotl in 1200 is blue. */
  public static final int BLUE_AXOLOTL_IN = 1200;

  /** {@code Axolotl.Variant}: the looks that spawn. */
  public static final List<String> COMMON_AXOLOTLS = List.of("lucy", "wild", "gold", "cyan");

  /** {@code MushroomCow#getOffspringVariant}: one calf in 1024 of two alike flips colour. */
  private static final int MOOSHROOM_FLIP_IN = 1024;

  /** {@code Horse.Variant}, in the game's order: picked at random. */
  public static final List<String> HORSE_COATS =
      List.of("white", "creamy", "chestnut", "brown", "black", "gray", "dark_brown");

  /** {@code Llama.Variant}, in the game's order: picked at random. */
  public static final List<String> LLAMA_COATS = List.of("creamy", "white", "brown", "gray");

  /** A weighted choice. */
  public record Weighted(String id, int weight) {}

  /** A climate's sheep colours ({@link #SHEEP_CLIMATES}). */
  public record SheepClimate(String climate, String common, List<String> uncommon, String rare) {
    public SheepClimate {
      uncommon = List.copyOf(uncommon);
    }

    /** The chance of {@code color} in this climate, zero when it never spawns there. */
    public Chance chanceOf(String color) {
      Chance commonShare = new Chance(SHEEP_COMMON_WEIGHT, SHEEP_WEIGHTS);
      if (color.equals(common)) {
        return commonShare.times(new Chance(SHEEP_PINK_IN - 1L, SHEEP_PINK_IN));
      }
      if (color.equals(SHEEP_PINK)) {
        return commonShare.times(new Chance(1, SHEEP_PINK_IN));
      }
      if (uncommon.contains(color)) {
        return new Chance(SHEEP_UNCOMMON_WEIGHT, SHEEP_WEIGHTS);
      }
      return color.equals(rare) ? new Chance(SHEEP_RARE_WEIGHT, SHEEP_WEIGHTS) : new Chance(0, 1);
    }
  }

  /**
   * A card's looks and the notes under them.
   *
   * @param cardId the card's first animal
   * @param variants its animals' looks, in the order shown; may be empty (notes only)
   * @param noteKeys texts under the grid ({@code note.<key>})
   */
  public record VariantSet(String cardId, List<AnimalVariant> variants, List<String> noteKeys) {
    public VariantSet {
      variants = List.copyOf(variants);
      noteKeys = List.copyOf(noteKeys);
    }
  }

  public static final List<VariantSet> SETS = build();

  private AnimalVariants() {}

  /** The looks of the card whose first animal is {@code entityId}, if it has any. */
  public static Optional<VariantSet> of(String entityId) {
    for (VariantSet set : SETS) {
      if (set.cardId().equals(entityId)) {
        return Optional.of(set);
      }
    }
    return Optional.empty();
  }

  private static List<VariantSet> build() {
    List<VariantSet> sets = new ArrayList<>();
    List<AnimalVariant> cows = new ArrayList<>(climates(VariantSpawnRules.COW, "cow"));
    cows.add(
        variant(
            "mooshroom",
            "red",
            name("red"),
            spawns(Optional.empty(), naturalBiomes("mooshroom"), Optional.empty())));
    cows.add(
        variant(
            "mooshroom",
            "brown",
            name("brown"),
            told("lightning", Optional.empty()),
            told("mooshroom_calf", Optional.of(new Chance(1, MOOSHROOM_FLIP_IN)))));
    sets.add(set("cow", cows, INHERITED));
    sets.add(set("sheep", sheep(), "sheep"));
    sets.add(set("pig", climates(VariantSpawnRules.PIG, "pig"), INHERITED));
    sets.add(set("chicken", chickens(), "chicken"));
    sets.add(set("rabbit", rabbits(), "rabbit"));
    sets.add(set("goat", List.of(), "goat"));
    sets.add(set("horse", coats("horse", HORSE_COATS), "horse"));
    sets.add(set("llama", coats("llama", LLAMA_COATS), "llama"));
    sets.add(set("wolf", wolves(), INHERITED));
    sets.add(set("cat", cats(), INHERITED));
    sets.add(set("fox", foxes(), INHERITED));
    sets.add(set("panda", pandas(), "panda"));
    sets.add(set("frog", frogs(), "frog"));
    sets.add(set("axolotl", axolotls(), "axolotl"));
    return List.copyOf(sets);
  }

  /** Temperate, warm and cold, each where the animal spawns with it. */
  private static List<AnimalVariant> climates(VariantRegistry registry, String animal) {
    Map<String, List<String>> biomes =
        VariantSpawnRules.biomesByWinner(registry, naturalBiomes(animal));
    List<AnimalVariant> looks = new ArrayList<>();
    for (String climate : List.of(TEMPERATE, WARM, COLD)) {
      String id = NAMESPACE + climate;
      List<String> where = biomes.getOrDefault(id, List.of());
      if (!where.isEmpty()) {
        looks.add(
            variant(
                animal, id, name(climate), spawns(Optional.of(climate), where, Optional.empty())));
      }
    }
    return looks;
  }

  /** Chickens by climate; brown and blue eggs hatch warm and cold chicks. */
  private static List<AnimalVariant> chickens() {
    List<AnimalVariant> looks = new ArrayList<>();
    for (AnimalVariant chicken : climates(VariantSpawnRules.CHICKEN, "chicken")) {
      String egg =
          chicken.variantId().equals(VariantSpawnRules.WARM_VARIANT)
              ? "brown_egg"
              : chicken.variantId().equals(VariantSpawnRules.COLD_VARIANT) ? "blue_egg" : "egg";
      List<Origin> origins = new ArrayList<>(chicken.origins());
      origins.add(told(egg, Optional.empty()));
      looks.add(
          new AnimalVariant(
              chicken.entityId(),
              chicken.variantId(),
              chicken.nameKey(),
              Optional.empty(),
              origins));
    }
    return looks;
  }

  /** Every natural colour, each with its chance in each climate (alike chances merged). */
  private static List<AnimalVariant> sheep() {
    List<String> all = naturalBiomes("sheep");
    List<String> warm = only(all, biome -> contains(VariantSpawnRules.WARM_FARM_ANIMALS, biome));
    List<String> cold =
        only(
            all,
            biome -> !warm.contains(biome) && contains(VariantSpawnRules.COLD_FARM_ANIMALS, biome));
    List<String> temperate = only(all, biome -> !warm.contains(biome) && !cold.contains(biome));
    List<List<String>> climateBiomes = List.of(temperate, warm, cold);
    List<String> colors = List.of("white", "black", "gray", "light_gray", "brown", SHEEP_PINK);
    List<AnimalVariant> looks = new ArrayList<>();
    for (String color : colors) {
      List<Chance> chances = new ArrayList<>();
      for (SheepClimate climate : SHEEP_CLIMATES) {
        chances.add(climate.chanceOf(color));
      }
      List<Origin> origins = new ArrayList<>();
      if (chances.stream().distinct().count() == 1) {
        origins.add(spawns(Optional.of(ALL), all, Optional.of(chances.getFirst())));
      } else {
        List<Integer> order = new ArrayList<>(List.of(0, 1, 2));
        order.sort(
            (first, second) ->
                Double.compare(chances.get(second).percent(), chances.get(first).percent()));
        for (int index : order) {
          origins.add(
              spawns(
                  Optional.of(SHEEP_CLIMATES.get(index).climate()),
                  climateBiomes.get(index),
                  Optional.of(chances.get(index))));
        }
      }
      looks.add(
          new AnimalVariant(
              NAMESPACE + "sheep", color, DYE_PREFIX + color, Optional.empty(), origins));
    }
    return looks;
  }

  /** By biome group, then Toast by name and the killer bunny by command. */
  private static List<AnimalVariant> rabbits() {
    List<String> all = naturalBiomes("rabbit");
    List<String> snowy = only(all, biome -> contains(VariantSpawnRules.WHITE_RABBITS, biome));
    List<String> desert =
        only(
            all,
            biome -> !snowy.contains(biome) && contains(VariantSpawnRules.GOLD_RABBITS, biome));
    List<String> other = only(all, biome -> !snowy.contains(biome) && !desert.contains(biome));
    List<AnimalVariant> looks = new ArrayList<>();
    looks.add(rabbit("brown", OTHER, other, RABBIT_BROWN));
    looks.add(rabbit("salt", OTHER, other, RABBIT_SALT));
    looks.add(rabbit("black", OTHER, other, RABBIT_BLACK));
    looks.add(rabbit("white", SNOWY, snowy, RABBIT_WHITE));
    looks.add(rabbit("white_splotched", SNOWY, snowy, RABBIT_WHITE_SPLOTCHED));
    looks.add(
        variant(
            "rabbit", "gold", name("gold"), spawns(Optional.empty(), desert, Optional.empty())));
    looks.add(
        new AnimalVariant(
            NAMESPACE + "rabbit",
            "brown",
            name("toast"),
            Optional.of(TOAST_NAME),
            List.of(told("toast", Optional.empty()))));
    looks.add(variant("rabbit", "evil", KILLER_BUNNY, told("killer_bunny", Optional.empty())));
    return looks;
  }

  private static AnimalVariant rabbit(String id, String label, List<String> biomes, int percent) {
    return variant(
        "rabbit",
        id,
        name(id),
        spawns(Optional.of(label), biomes, Optional.of(new Chance(percent, PERCENT))));
  }

  /** Coats picked at random, alike in a herd. */
  private static List<AnimalVariant> coats(String animal, List<String> coats) {
    List<AnimalVariant> looks = new ArrayList<>();
    for (String coat : coats) {
      looks.add(
          variant(
              animal, coat, name(coat), told("random", Optional.of(new Chance(1, coats.size())))));
    }
    return looks;
  }

  private static List<AnimalVariant> wolves() {
    Map<String, List<String>> biomes =
        VariantSpawnRules.biomesByWinner(VariantSpawnRules.WOLF, naturalBiomes("wolf"));
    List<AnimalVariant> looks = new ArrayList<>();
    for (VariantSpawnRules.Variant wolf : VariantSpawnRules.WOLF.variants()) {
      List<String> where = biomes.getOrDefault(wolf.variantId(), List.of());
      if (!where.isEmpty()) {
        looks.add(
            variant(
                "wolf",
                wolf.variantId(),
                name(path(wolf.variantId())),
                spawns(Optional.empty(), where, Optional.empty())));
      }
    }
    return looks;
  }

  /**
   * Village cats pick one of ten coats ({@code CatSpawner}, the village templates); swamp huts
   * always give an all-black cat, and at full moon it is an eleventh choice.
   */
  private static List<AnimalVariant> cats() {
    List<AnimalVariant> looks = new ArrayList<>();
    List<VariantSpawnRules.Variant> variants = VariantSpawnRules.CAT.variants();
    long villageCoats =
        variants.stream()
            .filter(cat -> !cat.variantId().equals(VariantSpawnRules.ALL_BLACK_CAT))
            .count();
    for (VariantSpawnRules.Variant cat : variants) {
      String path = path(cat.variantId());
      if (cat.variantId().equals(VariantSpawnRules.ALL_BLACK_CAT)) {
        looks.add(
            variant(
                "cat",
                cat.variantId(),
                name("black"),
                told("swamp_huts", Optional.empty()),
                told("full_moon", Optional.of(new Chance(1, villageCoats + 1)))));
      } else {
        // The game's "black" cat is the tuxedo one; "all_black" is the black one.
        String nameKey = path.equals("black") ? "tuxedo" : path;
        looks.add(
            variant(
                "cat",
                cat.variantId(),
                name(nameKey),
                told("villages", Optional.of(new Chance(1, villageCoats)))));
      }
    }
    return looks;
  }

  /** {@code Fox.Variant#byBiome}: snow foxes in snowy biomes, red elsewhere. */
  private static List<AnimalVariant> foxes() {
    List<String> all = naturalBiomes("fox");
    List<String> snow = only(all, biome -> contains(VariantSpawnRules.SNOW_FOXES, biome));
    List<String> red = only(all, biome -> !snow.contains(biome));
    return List.of(
        variant("fox", "red", name("red"), spawns(Optional.empty(), red, Optional.empty())),
        variant("fox", "snow", name("snow"), spawns(Optional.empty(), snow, Optional.empty())));
  }

  /**
   * {@code Panda#finalizeSpawn}: two random genes; a recessive one shows only when both match, a
   * normal panda otherwise.
   */
  private static List<AnimalVariant> pandas() {
    List<AnimalVariant> looks = new ArrayList<>();
    for (Weighted gene : PANDA_GENES) {
      looks.add(
          variant(
              "panda",
              gene.id(),
              name(gene.id()),
              told(
                  RECESSIVE_GENES.contains(gene.id()) ? "recessive_gene" : "gene",
                  Optional.of(pandaChance(gene.id())))));
    }
    return looks;
  }

  /** The chance a spawning panda shows {@code geneId}. */
  public static Chance pandaChance(String geneId) {
    int total = 0;
    for (Weighted gene : PANDA_GENES) {
      total += gene.weight();
    }
    long shown = 0;
    long square = (long) total * total;
    for (Weighted main : PANDA_GENES) {
      for (Weighted hidden : PANDA_GENES) {
        String visible =
            RECESSIVE_GENES.contains(main.id()) && !main.id().equals(hidden.id())
                ? PANDA_GENES.getFirst().id()
                : main.id();
        if (visible.equals(geneId)) {
          shown += (long) main.weight() * hidden.weight();
        }
      }
    }
    return new Chance(shown, square);
  }

  /**
   * Frogs spawn temperate in swamps and warm in mangrove swamps; a tadpole becomes the frog of the
   * biome it grows up in.
   */
  private static List<AnimalVariant> frogs() {
    Map<String, List<String>> spawning =
        VariantSpawnRules.biomesByWinner(VariantSpawnRules.FROG, naturalBiomes("frog"));
    List<AnimalVariant> looks = new ArrayList<>();
    for (String climate : List.of(TEMPERATE, WARM, COLD)) {
      String id = NAMESPACE + climate;
      List<Origin> origins = new ArrayList<>();
      List<String> spawns = spawning.getOrDefault(id, List.of());
      if (!spawns.isEmpty()) {
        origins.add(spawns(Optional.empty(), spawns, Optional.empty()));
      }
      BiomeTag grows =
          climate.equals(WARM)
              ? VariantSpawnRules.WARM_FROGS
              : climate.equals(COLD) ? VariantSpawnRules.COLD_FROGS : null;
      origins.add(
          grows == null
              ? told("tadpole_elsewhere", Optional.empty())
              : new InBiomes(
                  BiomeWay.GROWS, Optional.of(climate), grows.biomeIds(), Optional.empty()));
      looks.add(
          new AnimalVariant(NAMESPACE + "frog", id, name(climate), Optional.empty(), origins));
    }
    return looks;
  }

  /** Four colours spawn; blue only when bred. */
  private static List<AnimalVariant> axolotls() {
    List<AnimalVariant> looks = new ArrayList<>();
    Chance common = new Chance(1, COMMON_AXOLOTLS.size());
    for (String color : COMMON_AXOLOTLS) {
      looks.add(
          variant(
              "axolotl",
              color,
              name(color.equals("lucy") ? "leucistic" : color),
              spawns(Optional.empty(), naturalBiomes("axolotl"), Optional.of(common))));
    }
    looks.add(
        variant(
            "axolotl",
            "blue",
            name("blue"),
            told("blue_axolotl", Optional.of(new Chance(1, BLUE_AXOLOTL_IN)))));
    return looks;
  }

  /** The biomes {@link AnimalOrigins} lists for {@code animal}'s natural spawns. */
  private static List<String> naturalBiomes(String animal) {
    String entityId = NAMESPACE + animal;
    for (AnimalOrigin origin : AnimalOrigins.ORIGINS) {
      for (AnimalOrigin.Source source : origin.sources()) {
        if (source instanceof AnimalOrigin.NaturalSpawn spawn
            && spawn.entityId().equals(entityId)) {
          return spawn.biomeIds();
        }
      }
    }
    throw new IllegalStateException(entityId + " has no natural spawns");
  }

  private static VariantSet set(String card, List<AnimalVariant> variants, String noteKey) {
    return new VariantSet(NAMESPACE + card, variants, List.of(noteKey));
  }

  private static AnimalVariant variant(
      String animal, String id, String nameKey, Origin... origins) {
    return new AnimalVariant(NAMESPACE + animal, id, nameKey, Optional.empty(), List.of(origins));
  }

  private static InBiomes spawns(
      Optional<String> label, List<String> biomeIds, Optional<Chance> chance) {
    return new InBiomes(BiomeWay.SPAWNS, label, biomeIds, chance);
  }

  private static Told told(String key, Optional<Chance> chance) {
    return new Told(key, chance);
  }

  private static String name(String key) {
    return NAME_PREFIX + key;
  }

  private static String path(String id) {
    return id.substring(id.indexOf(':') + 1);
  }

  private static boolean contains(BiomeTag tag, String biomeId) {
    return tag.biomeIds().contains(biomeId);
  }

  private static List<String> only(List<String> biomes, Predicate<String> keep) {
    return biomes.stream().filter(keep).toList();
  }
}
