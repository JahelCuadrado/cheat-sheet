package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.mobfarms.FarmRules;
import com.einsteinmods.cheatsheet.core.mobfarms.FortressPiece;
import com.einsteinmods.cheatsheet.core.mobfarms.MobFarm;
import com.einsteinmods.cheatsheet.core.mobfarms.MobSpace;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.AABB;

/**
 * The farm numbers ({@link FarmRules}) against the running game: what vanilla exposes (mob caps,
 * dimension light limits, structure spawns, a spawner's settings, villager food, hitboxes, raid
 * waves, portal size) is compared, and where to spawn is asked of the game's own spawn rules in a
 * test world. Each farm's item icons must exist too.
 */
public final class FarmRulesGameTest {
  /**
   * Ticks a light check may take: the light engine runs on its own thread, so these checks retry
   * until the light has settled instead of reading it at a fixed tick.
   */
  private static final int LIGHT_TICKS = 5;

  private static final float SIZE_TOLERANCE = 0.001F;

  @GameTest
  public void mobCapAndDistancesMatch(GameTestHelper helper) {
    MobCategory monster = MobCategory.MONSTER;
    expect(helper, monster.getMaxInstancesPerChunk(), FarmRules.MONSTER_CAP, "monster cap");
    expect(helper, monster.getDespawnDistance(), FarmRules.INSTANT_DESPAWN_DISTANCE, "despawn");
    expect(helper, monster.getNoDespawnDistance(), FarmRules.RANDOM_DESPAWN_DISTANCE, "no despawn");
    helper.succeed();
  }

  @GameTest
  public void dimensionLightLimitsMatch(GameTestHelper helper) {
    Registry<DimensionType> types =
        helper.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
    DimensionType overworld = types.getValueOrThrow(BuiltinDimensionTypes.OVERWORLD);
    DimensionType end = types.getValueOrThrow(BuiltinDimensionTypes.END);
    DimensionType nether = types.getValueOrThrow(BuiltinDimensionTypes.NETHER);
    expect(
        helper,
        overworld.monsterSpawnBlockLightLimit(),
        FarmRules.MONSTER_BLOCK_LIGHT,
        "overworld block light");
    expect(helper, end.monsterSpawnBlockLightLimit(), FarmRules.MONSTER_BLOCK_LIGHT, "end");
    expect(
        helper,
        nether.monsterSpawnLightTest().maxInclusive(),
        FarmRules.NETHER_MONSTER_LIGHT,
        "nether light");
    expect(
        helper,
        nether.monsterSpawnLightTest().minInclusive(),
        FarmRules.NETHER_MONSTER_LIGHT,
        "nether light is fixed");
    helper.assertValueEqual(
        (double) nether.ambientLight(), (double) NETHER_AMBIENT_LIGHT, "nether ambient light");
    helper.assertValueEqual(
        blazeMaxLight(nether.ambientLight()), FarmRules.BLAZE_MAX_LIGHT, "blaze light");
    helper.assertTrue(end.hasSkyLight(), "the End has skylight: its farm text must not deny a sky");
    helper.succeed();
  }

  /** The Nether's ambient light, on which {@link FarmRules#BLAZE_MAX_LIGHT} rests. */
  private static final float NETHER_AMBIENT_LIGHT = 0.1F;

  private static final int MAX_LIGHT = 15;
  private static final double HALF = 0.5;

  /**
   * The brightest light at which {@code Monster#getWalkTargetValue} is still 0 or more, with {@code
   * LevelReader#getLightLevelDependentMagicValue}'s curve blended with the dimension's ambient
   * light.
   */
  private static int blazeMaxLight(float ambient) {
    int brightest = -1;
    for (int light = 0; light <= MAX_LIGHT; light++) {
      float level = (float) light / MAX_LIGHT;
      float curve = level / (4.0F - 3.0F * level);
      float value = curve + ambient * (1.0F - curve);
      if (-(value - HALF) >= 0.0F) {
        brightest = light;
      }
    }
    return brightest;
  }

