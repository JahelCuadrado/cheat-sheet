package com.einsteinmods.cheatsheet.core.pictures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DioramaTest {
  @Test
  void laterBlocksReplaceEarlierOnesAndClearEmpties() {
    Diorama diorama =
        Diorama.builder(2, 1, 2)
            .box("stone", 0, 0, 0, 1, 0, 1)
            .block("dirt", 1, 0, 1)
            .clear(0, 0, 0)
            .build();
    assertEquals(3, diorama.blocks().size());
    assertEquals(
        "minecraft:dirt",
        diorama.blocks().stream()
            .filter(block -> block.x() == 1 && block.z() == 1)
            .findFirst()
            .orElseThrow()
            .block()
            .blockId());
  }

  @Test
  void rejectsCellsOutsideTheBox() {
    Diorama.Builder builder = Diorama.builder(2, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> builder.block("stone", 2, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> builder.block("stone", 0, -1, 0));
  }
}
