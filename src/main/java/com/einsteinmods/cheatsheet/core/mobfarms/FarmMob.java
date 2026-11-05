package com.einsteinmods.cheatsheet.core.mobfarms;

import java.util.Optional;

/**
 * A mob standing in a farm's picture, in the diorama's block units.
 *
 * @param entityId the entity type id ({@code minecraft:zombie})
 * @param x east
 * @param y up (the top of a floor at y = 0 is 1)
 * @param z south
 * @param heldItemId an item in its main hand, if any
 * @param size a slime's or magma cube's size, 0 for other mobs
 */
public record FarmMob(
    String entityId, double x, double y, double z, Optional<String> heldItemId, int size) {
  /** A mob with empty hands. */
  public static FarmMob of(String entityId, double x, double y, double z) {
    return new FarmMob(entityId, x, y, z, Optional.empty(), 0);
  }

  /** The same mob holding {@code itemId}. */
  public FarmMob holding(String itemId) {
    return new FarmMob(entityId, x, y, z, Optional.of(itemId), size);
  }

  /** The same slime or magma cube at {@code cubeSize}. */
  public FarmMob sized(int cubeSize) {
    return new FarmMob(entityId, x, y, z, heldItemId, cubeSize);
  }
}
