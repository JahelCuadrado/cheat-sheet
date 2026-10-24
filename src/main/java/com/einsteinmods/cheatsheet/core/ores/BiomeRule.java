package com.einsteinmods.cheatsheet.core.ores;

import java.util.List;

/**
 * Which biomes of its dimension carry a placement.
 *
 * @param kind every biome, only the listed ones, or all but the listed ones
 * @param biomes biome ids ({@code minecraft:} namespace implied), empty for {@link Kind#ALL}
 */
public record BiomeRule(Kind kind, List<String> biomes) {
  public static final BiomeRule ALL = new BiomeRule(Kind.ALL, List.of());

  public enum Kind {
    ALL,
    ONLY,
    EXCEPT
  }

  public BiomeRule {
    biomes = List.copyOf(biomes);
    if ((kind == Kind.ALL) != biomes.isEmpty()) {
      throw new IllegalArgumentException(kind + " with biomes " + biomes);
    }
  }

  public static BiomeRule only(String... biomes) {
    return new BiomeRule(Kind.ONLY, List.of(biomes));
  }

  public static BiomeRule except(String... biomes) {
    return new BiomeRule(Kind.EXCEPT, List.of(biomes));
  }

  /** Only some biomes carry it: a bonus, not where the ore usually is. */
  public boolean isBiomeOnly() {
    return kind == Kind.ONLY;
  }
}
