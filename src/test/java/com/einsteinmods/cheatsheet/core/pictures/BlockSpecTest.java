package com.einsteinmods.cheatsheet.core.pictures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlockSpecTest {
  @Test
  void parsesIdWithDefaultNamespace() {
    BlockSpec spec = BlockSpec.parse("stone");
    assertEquals("minecraft:stone", spec.blockId());
    assertEquals(Map.of(), spec.properties());
  }

  @Test
  void parsesPropertiesInOrder() {
    BlockSpec spec = BlockSpec.parse("minecraft:oak_stairs[facing=east,half=top]");
    assertEquals("minecraft:oak_stairs", spec.blockId());
    assertEquals(List.of("facing", "half"), List.copyOf(spec.properties().keySet()));
    assertEquals("top", spec.properties().get("half"));
  }

  @Test
  void rejectsMalformedSpecs() {
    for (String bad :
        List.of("Stone", "oak_stairs[facing=east", "oak_stairs[facing]", "a[b=c,b=d]", "a b")) {
      assertThrows(IllegalArgumentException.class, () -> BlockSpec.parse(bad), bad);
    }
  }
}
