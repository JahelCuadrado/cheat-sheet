package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.mobfarms.FarmRules;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The shulker numbers ({@link FarmRules}) against the game's own shulker: real hits by a bullet,
 * with its teleport replaced where a test needs it to land in a known place. {@code Shulker} keeps
 * these numbers as private literals, so each is measured.
 */
public final class ShulkerRulesGameTest {
  /** Its shulker and player must not meet the shulkers the other tests count. */
  private static final String OWN_BATCH = "cheatsheet-gametest:morning";

  private static final BlockPos SPOT = new BlockPos(3, 3, 3);
  private static final float HIT = 1.0F;

  /** {@code Shulker#setRawPeekAmount}: fully open. */
  private static final byte OPEN_PEEK = 100;

  /** Hits per count of shulkers: at 1 chance in 5, none in 60 is 1.5 in a million. */
  private static final int COPY_TRIALS = 60;

  /** Hits for the low health chance: its standard error is 0.005 around 0.25. */
  private static final int LOW_HEALTH_HITS = 8000;

  private static final double LOW_HEALTH_RATE_MIN = 0.225;
  private static final double LOW_HEALTH_RATE_MAX = 0.285;

  /** Real teleports tried: few land, as only the 8 × 8 floor is sure to be there. */
  private static final int TELEPORT_HITS = 2000;

  private static final int TARGET_WAIT_TICKS = 100;
  private static final long SEED = 42L;
  private static final int AXES = 3;

  /** {@code Shulker}'s protected steps, reachable from a test. */
  private static class ProbeShulker extends Shulker {
    ProbeShulker(Level level) {
      super(EntityTypes.SHULKER, level);
    }

    void open() {
      entityData.set(DATA_PEEK_ID, OPEN_PEEK);
    }

    boolean teleport() {
      return teleportSomewhere();
    }

    @Nullable Direction surfaceAt(BlockPos pos) {
      return findAttachableSurface(pos);
    }
  }

  /** Its teleport always works and lands at the far corner of the range: the worst for a count. */
  private static final class JumpingShulker extends ProbeShulker {
    private int teleports;

    JumpingShulker(Level level) {
      super(level);
    }

    @Override
    protected boolean teleportSomewhere() {
      teleports++;
      int range = FarmRules.SHULKER_TELEPORT_RANGE;
      setPos(getX() + range, getY() + range, getZ() + range);
      return true;
    }
  }

  /** Counts its tries to teleport, which never move it, so the same shulker takes every hit. */
  private static final class StayingShulker extends ProbeShulker {
    private int teleports;

    StayingShulker(Level level) {
      super(level);
    }

    @Override
    protected boolean teleportSomewhere() {
      teleports++;
      return false;
    }
  }

  private static void hit(ServerLevel level, Shulker shulker) {
    ShulkerBullet bullet = new ShulkerBullet(EntityTypes.SHULKER_BULLET, level);
    shulker.damageCooldownTime = 0;
    shulker.hurtServer(level, level.damageSources().mobProjectile(bullet, null), HIT);
  }

  private static <T extends Entity> T placed(ServerLevel level, T entity, BlockPos at) {
    entity.snapTo(at.getX() + HALF, at.getY(), at.getZ() + HALF);
    level.addFreshEntity(entity);
    return entity;
  }

  private static final double HALF = 0.5;

  private static List<? extends Shulker> shulkers(GameTestHelper helper) {
    int reach = 2 * (FarmRules.SHULKER_COUNT_RANGE + 2);
    return helper
        .getLevel()
        .getEntities(
            EntityTypes.SHULKER,
            new AABB(helper.absolutePos(SPOT)).inflate(reach),
            Entity::isAlive);
  }

  private static void clearShulkers(GameTestHelper helper) {
    shulkers(helper).forEach(Entity::discard);
  }

  @GameTest
  public void onlyAnOpenShulkerIsCopied(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    BlockPos at = helper.absolutePos(SPOT);
    JumpingShulker closed = placed(level, new JumpingShulker(level), at);
    hit(level, closed);
    helper.assertValueEqual(closed.teleports, 0, "a closed shulker's teleports");
    helper.assertValueEqual(shulkers(helper).size(), 1, "shulkers after hitting a closed one");
    clearShulkers(helper);

    JumpingShulker open = placed(level, new JumpingShulker(level), at);
    open.setComponent(DataComponents.SHULKER_COLOR, DyeColor.RED);
    open.open();
    Vec3 was = open.position();
    hit(level, open);
    helper.assertValueEqual(open.teleports, 1, "an open shulker's teleports");
    Shulker copy =
        shulkers(helper).stream()
            .filter(shulker -> shulker != open)
            .findFirst()
            .orElseThrow(() -> helper.assertionException("no copy"));
    helper.assertValueEqual(copy.position(), was, "the copy's place");
    helper.assertValueEqual(copy.getColor(), DyeColor.RED, "the copy's colour");
    helper.assertValueEqual(copy.getHealth(), copy.getMaxHealth(), "the copy's health");
    clearShulkers(helper);
    helper.succeed();
  }

