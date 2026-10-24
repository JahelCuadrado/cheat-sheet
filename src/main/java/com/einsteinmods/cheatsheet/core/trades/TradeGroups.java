package com.einsteinmods.cheatsheet.core.trades;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Folds trades that differ only by an item's colour, wood or biome (sixteen wool colours, a boat
 * per biome) into one group, so the sheet shows one row for them.
 */
public final class TradeGroups {
  private static final List<String> VARIANTS =
      List.of(
          // Dye colours.
          "white",
          "orange",
          "magenta",
          "light_blue",
          "yellow",
          "lime",
          "pink",
          "gray",
          "light_gray",
          "cyan",
          "purple",
          "blue",
          "brown",
          "green",
          "red",
          "black",
          // Woods.
          "oak",
          "spruce",
          "birch",
          "jungle",
          "acacia",
          "dark_oak",
          "mangrove",
          "cherry",
          "pale_oak",
          "poplar",
          "bamboo",
          "crimson",
          "warped",
          // Village biomes (village maps).
          "plains",
          "desert",
          "savanna",
          "snowy",
          "taiga",
          // Corals.
          "brain",
          "bubble",
          "fire",
          "horn",
          "tube");

  private static final String SEPARATOR = "_";

  /** Longest first, so {@code light_blue_} wins over {@code blue_}. */
  private static final List<String> PREFIXES = prefixes();

  private TradeGroups() {}

  /** Groups in order of their first trade; trades keep their order inside a group. */
  public static List<List<TradeRow>> group(List<TradeRow> trades) {
    Map<Key, List<TradeRow>> groups = new LinkedHashMap<>();
    for (TradeRow trade : trades) {
      groups.computeIfAbsent(Key.of(trade), key -> new ArrayList<>()).add(trade);
    }
    List<List<TradeRow>> result = new ArrayList<>(groups.size());
    for (List<TradeRow> group : groups.values()) {
      result.add(List.copyOf(group));
    }
    return List.copyOf(result);
  }

  /** The item id without its colour, wood or biome prefix: {@code white_wool} → {@code wool}. */
  public static String family(String itemId) {
    for (String prefix : PREFIXES) {
      if (itemId.startsWith(prefix) && itemId.length() > prefix.length()) {
        return itemId.substring(prefix.length());
      }
    }
    return itemId;
  }

  private static List<String> prefixes() {
    List<String> prefixes = new ArrayList<>(VARIANTS.size());
    for (String variant : VARIANTS) {
      prefixes.add(variant + SEPARATOR);
    }
    prefixes.sort((first, second) -> Integer.compare(second.length(), first.length()));
    return List.copyOf(prefixes);
  }

  /** What trades of one group share: everything but the variant of their items. */
  private record Key(
      String wants,
      int wantsCount,
      String extraWants,
      int extraCount,
      String gives,
      int givesCount,
      TradeKind kind) {
    private static final String NONE = "";

    static Key of(TradeRow trade) {
      return new Key(
          family(trade.wants().itemId()),
          trade.wants().count(),
          trade.extraWants().map(item -> family(item.itemId())).orElse(NONE),
          trade.extraWants().map(TradeItem::count).orElse(0),
          family(trade.gives().itemId()),
          trade.gives().count(),
          Objects.requireNonNull(trade.kind()));
    }
  }
}
