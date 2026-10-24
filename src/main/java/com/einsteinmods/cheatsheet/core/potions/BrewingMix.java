package com.einsteinmods.cheatsheet.core.potions;

/**
 * One brewing stand recipe between drinkable potions: {@code input} potion plus {@code reagent}
 * item gives {@code output}. Vanilla repeats each one for splash and lingering potions.
 *
 * @param input potion id ({@code minecraft:} namespace implied)
 * @param reagent item id put in the top slot ({@code minecraft:} namespace implied)
 * @param output potion id ({@code minecraft:} namespace implied)
 */
public record BrewingMix(String input, String reagent, String output) {}
