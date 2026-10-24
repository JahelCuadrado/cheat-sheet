package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.beacon.BeaconDioramas;
import com.einsteinmods.cheatsheet.core.beacon.BeaconPyramid;
import com.einsteinmods.cheatsheet.core.beacon.ConduitFrame;
import com.einsteinmods.cheatsheet.core.pictures.BlockSpec;
import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import com.einsteinmods.cheatsheet.core.pictures.DioramaBlock;
import com.einsteinmods.cheatsheet.core.pictures.Place;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Every block of every diorama ({@link Place}, the beacon tab's pyramids and frame) names a real
 * block, and each of its properties exists with that value: the client draws an unknown spec as
 * air, silently.
 */
public final class DioramaBlocksGameTest {
  @GameTest
  public void dioramaBlocksExist(GameTestHelper helper) {
    List<String> problems = new ArrayList<>();
    Map<String, Diorama> dioramas = new LinkedHashMap<>();
    for (Place place : Place.values()) {
      dioramas.put(place.name(), place.diorama());
    }
    for (int level = 1; level <= BeaconPyramid.MAX_LEVELS; level++) {
      dioramas.put("beacon pyramid " + level, BeaconDioramas.pyramid(level));
    }
    dioramas.put("conduit frame", BeaconDioramas.conduitFrame(ConduitFrame.FULL_FRAME_BLOCKS));
    dioramas.put("conduit rings", BeaconDioramas.conduitRings(ConduitFrame.RINGS));
    for (Map.Entry<String, Diorama> diorama : dioramas.entrySet()) {
      String place = diorama.getKey();
      for (DioramaBlock cell : diorama.getValue().blocks()) {
        BlockSpec spec = cell.block();
        Identifier id = Identifier.parse(spec.blockId());
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
          problems.add(place + ": no block " + id);
          continue;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        for (Map.Entry<String, String> property : spec.properties().entrySet()) {
          Property<?> known = block.getStateDefinition().getProperty(property.getKey());
          if (known == null || known.getValue(property.getValue()).isEmpty()) {
            problems.add(
                place + ": " + id + " has no " + property.getKey() + "=" + property.getValue());
          }
        }
      }
    }
    helper.assertTrue(
        problems.isEmpty(), "unknown diorama blocks:\n" + String.join("\n", problems));
    helper.succeed();
  }
}