  @GameTest
  public void structureSpawnsMatch(GameTestHelper helper) {
    Registry<Structure> structures =
        helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
    StructureSpawnOverride hut =
        structures
            .getValueOrThrow(BuiltinStructures.SWAMP_HUT)
            .spawnOverrides()
            .get(MobCategory.MONSTER);
    helper.assertValueEqual(
        hut.boundingBox(), StructureSpawnOverride.BoundingBoxType.PIECE, "hut box");
    helper.assertValueEqual(weights(hut), Map.of("witch", 1), "hut monsters");
    StructureSpawnOverride monument =
        structures
            .getValueOrThrow(BuiltinStructures.OCEAN_MONUMENT)
            .spawnOverrides()
            .get(MobCategory.MONSTER);
    helper.assertValueEqual(
        monument.boundingBox(), StructureSpawnOverride.BoundingBoxType.STRUCTURE, "monument box");
    helper.assertValueEqual(weights(monument), Map.of("guardian", 1), "monument monsters");
    StructureSpawnOverride fortress =
        structures
            .getValueOrThrow(BuiltinStructures.FORTRESS)
            .spawnOverrides()
            .get(MobCategory.MONSTER);
    helper.assertValueEqual(
        fortress.boundingBox(), StructureSpawnOverride.BoundingBoxType.PIECE, "fortress box");
    helper.assertValueEqual(
        weights(fortress),
        Map.of(
            "blaze", FarmRules.FORTRESS_BLAZE,
            "wither_skeleton", FarmRules.FORTRESS_WITHER_SKELETON,
            "zombified_piglin", FarmRules.FORTRESS_ZOMBIFIED_PIGLIN,
            "magma_cube", FarmRules.FORTRESS_MAGMA_CUBE,
            "skeleton", FarmRules.FORTRESS_SKELETON),
        "fortress monsters");
    helper.succeed();
  }

  /**
   * {@link FortressPiece}'s sizes are each {@code NetherFortressPieces} class's own {@code WIDTH},
   * {@code DEPTH} and {@code HEIGHT}: private constants, read by reflection.
   */
  @GameTest
  public void fortressPieceSizesMatch(GameTestHelper helper) {
    for (FortressPiece piece : FortressPiece.ALL) {
      for (String name : piece.vanillaClasses()) {
        Class<?> type = fortressPieceClass(name);
        helper.assertValueEqual(sizeOf(type, "WIDTH"), piece.width(), name + " width");
        helper.assertValueEqual(sizeOf(type, "DEPTH"), piece.depth(), name + " depth");
        helper.assertValueEqual(sizeOf(type, "HEIGHT"), piece.height(), name + " height");
      }
    }
    helper.succeed();
  }

  private static Class<?> fortressPieceClass(String name) {
    for (Class<?> type : NetherFortressPieces.class.getDeclaredClasses()) {
      if (type.getSimpleName().equals(name)) {
        return type;
      }
    }
    throw new IllegalStateException("no fortress piece " + name);
  }

  private static int sizeOf(Class<?> type, String constant) {
    try {
      Field field = type.getDeclaredField(constant);
      field.setAccessible(true);
      return field.getInt(null);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(type.getSimpleName() + " has no " + constant, exception);
    }
  }

