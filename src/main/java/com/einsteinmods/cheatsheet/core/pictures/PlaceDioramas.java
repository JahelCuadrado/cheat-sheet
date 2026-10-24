package com.einsteinmods.cheatsheet.core.pictures;

/**
 * The dioramas of {@link Place}, block by block. Pictures look at them from the north-east and
 * above, like a block in the inventory: the cell nearest the viewer is (x = width - 1, z = 0), the
 * east (+x) faces show on the left and the north (-z) faces on the right, and tall parts stand at
 * the back (x = 0, z = depth - 1) so they hide nothing.
 */
final class PlaceDioramas {
  private PlaceDioramas() {}

  /**
   * A tall watchtower: dark oak and birch on a cobblestone foot, under a wider deck with a fence
   * and a roof. Only its three visible corners are built.
   */
  static Diorama pillagerOutpost() {
    Diorama.Builder tower = Diorama.builder(3, 6, 3);
    for (int y = 0; y < 4; y++) {
      String wall = y == 0 ? "cobblestone" : "birch_planks";
      tower.block(y == 0 ? "cobblestone" : "dark_oak_log", 1, y, 1);
      tower.block(wall, 0, y, 1);
      tower.block(wall, 1, y, 2);
    }
    return tower
        .box("dark_oak_planks", 0, 4, 0, 2, 4, 2)
        .block("dark_oak_fence", 2, 5, 0)
        .block("dark_oak_fence", 0, 5, 0)
        .block("dark_oak_fence", 2, 5, 2)
        .block("dark_oak_slab", 1, 5, 1)
        .build();
  }

  /** Stepped sandstone pyramid between two towers, with the orange terracotta of the temple. */
  static Diorama desertPyramid() {
    Diorama.Builder pyramid = Diorama.builder(5, 3, 5);
    for (int x = 0; x < 5; x++) {
      for (int z = 0; z < 5; z++) {
        if (x == 0 || z == 0 || x == 4 || z == 4) {
          pyramid.block("sandstone", x, 0, z);
        }
      }
    }
    for (int x = 1; x < 4; x++) {
      for (int z = 1; z < 4; z++) {
        if (x != 2 || z != 2) {
          pyramid.block("cut_sandstone", x, 1, z);
        }
      }
    }
    // Towers on the left and right corners, capped with orange terracotta as in the game.
    return pyramid
        .block("orange_terracotta", 3, 1, 2)
        .block("orange_terracotta", 2, 1, 1)
        .block("chiseled_sandstone", 2, 2, 2)
        .block("cut_sandstone", 4, 1, 4)
        .block("orange_terracotta", 4, 2, 4)
        .block("cut_sandstone", 0, 1, 0)
        .block("orange_terracotta", 0, 2, 0)
        .build();
  }

  /** A broken spruce hull with its mast, half in the sand. */
  static Diorama shipwreck() {
    return Diorama.builder(6, 4, 3)
        .box("sand", 0, 0, 0, 1, 0, 0)
        .box("sand", 4, 0, 2, 5, 0, 2)
        .box("spruce_planks", 0, 0, 1, 5, 0, 1)
        .box("spruce_planks", 0, 1, 2, 4, 1, 2)
        .box("spruce_planks", 1, 1, 0, 5, 1, 0)
        .block("spruce_stairs[facing=west]", 5, 1, 1)
        .box("spruce_log", 2, 1, 1, 2, 3, 1)
        .block("white_wool", 2, 3, 2)
        .build();
  }

  /** Mossy cobblestone temple in two steps, a chiseled door and jungle leaves. */
  static Diorama jungleTemple() {
    Diorama.Builder temple = Diorama.builder(4, 3, 4);
    for (int x = 0; x < 4; x++) {
      for (int z = 0; z < 4; z++) {
        if (x == 0 || z == 0 || x == 3 || z == 3) {
          temple.block((x + z) % 3 == 0 ? "cobblestone" : "mossy_cobblestone", x, 0, z);
          temple.block((x + z) % 2 == 0 ? "mossy_cobblestone" : "cobblestone", x, 1, z);
        }
      }
    }
    return temple
        .block("chiseled_stone_bricks", 2, 0, 0)
        .box("mossy_cobblestone", 1, 2, 1, 2, 2, 2)
        .block("jungle_leaves", 0, 2, 3)
        .block("jungle_leaves", 0, 2, 2)
        .build();
  }

