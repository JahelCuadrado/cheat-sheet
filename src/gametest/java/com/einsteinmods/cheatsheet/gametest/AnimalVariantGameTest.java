package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.farming.AnimalVariant;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariants;
import com.einsteinmods.cheatsheet.core.farming.VariantSpawnRules;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.SheepColorSpawnRules;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.MoonBrightnessCheck;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.StructureCheck;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;

/**
 * The farming tab's animal looks against the game: every variant registry's {@code
 * spawn_conditions} and the biome tags the animals' code tests equal {@link VariantSpawnRules}; the
 * looks written in code ({@link AnimalVariants}) exist under the names the card uses; egg colours
 * hatch the chick the card says; and the chances the game exposes (sheep colours by climate, panda
 * genes) match the table when sampled with a fixed seed.
 */
public final class AnimalVariantGameTest {
  private static final String NAMESPACE = "minecraft:";
  private static final long SEED = 26_3L;
  private static final int SAMPLES = 200_000;

  /** Draws that surely meet every axolotl colour of a group (four or one). */
  private static final int AXOLOTL_SAMPLES = 2_000;

  /** Allowed sampling error, in standard deviations: a real difference is far larger. */
  private static final double DEVIATIONS = 5.0;

  private static final double PERCENT = 100.0;

  /** A biome of each sheep climate, in {@link AnimalVariants#SHEEP_CLIMATES} order. */
  private static final List<String> SHEEP_BIOMES =
      List.of(NAMESPACE + "plains", NAMESPACE + "desert", NAMESPACE + "snowy_plains");

  /** Eggs, by the text that names them, and the chick each hatches. */
  private static final Map<String, Item> EGGS =
      Map.of("egg", Items.EGG, "brown_egg", Items.BROWN_EGG, "blue_egg", Items.BLUE_EGG);

