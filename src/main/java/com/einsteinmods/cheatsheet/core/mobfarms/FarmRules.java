package com.einsteinmods.cheatsheet.core.mobfarms;

/**
 * The numbers mob farms depend on, as Minecraft 26.3 (Java Edition) makes them; each names its
 * source in the decompiled game. {@code FarmRulesGameTest} compares those the game exposes (mob
 * caps, structure spawns, dimension light limits, spawner settings, villager food, hitboxes) with
 * the running game; the rest are private literals read in the code and cross-checked with the
 * Minecraft Wiki.
 */
public final class FarmRules {
  // Natural spawning (NaturalSpawner, MobCategory, Mob#checkDespawn).

  /**
   * {@code NaturalSpawner#isRightDistanceToPlayerAndSpawnPoint}: no spawn within 24 (576 = 24²).
   */
  public static final int MIN_PLAYER_DISTANCE = 24;

  /** {@code MobCategory#getDespawnDistance}: farther than this, a mob vanishes at once. */
  public static final int INSTANT_DESPAWN_DISTANCE = 128;

  /** {@code MobCategory#getNoDespawnDistance}: closer than this, a mob never vanishes. */
  public static final int RANDOM_DESPAWN_DISTANCE = 32;

  /**
   * {@code ChunkMap#playerIsCloseEnoughForSpawning}: chunks this close spawn mobs, and the monsters
   * in them (unnamed) count towards the player's cap ({@code LocalMobCapCalculator}).
   */
  public static final int CAP_CHUNK_RANGE = 128;

  /** {@code MobCategory.MONSTER}: the monster cap for one player (289 chunks around him). */
  public static final int MONSTER_CAP = 70;

  /** Overworld and End {@code monster_spawn_block_light_limit}: monsters need block light 0. */
  public static final int MONSTER_BLOCK_LIGHT = 0;

  /** Nether {@code monster_spawn_light_level}: light 7 or less. */
  public static final int NETHER_MONSTER_LIGHT = 7;

  // Monster spawners (BaseSpawner defaults).

  /** {@code DEFAULT_REQUIRED_PLAYER_RANGE}: it works while a player is this close. */
  public static final int SPAWNER_PLAYER_RANGE = 16;

  /** {@code DEFAULT_SPAWN_RANGE}: mobs appear up to this far on each side, one block up or down. */
  public static final int SPAWNER_SPAWN_RANGE = 4;

  /** {@code DEFAULT_SPAWN_COUNT}: tries per cycle. */
  public static final int SPAWNER_TRIES = 4;

  /** {@code DEFAULT_MIN_SPAWN_DELAY}: shortest wait between cycles, in game ticks. */
  public static final int SPAWNER_MIN_DELAY_TICKS = 200;

  /** {@code DEFAULT_MAX_SPAWN_DELAY}: the wait is drawn below this ({@code BaseSpawner#delay}). */
  public static final int SPAWNER_MAX_DELAY_TICKS = 800;

  /** {@code DEFAULT_MAX_NEARBY_ENTITIES}: it stops while this many of its mob are around it. */
  public static final int SPAWNER_MAX_NEARBY = 6;

  /**
   * Brightest light a blaze spawns in, in the Nether: blazes ignore the monster light rule ({@code
   * Monster#checkAnyLightMonsterSpawnRules}) but {@code PathfinderMob#checkSpawnRules} wants {@code
   * Monster#getWalkTargetValue} at least 0, which with the Nether's ambient light of 0.1 holds up
   * to light 11 ({@code LevelReader#getPathfindingCostFromLightLevels}).
   */
  public static final int BLAZE_MAX_LIGHT = 11;

  // Structures (worldgen/structure/*.json spawn_overrides, the pieces' sizes).

  /** {@code SwampHutPiece}: its box, 7 wide, 7 high and 9 long, from one block below its floor. */
  public static final int WITCH_HUT_WIDTH = 7;

  public static final int WITCH_HUT_HEIGHT = 7;
  public static final int WITCH_HUT_LENGTH = 9;

  /** {@code OceanMonumentPieces.MonumentBuilding}: 58 × 58, from Y 39, 23 high. */
  public static final int MONUMENT_SIZE = 58;

  public static final int MONUMENT_BOTTOM_Y = 39;
  public static final int MONUMENT_TOP_Y = 61;