  /**
   * The count runs after the teleport, on the old spot's box: the one hit is in it even at the far
   * corner of its range, the copy is not yet. So it alone always copies, with {@link
   * FarmRules#SHULKER_COUNT_LIMIT} others never, and with one fewer sometimes.
   */
  @GameTest
  public void nearbyShulkersStopCopies(GameTestHelper helper) {
    int limit = FarmRules.SHULKER_COUNT_LIMIT;
    int range = FarmRules.SHULKER_COUNT_RANGE;
    helper.assertValueEqual(copies(helper, 0, range), COPY_TRIALS, "copies alone");
    helper.assertTrue(copies(helper, limit - 1, range) > 0, "one under the limit still copies");
    helper.assertValueEqual(copies(helper, limit, range), 0, "copies at the limit");
    helper.assertValueEqual(copies(helper, limit, range + 1), COPY_TRIALS, "copies out of range");
    helper.succeed();
  }

  /** New shulkers in {@link #COPY_TRIALS} hits with {@code others} shulkers {@code away} west. */
  private static int copies(GameTestHelper helper, int others, int away) {
    ServerLevel level = helper.getLevel();
    BlockPos at = helper.absolutePos(SPOT);
    int copies = 0;
    for (int trial = 0; trial < COPY_TRIALS; trial++) {
      for (int other = 0; other < others; other++) {
        placed(level, new ProbeShulker(level), at.west(away));
      }
      JumpingShulker shulker = placed(level, new JumpingShulker(level), at);
      shulker.open();
      hit(level, shulker);
      copies += shulkers(helper).size() - others - 1;
      clearShulkers(helper);
    }
    return copies;
  }

  /**
   * Below half health after a hit, 1 hit in {@link FarmRules#SHULKER_ONLY_TELEPORT_ONE_IN} only
   * teleports it. Closed, a bullet's own teleport never runs, so every try counted is this one.
   */
  @GameTest
  public void lowHealthHitsOnlyTeleport(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    StayingShulker shulker = placed(level, new StayingShulker(level), helper.absolutePos(SPOT));
    float half = shulker.getMaxHealth() / 2.0F;
    for (int hit = 0; hit < LOW_HEALTH_HITS; hit++) {
      shulker.setHealth(half + HIT);
      hit(level, shulker);
    }
    helper.assertValueEqual(shulker.teleports, 0, "teleports left at exactly half health");
    for (int hit = 0; hit < LOW_HEALTH_HITS; hit++) {
      shulker.setHealth(half);
      hit(level, shulker);
    }
    double rate = (double) shulker.teleports / LOW_HEALTH_HITS;
    double expected = 1.0 / FarmRules.SHULKER_ONLY_TELEPORT_ONE_IN;
    helper.assertTrue(
        rate > LOW_HEALTH_RATE_MIN && rate < LOW_HEALTH_RATE_MAX,
        "low health teleports " + rate + " of hits, the sheet says " + expected);
    clearShulkers(helper);
    helper.succeed();
  }

  /**
   * Below the world's floor every spot fails, but each try still draws one random number per axis
   * from the range: the shulker's next number after a failed teleport tells how many it drew.
   */
  @GameTest
  public void teleportTriesAndRangeMatch(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    int range = FarmRules.SHULKER_TELEPORT_RANGE;
    ProbeShulker shulker = new ProbeShulker(level);
    shulker.snapTo(HALF, level.getMinY() - 2 * range, HALF);
    shulker.getRandom().setSeed(SEED);
    helper.assertFalse(shulker.teleport(), "teleport below the world");
    RandomSource expected = RandomSource.create(SEED);
    for (int draw = 0; draw < FarmRules.SHULKER_TELEPORT_TRIES * AXES; draw++) {
      Mth.randomBetweenInclusive(expected, -range, range);
    }
    helper.assertValueEqual(
        shulker.getRandom().nextLong(), expected.nextLong(), "random numbers a teleport draws");
    helper.assertValueEqual(Shulker.MAX_TELEPORT_DISTANCE, range, "teleport range");
    helper.succeed();
  }

