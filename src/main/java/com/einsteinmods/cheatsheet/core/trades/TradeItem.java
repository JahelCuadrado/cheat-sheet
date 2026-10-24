package com.einsteinmods.cheatsheet.core.trades;

import java.util.Optional;

/**
 * Items on one side of a trade.
 *
 * @param itemId item id ({@code minecraft:} namespace implied)
 * @param count how many
 * @param potion potion id when the item must be (or is) that potion, such as a water bottle
 */
public record TradeItem(String itemId, int count, Optional<String> potion) {
  public TradeItem {
    if (count < 0) {
      throw new IllegalArgumentException(itemId + ": negative count " + count);
    }
  }

  public static TradeItem of(String itemId, int count) {
    return new TradeItem(itemId, count, Optional.empty());
  }

  public TradeItem withPotion(String potionId) {
    return new TradeItem(itemId, count, Optional.of(potionId));
  }
}