  /** {@code Guardian#checkGuardianSpawnRules}: under open sky, only 1 try in 20. */
  public static final int GUARDIAN_OPEN_SKY_ONE_IN = 20;

  /**
   * {@code fortress.json} monster weights: blaze, wither skeleton, zombified piglin, magma,
   * skeleton.
   */
  public static final int FORTRESS_BLAZE = 10;

  public static final int FORTRESS_WITHER_SKELETON = 8;
  public static final int FORTRESS_ZOMBIFIED_PIGLIN = 5;
  public static final int FORTRESS_MAGMA_CUBE = 3;
  public static final int FORTRESS_SKELETON = 2;

  // Slimes (Slime#checkSlimeSpawnRules, the moon timeline).

  /** One chunk in 10 is a slime chunk ({@code WorldgenRandom.seedSlimeChunk(...).nextInt(10)}). */
  public static final int SLIME_CHUNK_ONE_IN = 10;

  /** Slime chunks spawn below this Y. */
  public static final int SLIME_CHUNK_BELOW_Y = 40;

  /** In a slime chunk, only 1 try in 10 goes on ({@code random.nextInt(10) == 0}). */
  public static final int SLIME_CHUNK_TRY_ONE_IN = 10;

  /** Swamps spawn slimes between these Y (above 50, below 70). */
  public static final int SWAMP_SLIME_MIN_Y = 51;

  public static final int SWAMP_SLIME_MAX_Y = 69;

  /** Swamp slimes: light no brighter than {@code random.nextInt(8)}, so 7 at most. */
  public static final int SWAMP_SLIME_MAX_LIGHT = 7;

  // Mob sizes (EntityTypes, AbstractCubeMob#setSpawnSize: sizes 1, 2 or 4 × 0.52).

  public static final String ENDERMAN_HEIGHT = "2.9";
  public static final String WITHER_SKELETON_HEIGHT = "2.4";
  public static final String BIG_SLIME_SIZE = "2.08";

  /** A spider is 1.4 wide: centred on a block it reaches into both neighbours, so 3 × 3. */
  public static final String SPIDER_WIDTH = "1.4";

  public static final String SPIDER_HEIGHT = "0.9";

  // The End (BiomeDefaultFeatures#endSpawns).

  /** Endermen spawn in groups of 4 in every End biome. */
  public static final int ENDERMAN_GROUP = 4;

  /** {@code Enderman}: follow range 64; it goes for an endermite it sees this close. */
  public static final int ENDERMITE_LURE_RANGE = 64;

  /** {@code Endermite.MAX_LIFE}: an endermite without a name vanishes after 2400 game ticks. */
  public static final int ENDERMITE_LIFE_TICKS = 2400;

  // Nether portals (NetherPortalBlock#randomTick, PortalShape).

  /** A portal block spawns a zombified piglin with chance difficulty (1 to 3) in 2000. */
  public static final int PORTAL_ONE_IN = 2000;

  /** {@code ChunkMap#playerIsCloseEnoughForSpawning}: a player within 128 blocks. */
  public static final int SPAWN_PLAYER_RANGE = 128;

  /** {@code PortalShape.MAX_WIDTH}, {@code MAX_HEIGHT}: the largest portal inside its frame. */
  public static final int PORTAL_MAX_INSIDE = 21;

  // Drowned (Drowned#checkDrownedSpawnRules, #populateDefaultEquipmentSlots).

  public static final int DROWNED_RIVER_ONE_IN = 15;
  public static final int DROWNED_OCEAN_ONE_IN = 40;

  /** Elsewhere than rivers, below sea level − 5: Y 58. */
  public static final int DROWNED_OCEAN_BELOW_Y = 58;

  /** 1 in 10 spawn holding an item, 10 in 16 of those a trident: 1 in 16. */
  public static final int DROWNED_TRIDENT_ONE_IN = 16;

  // Shulkers (Shulker#hurtServer, #hitByShulkerBullet, #teleportSomewhere): ShulkerRulesGameTest.

  /** Follow range: {@code NearestAttackableTargetGoal} looks for a player this close, any way. */
  public static final int SHULKER_TARGET_RANGE = 16;

  /** {@code MAX_TELEPORT_DISTANCE}: up to this far on each axis. */
  public static final int SHULKER_TELEPORT_RANGE = 8;

