package com.einsteinmods.cheatsheet.gametest;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The farming tab's crop texts against the game: what sugar cane stands on, where pumpkins and
 * melons grow, and what breaks a cactus ({@code cactus.cactus}: solid blocks and lava, not any
 * block).
 */
public final class CropRulesGameTest {
  private static final BlockPos GROUND = new BlockPos(1, 1, 1);
  private static final BlockPos PLANT = GROUND.above();

  /** The ground the sugar cane text names. */
  private static final List<Block> CANE_GROUNDS =
      List.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.MUD, Blocks.MOSS_BLOCK, Blocks.SAND);

  /** The ground the stems text names for the fruit. */
  private static final List<Block> FRUIT_GROUNDS =
      List.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.MUD, Blocks.MOSS_BLOCK, Blocks.FARMLAND);

  /** Blocks beside a cactus that break it. */
  private static final List<Block> BREAK_CACTUS = List.of(Blocks.STONE, Blocks.CACTUS, Blocks.LAVA);

  /** Blocks that are not solid: a cactus stays beside them. */
  private static final List<Block> KEEP_CACTUS = List.of(Blocks.TORCH, Blocks.WATER);

  @GameTest
  public void sugarCaneStandsOnEveryGroundShownBesideWater(GameTestHelper helper) {
    BlockPos besideGround = GROUND.relative(Direction.EAST);
    for (Block ground : CANE_GROUNDS) {
      helper.setBlock(GROUND, ground);
      helper.setBlock(besideGround, Blocks.AIR);
      helper.assertFalse(canSurvive(helper, Blocks.SUGAR_CANE), "cane without water on " + ground);
      helper.setBlock(besideGround, Blocks.WATER);
      helper.assertTrue(canSurvive(helper, Blocks.SUGAR_CANE), "cane beside water on " + ground);
    }
    helper.succeed();
  }

  @GameTest
  public void fruitGrowsOnEveryGroundShown(GameTestHelper helper) {
    for (Block ground : FRUIT_GROUNDS) {
      expectInTag(helper, ground, BlockTags.SUPPORTS_PUMPKIN_STEM_FRUIT);
      expectInTag(helper, ground, BlockTags.SUPPORTS_MELON_STEM_FRUIT);
    }
    helper.succeed();
  }

  @GameTest
  public void onlySolidBlocksAndLavaBreakACactus(GameTestHelper helper) {
    BlockPos side = PLANT.relative(Direction.EAST);
    helper.setBlock(GROUND, Blocks.SAND);
    helper.assertTrue(canSurvive(helper, Blocks.CACTUS), "cactus on sand");
    for (Block block : BREAK_CACTUS) {
      helper.setBlock(side, block);
      helper.assertFalse(canSurvive(helper, Blocks.CACTUS), "cactus beside " + block);
    }
    for (Block block : KEEP_CACTUS) {
      helper.setBlock(side, block);
      helper.assertTrue(canSurvive(helper, Blocks.CACTUS), "cactus beside " + block);
    }
    helper.succeed();
  }

  private static boolean canSurvive(GameTestHelper helper, Block plant) {
    return plant.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(PLANT));
  }

  private static void expectInTag(GameTestHelper helper, Block block, TagKey<Block> tag) {
    helper.assertTrue(block.defaultBlockState().is(tag), block + " not in " + tag);
  }
}
