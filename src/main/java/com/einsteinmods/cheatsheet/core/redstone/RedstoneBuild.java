package com.einsteinmods.cheatsheet.core.redstone;

import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Circuits and mechanics the tab teaches step by step: a diorama per step (all in one box so they
 * share a scale), in reading order. Each is a working build that {@code RedstoneBuildsGameTest}
 * places and runs in a world.
 */
public enum RedstoneBuild {
  QUASI_CONNECTIVITY(Kind.MECHANIC, RedstoneDioramas::quasiConnectivity),
  STRONG_AND_WEAK(Kind.MECHANIC, RedstoneDioramas::strongAndWeakPower, 0),
  TRANSPARENCY(Kind.MECHANIC, RedstoneDioramas::transparency, 0),
  STICKY_BLOCKS(Kind.MECHANIC, RedstoneDioramas::stickyBlocks, 1),
  VERTICAL_SIGNAL(Kind.MECHANIC, RedstoneDioramas::verticalSignal, 2),
  OBSERVER_CLOCK(Kind.CIRCUIT, RedstoneDioramas::observerClock),
  REPEATER_CLOCK(Kind.CIRCUIT, RedstoneDioramas::repeaterClock),
  HOPPER_CLOCK(Kind.CIRCUIT, RedstoneDioramas::hopperClock),
  T_FLIP_FLOP(Kind.CIRCUIT, RedstoneDioramas::copperBulbFlipFlop),
  PULSE_LIMITER(Kind.CIRCUIT, RedstoneDioramas::pulseLimiter),
  PULSE_EXTENDER(Kind.CIRCUIT, RedstoneDioramas::pulseExtender),
  ITEM_SORTER(Kind.CIRCUIT, RedstoneDioramas::itemSorter),
  AUTO_DROPPER(Kind.CIRCUIT, RedstoneDioramas::autoDropper),
  NETHER_ROOF(Kind.TECHNIQUE, RedstoneDioramas::netherRoof),
  BEDROCK_INSTANT_MINING(Kind.TECHNIQUE, RedstoneDioramas::instantMining, 0),
  BEDROCK_TNT_PISTON(Kind.TECHNIQUE, RedstoneDioramas::tntPiston, 0);

  /** Which section of the tab a build belongs to. */
  public enum Kind {
    MECHANIC,
    CIRCUIT,
    TECHNIQUE
  }

  private final Kind kind;
  private final List<Diorama> steps;
  private final int cover;

  RedstoneBuild(Kind kind, Supplier<List<Diorama>> steps) {
    this.kind = kind;
    this.steps = List.copyOf(steps.get());
    this.cover = this.steps.size() - 1;
  }

  /** A build whose card shows step {@code cover} (from 0) instead of the last. */
  RedstoneBuild(Kind kind, Supplier<List<Diorama>> steps, int cover) {
    this.kind = kind;
    this.steps = List.copyOf(steps.get());
    this.cover = cover;
  }

  public Kind kind() {
    return kind;
  }

  /** One diorama per step; the last is the finished build. */
  public List<Diorama> steps() {
    return steps;
  }

  public Diorama finished() {
    return steps.getLast();
  }

  /** The picture on the build's card: the finished build, or the step that shows it best. */
  public Diorama cover() {
    return steps.get(cover);
  }

  /** Lower-case id for lang keys. */
  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }
}
