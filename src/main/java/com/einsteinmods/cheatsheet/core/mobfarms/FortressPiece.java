package com.einsteinmods.cheatsheet.core.mobfarms;

import java.util.List;

/**
 * The pieces of a nether fortress (Minecraft 26.3 {@code NetherFortressPieces}) whose boxes take
 * the fortress' spawn list on any floor ({@code spawn_overrides} with {@code bounding_box: piece}).
 * A mob counts when its feet are inside a piece's box. Sizes are each class's {@code WIDTH}, {@code
 * DEPTH} and {@code HEIGHT}; the floor level is the first air layer of its {@code postProcess}
 * (bridges and the two big rooms lay their floor at layers 3-4, the rest at 0-1). The pillars under
 * the bridges are drawn below the box ({@code fillColumnDown}) and do not count. {@code
 * FarmRulesGameTest} checks the sizes against the game's classes.
 *
 * @param vanillaClasses the {@code NetherFortressPieces} classes of this row
 * @param key the row's lang key suffix
 * @param width blocks across the entrance
 * @param depth blocks along the piece
 * @param height blocks of box, bottom to top
 * @param lowestFloor box layers under the floor level, the lowest along the piece
 * @param highestFloor the same, the highest along it (only the corridor stairs climb)
 */
public record FortressPiece(
    List<String> vanillaClasses,
    String key,
    int width,
    int depth,
    int height,
    int lowestFloor,
    int highestFloor) {
  /** Floor level of the bridges and the two big rooms: their floor is at layers 3 and 4. */
  private static final int HIGH_FLOOR = 5;

  /** Floor level of the castle's corridors and small rooms: their floor is at layers 0 and 1. */
  private static final int LOW_FLOOR = 2;

  /**
   * The corridor stairs: its floor goes from layer 7 down to layer 1 ({@code max(1, 7 - step)}).
   */
  private static final int STAIRS_TOP_FLOOR = 8;

  public static final List<FortressPiece> ALL =
      List.of(
          piece("BridgeStraight", "bridge_straight", 5, 19, 10, HIGH_FLOOR),
          piece("BridgeCrossing", "bridge_crossing", 19, 19, 10, HIGH_FLOOR),
          piece("BridgeEndFiller", "bridge_end", 5, 8, 10, HIGH_FLOOR),
          piece("CastleEntrance", "entrance", 13, 13, 14, HIGH_FLOOR),
          piece("CastleStalkRoom", "wart_room", 13, 13, 14, HIGH_FLOOR),
          piece("MonsterThrone", "blaze_room", 7, 9, 8, LOW_FLOOR),
          piece("RoomCrossing", "room_crossing", 7, 7, 9, LOW_FLOOR),
          piece("StairsRoom", "stairs_room", 7, 7, 11, LOW_FLOOR),
          new FortressPiece(
              List.of(
                  "CastleSmallCorridorPiece",
                  "CastleSmallCorridorCrossingPiece",
                  "CastleSmallCorridorLeftTurnPiece",
                  "CastleSmallCorridorRightTurnPiece"),
              "corridors",
              5,
              5,
              7,
              LOW_FLOOR,
              LOW_FLOOR),
          piece("CastleCorridorTBalconyPiece", "balcony", 9, 9, 7, LOW_FLOOR),
          new FortressPiece(
              List.of("CastleCorridorStairsPiece"),
              "corridor_stairs",
              5,
              10,
              14,
              LOW_FLOOR,
              STAIRS_TOP_FLOOR));

  private static FortressPiece piece(
      String vanillaClass, String key, int width, int depth, int height, int floor) {
    return new FortressPiece(List.of(vanillaClass), key, width, depth, height, floor, floor);
  }

  /** Layers of box from the floor level up, the floor level included, the fewest along it. */
  public int lowestAbove() {
    return height - highestFloor;
  }

  /** The same, the most along it. */
  public int highestAbove() {
    return height - lowestFloor;
  }
}
