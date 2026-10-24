package com.einsteinmods.cheatsheet.core.discs;

import java.util.ArrayList;
import java.util.List;

/**
 * Every music disc and where it comes from. Loot tables and recipes are server data, never sent to
 * the client, so this is a copy of vanilla; {@code MusicDiscGameTest} checks it against the game.
 * Length and comparator signal are client data (jukebox songs) and are not copied here.
 */
public final class DiscTable {
  /** Disc 5 is crafted from this many fragments. */
  public static final int FRAGMENTS_PER_DISC = 9;

  public static final String FRAGMENT_ITEM_ID = "disc_fragment_5";

  /** Where fragments are found. */
  public static final DiscSource FRAGMENT_SOURCE = DiscSource.ANCIENT_CITY;

  private static final String PREFIX = "music_disc_";

  public static final List<Disc> DISCS =
      List.of(
          disc(
              "13",
              DiscSource.CREEPER_KILLED_BY_SKELETON,
              DiscSource.DUNGEON,
              DiscSource.ANCIENT_CITY,
              DiscSource.WOODLAND_MANSION),
          disc(
              "cat",
              DiscSource.CREEPER_KILLED_BY_SKELETON,
              DiscSource.DUNGEON,
              DiscSource.ANCIENT_CITY,
              DiscSource.WOODLAND_MANSION),
          disc("blocks", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("chirp", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("far", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("mall", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("mellohi", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("stal", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("strad", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("ward", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("11", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("wait", DiscSource.CREEPER_KILLED_BY_SKELETON),
          disc("otherside", DiscSource.DUNGEON, DiscSource.ANCIENT_CITY, DiscSource.STRONGHOLD),
          disc("5", DiscSource.CRAFTED),
          disc("bounce", DiscSource.MINESHAFT),
          disc("precipice", DiscSource.TRIAL_VAULT),
          disc("creator", DiscSource.OMINOUS_VAULT),
          disc("creator_music_box", DiscSource.TRIAL_POTS),
          disc("relic", DiscSource.TRAIL_RUINS),
          disc("pigstep", DiscSource.BASTION),
          disc("tears", DiscSource.GHAST_OWN_FIREBALL),
          disc("lava_chicken", DiscSource.CHICKEN_JOCKEY));

  private DiscTable() {}

  /** Discs from {@code source}, in table order. */
  public static List<Disc> discsFrom(DiscSource source) {
    List<Disc> found = new ArrayList<>();
    for (Disc disc : DISCS) {
      if (disc.sources().contains(source)) {
        found.add(disc);
      }
    }
    return List.copyOf(found);
  }

  private static Disc disc(String song, DiscSource... sources) {
    return new Disc(PREFIX + song, List.of(sources));
  }
}
