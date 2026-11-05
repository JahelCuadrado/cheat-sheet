package com.einsteinmods.cheatsheet.core.mobfarms;

import com.einsteinmods.cheatsheet.core.pictures.Diorama;

/**
 * The farms' places in miniature, a floor of 3 × 3 at y = 0 and what makes each place itself (a
 * spawner, a hut on stilts, a portal). Mobs stand on them ({@link FarmMob}); {@code
 * DioramaBlocksGameTest} checks every block against the game.
 */
final class FarmDioramas {
  private static final int SIDE = 3;
  private static final int LAST = SIDE - 1;

  private FarmDioramas() {}

  /** A bare 3 × 3 floor of {@code block}. */
  static Diorama platform(String block) {
    return floor(Diorama.builder(SIDE, 1, SIDE), block).build();
  }

  /** A dungeon's spawner on mossy cobblestone. */
  static Diorama spawner() {
    return floor(Diorama.builder(SIDE, 2, SIDE), "mossy_cobblestone")
        .block("spawner", 1, 1, 1)
        .build();
  }

  /** A villager's bed on grass, for the golem farm. */
  static Diorama ironFarm() {
    return bedOn("grass_block");
  }

  /** A bed on planks, for the breeder. */
  static Diorama breeder() {
    return bedOn("oak_planks");
  }

  /** A village bell on grass, for the raid farm. */
  static Diorama raid() {
    return floor(Diorama.builder(SIDE, 2, SIDE), "grass_block")
        .block("bell[attachment=floor,facing=north]", 0, 1, LAST)
        .build();
  }

  /** The witch hut: a spruce floor on oak stilts, a cauldron at the back. */
  static Diorama witchHut() {
    Diorama.Builder hut = Diorama.builder(SIDE, 3, SIDE);
    for (int x = 0; x < SIDE; x += LAST) {
      for (int z = 0; z < SIDE; z += LAST) {
        hut.block("oak_log", x, 0, z);
      }
    }
    return hut.box("spruce_planks", 0, 1, 0, LAST, 1, LAST).block("cauldron", 0, 2, LAST).build();
  }

  /** The monument: prismarine bricks, a sea lantern and dark prismarine. */
  static Diorama monument() {
    return floor(Diorama.builder(SIDE, 2, SIDE), "prismarine_bricks")
        .block("dark_prismarine", 0, 1, LAST)
        .block("sea_lantern", LAST, 1, LAST)
        .build();
  }

  /** A fortress bridge: nether bricks and a fence. */
  static Diorama fortress() {
    return floor(Diorama.builder(SIDE, 2, SIDE), "nether_bricks")
        .block("nether_brick_fence[east=true,west=true]", 1, 1, LAST)
        .block("nether_brick_fence[east=true]", 0, 1, LAST)
        .block("nether_brick_fence[west=true]", LAST, 1, LAST)
        .build();
  }

  /** A small lit nether portal in its obsidian frame, on stone. */
  static Diorama portal() {
    int width = 4;
    int height = 5;
    Diorama.Builder portal =
        Diorama.builder(width, height, 2).box("stone", 0, 0, 0, width - 1, 0, 0);
    portal
        .box("obsidian", 0, 0, 1, width - 1, 0, 1)
        .box("obsidian", 0, height - 1, 1, width - 1, height - 1, 1)
        .box("obsidian", 0, 1, 1, 0, height - 2, 1)
        .box("obsidian", width - 1, 1, 1, width - 1, height - 2, 1)
        .box("nether_portal[axis=x]", 1, 1, 1, width - 2, height - 2, 1);
    return portal.build();
  }

  /** A sandy seabed with seagrass. */
  static Diorama seabed() {
    return floor(Diorama.builder(SIDE, 2, SIDE), "sand")
        .block("seagrass", 0, 1, LAST)
        .block("seagrass", LAST, 1, 1)
        .build();
  }

  private static Diorama bedOn(String block) {
    return floor(Diorama.builder(SIDE, 2, SIDE), block)
        .block("red_bed[part=foot,facing=south]", 0, 1, 1)
        .block("red_bed[part=head,facing=south]", 0, 1, LAST)
        .build();
  }

  private static Diorama.Builder floor(Diorama.Builder builder, String block) {
    return builder.box(block, 0, 0, 0, LAST, 0, LAST);
  }
}
