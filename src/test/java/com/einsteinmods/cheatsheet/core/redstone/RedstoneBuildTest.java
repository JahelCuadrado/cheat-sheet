package com.einsteinmods.cheatsheet.core.redstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RedstoneBuildTest {
  @Test
  void everyBuildHasStepsInOneBox() {
    for (RedstoneBuild build : RedstoneBuild.values()) {
      assertTrue(build.steps().size() >= 2, build + " needs steps");
      Diorama first = build.steps().getFirst();
      for (Diorama step : build.steps()) {
        assertEquals(first.width(), step.width(), build + " width");
        assertEquals(first.height(), step.height(), build + " height");
        assertEquals(first.depth(), step.depth(), build + " depth");
        assertFalse(step.blocks().isEmpty(), build + " empty step");
      }
    }
  }

  @Test
  void bothSectionsHaveBuilds() {
    for (RedstoneBuild.Kind kind : RedstoneBuild.Kind.values()) {
      assertTrue(
          Arrays.stream(RedstoneBuild.values()).anyMatch(build -> build.kind() == kind),
          kind + " has no build");
    }
  }
}
