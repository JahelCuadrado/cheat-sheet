package com.einsteinmods.cheatsheet.core.farming;

import java.util.List;
import java.util.Optional;

/**
 * One look of a breeding card's animal (a cow's climate, a wolf's coat, a panda's personality) and
 * where or how it comes: what the zoomed card's variant grid shows.
 *
 * @param entityId the animal
 * @param variantId the look as the game names it: a registry id ({@code minecraft:cold}), an enum's
 *     serialized name ({@code white_splotched}) or a dye colour; the client applies it
 * @param nameKey the translation key of its name
 * @param customName a name tag that gives the look (a rabbit named "Toast"), if any
 * @param origins where or how it comes, most common first
 */
public record AnimalVariant(
    String entityId,
    String variantId,
    String nameKey,
    Optional<String> customName,
    List<Origin> origins) {
  public AnimalVariant {
    origins = List.copyOf(origins);
    if (origins.isEmpty()) {
      throw new IllegalArgumentException(entityId + " " + variantId + " comes from nowhere");
    }
  }

  /** Where or how a variant comes. */
  public sealed interface Origin permits InBiomes, Told {
    /** The share of animals coming this way that have the look, when it is a matter of luck. */
    Optional<Chance> chance();
  }

  /** How a variant comes in its biomes. */
  public enum BiomeWay {
    /** Spawns this way in the biomes. */
    SPAWNS,
    /** A tadpole that grows up in the biomes becomes it. */
    GROWS
  }

  /**
   * In these biomes.
   *
   * @param way spawns there, or grows up there
   * @param labelKey names the biomes as a group ({@code climate.<key>}), if they are one
   * @param biomeIds the biomes, in the order the animal's spawn table lists them
   * @param chance the share of spawns there with the look, if not all
   */
  public record InBiomes(
      BiomeWay way, Optional<String> labelKey, List<String> biomeIds, Optional<Chance> chance)
      implements Origin {
    public InBiomes {
      biomeIds = List.copyOf(biomeIds);
      if (biomeIds.isEmpty()) {
        throw new IllegalArgumentException("no biomes");
      }
    }
  }

  /**
   * Some other way, only told ({@code how.<key>}): written in the vanilla code the table cites.
   *
   * @param key names the text
   * @param chance how likely, if it is a matter of luck
   */
  public record Told(String key, Optional<Chance> chance) implements Origin {}

  /**
   * A probability as an exact fraction.
   *
   * @param numerator the favourable share
   * @param denominator the whole, above zero
   */
  public record Chance(long numerator, long denominator) {
    private static final double PERCENT = 100.0;

    public Chance {
      if (denominator <= 0 || numerator < 0 || numerator > denominator) {
        throw new IllegalArgumentException("bad chance " + numerator + "/" + denominator);
      }
      long divisor = gcd(numerator, denominator);
      numerator /= divisor;
      denominator /= divisor;
    }

    /** The chance as a percentage, 0 to 100. */
    public double percent() {
      return numerator * PERCENT / denominator;
    }

    /** This chance and then {@code other}, both. */
    public Chance times(Chance other) {
      return new Chance(numerator * other.numerator, denominator * other.denominator);
    }

    private static long gcd(long first, long second) {
      return second == 0 ? Math.max(1, first) : gcd(second, first % second);
    }
  }
}