  /** Prismarine steps around a sea lantern; the elder guardian floats over it. */
  static Diorama oceanMonument() {
    Diorama.Builder monument = Diorama.builder(5, 3, 5);
    for (int x = 0; x < 5; x++) {
      for (int z = 0; z < 5; z++) {
        if (x == 0 || z == 0 || x == 4 || z == 4) {
          monument.block((x + z) % 4 == 0 ? "sea_lantern" : "prismarine_bricks", x, 0, z);
        }
      }
    }
    for (int x = 1; x < 4; x++) {
      for (int z = 1; z < 4; z++) {
        if (x != 2 || z != 2) {
          monument.block("dark_prismarine", x, 1, z);
        }
      }
    }
    return monument.block("sea_lantern", 2, 2, 2).build();
  }

  /**
   * Two dark oak storeys on a cobblestone foot, glass windows and a gabled dark oak roof. The
   * hidden back walls are left out.
   */
  static Diorama woodlandMansion() {
    return Diorama.builder(4, 4, 3)
        .box("cobblestone", 0, 0, 0, 3, 0, 0)
        .box("cobblestone", 3, 0, 1, 3, 0, 2)
        .box("dark_oak_log", 0, 1, 0, 0, 2, 0)
        .box("dark_oak_log", 3, 1, 0, 3, 2, 0)
        .box("dark_oak_log", 3, 1, 2, 3, 2, 2)
        .box("glass", 1, 1, 0, 2, 1, 0)
        .block("glass", 3, 1, 1)
        .box("dark_oak_planks", 1, 2, 0, 2, 2, 0)
        .block("dark_oak_planks", 3, 2, 1)
        .box("dark_oak_stairs[facing=south]", 0, 3, 0, 3, 3, 0)
        .box("dark_oak_planks", 0, 3, 1, 3, 3, 1)
        .box("dark_oak_stairs[facing=north]", 0, 3, 2, 3, 3, 2)
        .build();
  }

  /** The end portal room: frames with eyes around the portal, on stone bricks. */
  static Diorama stronghold() {
    Diorama.Builder room = Diorama.builder(4, 2, 4);
    String[] floor = {"stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks"};
    for (int x = 0; x < 4; x++) {
      for (int z = 0; z < 4; z++) {
        boolean portal = x > 0 && x < 3 && z > 0 && z < 3;
        room.block(portal ? "black_concrete" : floor[(x * 3 + z) % floor.length], x, 0, z);
      }
    }
    for (int index = 1; index < 3; index++) {
      room.block("end_portal_frame[facing=south,eye=true]", index, 1, 0);
      room.block("end_portal_frame[facing=north,eye=true]", index, 1, 3);
      room.block("end_portal_frame[facing=east,eye=true]", 0, 1, index);
      room.block("end_portal_frame[facing=west,eye=true]", 3, 1, index);
    }
    return room.build();
  }

  /** The reinforced deepslate portal over deepslate tiles and sculk, with a shrieker. */
  static Diorama ancientCity() {
    Diorama.Builder city = Diorama.builder(5, 5, 3);
    for (int x = 0; x < 5; x++) {
      for (int z = 0; z < 3; z++) {
        city.block((x + z) % 2 == 0 ? "sculk" : "deepslate_tiles", x, 0, z);
      }
    }
    return city.box("reinforced_deepslate", 0, 1, 2, 0, 3, 2)
        .box("reinforced_deepslate", 4, 1, 2, 4, 3, 2)
        .box("reinforced_deepslate", 0, 4, 2, 4, 4, 2)
        .block("sculk_shrieker", 3, 1, 0)
        .block("soul_lantern", 1, 1, 0)
        .build();
  }

  /** Half-buried terracotta and mud brick walls, suspicious gravel and a pot. */
  static Diorama trailRuins() {
    return Diorama.builder(4, 3, 4)
        .box("gravel", 0, 0, 0, 3, 0, 3)
        .block("suspicious_gravel", 3, 0, 0)
        .block("suspicious_gravel", 1, 0, 1)
        .block("coarse_dirt", 2, 0, 2)
        .box("mud_bricks", 0, 1, 0, 0, 1, 3)
        .box("mud_bricks", 1, 1, 3, 3, 1, 3)
        .block("orange_terracotta", 0, 2, 3)
        .block("light_blue_terracotta", 0, 2, 2)
        .block("bricks", 1, 2, 3)
        .block("decorated_pot", 2, 1, 1)
        .build();
  }

