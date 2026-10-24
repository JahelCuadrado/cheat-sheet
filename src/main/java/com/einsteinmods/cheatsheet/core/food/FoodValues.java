package com.einsteinmods.cheatsheet.core.food;

/**
 * What eating one item restores ({@code FoodProperties}).
 *
 * @param id item id
 * @param nutrition hunger points (two per drumstick)
 * @param saturation saturation points
 */
public record FoodValues(String id, int nutrition, float saturation) {}