  /**
   * Inside a wither rose only the wither skeleton may spawn: {@code EntityType#isBlockDangerous}
   * spares it through its immunity tag and {@code NaturalSpawner#isValidEmptySpawnBlock} rejects
   * dangerous blocks. Asked of every mob the fortress lists, on each floor a rose grows on; the
   * floor itself must let them all spawn, or a rose would prove nothing.
   */
  @GameTest
  public void onlyWitherSkeletonsSpawnInWitherRoses(GameTestHelper helper) {
    Registry<Structure> structures =
        helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
    BlockPos floor = new BlockPos(1, 1, 1);
    BlockPos spawn = floor.above();
    BlockPos at = helper.absolutePos(spawn);
    var fortress =
        structures
            .getValueOrThrow(BuiltinStructures.FORTRESS)
            .spawnOverrides()
            .get(MobCategory.MONSTER);
    for (Block ground : List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.NETHERRACK)) {
      helper.setBlock(floor, ground);
      helper.assertTrue(
          ground.defaultBlockState().is(BlockTags.SUPPORTS_WITHER_ROSE), ground + " holds a rose");
      for (var entry : fortress.spawns().unwrap()) {
        EntityType<?> type = entry.value().type();
        helper.setBlock(spawn, Blocks.AIR);
        helper.assertTrue(
            SpawnPlacements.isSpawnPositionOk(type, helper.getLevel(), at), type + " on " + ground);
        helper.setBlock(spawn, Blocks.WITHER_ROSE);
        helper.assertValueEqual(
            SpawnPlacements.isSpawnPositionOk(type, helper.getLevel(), at),
            type == EntityTypes.WITHER_SKELETON,
            type + " in a rose on " + ground);
      }
    }
    helper.succeed();
  }

  /**
   * The fortress' two spawn lists hold equal entries: bug MC-271630 (packs split between nether
   * bricks and other floors, so bricks around a rose platform lowered its rate) came from {@code
   * NaturalSpawner#canSpawnMobAt} finding no entry of one list in the other. If this ever fails,
   * the farm's text must warn about bricks around a rose platform.
   */
  @GameTest
  public void fortressSpawnListsMatch(GameTestHelper helper) {
    var fortress =
        helper
            .getLevel()
            .registryAccess()
            .lookupOrThrow(Registries.STRUCTURE)
            .getValueOrThrow(BuiltinStructures.FORTRESS)
            .spawnOverrides()
            .get(MobCategory.MONSTER);
    for (var entry : fortress.spawns().unwrap()) {
      helper.assertTrue(
          NetherFortressStructure.FORTRESS_ENEMIES.contains(entry.value()),
          entry.value() + " is only in the structure's list");
    }
    helper.succeed();
  }

  /** Each mob's weight in an override, by entity path. */
  private static Map<String, Integer> weights(StructureSpawnOverride override) {
    Map<String, Integer> weights = new LinkedHashMap<>();
    for (var entry : override.spawns().unwrap()) {
      MobSpawnSettings.SpawnerData data = entry.value();
      weights.put(EntityType.getKey(data.type()).getPath(), entry.weight());
    }
    return weights;
  }

  @GameTest
  public void spawnerDefaultsMatch(GameTestHelper helper) {
    BlockPos pos = helper.absolutePos(BlockPos.ZERO);
    CompoundTag tag =
        new SpawnerBlockEntity(pos, Blocks.SPAWNER.defaultBlockState())
            .saveWithoutMetadata(helper.getLevel().registryAccess());
    expectShort(helper, tag, "MinSpawnDelay", FarmRules.SPAWNER_MIN_DELAY_TICKS);
    expectShort(helper, tag, "MaxSpawnDelay", FarmRules.SPAWNER_MAX_DELAY_TICKS);
    expectShort(helper, tag, "SpawnCount", FarmRules.SPAWNER_TRIES);
    expectShort(helper, tag, "MaxNearbyEntities", FarmRules.SPAWNER_MAX_NEARBY);
    expectShort(helper, tag, "RequiredPlayerRange", FarmRules.SPAWNER_PLAYER_RANGE);
    expectShort(helper, tag, "SpawnRange", FarmRules.SPAWNER_SPAWN_RANGE);
    helper.succeed();
  }

  private static void expectShort(GameTestHelper helper, CompoundTag tag, String key, int value) {
    helper.assertValueEqual((int) tag.getShortOr(key, (short) -1), value, key);
  }

  /**
   * Where a zombie may spawn, asked of the game: on a full solid top, not on glass, leaves, a
   * bottom slab or ice; not in a block holding a button, a pressure plate, a rail, dust or water.
   */
  @GameTest
  public void spawnSurfacesMatch(GameTestHelper helper) {
    Map<String, Boolean> floors = new LinkedHashMap<>();
    floors.put("stone", true);
    floors.put("cobblestone", true);
    floors.put("smooth_stone_slab[type=top]", true);
    floors.put("glass", false);
    floors.put("oak_leaves[persistent=true]", false);
    floors.put("smooth_stone_slab[type=bottom]", false);
    floors.put("ice", false);
    Map<String, Boolean> inside = new LinkedHashMap<>();
    inside.put("air", true);
    inside.put("stone_button[face=floor]", false);
    inside.put("oak_pressure_plate", false);
    inside.put("rail", false);
    inside.put("redstone_wire", false);
    inside.put("water", false);
    BlockPos floor = new BlockPos(1, 1, 1);
    BlockPos spawn = floor.above();
    for (Map.Entry<String, Boolean> entry : floors.entrySet()) {
      helper.setBlock(floor, DioramaPlacer.state(entry.getKey()));
      helper.assertValueEqual(
          SpawnPlacements.isSpawnPositionOk(
              EntityTypes.ZOMBIE, helper.getLevel(), helper.absolutePos(spawn)),
          entry.getValue(),
          "zombie on " + entry.getKey());
    }
    helper.setBlock(floor, Blocks.STONE);
    for (Map.Entry<String, Boolean> entry : inside.entrySet()) {
      helper.setBlock(spawn, DioramaPlacer.state(entry.getKey()));
      helper.assertValueEqual(
          SpawnPlacements.isSpawnPositionOk(
              EntityTypes.ZOMBIE, helper.getLevel(), helper.absolutePos(spawn)),
          entry.getValue(),
          "zombie in " + entry.getKey());
    }
    helper.succeed();
  }

  /**
   * A spawner's zombie needs block light 0: in a closed dark box its spawn rule holds; with a light
   * of 1 beside it, it fails every time.
   */
  @GameTest(maxTicks = LIGHT_TICKS * 8)
  public void spawnerZombiesNeedDarkness(GameTestHelper helper) {
    BlockPos spot = new BlockPos(2, 2, 2);
    for (int x = 1; x <= 3; x++) {
      for (int y = 1; y <= 4; y++) {
        for (int z = 1; z <= 3; z++) {
          helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        }
      }
    }
    helper.setBlock(spot, Blocks.AIR);
    helper.setBlock(spot.above(), Blocks.AIR);
    helper
        .startSequence()
        .thenWaitUntil(
            () -> {
              BlockPos at = helper.absolutePos(spot);
              helper.assertValueEqual(
                  helper.getLevel().getBrightness(LightLayer.BLOCK, at), 0, "block light");
              helper.assertValueEqual(
                  helper.getLevel().getBrightness(LightLayer.SKY, at), 0, "sky light");
              helper.assertTrue(
                  SpawnPlacements.checkSpawnRules(
                      EntityTypes.ZOMBIE,
                      helper.getLevel(),
                      EntitySpawnReason.SPAWNER,
                      at,
                      helper.getLevel().getRandom()),
                  "a dark spot spawns zombies");
              helper.setBlock(spot.above(), DioramaPlacer.state("light[level=2]"));
            })
        .thenWaitUntil(
            () -> {
              BlockPos at = helper.absolutePos(spot);
              helper.assertValueEqual(
                  helper.getLevel().getBrightness(LightLayer.BLOCK, at), 1, "block light");
              for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                helper.assertFalse(
                    SpawnPlacements.checkSpawnRules(
                        EntityTypes.ZOMBIE,
                        helper.getLevel(),
                        EntitySpawnReason.SPAWNER,
                        at,
                        helper.getLevel().getRandom()),
                    "block light 1 still spawns zombies");
              }
            })
        .thenSucceed();
  }

  private static final int ATTEMPTS = 50;

  /** A spot in the Nether's rock, away from lava and glowstone; light is read off it. */
  private static final int NETHER_PROBE_Y = 110;

  /**
   * How bright the Nether may be for each mob a fortress lists, asked of the game at each limit and
   * one above it: skeletons follow the monster light rule, as wither skeletons do, while zombified
   * piglins and blazes only need {@code Monster#getWalkTargetValue} and magma cubes nothing.
   */
  @GameTest(maxTicks = LIGHT_TICKS * 8)
  public void fortressMobsHaveTheirOwnLightLimits(GameTestHelper helper) {
    ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
    helper.assertTrue(nether != null, "the test server has a Nether");
    BlockPos spot = helper.absolutePos(new BlockPos(0, 0, 0)).atY(NETHER_PROBE_Y);
    ChunkPos chunk = ChunkPos.containing(spot);
    nether.setChunkForced(chunk.x(), chunk.z(), true);
    BlockPos.betweenClosed(spot.offset(-1, -1, -1), spot.offset(1, 1, 1))
        .forEach(pos -> nether.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));
    var sequence = helper.startSequence();
    int[] lights = {
      FarmRules.NETHER_MONSTER_LIGHT,
      FarmRules.NETHER_MONSTER_LIGHT + 1,
      FarmRules.BLAZE_MAX_LIGHT,
      FarmRules.BLAZE_MAX_LIGHT + 1
    };
    for (int light : lights) {
      sequence
          .thenExecute(
              () ->
                  nether.setBlockAndUpdate(spot, DioramaPlacer.state("light[level=" + light + "]")))
          .thenWaitUntil(
              () -> {
                expect(helper, nether.getBrightness(LightLayer.BLOCK, spot), light, "probe light");
                checkFortressLight(helper, nether, spot, light);
              });
    }
    sequence.thenExecute(() -> nether.setChunkForced(chunk.x(), chunk.z(), false)).thenSucceed();
  }

  private static void checkFortressLight(
      GameTestHelper helper, ServerLevel nether, BlockPos spot, int light) {
    boolean dim = light <= FarmRules.NETHER_MONSTER_LIGHT;
    for (EntityType<? extends Mob> type :
        List.of(EntityTypes.SKELETON, EntityTypes.WITHER_SKELETON)) {
      for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
        helper.assertValueEqual(
            SpawnPlacements.checkSpawnRules(
                type, nether, EntitySpawnReason.NATURAL, spot, nether.getRandom()),
            dim,
            type + " at light " + light);
      }
    }
    boolean walkable = light <= FarmRules.BLAZE_MAX_LIGHT;
    helper.assertValueEqual(
        acceptsSpot(EntityTypes.BLAZE, nether, spot), walkable, "blaze at light " + light);
    helper.assertValueEqual(
        acceptsSpot(EntityTypes.ZOMBIFIED_PIGLIN, nether, spot),
        walkable,
        "zombified piglin at light " + light);
    helper.assertTrue(
        acceptsSpot(EntityTypes.MAGMA_CUBE, nether, spot), "magma cube at light " + light);
  }

  /** {@code Mob#checkSpawnRules}, as {@code NaturalSpawner#isValidPositionForMob} asks it. */
  private static boolean acceptsSpot(
      EntityType<? extends Mob> type, ServerLevel level, BlockPos spot) {
    Mob mob = type.create(level, EntitySpawnReason.NATURAL);
    mob.snapTo(spot.getX() + HALF, spot.getY(), spot.getZ() + HALF);
    return mob.checkSpawnRules(level, EntitySpawnReason.NATURAL);
  }

  /** Sensors tick every 20 game ticks, from a random start. */
  private static final int SENSOR_TICKS = 45;

  private static final int SIGHT_CELL_HEIGHT = 3;

  /**
   * A villager only fears a zombie it sees, and {@code LivingEntity#hasLineOfSight} clips against
   * collision shapes: inside a glass cell the zombie beside it is no hostile, and it becomes one
   * when the pane at eye height goes. The panes left keep it in: out of the cell it wanders behind
   * the glass, which hides the zombie again.
   */
  @GameTest(maxTicks = SENSOR_TICKS * 3)
  public void glassHidesAZombieFromAVillager(GameTestHelper helper) {
    BlockPos inside = new BlockPos(2, 2, 2);
    BlockPos zombieAt = new BlockPos(6, 2, 2);
    for (int x = 0; x < 8; x++) {
      for (int z = 0; z < 8; z++) {
        helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
      }
    }
    for (int y = 0; y < SIGHT_CELL_HEIGHT; y++) {
      for (BlockPos wall : List.of(inside.north(), inside.south(), inside.west(), inside.east())) {
        helper.setBlock(wall.above(y), Blocks.GLASS);
      }
    }
    Villager villager = helper.spawn(EntityTypes.VILLAGER, inside);
    helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, zombieAt);
    helper
        .startSequence()
        .thenIdle(SENSOR_TICKS)
        .thenExecute(
            () -> {
              helper.assertFalse(
                  villager.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE),
                  "the villager sees a zombie through glass");
              helper.setBlock(inside.east().above(), Blocks.AIR);
            })
        .thenIdle(SENSOR_TICKS)
        .thenExecute(
            () ->
                helper.assertTrue(
                    villager.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE),
                    "the villager misses a zombie in plain sight"))
        .thenSucceed();
  }

  @GameTest
  public void villagerFoodMatches(GameTestHelper helper) {
    expect(helper, Villager.BREEDING_FOOD_THRESHOLD, FarmRules.BREEDING_FOOD, "breeding food");
    expect(helper, nutrition(Items.BREAD), FarmRules.BREAD_FOOD, "bread");
    for (Item crop : List.of(Items.CARROT, Items.POTATO, Items.BEETROOT)) {
      expect(helper, nutrition(crop), FarmRules.CROP_FOOD, crop.toString());
    }
    helper.succeed();
  }

  private static int nutrition(Item item) {
    var food = item.components().get(DataComponents.VILLAGER_FOOD);
    return food == null ? -1 : food.nutrition();
  }

  @GameTest
  public void hitboxesMatch(GameTestHelper helper) {
    expectSize(helper, EntityTypes.ENDERMAN.getHeight(), FarmRules.ENDERMAN_HEIGHT, "enderman");
    expectSize(
        helper,
        EntityTypes.WITHER_SKELETON.getHeight(),
        FarmRules.WITHER_SKELETON_HEIGHT,
        "wither skeleton");
    expectSize(helper, EntityTypes.SPIDER.getWidth(), FarmRules.SPIDER_WIDTH, "spider");
    expectSize(
        helper, EntityTypes.SLIME.getWidth() * BIGGEST_SLIME, FarmRules.BIG_SLIME_SIZE, "slime");
    expectSize(
        helper,
        EntityTypes.MAGMA_CUBE.getWidth() * BIGGEST_SLIME,
        FarmRules.BIG_SLIME_SIZE,
        "magma cube");
    helper.succeed();
  }

  /**
   * Each farm's "room needed" is the game's spawn box ({@code EntityType#getSpawnAABB}: slimes and
   * magma cubes check size 4 whatever size they get), and each mob has its spawn egg.
   */
  @GameTest
  public void mobSpacesMatch(GameTestHelper helper) {
    for (MobFarm farm : MobFarm.values()) {
      for (MobSpace space : farm.spaces()) {
        EntityType<?> type =
            BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(space.entityId()));
        AABB box = type.getSpawnAABB(HALF_BLOCK, 0.0, HALF_BLOCK);
        expectSize(helper, (float) box.getXsize(), space.spawnWidth(), space.entityId() + " width");
        expectSize(
            helper, (float) box.getYsize(), space.spawnHeight(), space.entityId() + " height");
        helper.assertTrue(
            BuiltInRegistries.ITEM.containsKey(
                Identifier.withDefaultNamespace(space.path() + "_spawn_egg")),
            space.entityId() + " spawn egg");
      }
    }
    helper.succeed();
  }

  private static void expectSize(GameTestHelper helper, float actual, float shown, String what) {
    helper.assertTrue(
        Math.abs(actual - shown) < SIZE_TOLERANCE,
        what + " is " + actual + ", the sheet says " + shown);
  }

  /**
   * A villager's bed is a wool bed's head ({@code PoiTypes.HOME}); a straw bed is no villager bed.
   */
  @GameTest
  public void villagerBedsAreWoolBeds(GameTestHelper helper) {
    BlockState head = DioramaPlacer.state("red_bed").setValue(AbstractBedBlock.PART, BedPart.HEAD);
    helper.assertTrue(
        PoiTypes.forState(head).filter(poi -> poi.is(PoiTypes.HOME)).isPresent(),
        "a red bed is a villager bed");
    for (BlockState straw : Blocks.STRAW_BED.getStateDefinition().getPossibleStates()) {
      helper.assertFalse(
          PoiTypes.forState(straw).filter(poi -> poi.is(PoiTypes.HOME)).isPresent(),
          "a straw bed is a villager bed: " + straw);
    }
    helper.succeed();
  }

  /**
   * An iron golem stands on a full top with {@link FarmRules#GOLEM_SPACE_HIGH} free blocks: a block
   * in the third stops it, and so does a bottom slab under it ({@code
   * IronGolem#checkSpawnObstruction}).
   */
  @GameTest
  public void golemNeedsThreeFreeBlocksOnAFullTop(GameTestHelper helper) {
    BlockPos floor = new BlockPos(1, 1, 1);
    BlockPos feet = floor.above();
    IronGolem golem =
        EntityTypes.IRON_GOLEM.create(helper.getLevel(), EntitySpawnReason.MOB_SUMMONED);
    helper.assertTrue(golem != null, "no golem");
    BlockPos at = helper.absolutePos(feet);
    golem.snapTo(at.getX() + HALF_BLOCK, at.getY(), at.getZ() + HALF_BLOCK, 0.0F, 0.0F);
    helper.setBlock(floor, Blocks.STONE);
    helper.assertTrue(golem.checkSpawnObstruction(helper.getLevel()), "golem on stone");
    helper.setBlock(feet.above(FarmRules.GOLEM_SPACE_HIGH - 1), Blocks.STONE);
    helper.assertFalse(
        golem.checkSpawnObstruction(helper.getLevel()), "golem under a block in its third block");
    helper.setBlock(feet.above(FarmRules.GOLEM_SPACE_HIGH - 1), Blocks.AIR);
    helper.setBlock(floor, DioramaPlacer.state("smooth_stone_slab[type=bottom]"));
    helper.assertFalse(golem.checkSpawnObstruction(helper.getLevel()), "golem on a bottom slab");
    golem.discard();
    helper.succeed();
  }

  private static final double HALF_BLOCK = 0.5;

  /** {@code AbstractCubeMob#setSpawnSize}: natural sizes are 1, 2 or 4. */
  private static final int BIGGEST_SLIME = 4;

  private static void expectSize(GameTestHelper helper, float actual, String shown, String what) {
    helper.assertTrue(
        Math.abs(actual - Float.parseFloat(shown)) < SIZE_TOLERANCE,
        what + " is " + actual + ", the sheet says " + shown);
  }

  @GameTest
  public void raidWavesAndPortalMatch(GameTestHelper helper) {
    Raid raid = new Raid(helper.absolutePos(BlockPos.ZERO), Difficulty.NORMAL);
    expect(helper, raid.getNumGroups(Difficulty.EASY), FarmRules.RAID_WAVES_EASY, "easy waves");
    expect(
        helper, raid.getNumGroups(Difficulty.NORMAL), FarmRules.RAID_WAVES_NORMAL, "normal waves");
    expect(helper, raid.getNumGroups(Difficulty.HARD), FarmRules.RAID_WAVES_HARD, "hard waves");
    expect(helper, PortalShape.MAX_WIDTH, FarmRules.PORTAL_MAX_INSIDE, "portal width");
    expect(helper, PortalShape.MAX_HEIGHT, FarmRules.PORTAL_MAX_INSIDE, "portal height");
    helper.succeed();
  }

  @GameTest
  public void biomeTagsMatch(GameTestHelper helper) {
    var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
    for (var swamp : List.of(Biomes.SWAMP, Biomes.MANGROVE_SWAMP)) {
      helper.assertTrue(
          biomes.getOrThrow(swamp).is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS),
          swamp + " spawns surface slimes");
    }
    for (var river : List.of(Biomes.RIVER, Biomes.FROZEN_RIVER)) {
      helper.assertTrue(
          biomes.getOrThrow(river).is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS),
          river + " spawns more drowned");
    }
    helper.assertFalse(
        biomes.getOrThrow(Biomes.OCEAN).is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS),
        "oceans spawn drowned like rivers");
    helper.assertFalse(
        biomes.getOrThrow(Biomes.DRIPSTONE_CAVES).is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS),
        "dripstone caves spawn drowned like rivers");
    helper.succeed();
  }

  /** Every icon and mob a farm card names exists. */
  @GameTest
  public void farmItemsAndMobsExist(GameTestHelper helper) {
    for (MobFarm farm : MobFarm.values()) {
      for (var fact : farm.facts()) {
        helper.assertTrue(
            BuiltInRegistries.ITEM.containsKey(Identifier.withDefaultNamespace(fact.iconItemId())),
            farm + " icon " + fact.iconItemId());
      }
      for (var mob : farm.mobs()) {
        helper.assertTrue(
            BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(mob.entityId())),
            farm + " mob " + mob.entityId());
        mob.heldItemId()
            .ifPresent(
                item ->
                    helper.assertTrue(
                        BuiltInRegistries.ITEM.containsKey(Identifier.withDefaultNamespace(item)),
                        farm + " held " + item));
      }
    }
    helper.succeed();
  }

  private static void expect(GameTestHelper helper, int actual, int expected, String what) {
    helper.assertValueEqual(actual, expected, what);
  }
}
