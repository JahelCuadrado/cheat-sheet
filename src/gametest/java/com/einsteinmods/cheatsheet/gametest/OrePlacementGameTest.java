package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.ores.BiomeRule;
import com.einsteinmods.cheatsheet.core.ores.HeightShape;
import com.einsteinmods.cheatsheet.core.ores.OreDimension;
import com.einsteinmods.cheatsheet.core.ores.OreRow;
import com.einsteinmods.cheatsheet.core.ores.OreTable;
import com.einsteinmods.cheatsheet.core.ores.PlacementCount;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.jspecify.annotations.Nullable;

/**
 * The cheat sheet's fixed ore table ({@link OreTable}) against the game's own placed features: a
 * Minecraft update that moves an ore, changes its count or its biomes fails the build here, so the
 * table (and the Ores tab) gets updated with it.
 *
 * <p>Placement modifiers are read through their codec ({@code PlacementModifier.CODEC} to JSON):
 * {@code count} (constant or {@code uniform} {@code IntProvider}), {@code rarity_filter}, {@code
 * height_range} with a {@code uniform} or {@code trapezoid} height provider whose anchors are
 * {@code absolute}, {@code above_bottom} or {@code below_top} ({@code VerticalAnchor}), resolved as
 * {@code WorldGenerationContext} does with the dimension's noise settings.
 */
public final class OrePlacementGameTest {
  private static final String TYPE = "type";
  private static final String COUNT_TYPE = "minecraft:count";
  private static final String RARITY_TYPE = "minecraft:rarity_filter";
  private static final String HEIGHT_RANGE_TYPE = "minecraft:height_range";
  private static final String UNIFORM_TYPE = "minecraft:uniform";
  private static final String TRAPEZOID_TYPE = "minecraft:trapezoid";

  @GameTest
  public void oreTableMatchesTheGame(GameTestHelper helper) {
    RegistryAccess registries = helper.getLevel().registryAccess();
    List<String> mismatches = new ArrayList<>();
    checkDimension(
        registries, NoiseGeneratorSettings.OVERWORLD, OreDimension.OVERWORLD, mismatches);
    checkDimension(registries, NoiseGeneratorSettings.NETHER, OreDimension.NETHER, mismatches);

    Registry<PlacedFeature> placedFeatures = registries.lookupOrThrow(Registries.PLACED_FEATURE);
    Registry<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);
    Map<OreDimension, Set<String>> everyBiome =
        Map.of(
            OreDimension.OVERWORLD,
            biomesCarryingAll(placedFeatures, biomes, OreDimension.OVERWORLD),
            OreDimension.NETHER,
            biomesCarryingAll(placedFeatures, biomes, OreDimension.NETHER));
    for (OreRow row : OreTable.ROWS) {
      Holder<PlacedFeature> feature = placedFeature(placedFeatures, row.featureId());
      if (feature == null) {
        mismatches.add(row.featureId() + ": no such placed feature");
        continue;
      }
      checkPlacement(row, feature.value(), mismatches);
      checkBiomes(row, feature, biomes, everyBiome.get(row.ore().dimension()), mismatches);
    }
    helper.assertTrue(
        mismatches.isEmpty(), "ore table differs from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  private static void checkDimension(
      RegistryAccess registries,
      ResourceKey<NoiseGeneratorSettings> key,
      OreDimension dimension,
      List<String> mismatches) {
    NoiseSettings noise =
        registries.lookupOrThrow(Registries.NOISE_SETTINGS).getValueOrThrow(key).noiseSettings();
    if (noise.minY() != dimension.genMinY() || noise.height() != dimension.genDepth()) {
      mismatches.add(dimension + ": game has minY " + noise.minY() + ", height " + noise.height());
    }
  }

