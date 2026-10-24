package com.einsteinmods.cheatsheet.core.trades;

import java.util.List;

/**
 * Everything one merchant can offer.
 *
 * @param merchantId profession id ({@code minecraft:} namespace implied), or {@code
 *     wandering_trader}
 * @param tiers its trade sets: villager levels 1 to 5, or the wandering trader's sets
 */
public record MerchantTrades(String merchantId, List<TradeTier> tiers) {
  public MerchantTrades {
    tiers = List.copyOf(tiers);
  }
}
