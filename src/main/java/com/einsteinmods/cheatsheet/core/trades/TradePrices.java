package com.einsteinmods.cheatsheet.core.trades;

/**
 * Prices of trades whose given item adds an extra cost ({@code VillagerTrade.getOffer}: the {@code
 * additional_trade_cost} component is added to the first wanted item's count, then clamped to its
 * stack size).
 */
public final class TradePrices {
  /** Stack size of an emerald: no price goes above it ({@code TradeCost.toItemCost}). */
  public static final int MAX_PRICE = 64;

  /** Enchantment levels of enchanted gear, added to the price ({@code enchant_with_levels}). */
  public static final CountRange ENCHANT_LEVELS = new CountRange(5, 19);

  /**
   * Highest level of an enchantment villagers sell as a book ({@code #minecraft:tradeable}); the
   * GameTest checks it.
   */
  public static final int MAX_BOOK_LEVEL = 5;

  /** Mending's only level (data {@code enchantment/mending}); the GameTest checks it. */
  public static final int MENDING_LEVEL = 1;

  /** Treasure enchantments ({@code #minecraft:double_trade_price}) cost twice as much. */
  public static final int TREASURE_FACTOR = 2;

  // EnchantRandomlyFunction: 2 + random(5 + level * 10) + 3 * level.
  private static final int BOOK_BASE = 2;
  private static final int BOOK_SPREAD_BASE = 5;
  private static final int BOOK_SPREAD_PER_LEVEL = 10;
  private static final int BOOK_PER_LEVEL = 3;

  private TradePrices() {}

  /** Price of a trade wanting {@code baseCount} items, enchantment cost included. */
  public static CountRange price(int baseCount, TradeKind kind) {
    return switch (kind) {
      case ENCHANTED_ITEM ->
          clamped(baseCount + ENCHANT_LEVELS.min(), baseCount + ENCHANT_LEVELS.max());
      case ENCHANTED_BOOK -> {
        CountRange book = bookCost();
        yield clamped(baseCount + book.min(), baseCount + book.max());
      }
      default -> CountRange.exactly(Math.min(baseCount, MAX_PRICE));
    };
  }

  /** Extra cost of an enchanted book, from level 1 to {@link #MAX_BOOK_LEVEL}, before clamping. */
  public static CountRange bookCost() {
    return new CountRange(cheapestBookCost(1), dearestBookCost(MAX_BOOK_LEVEL));
  }

  /** Price of a Mending book: its one level's book cost, doubled as a treasure, clamped. */
  public static CountRange mendingBookPrice() {
    return clamped(
        cheapestBookCost(MENDING_LEVEL) * TREASURE_FACTOR,
        dearestBookCost(MENDING_LEVEL) * TREASURE_FACTOR);
  }

  private static int cheapestBookCost(int level) {
    return BOOK_BASE + BOOK_PER_LEVEL * level;
  }

  private static int dearestBookCost(int level) {
    return BOOK_BASE
        + BOOK_SPREAD_BASE
        + level * BOOK_SPREAD_PER_LEVEL
        - 1
        + BOOK_PER_LEVEL * level;
  }

  private static CountRange clamped(int min, int max) {
    return new CountRange(Math.min(min, MAX_PRICE), Math.min(max, MAX_PRICE));
  }
}