  /** {@code Shulker#teleportSomewhere}: random spots tried, then it gives up. */
  public static final int SHULKER_TELEPORT_TRIES = 5;

  /** {@code OTHER_SHULKER_SCAN_RADIUS}: shulkers counted around the spot it left, on each axis. */
  public static final int SHULKER_COUNT_RANGE = 8;

  /** {@code OTHER_SHULKER_LIMIT}: a copy fails with chance (count − 1) / this. */
  public static final int SHULKER_COUNT_LIMIT = 5;

  /** {@code Shulker#hurtServer}: below half health, 1 hit in 4 only teleports it. */
  public static final int SHULKER_ONLY_TELEPORT_ONE_IN = 4;

  // Iron golems (Villager#spawnGolemIfNeeded, VillagerPanicTrigger, GolemSensor).

  /** {@code VillagerPanicTrigger#tick}: scared villagers who agree. */
  public static final int GOLEM_SCARED_VILLAGERS = 3;

  /** {@code Villager#gossip}: gossiping villagers who agree. */
  public static final int GOLEM_GOSSIP_VILLAGERS = 5;

  /** Villagers who agree are counted this far around ({@code inflate(10)}). */
  public static final int GOLEM_VILLAGER_RANGE = 10;

  /** {@code Villager#golemSpawnConditionsMet}: slept within 24000 game ticks (20 minutes). */
  public static final int GOLEM_SLEPT_TICKS = 24_000;

  /** {@code VillagerHostilesSensor}: a zombie this close (and seen) scares a villager. */
  public static final int ZOMBIE_SCARE_RANGE = 8;

  /** Scared villagers try every 100 game ticks. */
  public static final int GOLEM_PANIC_TRY_TICKS = 100;

  /** {@code GolemSensor}: a golem this close (follow range 16) is seen. */
  public static final int GOLEM_SEEN_RANGE = 16;

  /** {@code GolemSensor#golemDetected}: after seeing a golem, 599 game ticks without summoning. */
  public static final int GOLEM_COOLDOWN_TICKS = 599;

  /** {@code SpawnUtil#trySpawnMob(..., 10, 8, 6, ...)}: tries and reach around the villager. */
  public static final int GOLEM_SPAWN_TRIES = 10;

  public static final int GOLEM_SPAWN_REACH = 8;
  public static final int GOLEM_SPAWN_REACH_UP = 6;

  /** {@code IronGolem#checkSpawnObstruction}: its block and the two above must be free. */
  public static final int GOLEM_SPACE_HIGH = 3;

  // Villager breeding (Villager#canBreed, Items villagerFood, VillagerMakeLove).

  /** {@code Villager.BREEDING_FOOD_THRESHOLD}: food points each parent needs. */
  public static final int BREEDING_FOOD = 12;

  public static final int BREAD_FOOD = 4;
  public static final int CROP_FOOD = 1;

  /** {@code VillagerMakeLove#takeVacantBed}: the child's bed, this close. */
  public static final int BED_RANGE = 48;

  /**
   * Air over a bed so it counts for a baby. {@code VillagerMakeLove#canReach} asks for a path to
   * the first non-solid block over the pillow ({@code GroundPathNavigation#findSurfacePosition}),
   * within 1 block, which a villager only gets standing on the bed. {@code VillagerBedGameTest}.
   */
  public static final int BED_FREE_BLOCKS_ABOVE = 2;

  /** {@code VillagerMakeLove#start}: 275 + random(50) game ticks together. */
  public static final int LOVE_MIN_TICKS = 275;

  public static final int LOVE_MAX_TICKS = 324;

  /** {@code VillagerMakeLove#breed}: parents' wait, and a child's time to grow up. */
  public static final int PARENT_WAIT_TICKS = 6000;

  public static final int CHILD_GROWTH_TICKS = 24_000;

  /** {@code VillagerGoalPackages#getIdlePackage}: a partner this close, in sight. */
  public static final int BREEDING_PARTNER_RANGE = 8;

  /**
   * The villager day ({@code Timelines}, {@code villager_activity}): free time ({@code IDLE}, when
   * they breed) until 2000, work until 9000, meeting at the bell until 11000, free time until
   * 12000, then rest. Without a job site or a bell, work or meeting time is free time too.
   */
  public static final int VILLAGER_WORK_START_TICKS = 2000;

