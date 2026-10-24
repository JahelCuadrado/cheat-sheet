package com.einsteinmods.cheatsheet.core.trades;

import java.util.List;

/**
 * One trade set: the trades a merchant draws from at one level ({@code data/minecraft/trade_set}).
 *
 * @param tradeSetId trade set id ({@code minecraft:} namespace implied), such as {@code
 *     armorer/level_1}
 * @param amount trades drawn from the set ({@code amount})
 * @param trades the set's trades, in the game's order
 */
public record TradeTier(String tradeSetId, int amount, List<TradeRow> trades) {
  public TradeTier {
    trades = List.copyOf(trades);
    if (trades.isEmpty() || amount < 1) {
      throw new IllegalArgumentException(tradeSetId + ": empty tier");
    }
  }

  /** Offers a merchant gets from this tier: the amount, unless the set has fewer trades. */
  public int offers() {
    return Math.min(amount, trades.size());
  }
}
