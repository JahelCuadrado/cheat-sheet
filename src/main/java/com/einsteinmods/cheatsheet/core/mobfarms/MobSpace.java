package com.einsteinmods.cheatsheet.core.mobfarms;

/**
 * The room a mob needs to spawn, in whole blocks: {@code
 * NaturalSpawner#isValidSpawnPostitionForType} and {@code BaseSpawner#serverTick} want its spawn
 * box ({@code EntityType#getSpawnAABB}: its hitbox times its spawn scale, centred on the block)
 * free of blocks. A box wider than one block reaches into the blocks around: 3 × 3. {@code
 * FarmRulesGameTest} compares each size with the game's spawn box.
 *
 * @param entityId the entity type id ({@code minecraft:spider})
 * @param width its hitbox width ({@code EntityTypes}, {@code sized})
 * @param height its hitbox height
 * @param spawnScale its {@code spawnDimensionsScale}: slimes and magma cubes check the room of the
 *     largest natural size (4), whatever size they get
 */
public record MobSpace(String entityId, float width, float height, float spawnScale) {
  private static final float BLOCK_HALF = 0.5F;

  /** {@code EntityTypes}: slimes and magma cubes check the room of size 4 ({@code 0.52 × 4}). */
  private static final float CUBE_SPAWN_SCALE = 4.0F;

  public static final MobSpace ZOMBIE = of("minecraft:zombie", 0.6F, 1.95F);
  public static final MobSpace SKELETON = of("minecraft:skeleton", 0.6F, 1.99F);
  public static final MobSpace CREEPER = of("minecraft:creeper", 0.6F, 1.7F);
  public static final MobSpace WITCH = of("minecraft:witch", 0.6F, 1.95F);
  public static final MobSpace SPIDER = of("minecraft:spider", 1.4F, 0.9F);
  public static final MobSpace CAVE_SPIDER = of("minecraft:cave_spider", 0.7F, 0.5F);
  public static final MobSpace ENDERMAN = of("minecraft:enderman", 0.6F, 2.9F);
  public static final MobSpace BLAZE = of("minecraft:blaze", 0.6F, 1.8F);
  public static final MobSpace WITHER_SKELETON = of("minecraft:wither_skeleton", 0.7F, 2.4F);
  public static final MobSpace DROWNED = of("minecraft:drowned", 0.6F, 1.95F);
  public static final MobSpace GUARDIAN = of("minecraft:guardian", 0.85F, 0.85F);
  public static final MobSpace SLIME =
      new MobSpace("minecraft:slime", 0.52F, 0.52F, CUBE_SPAWN_SCALE);
  public static final MobSpace MAGMA_CUBE =
      new MobSpace("minecraft:magma_cube", 0.52F, 0.52F, CUBE_SPAWN_SCALE);

  /** A mob checked at its own size. */
  public static MobSpace of(String entityId, float width, float height) {
    return new MobSpace(entityId, width, height, 1.0F);
  }

  /** The spawn box's width. */
  public float spawnWidth() {
    return width * spawnScale;
  }

  /** The spawn box's height. */
  public float spawnHeight() {
    return height * spawnScale;
  }

  /** Blocks across (and deep) the spawn box takes, centred on its block: 1, 3, 5... */
  public int blocksAcross() {
    float beyond = spawnWidth() / 2.0F - BLOCK_HALF;
    return beyond <= 0.0F ? 1 : 2 * (int) Math.ceil(beyond) + 1;
  }

  /** Blocks of height the spawn box takes. */
  public int blocksHigh() {
    return (int) Math.ceil(spawnHeight());
  }

  /** The entity id's path, for its spawn egg ({@code spider} → {@code spider_spawn_egg}). */
  public String path() {
    return entityId.substring(entityId.indexOf(':') + 1);
  }
}
