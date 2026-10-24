package com.einsteinmods.cheatsheet.core.food;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Orders foods for the sheet and formats their values. */
public final class FoodRanking {
  /** Hunger points per drumstick in the hunger bar. */
  public static final int POINTS_PER_DRUMSTICK = 2;

  /** Most filling first; then most saturating; then by id, so the order is stable. */
  public static final Comparator<FoodValues> BEST_FIRST =
      Comparator.comparingInt(FoodValues::nutrition)
          .reversed()
          .thenComparing(Comparator.comparingDouble(FoodValues::saturation).reversed())
          .thenComparing(FoodValues::id);

  private static final String HALF = ".5";
  private static final String ONE_DECIMAL = "%.1f";
  private static final float WHOLE_TOLERANCE = 0.05F;

  private FoodRanking() {}

  public static List<FoodValues> sorted(List<FoodValues> foods) {
    List<FoodValues> sorted = new ArrayList<>(foods);
    sorted.sort(BEST_FIRST);
    return List.copyOf(sorted);
  }

  /**
   * Ids of the foods holding the {@code places} highest saturation values (ties share a place): the
   * ones that keep you full longest.
   */
  public static Set<String> bestSaturation(List<FoodValues> foods, int places) {
    TreeSet<Float> values = new TreeSet<>(Comparator.reverseOrder());
    for (FoodValues food : foods) {
      values.add(food.saturation());
    }
    Set<Float> top = new HashSet<>();
    for (Float value : values) {
      if (top.size() == places) {
        break;
      }
      top.add(value);
    }
    Set<String> ids = new HashSet<>();
    for (FoodValues food : foods) {
      if (top.contains(food.saturation())) {
        ids.add(food.id());
      }
    }
    return Set.copyOf(ids);
  }

  /** Drumsticks filled: {@code 4}, {@code 2.5}. */
  public static String drumsticks(int nutrition) {
    int whole = nutrition / POINTS_PER_DRUMSTICK;
    return nutrition % POINTS_PER_DRUMSTICK == 0 ? Integer.toString(whole) : whole + HALF;
  }

  /** Saturation with one decimal, none when whole: {@code 12.8}, {@code 6}. */
  public static String saturation(float saturation) {
    long rounded = Math.round(saturation);
    if (Math.abs(saturation - rounded) < WHOLE_TOLERANCE) {
      return Long.toString(rounded);
    }
    return String.format(Locale.ROOT, ONE_DECIMAL, saturation);
  }
}