  @GameTest
  public void animalVariantsMatchTheGame(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
    List<String> mismatches = new ArrayList<>();
    for (VariantSpawnRules.BiomeTag tag : VariantSpawnRules.TAGS) {
      Set<String> actual = new TreeSet<>();
      for (Holder<Biome> biome :
          biomes.getTagOrEmpty(TagKey.create(Registries.BIOME, Identifier.parse(tag.tagId())))) {
        actual.add(biome.getRegisteredName());
      }
      compare("tag " + tag.tagId(), tag.biomeIds(), actual, mismatches);
    }
    for (VariantSpawnRules.VariantRegistry registry : VariantSpawnRules.REGISTRIES) {
      checkRegistry(level, registry, mismatches);
    }
    checkCodeLooks(mismatches);
    checkEggs(mismatches);
    checkSheep(biomes, mismatches);
    checkPandas(mismatches);
    helper.assertTrue(
        mismatches.isEmpty(),
        "animal variants differ from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  /** Same variants, each with the same selectors (priority and condition). */
  private static void checkRegistry(
      ServerLevel level, VariantSpawnRules.VariantRegistry table, List<String> mismatches) {
    ResourceKey<? extends Registry<?>> key =
        ResourceKey.createRegistryKey(Identifier.parse(table.registryId()));
    Registry<?> registry = level.registryAccess().lookupOrThrow(key);
    Map<String, List<String>> actual = new HashMap<>();
    for (Holder.Reference<?> entry : registry.listElements().toList()) {
      List<String> selectors = new ArrayList<>();
      if (entry.value() instanceof PriorityProvider<?, ?> provider) {
        for (PriorityProvider.Selector<?, ?> selector : provider.selectors()) {
          selectors.add(describe(selector.condition(), selector.priority()));
        }
      }
      actual.put(entry.key().identifier().toString(), selectors);
    }
    Map<String, List<String>> expected = new HashMap<>();
    for (VariantSpawnRules.Variant variant : table.variants()) {
      expected.put(
          variant.variantId(),
          variant.selectors().stream()
              .map(selector -> describe(selector.condition(), selector.priority()))
              .toList());
    }
    if (!expected.equals(actual)) {
      mismatches.add(table.registryId() + ": table " + expected + "\n  game " + actual);
    }
  }

  private static String describe(Optional<?> condition, int priority) {
    String what =
        switch (condition.orElse(null)) {
          case null -> "always";
          case BiomeCheck biomes -> "biomes " + ids(biomes.requiredBiomes());
          case StructureCheck structures -> "structures " + ids(structures.requiredStructures());
          case MoonBrightnessCheck moon ->
              "moon " + moon.range().min().orElse(null) + ".." + moon.range().max().orElse(null);
          default -> "unknown " + condition.get();
        };
    return what + " @" + priority;
  }

  private static String describe(VariantSpawnRules.Condition condition, int priority) {
    String what =
        switch (condition) {
          case VariantSpawnRules.Always always -> "always";
          case VariantSpawnRules.InBiomes biomes -> "biomes " + new TreeSet<>(biomes.biomeIds());
          case VariantSpawnRules.InStructures structures ->
              "structures " + new TreeSet<>(structures.structureIds());
          case VariantSpawnRules.MoonBrightness moon -> "moon " + moon.min() + "..null";
        };
    return what + " @" + priority;
  }

  private static Set<String> ids(HolderSet<?> set) {
    Set<String> ids = new TreeSet<>();
    for (Holder<?> holder : set) {
      ids.add(holder.getRegisteredName());
    }
    return ids;
  }

  /** Every look the card names in code exists, and every look the code has is on the card. */
  private static void checkCodeLooks(List<String> mismatches) {
    compareNames("rabbit", Rabbit.Variant.values(), mismatches);
    compareNames("fox", Fox.Variant.values(), mismatches);
    compareNames("axolotl", Axolotl.Variant.values(), mismatches);
    compareNames("mooshroom", MushroomCow.Variant.values(), mismatches);
    compareNames("panda", Panda.Gene.values(), mismatches);
    compareNames("sheep-natural", DyeColor.values(), mismatches);
    compare("horse coats", AnimalVariants.HORSE_COATS, names(Variant.values()), mismatches);
    compare("llama coats", AnimalVariants.LLAMA_COATS, names(Llama.Variant.values()), mismatches);
    Set<String> recessive = new TreeSet<>();
    for (Panda.Gene gene : Panda.Gene.values()) {
      if (gene.isRecessive()) {
        recessive.add(gene.getSerializedName());
      }
    }
    compare("recessive panda genes", AnimalVariants.RECESSIVE_GENES, recessive, mismatches);
    RandomSource random = RandomSource.create(SEED);
    Set<String> common = new TreeSet<>();
    Set<String> rare = new TreeSet<>();
    for (int sample = 0; sample < AXOLOTL_SAMPLES; sample++) {
      common.add(Axolotl.Variant.getCommonSpawnVariant(random).getSerializedName());
      rare.add(Axolotl.Variant.getRareSpawnVariant(random).getSerializedName());
    }
    compare("spawning axolotls", AnimalVariants.COMMON_AXOLOTLS, common, mismatches);
    compare("bred-only axolotls", List.of("blue"), rare, mismatches);
    if (Axolotl.RARE_VARIANT_CHANCE != AnimalVariants.BLUE_AXOLOTL_IN) {
      mismatches.add("blue axolotl: 1 in " + Axolotl.RARE_VARIANT_CHANCE);
    }
    if (Goat.GOAT_SCREAMING_CHANCE * PERCENT != AnimalVariants.SCREAMING_GOAT.percent()) {
      mismatches.add("screaming goat: " + Goat.GOAT_SCREAMING_CHANCE);
    }
  }

  /**
   * The card's looks of {@code animal} (or, for sheep, its natural colours) exist in {@code
   * values}; for every animal but sheep, every value is on the card.
   */
  private static <E extends Enum<E> & StringRepresentable> void compareNames(
      String animal, E[] values, List<String> mismatches) {
    boolean sheep = animal.startsWith("sheep");
    String entityId = NAMESPACE + (sheep ? "sheep" : animal);
    String cardId = animal.equals("mooshroom") ? NAMESPACE + "cow" : entityId;
    Set<String> card = new TreeSet<>();
    for (AnimalVariant variant : AnimalVariants.of(cardId).orElseThrow().variants()) {
      if (variant.entityId().equals(entityId)) {
        card.add(variant.variantId());
      }
    }
    Set<String> game = names(values);
    if (sheep ? !game.containsAll(card) : !card.equals(game)) {
      mismatches.add(animal + ": card " + card + ", game " + game);
    }
  }

  private static <E extends Enum<E> & StringRepresentable> Set<String> names(E[] values) {
    Set<String> names = new TreeSet<>();
    for (E value : values) {
      names.add(value.getSerializedName());
    }
    return names;
  }

  /** The egg each chicken's text names hatches that chicken. */
  private static void checkEggs(List<String> mismatches) {
    for (AnimalVariant chicken :
        AnimalVariants.of(NAMESPACE + "chicken").orElseThrow().variants()) {
      for (AnimalVariant.Origin origin : chicken.origins()) {
        if (origin instanceof AnimalVariant.Told told && EGGS.containsKey(told.key())) {
          Holder<ChickenVariant> hatched =
              new ItemStack(EGGS.get(told.key())).get(DataComponents.CHICKEN_VARIANT);
          String id = hatched == null ? "none" : hatched.getRegisteredName();
          if (!id.equals(chicken.variantId())) {
            mismatches.add(told.key() + ": hatches " + id + ", not " + chicken.variantId());
          }
        }
      }
    }
  }

  /** Each climate's colours, sampled, match {@link AnimalVariants.SheepClimate#chanceOf}. */
  private static void checkSheep(Registry<Biome> biomes, List<String> mismatches) {
    RandomSource random = RandomSource.create(SEED);
    for (int index = 0; index < SHEEP_BIOMES.size(); index++) {
      AnimalVariants.SheepClimate climate = AnimalVariants.SHEEP_CLIMATES.get(index);
      Holder<Biome> biome = biomes.get(Identifier.parse(SHEEP_BIOMES.get(index))).orElseThrow();
      Map<DyeColor, Integer> counts = new EnumMap<>(DyeColor.class);
      for (int sample = 0; sample < SAMPLES; sample++) {
        counts.merge(SheepColorSpawnRules.getSheepColor(biome, random), 1, Integer::sum);
      }
      for (DyeColor color : DyeColor.values()) {
        checkShare(
            "sheep " + climate.climate() + " " + color.getSerializedName(),
            climate.chanceOf(color.getSerializedName()).percent(),
            counts.getOrDefault(color, 0),
            mismatches);
      }
    }
  }

  /** Each gene, sampled, matches {@link AnimalVariants#PANDA_GENES}. */
  private static void checkPandas(List<String> mismatches) {
    RandomSource random = RandomSource.create(SEED);
    Map<String, Integer> counts = new HashMap<>();
    for (int sample = 0; sample < SAMPLES; sample++) {
      counts.merge(Panda.Gene.getRandom(random).getSerializedName(), 1, Integer::sum);
    }
    int total = AnimalVariants.PANDA_GENES.stream().mapToInt(AnimalVariants.Weighted::weight).sum();
    for (AnimalVariants.Weighted gene : AnimalVariants.PANDA_GENES) {
      checkShare(
          "panda gene " + gene.id(),
          gene.weight() * PERCENT / total,
          counts.getOrDefault(gene.id(), 0),
          mismatches);
    }
    Stream.of(Panda.Gene.values())
        .map(Panda.Gene::getSerializedName)
        .filter(id -> AnimalVariants.PANDA_GENES.stream().noneMatch(gene -> gene.id().equals(id)))
        .forEach(id -> mismatches.add("panda gene " + id + " not in the table"));
  }

  private static void checkShare(String what, double percent, int count, List<String> mismatches) {
    double expected = percent / PERCENT;
    double deviation = Math.sqrt(expected * (1 - expected) / SAMPLES);
    double actual = (double) count / SAMPLES;
    if (Math.abs(actual - expected) > DEVIATIONS * deviation + Double.MIN_VALUE) {
      mismatches.add(what + ": table " + percent + "%, sampled " + actual * PERCENT + "%");
    }
  }

  private static void compare(
      String what, List<String> expected, Set<String> actual, List<String> mismatches) {
    if (!new TreeSet<>(expected).equals(actual)) {
      mismatches.add(what + ": table " + new TreeSet<>(expected) + ", game " + actual);
    }
  }
}