  public static final int VILLAGER_MEET_START_TICKS = 9000;
  public static final int VILLAGER_MEET_END_TICKS = 11_000;
  public static final int VILLAGER_REST_START_TICKS = 12_000;

  // Raids (OminousBottleAmplifier, BadOmenMobEffect, Raid).

  /** {@code OminousBottleAmplifier.EFFECT_DURATION}: Bad Omen lasts 120000 game ticks. */
  public static final int BAD_OMEN_TICKS = 120_000;

  /** {@code BadOmenMobEffect#applyEffectTick}: Raid Omen, then the raid. */
  public static final int RAID_OMEN_TICKS = 600;

  /** {@code Raid#getNumGroups}. */
  public static final int RAID_WAVES_EASY = 3;

  public static final int RAID_WAVES_NORMAL = 5;
  public static final int RAID_WAVES_HARD = 7;

  /** {@code Raid.DEFAULT_PRE_RAID_TICKS}: the wait before each wave. */
  public static final int RAID_WAVE_WAIT_TICKS = 300;

  /** {@code Raids#createOrExtendRaid}: the raid's centre averages village blocks this close. */
  public static final int RAID_CENTER_RANGE = 64;

  /** {@code Raid#updateRaiders}: a raider this far from the centre (12544 = 112²) leaves. */
  public static final int RAID_RAIDER_RANGE = 112;

  /** {@code Raid.RAID_TIMEOUT_TICKS}: a raid stops after this long. */
  public static final int RAID_TIMEOUT_TICKS = 48_000;

  /**
   * {@code ServerLevel#isVillage}: within one section (16 × 16 × 16) of a section holding a bed,
   * job site or bell a villager has claimed ({@code PoiManager#isVillageCenter}).
   */
  public static final int VILLAGE_SECTION = 16;

  public static final int TICKS_PER_SECOND = 20;
  public static final int SECONDS_PER_MINUTE = 60;
  private static final int PERCENT = 100;

  private FarmRules() {}

  /** The total of the fortress' monster weights. */
  public static int fortressWeights() {
    return FORTRESS_BLAZE
        + FORTRESS_WITHER_SKELETON
        + FORTRESS_ZOMBIFIED_PIGLIN
        + FORTRESS_MAGMA_CUBE
        + FORTRESS_SKELETON;
  }

  /** How many shulkers around stop every copy: the one hit counts, so one over the limit. */
  public static int shulkerNeverCopiesAt() {
    return SHULKER_COUNT_LIMIT + 1;
  }

  /** What each other shulker around takes off the chance of a copy, in percent. */
  public static int shulkerCopyPenaltyPercent() {
    return PERCENT / SHULKER_COUNT_LIMIT;
  }

  /** The spawner's area across: its range on each side and its own block. */
  public static int spawnerAreaWidth() {
    return SPAWNER_SPAWN_RANGE * 2 + 1;
  }

  /** The longest wait between spawner cycles: the delay is drawn below the maximum. */
  public static int spawnerLongestWaitTicks() {
    return SPAWNER_MAX_DELAY_TICKS - 1;
  }

  /** The villagers' free time before work and before bed, and the meeting time, in game ticks. */
  public static int villagerMorningTicks() {
    return VILLAGER_WORK_START_TICKS;
  }

  public static int villagerEveningTicks() {
    return VILLAGER_REST_START_TICKS - VILLAGER_MEET_END_TICKS;
  }

  public static int villagerMeetTicks() {
    return VILLAGER_MEET_END_TICKS - VILLAGER_MEET_START_TICKS;
  }

  /** The iron golem's spawn box across, and its height. */
  public static int golemSpawnWidth() {
    return GOLEM_SPAWN_REACH * 2 + 1;
  }

  public static int golemSpawnHeight() {
    return GOLEM_SPAWN_REACH_UP * 2 + 1;
  }

  /** Whole minutes in {@code gameTicks}. */
  public static int minutes(int gameTicks) {
    return gameTicks / (TICKS_PER_SECOND * SECONDS_PER_MINUTE);
  }

  /** {@code gameTicks} in seconds, rounded to the nearest whole second. */
  public static int roundSeconds(int gameTicks) {
    return Math.round((float) gameTicks / TICKS_PER_SECOND);
  }
}
