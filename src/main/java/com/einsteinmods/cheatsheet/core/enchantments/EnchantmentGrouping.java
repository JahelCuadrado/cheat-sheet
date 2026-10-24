package com.einsteinmods.cheatsheet.core.enchantments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sorts enchantments into the Enchantments tab's cards from the gear they support, so datapack and
 * modded enchantments land somewhere too. An enchantment goes on the most general cards that fit:
 * one fitting every gear goes only on {@link GearCard#ANY}, one fitting every armor piece only on
 * {@link GearCard#ARMOR}, otherwise on each gear's card; one fitting no tested gear on {@link
 * GearCard#OTHER}.
 */
public final class EnchantmentGrouping {
  /** Inside a card: normal enchantments, then treasure, then curses; then vanilla tooltip order. */
  static final Comparator<EnchantmentFacts> CARD_ORDER =
      Comparator.comparing(EnchantmentFacts::curse)
          .thenComparing(EnchantmentFacts::treasure)
          .thenComparingInt(EnchantmentFacts::order)
          .thenComparing(EnchantmentFacts::id);

  private EnchantmentGrouping() {}

  /**
   * Cards with their enchantments, in card order; empty cards left out.
   *
   * @param enchantments every enchantment of the registry
   * @param available gear whose item exists in this game; the others count as absent
   */
  public static Map<GearCard, List<EnchantmentFacts>> group(
      List<EnchantmentFacts> enchantments, Set<Gear> available) {
    Map<GearCard, List<EnchantmentFacts>> byCard = new EnumMap<>(GearCard.class);
    for (EnchantmentFacts enchantment : enchantments) {
      for (GearCard card : cardsFor(enchantment.supported(), available)) {
        byCard.computeIfAbsent(card, unused -> new ArrayList<>()).add(enchantment);
      }
    }
    Map<GearCard, List<EnchantmentFacts>> cards = new LinkedHashMap<>();
    for (Map.Entry<GearCard, List<EnchantmentFacts>> entry : byCard.entrySet()) {
      List<EnchantmentFacts> sorted = new ArrayList<>(entry.getValue());
      sorted.sort(CARD_ORDER);
      cards.put(entry.getKey(), List.copyOf(sorted));
    }
    return Collections.unmodifiableMap(cards);
  }

  /** Cards for an enchantment supporting {@code supported}, general cards first. */
  static List<GearCard> cardsFor(Set<Gear> supported, Set<Gear> available) {
    Set<Gear> fitting = EnumSet.noneOf(Gear.class);
    for (Gear gear : supported) {
      if (available.contains(gear)) {
        fitting.add(gear);
      }
    }
    List<GearCard> cards = new ArrayList<>();
    Set<Gear> covered = EnumSet.noneOf(Gear.class);
    for (GearCard card : GearCard.values()) {
      Set<Gear> cardGears = EnumSet.noneOf(Gear.class);
      for (Gear gear : card.gears()) {
        if (available.contains(gear)) {
          cardGears.add(gear);
        }
      }
      if (!cardGears.isEmpty()
          && fitting.containsAll(cardGears)
          && !covered.containsAll(cardGears)) {
        cards.add(card);
        covered.addAll(cardGears);
      }
    }
    if (cards.isEmpty()) {
      cards.add(GearCard.OTHER);
    }
    return List.copyOf(cards);
  }
}
