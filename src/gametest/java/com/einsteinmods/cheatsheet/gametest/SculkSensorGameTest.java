package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.redstone.SculkSensors;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

/**
 * The redstone tab's sculk sensor numbers ({@link SculkSensors}) against the game: the frequency
 * table against {@code VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT} for every game event, the
 * ranges and times against the sensors themselves, and in a world a sensor's signal at a distance,
 * wool cutting a vibration off and a calibrated sensor hearing only the frequency on its input.
 */
public final class SculkSensorGameTest {
  private static final String RESONANCE_PREFIX = "resonate_";

  /** Distances to compare the signal formula at, in quarter blocks up to the calibrated range. */
  private static final int QUARTERS = 4;

  /** Long enough for a vibration a few blocks away to arrive and switch a sensor on. */
  private static final int HEAR_TICKS = 10;

  @GameTest
  public void frequencyTableMatches(GameTestHelper helper) {
    Map<String, Integer> expected = new TreeMap<>();
    for (SculkSensors.Frequency frequency : SculkSensors.FREQUENCIES) {
      for (String event : frequency.gameEvents()) {
        expected.put(event, frequency.frequency());
      }
    }
    Map<String, Integer> actual = new TreeMap<>();
    BuiltInRegistries.GAME_EVENT
        .listElements()
        .forEach(
            holder -> {
              String path = holder.key().identifier().getPath();
              int frequency = VibrationSystem.getGameEventFrequency(holder.key());
              if (frequency > 0 && !path.startsWith(RESONANCE_PREFIX)) {
                actual.put(path, frequency);
                helper.assertTrue(
                    holder.is(GameEventTags.VIBRATIONS), path + " is not a vibration");
              }
            });
    helper.assertValueEqual(actual, expected, "vibration frequencies");
    helper.succeed();
  }

  @GameTest
  public void rangesTimesAndSignalMatch(GameTestHelper helper) {
    BlockPos origin = BlockPos.ZERO;
    VibrationSystem.User sensor =
        new SculkSensorBlockEntity(origin, Blocks.SCULK_SENSOR.defaultBlockState())
            .getVibrationUser();
    VibrationSystem.User calibrated =
        new CalibratedSculkSensorBlockEntity(
                origin, Blocks.CALIBRATED_SCULK_SENSOR.defaultBlockState())
            .getVibrationUser();
    helper.assertValueEqual(sensor.getListenerRadius(), SculkSensors.RANGE, "range");
    helper.assertValueEqual(
        calibrated.getListenerRadius(), SculkSensors.CALIBRATED_RANGE, "calibrated range");
    helper.assertValueEqual(
        ((SculkSensorBlock) Blocks.SCULK_SENSOR).getActiveTicks(),
        SculkSensors.ACTIVE_TICKS,
        "active ticks");
    helper.assertValueEqual(
        ((SculkSensorBlock) Blocks.CALIBRATED_SCULK_SENSOR).getActiveTicks(),
        SculkSensors.CALIBRATED_ACTIVE_TICKS,
        "calibrated active ticks");
    helper.assertValueEqual(
        SculkSensorBlock.COOLDOWN_TICKS, SculkSensors.COOLDOWN_TICKS, "cooldown ticks");
    for (int quarters = 0; quarters <= SculkSensors.CALIBRATED_RANGE * QUARTERS; quarters++) {
      float distance = (float) quarters / QUARTERS;
      helper.assertValueEqual(
          sensor.calculateTravelTimeInTicks(distance),
          (int) Math.floor(distance) * SculkSensors.TRAVEL_TICKS_PER_BLOCK,
          "travel ticks at " + distance);
      for (int range : new int[] {SculkSensors.RANGE, SculkSensors.CALIBRATED_RANGE}) {
        helper.assertValueEqual(
            SculkSensors.signalAtDistance(distance, range),
            VibrationSystem.getRedstoneStrengthForDistance(distance, range),
            "signal at " + distance + " of " + range);
      }
    }
    helper.succeed();
  }

  /** A block placed two blocks from a sensor gives the signal the formula says. */
  @GameTest(maxTicks = HEAR_TICKS * 2)
  public void sensorSignalFadesWithDistance(GameTestHelper helper) {
    BlockPos sensor = new BlockPos(1, 1, 1);
    int distance = 2;
    helper.setBlock(sensor.below(), Blocks.STONE);
    helper.setBlock(sensor, Blocks.SCULK_SENSOR);
    helper.runAfterDelay(1, () -> vibrate(helper, GameEvent.BLOCK_PLACE, sensor.east(distance)));
    helper.runAfterDelay(
        HEAR_TICKS,
        () -> {
          helper.assertValueEqual(
              helper.getBlockState(sensor).getValue(SculkSensorBlock.POWER),
              SculkSensors.signalAtDistance(distance, SculkSensors.RANGE),
              "signal at " + distance + " blocks");
          helper.succeed();
        });
  }

