package com.einsteinmods.cheatsheet.gametest;

import static com.einsteinmods.cheatsheet.gametest.DioramaPlacer.at;

import com.einsteinmods.cheatsheet.core.redstone.InstantMining;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneBuild;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneDioramas;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Techniques section's bedrock breaking, in a world: the piston swap behind both designs
 * ({@code PistonBaseBlock#triggerEvent} removes the block the piston faces when its queued
 * retraction runs), the exploding piston as drawn, and the instant mining table.
 */
public class BedrockBreakingGameTest {
  private static final BlockPos BEDROCK = at(RedstoneDioramas.BEDROCK_X, 0, 0);
  private static final BlockPos PISTON = at(RedstoneDioramas.BEDROCK_X, 1, 0);
  private static final int SETTLE = 4;
  private static final float PROGRESS_TOLERANCE = 0.001F;
  private static final int HASTE_TICKS = 200;

  /** Blasts tried before the exploding piston fails: about 4 in 10 work (16 of 40 measured). */
  private static final int EXPLOSION_ATTEMPTS = 20;

  private static final int ATTEMPT_TICKS = 20;

  /**
   * Torch and extended piston broken, a piston facing down placed in the same tick: the next tick
   * the bedrock under it is gone.
   */
  @GameTest(maxTicks = 40)
  public void pistonSwapRemovesBedrock(GameTestHelper helper) {
    DioramaPlacer.place(helper, RedstoneBuild.BEDROCK_INSTANT_MINING.steps().getFirst());
    helper
        .startSequence()
        .thenIdle(SETTLE)
        .thenExecute(
            () -> {
              helper.assertBlockPresent(Blocks.BEDROCK, BEDROCK);
              helper.setBlock(at(RedstoneDioramas.BEDROCK_X - 1, 1, 0), Blocks.AIR);
              helper.setBlock(PISTON.above(), Blocks.AIR);
              helper.setBlock(PISTON, Blocks.AIR);
              helper.setBlock(PISTON, facingDown());
            })
        .thenIdle(SETTLE)
        .thenExecute(() -> helper.assertBlockNotPresent(Blocks.BEDROCK, BEDROCK))
        .thenSucceed();
  }

  /**
   * The exploding piston as drawn: the blast breaks the piston's lever and the piston in a random
   * order, so an attempt works only when the lever goes first; the test builds it again until one
   * works. The piston facing down is placed in the gap after the blast, as a player's quick click
   * would.
   */
  @GameTest(maxTicks = EXPLOSION_ATTEMPTS * ATTEMPT_TICKS + SETTLE)
  public void explodingPistonRemovesBedrock(GameTestHelper helper) {
    explosionAttempt(helper, new int[] {0});
  }

  private static void explosionAttempt(GameTestHelper helper, int[] attempts) {
    attempts[0]++;
    DioramaPlacer.place(helper, RedstoneBuild.BEDROCK_TNT_PISTON.steps().getFirst());
    helper.pullLever(at(RedstoneDioramas.BEDROCK_X, 1, 1));
    boolean[] placed = {false};
    helper.runAfterDelay(
        SETTLE,
        () -> {
          BlockPos tnt = at(RedstoneDioramas.TNT_X, 1, 0);
          helper.setBlock(tnt, Blocks.AIR);
          PrimedTnt primed = helper.spawn(EntityTypes.TNT, tnt);
          primed.setFuse(1);
          for (int tick = 1; tick <= ATTEMPT_TICKS - SETTLE * 2; tick++) {
            helper.runAfterDelay(
                tick,
                () -> {
                  if (!placed[0] && helper.getBlockState(PISTON).isAir()) {
                    placed[0] = true;
                    helper.setBlock(PISTON, facingDown());
                  }
                });
          }
        });
    helper.runAfterDelay(
        ATTEMPT_TICKS,
        () -> {
          if (!helper.getBlockState(BEDROCK).is(Blocks.BEDROCK)) {
            helper.succeed();
          } else if (attempts[0] < EXPLOSION_ATTEMPTS) {
            explosionAttempt(helper, attempts);
          } else {
            helper.fail("the bedrock stayed after " + attempts[0] + " blasts");
          }
        });
  }

  /** Each row of the table mines a piston at the progress the game gives, on the ground. */
  @GameTest
  public void instantMiningMatchesTheGame(GameTestHelper helper) {
    BlockPos pos = at(0, 1, 0);
    helper.setBlock(pos, Blocks.PISTON);
    BlockState piston = helper.getBlockState(pos);
    for (InstantMining row : InstantMining.ROWS) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack pickaxe =
          new ItemStack(
              BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(row.pickaxe())));
      pickaxe.enchant(
          helper
              .getLevel()
              .registryAccess()
              .lookupOrThrow(Registries.ENCHANTMENT)
              .getOrThrow(Enchantments.EFFICIENCY),
          row.efficiency());
      player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
      // What equipping does on a tick: the enchantment's attribute modifiers.
      EnchantmentHelper.forEachModifier(
          pickaxe,
          EquipmentSlot.MAINHAND,
          (attribute, modifier) ->
              player.getAttributes().getInstance(attribute).addTransientModifier(modifier));
      if (row.haste() > 0) {
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, HASTE_TICKS, row.haste() - 1));
      }
      player.setOnGround(true);
      float progress =
          piston.getDestroyProgress(player, helper.getLevel(), helper.absolutePos(pos));
      helper.assertTrue(
          Math.abs(progress - row.progress()) < PROGRESS_TOLERANCE,
          row + " mines " + progress + ", the table says " + row.progress());
    }
    helper.succeed();
  }

  private static BlockState facingDown() {
    return Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.DOWN);
  }
}
