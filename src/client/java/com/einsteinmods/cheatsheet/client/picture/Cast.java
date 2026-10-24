package com.einsteinmods.cheatsheet.client.picture;

import java.util.List;
import net.minecraft.world.entity.Entity;

/**
 * The mobs of a picture: creates them on a {@link Stage} (never added to the level, only drawn
 * once) at their places in the scene, in block units like the diorama's cells, already turned and
 * dressed.
 */
@FunctionalInterface
public interface Cast {
  /** No mobs: a place only. */
  Cast NONE = stage -> List.of();

  List<Entity> create(Stage stage);
}
