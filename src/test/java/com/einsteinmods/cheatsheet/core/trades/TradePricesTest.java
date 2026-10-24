package com.einsteinmods.cheatsheet.core.trades;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TradePricesTest {
  @Test
  void enchantedGearAddsItsEnchantmentLevelsToThePrice() {
    // Diamond chestplate: 16 emeralds + levels 5..19.
    assertEquals(new CountRange(21, 35), TradePrices.price(16, TradeKind.ENCHANTED_ITEM));
  }

  @Test
  void pricesNeverPassAStackOfEmeralds() {
    assertEquals(new CountRange(55, 64), TradePrices.price(50, TradeKind.ENCHANTED_ITEM));
    assertEquals(CountRange.exactly(64), TradePrices.price(70, TradeKind.PLAIN));
  }

  @Test
  void enchantedBooksCostFiveToSixtyFour() {
    // Level 1: 2 + 0 + 3 at least; level 5: 2 + 54 + 15 = 71 at most, clamped.
    assertEquals(new CountRange(5, 71), TradePrices.bookCost());
    assertEquals(new CountRange(5, 64), TradePrices.price(0, TradeKind.ENCHANTED_BOOK));
    assertEquals(new CountRange(10, 64), TradePrices.treasureBookPrice());
  }

  @Test
  void plainTradesKeepTheirCount() {
    assertEquals(CountRange.exactly(7), TradePrices.price(7, TradeKind.PLAIN));
  }
}
