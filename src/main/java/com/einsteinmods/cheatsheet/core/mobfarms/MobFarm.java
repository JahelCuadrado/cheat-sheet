package com.einsteinmods.cheatsheet.core.mobfarms;

import com.einsteinmods.cheatsheet.core.mobfarms.FarmFact.Label;
import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneRules;
import java.util.List;
import java.util.Locale;

/**
 * The farms the redstone tab lists, in reading order: each card shows a small picture (the place
 * and its mob) and one key fact; its zoom lists every condition needed to build the farm, with the
 * numbers of {@link FarmRules}, and the room each of its mobs needs ({@link MobSpace}). Java
 * Edition only.
 */
public enum MobFarm {
  MOB_FARM(
      FarmDioramas.platform("smooth_stone"),
      List.of(
          FarmMob.of("minecraft:zombie", 1.0, 1, 1.4),
          FarmMob.of("minecraft:skeleton", 2.1, 1, 1.0).holding("bow")),
      List.of(FarmRules.MIN_PLAYER_DISTANCE, FarmRules.INSTANT_DESPAWN_DISTANCE),
      List.of(
          dark(),
          fact("cobblestone", Label.FLOOR, "mob_farm.floor"),
          fact("armor_stand", Label.ROOM, "mob_farm.room"),
          distance(),
          cap(),
          fact("ladder", Label.TIP, "mob_farm.height")),
      List.of(
          MobSpace.ZOMBIE,
          MobSpace.SKELETON,
          MobSpace.CREEPER,
          MobSpace.WITCH,
          MobSpace.SPIDER,
          MobSpace.ENDERMAN)),
  SPAWNER(
      FarmDioramas.spawner(),
      List.of(FarmMob.of("minecraft:zombie", 2.4, 1, 0.6)),
      List.of(
          FarmRules.SPAWNER_PLAYER_RANGE,
          RedstoneRules.seconds(FarmRules.SPAWNER_MIN_DELAY_TICKS),
          RedstoneRules.seconds(FarmRules.spawnerLongestWaitTicks())),
      List.of(
          fact("player_head", Label.NEEDS, "spawner.player", FarmRules.SPAWNER_PLAYER_RANGE),
          fact(
              "spawner",
              Label.WHERE,
              "spawner.area",
              FarmRules.spawnerAreaWidth(),
              FarmRules.spawnerAreaWidth(),
              FarmRules.SPAWNER_SPAWN_RANGE),
          fact("armor_stand", Label.ROOM, "spawner.room"),
          fact(
              "clock",
              Label.RATE,
              "spawner.rate",
              FarmRules.SPAWNER_TRIES,
              RedstoneRules.seconds(FarmRules.SPAWNER_MIN_DELAY_TICKS),
              RedstoneRules.seconds(FarmRules.spawnerLongestWaitTicks())),
          fact(
              "zombie_head",
              Label.CAP,
              "spawner.cap",
              FarmRules.SPAWNER_MAX_NEARBY,
              FarmRules.spawnerAreaWidth(),
              FarmRules.spawnerAreaWidth(),
              FarmRules.spawnerAreaWidth()),
          fact("torch", Label.LIGHT, "spawner.light", FarmRules.BLAZE_MAX_LIGHT)),
      List.of(
          MobSpace.ZOMBIE,
          MobSpace.SKELETON,
          MobSpace.SPIDER,
          MobSpace.CAVE_SPIDER,
          MobSpace.BLAZE)),
  IRON_FARM(
      FarmDioramas.ironFarm(),
      List.of(
          FarmMob.of("minecraft:villager", 0.9, 1, 0.8),
          FarmMob.of("minecraft:iron_golem", 2.2, 1, 1.6)),
      List.of(FarmRules.GOLEM_SCARED_VILLAGERS),
      List.of(
          fact(
              "red_bed",
              Label.NEEDS,
              "iron_farm.villagers",
              FarmRules.GOLEM_SCARED_VILLAGERS,
              FarmRules.GOLEM_VILLAGER_RANGE,
              FarmRules.minutes(FarmRules.GOLEM_SLEPT_TICKS)),
          fact(
              "zombie_head",
              Label.WHEN,
              "iron_farm.scare",
              FarmRules.ZOMBIE_SCARE_RANGE,
              RedstoneRules.seconds(FarmRules.GOLEM_PANIC_TRY_TICKS),
              FarmRules.GOLEM_GOSSIP_VILLAGERS),
          fact(
              "clock",
              Label.RATE,
              "iron_farm.cooldown",
              FarmRules.GOLEM_SEEN_RANGE,
              FarmRules.roundSeconds(FarmRules.GOLEM_COOLDOWN_TICKS)),
          fact(
              "iron_block",
              Label.WHERE,
              "iron_farm.spawn",
              FarmRules.golemSpawnWidth(),
              FarmRules.golemSpawnHeight(),
              FarmRules.golemSpawnWidth(),
              FarmRules.GOLEM_SPAWN_TRIES,
              FarmRules.GOLEM_SPACE_HIGH)),
      List.of()),
  BREEDER(
      FarmDioramas.breeder(),
      List.of(
          FarmMob.of("minecraft:villager", 1.0, 1, 0.7),
          FarmMob.of("minecraft:villager", 2.2, 1, 1.3)),
      List.of(FarmRules.BREEDING_FOOD),
      List.of(
          fact(
              "bread",
              Label.NEEDS,
              "breeder.food",
              FarmRules.BREEDING_FOOD,
              FarmRules.BREEDING_FOOD / FarmRules.BREAD_FOOD,
              FarmRules.BREEDING_FOOD / FarmRules.CROP_FOOD),
          fact("emerald", Label.NEEDS, "breeder.partner", FarmRules.BREEDING_PARTNER_RANGE),
          fact(
              "red_bed",
              Label.WHERE,
              "breeder.beds",
              FarmRules.BED_RANGE,
              FarmRules.BED_FREE_BLOCKS_ABOVE),
          fact(
              "bell",
              Label.WHEN,
              "breeder.when",
              FarmRules.minutes(FarmRules.VILLAGER_REST_START_TICKS),
              RedstoneRules.seconds(FarmRules.villagerMorningTicks()),
              RedstoneRules.seconds(FarmRules.villagerEveningTicks()),
              RedstoneRules.seconds(FarmRules.villagerMeetTicks())),
          fact(
              "clock",
              Label.RATE,
              "breeder.times",
              RedstoneRules.seconds(FarmRules.LOVE_MIN_TICKS),
              RedstoneRules.seconds(FarmRules.LOVE_MAX_TICKS),
              FarmRules.minutes(FarmRules.PARENT_WAIT_TICKS),
              FarmRules.minutes(FarmRules.CHILD_GROWTH_TICKS))),
      List.of()),
  RAID_FARM(
      FarmDioramas.raid(),
      List.of(FarmMob.of("minecraft:pillager", 1.6, 1, 1.0).holding("crossbow")),
      List.of(),
      List.of(
          fact(
              "ominous_bottle",
              Label.NEEDS,
              "raid_farm.start",
              FarmRules.minutes(FarmRules.BAD_OMEN_TICKS),
              RedstoneRules.seconds(FarmRules.RAID_OMEN_TICKS)),
          fact("bell", Label.WHERE, "raid_farm.village", FarmRules.VILLAGE_SECTION),
          fact("compass", Label.WHERE, "raid_farm.center", FarmRules.RAID_CENTER_RANGE),
          fact(
              "crossbow",
              Label.RATE,
              "raid_farm.waves",
              FarmRules.RAID_WAVES_EASY,
              FarmRules.RAID_WAVES_NORMAL,
              FarmRules.RAID_WAVES_HARD,
              RedstoneRules.seconds(FarmRules.RAID_WAVE_WAIT_TICKS)),
          fact(
              "barrier",
              Label.STOPS,
              "raid_farm.stops",
              FarmRules.RAID_RAIDER_RANGE,
              FarmRules.minutes(FarmRules.RAID_TIMEOUT_TICKS))),
      List.of()),
  WITCH_HUT(
      FarmDioramas.witchHut(),
      List.of(FarmMob.of("minecraft:witch", 1.5, 2, 1.5)),
      List.of(FarmRules.WITCH_HUT_WIDTH, FarmRules.WITCH_HUT_LENGTH, FarmRules.WITCH_HUT_HEIGHT),
      List.of(
          fact(
              "spruce_planks",
              Label.WHERE,
              "witch_hut.box",
              FarmRules.WITCH_HUT_WIDTH,
              FarmRules.WITCH_HUT_LENGTH,
              FarmRules.WITCH_HUT_HEIGHT),
          fact("cauldron", Label.CHANCE, "witch_hut.only"),
          dark(),
          distance(),
          cap()),
      List.of(MobSpace.WITCH)),
  SLIME_FARM(
      FarmDioramas.platform("stone"),
      List.of(FarmMob.of("minecraft:slime", 1.5, 1, 1.5).sized(2)),
      List.of(FarmRules.SLIME_CHUNK_BELOW_Y),
      List.of(
          fact(
              "slime_ball",
              Label.WHERE,
              "slime_farm.chunk",
              FarmRules.SLIME_CHUNK_ONE_IN,
              FarmRules.SLIME_CHUNK_BELOW_Y,
              FarmRules.SLIME_CHUNK_TRY_ONE_IN),
          fact(
              "lily_pad",
              Label.WHERE,
              "slime_farm.swamp",
              FarmRules.SWAMP_SLIME_MIN_Y,
              FarmRules.SWAMP_SLIME_MAX_Y,
              FarmRules.SWAMP_SLIME_MAX_LIGHT),
          fact("armor_stand", Label.ROOM, "slime_farm.room", FarmRules.BIG_SLIME_SIZE),
          distance(),
          cap()),
      List.of(MobSpace.SLIME)),
  GUARDIAN_FARM(
      FarmDioramas.monument(),
      List.of(FarmMob.of("minecraft:guardian", 1.5, 1.3, 1.5)),
      List.of(),
      List.of(
          fact(
              "prismarine_bricks",
              Label.WHERE,
              "guardian_farm.box",
              FarmRules.MONUMENT_SIZE,
              FarmRules.MONUMENT_SIZE,
              FarmRules.MONUMENT_BOTTOM_Y,
              FarmRules.MONUMENT_TOP_Y),
          fact(
              "water_bucket",
              Label.NEEDS,
              "guardian_farm.water",
              FarmRules.GUARDIAN_OPEN_SKY_ONE_IN),
          fact("sea_lantern", Label.LIGHT, "guardian_farm.light"),
          fact(
              "ladder",
              Label.TIP,
              "guardian_farm.height",
              FarmRules.MONUMENT_BOTTOM_Y,
              FarmRules.MONUMENT_TOP_Y),
          distance(),
          cap()),
      List.of(MobSpace.GUARDIAN)),
  WITHER_SKELETON_FARM(
      FarmDioramas.fortress(),
      List.of(FarmMob.of("minecraft:wither_skeleton", 1.5, 1, 1.4).holding("stone_sword")),
      List.of(),
      List.of(
          fact("nether_bricks", Label.FLOOR, "wither_farm.floor"),
          fact("wither_rose", Label.TIP, "wither_farm.roses"),
          fact(
              "wither_skeleton_skull",
              Label.CHANCE,
              "wither_farm.mobs",
              FarmRules.fortressWeights(),
              FarmRules.FORTRESS_BLAZE,
              FarmRules.FORTRESS_WITHER_SKELETON,
              FarmRules.FORTRESS_ZOMBIFIED_PIGLIN,
              FarmRules.FORTRESS_MAGMA_CUBE,
              FarmRules.FORTRESS_SKELETON),
          fact(
              "torch",
              Label.LIGHT,
              "wither_farm.light",
              FarmRules.NETHER_MONSTER_LIGHT,
              FarmRules.BLAZE_MAX_LIGHT),
          distance(),
          cap()),
      List.of(MobSpace.WITHER_SKELETON, MobSpace.BLAZE, MobSpace.MAGMA_CUBE)),
  ENDERMAN_FARM(
      FarmDioramas.platform("end_stone"),
      List.of(FarmMob.of("minecraft:enderman", 1.5, 1, 1.5)),
      List.of(),
      List.of(
          fact("end_stone", Label.WHERE, "enderman_farm.end", FarmRules.ENDERMAN_GROUP),
          fact("torch", Label.LIGHT, "enderman_farm.light"),
          fact(
              "minecart",
              Label.TIP,
              "enderman_farm.lure",
              FarmRules.ENDERMITE_LURE_RANGE,
              FarmRules.minutes(FarmRules.ENDERMITE_LIFE_TICKS)),
          distance(),
          cap()),
      List.of(MobSpace.ENDERMAN)),
  MAGMA_CUBE_FARM(
      FarmDioramas.platform("basalt"),
      List.of(FarmMob.of("minecraft:magma_cube", 1.5, 1, 1.5).sized(2)),
      List.of(),
      List.of(
          fact("basalt", Label.WHERE, "magma_farm.deltas"),
          fact("torch", Label.LIGHT, "magma_farm.light"),
          fact("armor_stand", Label.ROOM, "magma_farm.room", FarmRules.BIG_SLIME_SIZE),
          distance(),
          cap()),
      List.of(MobSpace.MAGMA_CUBE)),
  GOLD_FARM(
      FarmDioramas.portal(),
      List.of(FarmMob.of("minecraft:zombified_piglin", 2.0, 1, 0.5).holding("golden_sword")),
      List.of(FarmRules.PORTAL_ONE_IN),
      List.of(
          fact("obsidian", Label.CHANCE, "gold_farm.chance", FarmRules.PORTAL_ONE_IN),
          fact("flint_and_steel", Label.WHERE, "gold_farm.where"),
          fact("player_head", Label.NEEDS, "gold_farm.player", FarmRules.SPAWN_PLAYER_RANGE),
          fact(
              "crying_obsidian",
              Label.TIP,
              "gold_farm.size",
              FarmRules.PORTAL_MAX_INSIDE,
              FarmRules.PORTAL_MAX_INSIDE)),
      List.of()),
  DROWNED_FARM(
      FarmDioramas.seabed(),
      List.of(FarmMob.of("minecraft:drowned", 1.5, 1, 1.5).holding("trident")),
      List.of(FarmRules.DROWNED_RIVER_ONE_IN),
      List.of(
          fact(
              "water_bucket",
              Label.WHERE,
              "drowned_farm.water",
              FarmRules.DROWNED_RIVER_ONE_IN,
              FarmRules.DROWNED_OCEAN_ONE_IN,
              FarmRules.DROWNED_OCEAN_BELOW_Y),
          dark(),
          fact("trident", Label.CHANCE, "drowned_farm.trident", FarmRules.DROWNED_TRIDENT_ONE_IN),
          distance(),
          cap()),
      List.of(MobSpace.DROWNED)),
  SHULKER_FARM(
      FarmDioramas.platform("purpur_block"),
      List.of(FarmMob.of("minecraft:shulker", 1.0, 1, 1.0)),
      List.of(),
      List.of(
          fact("shulker_shell", Label.WHAT, "shulker_farm.bullet", FarmRules.SHULKER_TARGET_RANGE),
          fact("shulker_box", Label.HOW, "shulker_farm.how"),
          fact(
              "chorus_fruit",
              Label.ROOM,
              "shulker_farm.teleport",
              FarmRules.SHULKER_TELEPORT_TRIES,
              FarmRules.SHULKER_TELEPORT_RANGE),
          fact(
              "purple_shulker_box",
              Label.COPY_CHANCE,
              "shulker_farm.chance",
              FarmRules.SHULKER_COUNT_RANGE,
              FarmRules.shulkerCopyPenaltyPercent(),
              FarmRules.shulkerNeverCopiesAt()),
          fact(
              "glistering_melon_slice",
              Label.LOW_HEALTH,
              "shulker_farm.low_health",
              FarmRules.SHULKER_ONLY_TELEPORT_ONE_IN),
          fact("oak_boat", Label.WHERE, "shulker_farm.where")),
      List.of());

