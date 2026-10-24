package com.einsteinmods.cheatsheet.core.food;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FoodRankingTest {
  private static final FoodValues STEAK = new FoodValues("cooked_beef", 8, 12.8F);
  private static final FoodValues PORKCHOP = new FoodValues("cooked_porkchop", 8, 12.8F);
  private static final FoodValues GOLDEN_CARROT = new FoodValues("golden_carrot", 6, 14.4F);
  private static final FoodValues RABBIT_STEW = new FoodValues("rabbit_stew", 10, 12.0F);
  private static final FoodValues BREAD = new FoodValues("bread", 5, 6.0F);

  @Test
  void mostFillingFirstThenMostSaturating() {
    assertEquals(
        List.of(RABBIT_STEW, STEAK, PORKCHOP, GOLDEN_CARROT, BREAD),
        FoodRanking.sorted(List.of(BREAD, GOLDEN_CARROT, PORKCHOP, STEAK, RABBIT_STEW)));
  }

  @Test
  void bestSaturationKeepsTiesTogether() {
    List<FoodValues> foods = List.of(STEAK, PORKCHOP, GOLDEN_CARROT, RABBIT_STEW, BREAD);
    assertEquals(
        Set.of("golden_carrot", "cooked_beef", "cooked_porkchop"),
        FoodRanking.bestSaturation(foods, 2));
  }

  @Test
  void valuesPrintShort() {
    assertEquals("4", FoodRanking.drumsticks(8));
    assertEquals("2.5", FoodRanking.drumsticks(5));
    assertEquals("12.8", FoodRanking.saturation(12.8F));
    assertEquals("6", FoodRanking.saturation(6.0F));
    assertEquals("0.4", FoodRanking.saturation(0.39999998F));
  }
}
