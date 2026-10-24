package com.einsteinmods.cheatsheet.core.enchantments;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnchantmentGroupingTest {
  private static final Set<Gear> ALL_GEAR = EnumSet.allOf(Gear.class);
  private static final Set<Gear> ARMOR =
      EnumSet.of(Gear.HELMET, Gear.CHESTPLATE, Gear.LEGGINGS, Gear.BOOTS);

  @Test
  void gearEverywhereGoesOnlyOnTheSharedCard() {
    assertEquals(List.of(GearCard.ANY), EnchantmentGrouping.cardsFor(ALL_GEAR, ALL_GEAR));
  }

  @Test
  void everyArmorPieceGoesOnlyOnTheArmorCard() {
    assertEquals(List.of(GearCard.ARMOR), EnchantmentGrouping.cardsFor(ARMOR, ALL_GEAR));
  }

  @Test
  void someGearGoesOnEachOfItsCards() {
    // Sharpness: swords, spears and axes.
    assertEquals(
        List.of(GearCard.SWORD, GearCard.SPEAR, GearCard.AXE),
        EnchantmentGrouping.cardsFor(EnumSet.of(Gear.SWORD, Gear.SPEAR, Gear.AXE), ALL_GEAR));
    // Armor plus one more gear: the armor card and that gear's card.
    Set<Gear> armorAndMace = EnumSet.copyOf(ARMOR);
    armorAndMace.add(Gear.MACE);
    assertEquals(
        List.of(GearCard.ARMOR, GearCard.MACE),
        EnchantmentGrouping.cardsFor(armorAndMace, ALL_GEAR));
  }

  @Test
  void noTestedGearGoesOnTheOtherCard() {
    assertEquals(List.of(GearCard.OTHER), EnchantmentGrouping.cardsFor(Set.of(), ALL_GEAR));
  }

  @Test
  void missingGearDoesNotStopTheSharedCard() {
    Set<Gear> withoutSpear = EnumSet.allOf(Gear.class);
    withoutSpear.remove(Gear.SPEAR);
    assertEquals(List.of(GearCard.ANY), EnchantmentGrouping.cardsFor(withoutSpear, withoutSpear));
    assertEquals(
        List.of(GearCard.OTHER),
        EnchantmentGrouping.cardsFor(EnumSet.of(Gear.SPEAR), withoutSpear));
  }

  @Test
  void cardsKeepDisplayOrderAndSortTheirEnchantments() {
    EnchantmentFacts mending = facts("minecraft:mending", ALL_GEAR, false, true, 42);
    EnchantmentFacts unbreaking = facts("minecraft:unbreaking", ALL_GEAR, false, false, 40);
    EnchantmentFacts vanishing = facts("minecraft:vanishing_curse", ALL_GEAR, true, true, 1);
    EnchantmentFacts power = facts("minecraft:power", EnumSet.of(Gear.BOW), false, false, 10);
    EnchantmentFacts modded =
        facts("other:zap", EnumSet.of(Gear.BOW), false, false, Integer.MAX_VALUE);
    Map<GearCard, List<EnchantmentFacts>> cards =
        EnchantmentGrouping.group(List.of(modded, power, mending, vanishing, unbreaking), ALL_GEAR);
    assertEquals(List.of(GearCard.ANY, GearCard.BOW), List.copyOf(cards.keySet()));
    assertEquals(List.of(unbreaking, mending, vanishing), cards.get(GearCard.ANY));
    assertEquals(List.of(power, modded), cards.get(GearCard.BOW));
  }

  private static EnchantmentFacts facts(
      String id, Set<Gear> supported, boolean curse, boolean treasure, int order) {
    return new EnchantmentFacts(id, supported, curse, treasure, order);
  }
}