  /**
   * A comparator reads the frequency of the vibration a sensor heard while the sensor is active,
   * then 0.
   */
  @GameTest(maxTicks = SculkSensors.ACTIVE_TICKS + HEAR_TICKS * 2)
  public void comparatorReadsFrequencyWhileActive(GameTestHelper helper) {
    BlockPos sensor = new BlockPos(1, 1, 1);
    BlockPos comparator = sensor.west();
    int blockPlaceFrequency =
        VibrationSystem.getGameEventFrequency(GameEvent.BLOCK_PLACE.unwrapKey().orElseThrow());
    helper.setBlock(sensor.below(), Blocks.STONE);
    helper.setBlock(comparator.below(), Blocks.STONE);
    helper.setBlock(sensor, Blocks.SCULK_SENSOR);
    helper.setBlock(comparator, DioramaPlacer.state("comparator[facing=east]"));
    helper
        .startSequence()
        .thenExecute(() -> vibrate(helper, GameEvent.BLOCK_PLACE, sensor.east(2)))
        .thenIdle(HEAR_TICKS)
        .thenExecute(() -> expectComparator(helper, comparator, blockPlaceFrequency))
        .thenIdle(SculkSensors.ACTIVE_TICKS)
        .thenExecute(() -> expectComparator(helper, comparator, 0))
        .thenSucceed();
  }

  /** Wool between a vibration and a sensor keeps the sensor from hearing it. */
  @GameTest(maxTicks = HEAR_TICKS * 2)
  public void woolBlocksVibrations(GameTestHelper helper) {
    BlockPos sensor = new BlockPos(1, 1, 1);
    helper.setBlock(sensor.below(), Blocks.STONE);
    helper.setBlock(sensor, Blocks.SCULK_SENSOR);
    helper.setBlock(sensor.east(), DioramaPlacer.state("white_wool"));
    helper.runAfterDelay(1, () -> vibrate(helper, GameEvent.EXPLODE, sensor.east(2)));
    helper.runAfterDelay(
        HEAR_TICKS,
        () -> {
          helper.assertValueEqual(
              SculkSensorBlock.getPhase(helper.getBlockState(sensor)),
              SculkSensorPhase.INACTIVE,
              "sensor behind wool");
          helper.succeed();
        });
  }

  /**
   * A calibrated sensor with a signal of 15 on its input side ignores a block placed (13) and hears
   * an explosion (15).
   */
  @GameTest(maxTicks = HEAR_TICKS * 3)
  public void calibratedSensorHearsOnlyItsInput(GameTestHelper helper) {
    BlockPos sensor = new BlockPos(2, 1, 2);
    helper.setBlock(sensor.below(), Blocks.STONE);
    helper.setBlock(sensor, DioramaPlacer.state("calibrated_sculk_sensor[facing=west]"));
    // The input side is opposite its facing: the east.
    helper.setBlock(sensor.east(), Blocks.REDSTONE_BLOCK);
    BlockPos source = sensor.west(2);
    helper
        .startSequence()
        .thenExecute(() -> vibrate(helper, GameEvent.BLOCK_PLACE, source))
        .thenIdle(HEAR_TICKS)
        .thenExecute(
            () ->
                helper.assertValueEqual(
                    SculkSensorBlock.getPhase(helper.getBlockState(sensor)),
                    SculkSensorPhase.INACTIVE,
                    "calibrated sensor after a block placed"))
        .thenExecute(() -> vibrate(helper, GameEvent.EXPLODE, source))
        .thenIdle(HEAR_TICKS)
        .thenExecute(
            () ->
                helper.assertValueEqual(
                    SculkSensorBlock.getPhase(helper.getBlockState(sensor)),
                    SculkSensorPhase.ACTIVE,
                    "calibrated sensor after an explosion"))
        .thenSucceed();
  }

  private static void expectComparator(GameTestHelper helper, BlockPos pos, int signal) {
    helper.assertValueEqual(
        helper.getBlockEntity(pos, ComparatorBlockEntity.class).getOutputSignal(),
        signal,
        "comparator");
  }

  private static void vibrate(GameTestHelper helper, Holder<GameEvent> event, BlockPos pos) {
    helper.getLevel().gameEvent(null, event, helper.absolutePos(pos));
  }
}
