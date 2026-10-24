package com.einsteinmods.cheatsheet.core.trades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VillagerTradeTableTest {
  private static final int LEVELS = 5;

  @Test
  void everyProfessionHasFiveLevelsInOrder() {
    assertEquals(13, VillagerTradeTable.PROFESSIONS.size());
    for (MerchantTrades profession : VillagerTradeTable.PROFESSIONS) {
      assertEquals(LEVELS, profession.tiers().size(), profession.merchantId());
      for (int level = 1; level <= LEVELS; level++) {
        assertEquals(
            profession.merchantId() + "/level_" + level,
            profession.tiers().get(level - 1).tradeSetId());
      }
    }
  }

  @Test
  void noTradeRepeatsInsideATier() {
    for (MerchantTrades merchant : VillagerTradeTable.all()) {
      for (TradeTier tier : merchant.tiers()) {
        Set<String> ids = new HashSet<>();
        for (TradeRow trade : tier.trades()) {
          assertTrue(ids.add(trade.id()), tier.tradeSetId() + " repeats " + trade.id());
        }
      }
    }
  }

  @Test
  void offersNeverExceedTheTrades() {
    TradeTier masterLibrarian = TradeGroupsTest.tier("librarian/level_5");
    assertEquals(3, masterLibrarian.amount());
    assertEquals(2, masterLibrarian.offers());
  }
}
