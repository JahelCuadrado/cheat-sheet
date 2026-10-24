package com.einsteinmods.cheatsheet.core.trades;

import java.util.List;
import java.util.Optional;

/**
 * One vanilla trade ({@code data/minecraft/villager_trade}).
 *
 * @param id trade id ({@code minecraft:} namespace implied)
 * @param wants what the merchant asks for
 * @param extraWants a second item it also asks for (a book, a compass)
 * @param gives what the player gets
 * @param kind what happens to the given item
 * @param villagerTypes villager types that offer it ({@code merchant_predicate}); empty: all
 */
public record TradeRow(
    String id,
    TradeItem wants,
    Optional<TradeItem> extraWants,
    TradeItem gives,
    TradeKind kind,
    List<String> villagerTypes) {
  public TradeRow {
    villagerTypes = List.copyOf(villagerTypes);
  }

  public TradeRow as(TradeKind newKind) {
    return new TradeRow(id, wants, extraWants, gives, newKind, villagerTypes);
  }

  public TradeRow onlyFrom(String... types) {
    return new TradeRow(id, wants, extraWants, gives, kind, List.of(types));
  }

  public TradeRow wantsPotion(String potionId) {
    return new TradeRow(id, wants.withPotion(potionId), extraWants, gives, kind, villagerTypes);
  }

  /** The given item is this potion ({@link TradeKind#POTION}). */
  public TradeRow givesPotion(String potionId) {
    return new TradeRow(
        id, wants, extraWants, gives.withPotion(potionId), TradeKind.POTION, villagerTypes);
  }

  /** What the first wanted item really costs, extra cost of enchantments included. */
  public CountRange price() {
    return TradePrices.price(wants.count(), kind);
  }
}
