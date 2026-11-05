package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.Cast;
import com.einsteinmods.cheatsheet.client.picture.Casts;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.core.mobfarms.FarmMob;
import com.einsteinmods.cheatsheet.core.mobfarms.MobFarm;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.item.ItemStack;

/** A farm's picture: its place in miniature with its mobs standing on it, seen like a block. */
final class FarmPictures {
  private static final String NAMESPACE = "minecraft:";

  private FarmPictures() {}

  static Scene scene(MobFarm farm) {
    return Scene.of(farm.picture(), cast(farm.mobs()));
  }

  /** Creates the mobs at their places, facing the viewer, with what they hold and their size. */
  private static Cast cast(List<FarmMob> mobs) {
    return stage -> {
      List<Entity> created = new ArrayList<>(mobs.size());
      for (FarmMob mob : mobs) {
        EntityType<?> type =
            BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(mob.entityId()));
        Optional<? extends Entity> spawned =
            stage.spawn(type, mob.x(), mob.y(), mob.z(), Casts.FACING_VIEWER);
        if (spawned.isEmpty()) {
          continue;
        }
        Entity entity = spawned.get();
        if (entity instanceof AbstractCubeMob cube && mob.size() > 0) {
          cube.setSize(mob.size(), false);
        }
        if (entity instanceof LivingEntity living && mob.heldItemId().isPresent()) {
          living.setItemSlot(
              EquipmentSlot.MAINHAND,
              new ItemStack(
                  BuiltInRegistries.ITEM.getValue(
                      Identifier.parse(NAMESPACE + mob.heldItemId().get()))));
        }
        created.add(entity);
      }
      return created;
    };
  }
}