  /** A spot needs a solid face and the next block free to open; bedrock is refused. */
  @GameTest
  public void teleportRoomMatches(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    ProbeShulker shulker = new ProbeShulker(level);
    BlockPos floor = new BlockPos(1, 1, 1);
    BlockPos spot = floor.above();
    helper.setBlock(floor, Blocks.STONE);
    helper.assertValueEqual(
        shulker.surfaceAt(helper.absolutePos(spot)), Direction.DOWN, "room to open");
    helper.setBlock(spot.above(), Blocks.STONE);
    helper.assertTrue(
        shulker.surfaceAt(helper.absolutePos(spot)) == null, "one free block between two");
    helper.assertTrue(
        Blocks.BEDROCK.defaultBlockState().is(BlockTags.SHULKER_DOES_NOT_TELEPORT_TO),
        "bedrock refuses shulkers");

    // A real teleport lands inside the range, on a place it can stay.
    for (int x = 0; x < EMPTY_TEST_SIDE; x++) {
      for (int z = 0; z < EMPTY_TEST_SIDE; z++) {
        helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
      }
    }
    helper.setBlock(spot.above(), Blocks.AIR);
    BlockPos at = helper.absolutePos(SPOT);
    int moved = 0;
    for (int hit = 0; hit < TELEPORT_HITS; hit++) {
      shulker.snapTo(at.getX() + HALF, at.getY(), at.getZ() + HALF);
      if (shulker.teleport()) {
        moved++;
        BlockPos landed = shulker.blockPosition();
        helper.assertTrue(
            Math.abs(landed.getX() - at.getX()) <= FarmRules.SHULKER_TELEPORT_RANGE
                && Math.abs(landed.getY() - at.getY()) <= FarmRules.SHULKER_TELEPORT_RANGE
                && Math.abs(landed.getZ() - at.getZ()) <= FarmRules.SHULKER_TELEPORT_RANGE,
            "landed at " + landed + " from " + at);
      }
    }
    helper.assertTrue(moved > 0, "no teleport worked");
    helper.succeed();
  }

  private static final int EMPTY_TEST_SIDE = 8;

  /**
   * A shulker on the floor targets a survival player it sees straight above it: its target search
   * has no limit along the side it sticks to, whatever {@code getTargetSearchArea} says, because
   * {@code NearestAttackableTargetGoal#findTarget} only uses that box for mobs, not players.
   */
  @GameTest(environment = OWN_BATCH, maxTicks = TARGET_WAIT_TICKS * 3, skyAccess = true)
  public void shulkersTargetPlayersInTheirWholeRange(GameTestHelper helper) {
    BlockPos floor = new BlockPos(3, 1, 3);
    helper.setBlock(floor, Blocks.STONE);
    Shulker shulker = helper.spawn(EntityTypes.SHULKER, floor.above());
    ServerPlayer player = survivalPlayer(helper);
    BlockPos at = helper.absolutePos(floor.above());
    player.setPos(
        at.getX() + HALF, at.getY() + FarmRules.SHULKER_TARGET_RANGE + 2.0, at.getZ() + HALF);
    helper
        .startSequence()
        .thenIdle(TARGET_WAIT_TICKS)
        .thenExecute(
            () -> {
              helper.assertTrue(shulker.getTarget() == null, "target out of range");
              player.setPos(
                  at.getX() + HALF,
                  at.getY() + FarmRules.SHULKER_TARGET_RANGE - 2.0,
                  at.getZ() + HALF);
            })
        .thenIdle(TARGET_WAIT_TICKS)
        .thenExecute(
            () -> {
              helper.assertValueEqual(shulker.getTarget(), player, "target straight above");
              helper.getLevel().getServer().getPlayerList().remove(player);
            })
        .thenSucceed();
  }

  /**
   * {@code GameTestHelper#makeMockServerPlayerInLevel}, in survival: creative is never a target.
   */
  private static ServerPlayer survivalPlayer(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    GameProfile profile = new GameProfile(UUID.randomUUID(), "test-mock-player");
    CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
    ServerPlayer player =
        new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation()) {
          @Override
          public GameType gameMode() {
            return GameType.SURVIVAL;
          }
        };
    Connection connection = new Connection(PacketFlow.SERVERBOUND);
    new EmbeddedChannel(connection);
    level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
    GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
    return player;
  }
}
