package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.pictures.BlockSpec;
import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import com.einsteinmods.cheatsheet.core.pictures.DioramaBlock;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Builds a diorama in a test's world with exactly the states it draws (no block updates while
 * placing, so a drawn powered wire stays as drawn), on a stone floor. Tests then work the build's
 * input with normal updates and check what it does.
 */
final class DioramaPlacer {
  /** Where a diorama's corner cell goes in the test's structure, above the floor. */
  static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

  private static final int FLOOR_SIZE = 8;

  /** Place exact states: tell clients, but no neighbour or shape updates and no onPlace. */
  private static final int EXACT =
      Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE;

  private DioramaPlacer() {}

  /** Lays a stone floor under the structure and places {@code diorama} at {@link #ORIGIN}. */
  static void place(GameTestHelper helper, Diorama diorama) {
    for (int x = 0; x < FLOOR_SIZE; x++) {
      for (int z = 0; z < FLOOR_SIZE; z++) {
        helper
            .getLevel()
            .setBlock(
                helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), EXACT);
      }
    }
    for (DioramaBlock block : diorama.blocks()) {
      helper
          .getLevel()
          .setBlock(
              helper.absolutePos(at(block.x(), block.y(), block.z())), state(block.block()), EXACT);
    }
  }

  /** The test-relative position of a diorama cell. */
  static BlockPos at(int x, int y, int z) {
    return ORIGIN.offset(x, y, z);
  }

  /** The block state a spec names; fails on an unknown spec. */
  static BlockState state(BlockSpec spec) {
    Identifier id = Identifier.parse(spec.blockId());
    if (!BuiltInRegistries.BLOCK.containsKey(id)) {
      throw new IllegalArgumentException("no block " + id);
    }
    Block block = BuiltInRegistries.BLOCK.getValue(id);
    BlockState state = block.defaultBlockState();
    for (Map.Entry<String, String> property : spec.properties().entrySet()) {
      Property<?> known = block.getStateDefinition().getProperty(property.getKey());
      if (known == null) {
        throw new IllegalArgumentException(id + " has no " + property.getKey());
      }
      state = withValue(state, known, property.getValue()).orElseThrow();
    }
    return state;
  }

  /** {@code state} as written in commands: {@code repeater[delay=4]}. */
  static BlockState state(String text) {
    return state(BlockSpec.parse(text));
  }

  private static <T extends Comparable<T>> Optional<BlockState> withValue(
      BlockState state, Property<T> property, String value) {
    return property.getValue(value).map(parsed -> state.setValue(property, parsed));
  }
}