  private static void checkPlacement(OreRow row, PlacedFeature feature, List<String> mismatches) {
    OreDimension dimension = row.ore().dimension();
    int minCount = 1;
    int maxCount = 1;
    int oneInChunks = 1;
    @Nullable JsonObject height = null;
    for (PlacementModifier modifier : feature.placement()) {
      JsonObject json =
          PlacementModifier.CODEC
              .encodeStart(JsonOps.INSTANCE, modifier)
              .getOrThrow()
              .getAsJsonObject();
      String type = json.get(TYPE).getAsString();
      switch (type) {
        case COUNT_TYPE -> {
          JsonElement count = json.get("count");
          if (count.isJsonPrimitive()) {
            minCount = count.getAsInt();
            maxCount = minCount;
          } else {
            JsonObject provider = count.getAsJsonObject();
            if (!UNIFORM_TYPE.equals(provider.get(TYPE).getAsString())) {
              mismatches.add(row.featureId() + ": unexpected count provider " + provider);
            }
            minCount = provider.get("min_inclusive").getAsInt();
            maxCount = provider.get("max_inclusive").getAsInt();
          }
        }
        case RARITY_TYPE -> oneInChunks = json.get("chance").getAsInt();
        case HEIGHT_RANGE_TYPE -> height = json.getAsJsonObject("height");
        default -> {
          // Spread in the chunk, biome filter: they do not change heights or counts.
        }
      }
    }
    PlacementCount count = new PlacementCount(minCount, maxCount, oneInChunks);
    if (!count.equals(row.count())) {
      mismatches.add(row.featureId() + ": game count " + count + ", table " + row.count());
    }
    if (height == null) {
      mismatches.add(row.featureId() + ": no height range");
      return;
    }
    String heightType = height.get(TYPE).getAsString();
    HeightShape shape =
        switch (heightType) {
          case UNIFORM_TYPE -> HeightShape.UNIFORM;
          case TRAPEZOID_TYPE -> HeightShape.TRIANGLE;
          default -> null;
        };
    int plateau = height.has("plateau") ? height.get("plateau").getAsInt() : 0;
    int minY = resolve(height.getAsJsonObject("min_inclusive"), dimension);
    int maxY = resolve(height.getAsJsonObject("max_inclusive"), dimension);
    if (shape != row.shape() || plateau != 0 || minY != row.minY() || maxY != row.maxY()) {
      mismatches.add(
          row.featureId()
              + ": game height "
              + heightType
              + "(plateau "
              + plateau
              + ") "
              + minY
              + ".."
              + maxY
              + ", table "
              + row.shape()
              + " "
              + row.minY()
              + ".."
              + row.maxY());
    }
  }

  /** {@code VerticalAnchor.resolveY} for a dimension's generation limits. */
  private static int resolve(JsonObject anchor, OreDimension dimension) {
    if (anchor.has("absolute")) {
      return anchor.get("absolute").getAsInt();
    }
    if (anchor.has("above_bottom")) {
      return dimension.genMinY() + anchor.get("above_bottom").getAsInt();
    }
    if (anchor.has("below_top")) {
      return dimension.genDepth() - 1 + dimension.genMinY() - anchor.get("below_top").getAsInt();
    }
    throw new IllegalArgumentException("unknown vertical anchor " + anchor);
  }

  private static void checkBiomes(
      OreRow row,
      Holder<PlacedFeature> feature,
      Registry<Biome> biomes,
      Set<String> everyBiome,
      List<String> mismatches) {
    Set<String> carrying = biomesCarrying(feature, biomes, row.ore().dimension());
    BiomeRule rule = row.biomes();
    Set<String> expected =
        switch (rule.kind()) {
          case ALL -> everyBiome;
          case ONLY -> Set.copyOf(rule.biomes());
          case EXCEPT -> {
            Set<String> remaining = new HashSet<>(everyBiome);
            rule.biomes().forEach(remaining::remove);
            yield remaining;
          }
        };
    if (!carrying.equals(expected)) {
      mismatches.add(row.featureId() + ": game biomes " + carrying + ", table " + expected);
    }
  }

  /**
   * Biomes of the dimension carrying every placement the table marks as found in all biomes; the
   * table's "all" means this set (biomes without ores, such as the void, stay out).
   */
  private static Set<String> biomesCarryingAll(
      Registry<PlacedFeature> placedFeatures, Registry<Biome> biomes, OreDimension dimension) {
    Set<String> common = null;
    for (OreRow row : OreTable.ROWS) {
      if (row.ore().dimension() != dimension || row.biomes().kind() != BiomeRule.Kind.ALL) {
        continue;
      }
      Holder<PlacedFeature> feature = placedFeature(placedFeatures, row.featureId());
      if (feature == null) {
        continue;
      }
      Set<String> carrying = biomesCarrying(feature, biomes, dimension);
      if (common == null) {
        common = new HashSet<>(carrying);
      } else {
        common.retainAll(carrying);
      }
    }
    return common == null ? Set.of() : Set.copyOf(common);
  }

  private static Set<String> biomesCarrying(
      Holder<PlacedFeature> feature, Registry<Biome> biomes, OreDimension dimension) {
    TagKey<Biome> dimensionTag =
        dimension == OreDimension.OVERWORLD ? BiomeTags.IS_OVERWORLD : BiomeTags.IS_NETHER;
    Set<String> carrying = new HashSet<>();
    biomes
        .listElements()
        .filter(biome -> biome.is(dimensionTag))
        .forEach(
            biome -> {
              for (HolderSet<PlacedFeature> step :
                  biome.value().getGenerationSettings().features()) {
                if (step.contains(feature)) {
                  carrying.add(biome.key().identifier().getPath());
                  return;
                }
              }
            });
    return carrying;
  }

  private static @Nullable Holder<PlacedFeature> placedFeature(
      Registry<PlacedFeature> placedFeatures, String featureId) {
    return placedFeatures.get(Identifier.withDefaultNamespace(featureId)).orElse(null);
  }
}