  /** Tuff and copper chamber with a trial spawner, a vault and a copper bulb. */
  static Diorama trialChambers() {
    Diorama.Builder chamber = Diorama.builder(4, 3, 4);
    for (int x = 0; x < 4; x++) {
      for (int z = 0; z < 4; z++) {
        chamber.block((x + z) % 2 == 0 ? "tuff_bricks" : "polished_tuff", x, 0, z);
      }
    }
    return chamber
        .box("waxed_copper_grate", 0, 1, 1, 0, 1, 3)
        .box("waxed_cut_copper", 1, 1, 3, 3, 1, 3)
        .block("waxed_copper_bulb[lit=true]", 0, 2, 3)
        .block("trial_spawner", 1, 1, 1)
        .block("vault[facing=north]", 3, 1, 0)
        .build();
  }

  /** A nether brick bridge with fences and a pillar into the lava below. */
  static Diorama netherFortress() {
    return Diorama.builder(5, 3, 3)
        .box("nether_bricks", 0, 1, 0, 4, 1, 2)
        .box("nether_brick_fence[east=true,west=true]", 0, 2, 2, 4, 2, 2)
        .box("nether_brick_fence[east=true,west=true]", 0, 2, 0, 4, 2, 0)
        .block("nether_bricks", 2, 0, 1)
        .block("nether_bricks", 2, 0, 0)
        .build();
  }

  /** Blackstone walls with gilded blackstone and a block of gold on polished bricks. */
  static Diorama bastion() {
    Diorama.Builder bastion = Diorama.builder(4, 3, 4);
    for (int x = 0; x < 4; x++) {
      for (int z = 0; z < 4; z++) {
        bastion.block(
            (x * z) % 3 == 1 ? "cracked_polished_blackstone_bricks" : "polished_blackstone_bricks",
            x,
            0,
            z);
      }
    }
    return bastion
        .box("blackstone", 0, 1, 0, 0, 1, 3)
        .box("blackstone", 1, 1, 3, 3, 1, 3)
        .block("gilded_blackstone", 0, 1, 2)
        .block("gilded_blackstone", 2, 1, 3)
        .block("blackstone", 0, 2, 3)
        .block("gilded_blackstone", 0, 2, 2)
        .block("gold_block", 2, 1, 1)
        .build();
  }

  /**
   * A purpur tower with magenta windows under a purpur roof and an end rod. Only the walls facing
   * the viewer are built: the roof hides the rest.
   */
  static Diorama endCity() {
    Diorama.Builder tower = Diorama.builder(3, 5, 3);
    for (int y = 0; y < 3; y++) {
      String wall = y == 1 ? "magenta_stained_glass" : "purpur_block";
      tower.block("purpur_pillar", 2, y, 0);
      tower.block(wall, 1, y, 0);
      tower.block("purpur_pillar", 0, y, 0);
      tower.block(wall, 2, y, 1);
      tower.block("purpur_pillar", 2, y, 2);
    }
    return tower.box("purpur_block", 0, 3, 0, 2, 3, 2).block("end_rod", 1, 4, 1).build();
  }

  /** Cobblestone and mossy cobblestone room with a spawner and a chest. */
  static Diorama dungeon() {
    Diorama.Builder room = Diorama.builder(4, 3, 4);
    for (int x = 0; x < 4; x++) {
      for (int z = 0; z < 4; z++) {
        room.block((x + 2 * z) % 3 == 0 ? "cobblestone" : "mossy_cobblestone", x, 0, z);
      }
    }
    return room.box("cobblestone", 0, 1, 0, 0, 1, 3)
        .box("mossy_cobblestone", 1, 1, 3, 3, 1, 3)
        .block("mossy_cobblestone", 0, 2, 3)
        .block("spawner", 2, 1, 1)
        .block("chest[facing=north]", 1, 1, 2)
        .build();
  }

