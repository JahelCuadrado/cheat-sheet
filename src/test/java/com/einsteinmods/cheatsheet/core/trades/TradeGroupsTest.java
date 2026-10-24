package com.einsteinmods.cheatsheet.core.trades;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TradeGroupsTest {
  @Test
  void familyDropsColourWoodAndBiomePrefixesLongestFirst() {
    assertEquals("wool", TradeGroups.family("light_blue_wool"));
    assertEquals("wool", TradeGroups.family("blue_wool"));
    assertEquals("boat", TradeGroups.family("dark_oak_boat"));
    assertEquals("village_map", TradeGroups.family("snowy_village_map"));
    assertEquals("coral_block", TradeGroups.family("brain_coral_block"));
    assertEquals("emerald", TradeGroups.family("emerald"));
    assertEquals("packed_ice", TradeGroups.family("packed_ice"));
  }

  @Test
  void shepherdColoursFoldIntoOneRowPerProduct() {
    List<List<TradeRow>> groups = TradeGroups.group(tier("shepherd/level_2").trades());
    // Five dyes bought, sixteen wools and sixteen carpets sold.
    assertEquals(List.of(5, 16, 16), sizes(groups));
  }

  @Test
  void biomeBoatsFoldButKeepTheirVillagerTypes() {
    List<List<TradeRow>> groups = TradeGroups.group(tier("fisherman/level_5").trades());
    assertEquals(List.of(1, 5), sizes(groups));
    assertEquals(List.of("plains"), groups.get(1).getFirst().villagerTypes());
  }

  @Test
  void differentPricesStayApart() {
    // Polished stones cost the same but are different items: no shared family.
    List<List<TradeRow>> groups = TradeGroups.group(tier("mason/level_3").trades());
    assertEquals(7, groups.size());
  }

  private static List<Integer> sizes(List<List<TradeRow>> groups) {
    return groups.stream().map(List::size).toList();
  }

  static TradeTier tier(String tradeSetId) {
    for (MerchantTrades merchant : VillagerTradeTable.all()) {
      for (TradeTier tier : merchant.tiers()) {
        if (tier.tradeSetId().equals(tradeSetId)) {
          return tier;
        }
      }
    }
    throw new AssertionError("no tier " + tradeSetId);
  }
}
