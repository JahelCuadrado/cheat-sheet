package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.commands.BuiltInCommand;
import com.einsteinmods.cheatsheet.core.commands.BuiltInCommands;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What the sheet says of some built-in commands, run for real ({@link BuiltInCommands}): the mobs
 * "kill monsters" spares and the data of summoned mobs, which only a run checks ({@code
 * BuiltInCommandsGameTest} parses). Commands run high above the test structures and everything
 * happens and is undone within one tick, so parallel tests never see it. No blocks: new block
 * sections up there make light work that delays {@code FarmRulesGameTest}'s light checks.
 */
public final class BuiltInCommandEffectsGameTest {
  /** High enough for no other test's mob to be within kill_monsters' 64 blocks. */
  private static final int TEST_Y = 200;

  private static final int KILL_RADIUS = 64;
  private static final int VILLAGER_TRADE_USES = 9999;
  private static final double SEARCH_REACH = 72;

  @GameTest
  public void killMonstersSparesNamedOthersAndFarOnes(GameTestHelper helper) {
    Stage stage = new Stage(helper);
    stage.run("summon zombie ~ ~ ~ {NoAI:1b}");
    stage.run("summon zombie ~ ~ ~ {NoAI:1b,CustomName:\"Named\"}");
    stage.run("summon zombie ~ ~" + (KILL_RADIUS + 1) + " ~ {NoAI:1b}");
    stage.run("summon cow ~ ~ ~ {NoAI:1b}");
    stage.run(line("kill_monsters"));
    List<Zombie> zombies = stage.entities(Zombie.class);
    List<Cow> cows = stage.entities(Cow.class);
    int survivors = 0;
    int named = 0;
    for (Zombie zombie : zombies) {
      if (zombie.isAlive()) {
        survivors++;
        named += zombie.hasCustomName() ? 1 : 0;
      }
    }
    boolean cowAlive = cows.getFirst().isAlive();
    stage.discard(zombies);
    stage.discard(cows);
    helper.assertTrue(survivors == 2 && named == 1, "zombies alive: " + survivors);
    helper.assertTrue(cowAlive, "kill monsters killed the cow");
    helper.succeed();
  }

  @GameTest
  public void customVillagerStaysPutAndTradesAsSaid(GameTestHelper helper) {
    Stage stage = new Stage(helper);
    stage.run(line("villager_trades"));
    List<Villager> villagers = stage.entities(Villager.class);
    helper.assertTrue(villagers.size() == 1, "no villager summoned");
    Villager villager = villagers.getFirst();
    boolean noAi = villager.isNoAi();
    List<MerchantOffer> offers = List.copyOf(villager.getOffers());
    stage.discard(villagers);
    helper.assertTrue(noAi && offers.size() == 2, "offers: " + offers.size());
    MerchantOffer diamond = offers.getFirst();
    helper.assertTrue(
        diamond.getCostA().is(Items.EMERALD) && diamond.getResult().is(Items.DIAMOND),
        "first trade is not an emerald for a diamond");
    MerchantOffer emerald = offers.getLast();
    helper.assertTrue(
        emerald.getCostA().is(Items.DIRT) && emerald.getResult().is(Items.EMERALD),
        "second trade is not dirt for an emerald");
    helper.assertTrue(diamond.getMaxUses() == VILLAGER_TRADE_USES, "max uses");
    helper.succeed();
  }

  @GameTest
  public void summonDataApplies(GameTestHelper helper) {
    Stage stage = new Stage(helper);
    stage.run(line("frozen_mob"));
    stage.run(line("invisible_stand"));
    List<Zombie> zombies = stage.entities(Zombie.class);
    List<ArmorStand> stands = stage.entities(ArmorStand.class);
    boolean frozen = zombies.getFirst().isNoAi() && zombies.getFirst().isPersistenceRequired();
    ArmorStand stand = stands.getFirst();
    boolean shown = stand.isInvisible() && stand.showArms() && !stand.showBasePlate();
    stage.discard(zombies);
    stage.discard(stands);
    helper.assertTrue(frozen, "frozen mob has AI or can despawn");
    helper.assertTrue(shown, "invisible stand data did not apply");
    helper.succeed();
  }

  private static String line(String key) {
    return command(key).lines().getFirst();
  }

  private static BuiltInCommand command(String key) {
    for (BuiltInCommand command : BuiltInCommands.ALL) {
      if (command.key().equals(key)) {
        return command;
      }
    }
    throw new IllegalArgumentException("no built-in " + key);
  }

  /** A spot high above the test, with a command source standing on it. */
  private record Stage(GameTestHelper helper, ServerLevel level, BlockPos origin) {
    Stage(GameTestHelper helper) {
      this(helper, helper.getLevel(), helper.absolutePos(BlockPos.ZERO).atY(TEST_Y));
    }

    void run(String command) {
      CommandSourceStack source =
          level
              .getServer()
              .createCommandSourceStack()
              .withLevel(level)
              .withPosition(Vec3.atBottomCenterOf(origin))
              .withPermission(LevelBasedPermissionSet.OWNER)
              .withSuppressedOutput();
      level.getServer().getCommands().performPrefixedCommand(source, command);
    }

    <T extends Entity> List<T> entities(Class<T> type) {
      return level.getEntitiesOfClass(type, new AABB(origin).inflate(SEARCH_REACH));
    }

    void discard(List<? extends Entity> entities) {
      entities.forEach(Entity::discard);
    }
  }
}