  /**
   * A corridor through stone: oak support frames, a rail and a cobweb. The back frame's hidden post
   * is left out.
   */
  static Diorama mineshaft() {
    return Diorama.builder(4, 4, 3)
        .box("stone", 0, 0, 0, 3, 0, 2)
        .block("gravel", 2, 0, 2)
        .block("andesite", 1, 0, 0)
        .box("rail[shape=east_west]", 0, 1, 1, 3, 1, 1)
        .box("oak_fence", 3, 1, 0, 3, 2, 0)
        .box("oak_fence", 3, 1, 2, 3, 2, 2)
        .box("oak_planks", 3, 3, 0, 3, 3, 2)
        .box("oak_fence", 0, 1, 0, 0, 2, 0)
        .box("oak_planks", 0, 3, 0, 0, 3, 2)
        .block("cobweb", 1, 2, 2)
        .build();
  }

  /**
   * A mesa: a terracotta butte in coloured layers behind a low terrace, on red sand, with a cactus
   * and a dead bush. Cells under the butte and terrace never show and are left out.
   */
  static Diorama badlands() {
    return Diorama.builder(4, 4, 4)
        .box("red_sand", 2, 0, 0, 3, 0, 1)
        .box("red_sand", 3, 0, 2, 3, 0, 3)
        .box("red_sand", 0, 0, 0, 1, 0, 0)
        .box("orange_terracotta", 2, 1, 2, 2, 1, 3)
        .box("orange_terracotta", 0, 1, 1, 1, 1, 1)
        .box("orange_terracotta", 0, 1, 2, 1, 1, 3)
        .box("white_terracotta", 0, 2, 2, 1, 2, 3)
        .box("red_terracotta", 0, 3, 2, 1, 3, 3)
        .block("cactus", 3, 1, 0)
        .block("dead_bush", 2, 1, 1)
        .build();
  }

  /** A snowy stone peak with emerald ore showing. */
  static Diorama mountains() {
    Diorama.Builder peak = Diorama.builder(4, 4, 4);
    for (int x = 0; x < 4; x++) {
      for (int z = 0; z < 4; z++) {
        int top = Math.min(3, Math.max(0, 2 - x + z - 1));
        for (int y = 0; y <= top; y++) {
          boolean visible = y == top || x == 3 || z == 0;
          if (visible) {
            peak.block(y == top && top >= 2 ? "snow_block" : "stone", x, y, z);
          }
        }
      }
    }
    return peak.block("emerald_ore", 3, 0, 1).block("emerald_ore", 1, 1, 0).build();
  }

  /** Dripstone blocks with stalagmites and a copper ore. */
  static Diorama dripstoneCaves() {
    Diorama.Builder cave = Diorama.builder(4, 4, 3);
    for (int x = 0; x < 4; x++) {
      for (int z = 0; z < 3; z++) {
        cave.block((x + z) % 4 == 1 ? "copper_ore" : "dripstone_block", x, 0, z);
      }
    }
    String up = "pointed_dripstone[vertical_direction=up,thickness=";
    return cave.box("dripstone_block", 0, 1, 2, 0, 2, 2)
        .block(up + "base]", 0, 1, 1)
        .block(up + "middle]", 0, 2, 1)
        .block(up + "tip]", 0, 3, 1)
        .block(up + "frustum]", 2, 1, 2)
        .block(up + "tip]", 2, 2, 2)
        .block(up + "tip]", 3, 1, 0)
        .block("raw_copper_block", 1, 1, 0)
        .build();
  }

  /** Basalt pillars, blackstone and magma, with nether gold ore. */
  static Diorama basaltDeltas() {
    Diorama.Builder delta = Diorama.builder(4, 4, 3);
    for (int x = 0; x < 4; x++) {
      for (int z = 0; z < 3; z++) {
        delta.block((x + z) % 3 == 0 ? "magma_block" : "blackstone", x, 0, z);
      }
    }
    return delta
        .box("basalt", 0, 1, 2, 0, 3, 2)
        .box("basalt", 1, 1, 2, 1, 2, 2)
        .box("basalt", 0, 1, 1, 0, 1, 1)
        .block("nether_gold_ore", 3, 0, 0)
        .block("nether_gold_ore", 2, 1, 1)
        .build();
  }
}
