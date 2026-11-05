package com.einsteinmods.cheatsheet.core.mobfarms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MobFarmTest {
  @Test
  void everyFarmHasAPictureAMobAndConditions() {
    for (MobFarm farm : MobFarm.values()) {
      assertFalse(farm.picture().blocks().isEmpty(), farm + " picture");
      assertFalse(farm.mobs().isEmpty(), farm + " mob");
      assertTrue(farm.facts().size() >= 3, farm + " conditions");
      for (FarmMob mob : farm.mobs()) {
        assertTrue(mob.x() >= 0 && mob.x() <= farm.picture().width(), farm + " mob x");
        assertTrue(mob.z() >= 0 && mob.z() <= farm.picture().depth(), farm + " mob z");
      }
    }
  }

  @Test
  void spacesCountTheBlocksTheSpawnBoxTakes() {
    assertEquals(1, MobSpace.ZOMBIE.blocksAcross());
    assertEquals(2, MobSpace.ZOMBIE.blocksHigh());
    assertEquals(3, MobSpace.SPIDER.blocksAcross());
    assertEquals(1, MobSpace.SPIDER.blocksHigh());
    assertEquals(1, MobSpace.CAVE_SPIDER.blocksAcross());
    assertEquals(3, MobSpace.ENDERMAN.blocksHigh());
    assertEquals(3, MobSpace.WITHER_SKELETON.blocksHigh());
    assertEquals(3, MobSpace.SLIME.blocksAcross());
    assertEquals(3, MobSpace.SLIME.blocksHigh());
    assertEquals(1, MobSpace.GUARDIAN.blocksAcross());
    assertEquals("magma_cube", MobSpace.MAGMA_CUBE.path());
  }

  @Test
  void villagerTimesAddUp() {
    assertEquals(100, FarmRules.villagerMorningTicks() / FarmRules.TICKS_PER_SECOND);
    assertEquals(50, FarmRules.villagerEveningTicks() / FarmRules.TICKS_PER_SECOND);
    assertEquals(100, FarmRules.villagerMeetTicks() / FarmRules.TICKS_PER_SECOND);
    assertEquals(10, FarmRules.minutes(FarmRules.VILLAGER_REST_START_TICKS));
  }

  @Test
  void derivedNumbers() {
    assertEquals(28, FarmRules.fortressWeights());
    assertEquals(9, FarmRules.spawnerAreaWidth());
    assertEquals(799, FarmRules.spawnerLongestWaitTicks());
    assertEquals(17, FarmRules.golemSpawnWidth());
    assertEquals(13, FarmRules.golemSpawnHeight());
    assertEquals(20, FarmRules.minutes(FarmRules.GOLEM_SLEPT_TICKS));
    assertEquals(100, FarmRules.minutes(FarmRules.BAD_OMEN_TICKS));
    assertEquals(30, FarmRules.roundSeconds(FarmRules.GOLEM_COOLDOWN_TICKS));
    assertEquals(6, FarmRules.shulkerNeverCopiesAt());
    assertEquals(20, FarmRules.shulkerCopyPenaltyPercent());
  }
}
