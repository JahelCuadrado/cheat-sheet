package com.einsteinmods.cheatsheet.client.picture;

import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Where casts create their mobs during one bake: the client's level (mobs need one, but are never
 * added to it) and ids of their own. Renderers read a mob's id (item seeds) and mobs compare by id,
 * so each mob of a bake gets a distinct negative id, never one the server hands out.
 */
public final class Stage {
  private final ClientLevel level;
  private int lastId;

  Stage(ClientLevel level) {
    this.level = level;
  }

  /**
   * A new mob of {@code type} at (x, y, z) in block units, its body and head turned to {@code yaw};
   * empty if the type cannot be created.
   */
  public <T extends Entity> Optional<T> spawn(
      EntityType<T> type, double x, double y, double z, float yaw) {
    T mob = type.create(level, EntitySpawnReason.LOAD);
    if (mob == null) {
      return Optional.empty();
    }
    lastId--;
    mob.setId(lastId);
    mob.setPos(x, y, z);
    mob.setYRot(yaw);
    mob.yRotO = yaw;
    if (mob instanceof LivingEntity living) {
      living.yBodyRot = yaw;
      living.yBodyRotO = yaw;
      living.yHeadRot = yaw;
      living.yHeadRotO = yaw;
    }
    return Optional.of(mob);
  }

  public ClientLevel level() {
    return level;
  }
}