  private final Diorama picture;
  private final List<FarmMob> mobs;
  private final List<Object> keyArgs;
  private final List<FarmFact> facts;
  private final List<MobSpace> spaces;

  MobFarm(
      Diorama picture,
      List<FarmMob> mobs,
      List<Object> keyArgs,
      List<FarmFact> facts,
      List<MobSpace> spaces) {
    this.picture = picture;
    this.mobs = List.copyOf(mobs);
    this.keyArgs = List.copyOf(keyArgs);
    this.facts = List.copyOf(facts);
    this.spaces = List.copyOf(spaces);
  }

  /** The place in miniature. */
  public Diorama picture() {
    return picture;
  }

  /** The mobs standing in the picture. */
  public List<FarmMob> mobs() {
    return mobs;
  }

  /** The numbers of the card's key fact. */
  public List<Object> keyArgs() {
    return keyArgs;
  }

  /** Every condition, in the zoom's order. */
  public List<FarmFact> facts() {
    return facts;
  }

  /** The room each mob of the farm needs to spawn; empty when the farm's mobs never check it. */
  public List<MobSpace> spaces() {
    return spaces;
  }

  /** Lower-case id for lang keys. */
  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }

  private static FarmFact fact(String iconItemId, Label label, String textKey, Object... args) {
    return new FarmFact(iconItemId, label, textKey, List.of(args));
  }

  /** Monsters' light: block light 0, and dark from the sky where the sky reaches. */
  private static FarmFact dark() {
    return fact("torch", Label.LIGHT, "common.dark", FarmRules.MONSTER_BLOCK_LIGHT);
  }

  /** How far from the player monsters spawn and despawn. */
  private static FarmFact distance() {
    return fact(
        "compass",
        Label.DISTANCE,
        "common.distance",
        FarmRules.MIN_PLAYER_DISTANCE,
        FarmRules.INSTANT_DESPAWN_DISTANCE,
        FarmRules.RANDOM_DESPAWN_DISTANCE);
  }

  /** The monster cap. */
  private static FarmFact cap() {
    return fact(
        "iron_sword", Label.CAP, "common.cap", FarmRules.MONSTER_CAP, FarmRules.CAP_CHUNK_RANGE);
  }
}
