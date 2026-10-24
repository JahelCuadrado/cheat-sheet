package com.einsteinmods.cheatsheet.client.picture;

import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import java.util.Optional;
import net.minecraft.world.phys.AABB;

/**
 * What a picture shows: a diorama, mobs, or mobs over a diorama, seen from above at an angle (the
 * camera stands at {@code yawDegrees}, a game yaw: 180 is north, 225 north-east, and looks down
 * {@code pitchDegrees}). The scene is scaled to fill its picture: its bounds are the blocks' cells
 * and the mobs' hitboxes, plus {@code extraBelow} blocks for models hanging under their hitbox (a
 * ghast's tentacles). With a {@code focus} box (block units), only that part of the scene fills the
 * picture: what lies outside it is cropped (an armour stand's chest).
 */
public record Scene(
    Optional<Diorama> diorama,
    Cast cast,
    float yawDegrees,
    float pitchDegrees,
    float extraBelow,
    Optional<AABB> focus) {
  /** Blocks are seen as in the inventory: from the north-east, their top, north and east faces. */
  public static final float BLOCK_YAW = 225.0F;

  public static final float BLOCK_PITCH = 30.0F;

  /** Mobs alone: seen from the north, a little from above. */
  public static final float MOB_YAW = 200.0F;

  public static final float MOB_PITCH = 10.0F;

  public static Scene of(Diorama diorama) {
    return of(diorama, Cast.NONE);
  }

  public static Scene of(Diorama diorama, Cast cast) {
    return new Scene(Optional.of(diorama), cast, BLOCK_YAW, BLOCK_PITCH, 0.0F, Optional.empty());
  }

  public static Scene of(Cast cast) {
    return new Scene(Optional.empty(), cast, MOB_YAW, MOB_PITCH, 0.0F, Optional.empty());
  }

  /** The same scene with {@code blocks} more room under its bounds. */
  public Scene withExtraBelow(float blocks) {
    return new Scene(diorama, cast, yawDegrees, pitchDegrees, blocks, focus);
  }

  /** The same scene framed on {@code box} only, the rest cropped. */
  public Scene focusedOn(AABB box) {
    return new Scene(diorama, cast, yawDegrees, pitchDegrees, extraBelow, Optional.of(box));
  }
}
