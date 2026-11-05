package com.einsteinmods.cheatsheet.core.redstone;

import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The redstone tab's dioramas, block by block, with the states the game would show (powered dust,
 * repeaters, extended pistons). Builds are working machines: {@code RedstoneBuildsGameTest} places
 * each in a world, works its input and checks its output. Pictures look from the north-east: east
 * (+x) faces on the left, north (-z) faces on the right. Builds stand on a floor at y = 0.
 *
 * <p>Repeaters and comparators name the side their input comes from ({@code facing=east}: in from
 * the east, out to the west); hoppers and pistons the side they push to; observers the side they
 * watch.
 */
public final class RedstoneDioramas {
  private static final String FLOOR = "smooth_stone";
  private static final String SOLID = "stone_bricks";
  private static final String DUST_CROSS =
      "redstone_wire[east=side,west=side,north=side,south=side,power=0]";

  /** Where {@link #comparatorExample} shows its dust, for the numbers drawn over the picture. */
  public static final int EXAMPLE_INPUT_COLUMN = 0;

  public static final int EXAMPLE_COMPARATOR_COLUMN = 1;
  public static final int EXAMPLE_OUTPUT_COLUMN = 2;
  public static final int EXAMPLE_SIDE_ROW = 0;
  public static final int EXAMPLE_ROW = 1;
  private static final int EXAMPLE_COLUMNS = 3;
  private static final int EXAMPLE_ROWS = 2;

  /** The Nether roof's picture: the ceiling's layer. */
  public static final int ROOF_CEILING_Y = 5;

  /** The bedrock breaking pictures: the bedrock under the piston, and the TNT. */
  public static final int BEDROCK_X = 1;

  public static final int TNT_X = 3;

  /** The self-emptying dropper: its row, the dropper and the chest it fills. */
  public static final int DROPPER_ROW = 0;

  public static final int DROPPER_X = 1;
  public static final int DROPPER_CHEST_X = 0;

  private RedstoneDioramas() {}

  /** Redstone dust in a straight east-west line at {@code power}. */
  static String dust(int power) {
    return wire(power, "side", "side", "none", "none");
  }

  /** Redstone dust with each side ({@code none}, {@code side} or {@code up}) and {@code power}. */
  private static String wire(int power, String east, String west, String north, String south) {
    return "redstone_wire[east="
        + east
        + ",west="
        + west
        + ",north="
        + north
        + ",south="
        + south
        + ",power="
        + power
        + "]";
  }

  // Reference cards (one picture each).

  /** A wooden pressure plate on a block. */
  static Diorama pressurePlate() {
    return Diorama.builder(1, 2, 1)
        .block(SOLID, 0, 0, 0)
        .block("oak_pressure_plate", 0, 1, 0)
        .build();
  }

  /** A comparator reading a barrel, its signal out into dust. */
  static Diorama comparator() {
    return Diorama.builder(3, 2, 1)
        .box(FLOOR, 0, 0, 0, 2, 0, 0)
        .block("barrel[facing=up]", 2, 1, 0)
        .block("comparator[facing=east,powered=true]", 1, 1, 0)
        .block(dust(7), 0, 1, 0)
        .build();
  }

  /** Blocks a comparator reads by something other than items: a composter, a cake, a cauldron. */
  static Diorama specialBlocks() {
    return Diorama.builder(3, 1, 1)
        .block("composter[level=8]", 2, 0, 0)
        .block("cake", 1, 0, 0)
        .block("water_cauldron[level=3]", 0, 0, 0)
        .build();
  }

  /** A sculk sensor and a calibrated one. */
  static Diorama sculkSensors() {
    return Diorama.builder(2, 1, 1)
        .block("sculk_sensor", 1, 0, 0)
        .block("calibrated_sculk_sensor[facing=west]", 0, 0, 0)
        .build();
  }

  /** Dust from a redstone block into a repeater set to 4, then dust out again. */
  static Diorama repeater() {
    return Diorama.builder(4, 2, 1)
        .box(FLOOR, 0, 0, 0, 3, 0, 0)
        .block("redstone_block", 3, 1, 0)
        .block(dust(RedstoneRules.MAX_SIGNAL), 2, 1, 0)
        .block("repeater[facing=east,delay=4,powered=true]", 1, 1, 0)
        .block(dust(RedstoneRules.MAX_SIGNAL), 0, 1, 0)
        .build();
  }

  // Mechanics: one picture per step, in one box.

  /** Quasi-connectivity: the piston's row (z), the piston (x) and the block placed beside it. */
  public static final int QC_ROW = 1;

  public static final int QC_PISTON_X = 2;
  public static final int QC_PUSHED_X = 1;

  /** Where the redstone block powers the spot above the piston, from the side. */
  public static final int QC_SOURCE_X = 3;

  /**
   * A piston with a redstone block diagonally above it (on a pillar behind it): the spot over the
   * piston is powered, not the piston. Then any block placed next to it updates it, and it pushes.
   */
  static List<Diorama> quasiConnectivity() {
    Steps steps = new Steps(Diorama.builder(4, 3, 2).box(FLOOR, 0, 0, 0, 3, 0, 1));
    steps
        .next()
        .block(SOLID, QC_PUSHED_X, 1, QC_ROW)
        .block("piston[facing=west]", QC_PISTON_X, 1, QC_ROW)
        .block(SOLID, QC_SOURCE_X, 1, QC_ROW)
        .block("redstone_block", QC_SOURCE_X, 2, QC_ROW);
    steps
        .next()
        .block("oak_planks", QC_PISTON_X, 1, QC_ROW - 1)
        .block("piston[facing=west,extended=true]", QC_PISTON_X, 1, QC_ROW)
        .block("piston_head[facing=west,type=normal]", QC_PUSHED_X, 1, QC_ROW)
        .block(SOLID, QC_PUSHED_X - 1, 1, QC_ROW);
    return steps.build();
  }

  /** Strong and weak power: where each picture's powered block stands (x), on the floor. */
  public static final int POWERED_BLOCK_X = 1;

  /** The weak picture: the block the dust points into, and the dust beyond it. */
  public static final int WEAK_BLOCK_X = 2;

  public static final int WEAK_FAR_DUST_X = 3;

  /** The torch picture: the torch's height, and the lamps beside its block and the block above. */
  public static final int TORCH_Y = 2;

  public static final int TORCH_LAMP_X = 2;

  /**
   * Three pictures: a lever strongly powers its block, which powers the dust and lamp beside it;
   * dust weakly powers the block it points into, which lights a lamp but not the dust beyond; a
   * torch strongly powers the block above it, never the one it stands on.
   */
  static List<Diorama> strongAndWeakPower() {
    int width = 4;
    int height = 4;
    String lit = "redstone_lamp[lit=true]";
    Diorama strong =
        Diorama.builder(width, height, 1)
            .box(FLOOR, 0, 0, 0, width - 1, 0, 0)
            .block(SOLID, POWERED_BLOCK_X, 1, 0)
            .block("lever[face=floor,facing=east,powered=true]", POWERED_BLOCK_X, 2, 0)
            .block(
                "redstone_wire[east=side,west=side,north=side,south=side,power=15]",
                POWERED_BLOCK_X - 1,
                1,
                0)
            .block(lit, POWERED_BLOCK_X + 1, 1, 0)
            .build();
    Diorama weak =
        Diorama.builder(width, height, 1)
            .box(FLOOR, 0, 0, 0, width - 1, 0, 0)
            .block("redstone_block", 0, 1, 0)
            .block(dust(RedstoneRules.MAX_SIGNAL), WEAK_BLOCK_X - 1, 1, 0)
            .block(SOLID, WEAK_BLOCK_X, 1, 0)
            .block(lit, WEAK_BLOCK_X, 2, 0)
            .block(DUST_CROSS, WEAK_FAR_DUST_X, 1, 0)
            .build();
    Diorama torch =
        Diorama.builder(width, height, 1)
            .box(FLOOR, 0, 0, 0, width - 1, 0, 0)
            .block(SOLID, POWERED_BLOCK_X, TORCH_Y - 1, 0)
            .block("redstone_torch[lit=true]", POWERED_BLOCK_X, TORCH_Y, 0)
            .block(SOLID, POWERED_BLOCK_X, TORCH_Y + 1, 0)
            .block(lit, TORCH_LAMP_X, TORCH_Y + 1, 0)
            .block("redstone_lamp", TORCH_LAMP_X, TORCH_Y - 1, 0)
            .build();
    return List.of(strong, weak, torch);
  }

  /** Transparency: the slab (x) and glass pillar (x) the dust climbs, and the top dust's height. */
  public static final int CLIMB_SLAB_X = 2;

  public static final int CLIMB_GLASS_X = 3;
  public static final int CLIMB_TOP_Y = 3;

  /** The spot over the lower dust where a block cuts the climb, or glass does not. */
  public static final int CLIMB_COVER_X = 1;

  /** The redstone block powering the top dust, from the side, when power comes from above. */
  public static final int CLIMB_TOP_SOURCE_X = 4;

  /** The picture of a lever on glass: the glass (x) and the lamp beside it. */
  public static final int LEVER_GLASS_X = 1;

  public static final int LEVER_LAMP_X = 2;

  /**
   * Four pictures: dust climbing an upside-down slab and glass, lit all the way up; the same climb
   * powered from the top, dark below (the signal never comes down); glass over the lower dust
   * leaves the climb whole; a lever on glass powers nothing (glass carries no power).
   */
  static List<Diorama> transparency() {
    int width = 5;
    int height = 4;
    String glass = "glass";
    int max = RedstoneRules.MAX_SIGNAL;
    Diorama.Builder climb =
        Diorama.builder(width, height, 1)
            .box(FLOOR, 0, 0, 0, width - 1, 0, 0)
            .block("redstone_block", 0, 1, 0)
            .block(dust(max), 1, 1, 0)
            .block("smooth_stone_slab[type=top]", CLIMB_SLAB_X, 1, 0)
            .block(wire(max - 1, "up", "side", "none", "none"), CLIMB_SLAB_X, 2, 0)
            .box(glass, CLIMB_GLASS_X, 1, 0, CLIMB_GLASS_X, CLIMB_TOP_Y - 1, 0)
            .block(dust(max - 2), CLIMB_GLASS_X, CLIMB_TOP_Y, 0);
    Diorama up = climb.build();
    Diorama down =
        Diorama.builder(width, height, 1)
            .box(FLOOR, 0, 0, 0, width - 1, 0, 0)
            .block(dust(0), 1, 1, 0)
            .block("smooth_stone_slab[type=top]", CLIMB_SLAB_X, 1, 0)
            .block(wire(0, "up", "side", "none", "none"), CLIMB_SLAB_X, 2, 0)
            .box(glass, CLIMB_GLASS_X, 1, 0, CLIMB_GLASS_X, CLIMB_TOP_Y - 1, 0)
            .block(dust(max), CLIMB_GLASS_X, CLIMB_TOP_Y, 0)
            .box(glass, CLIMB_TOP_SOURCE_X, 1, 0, CLIMB_TOP_SOURCE_X, CLIMB_TOP_Y - 1, 0)
            .block("redstone_block", CLIMB_TOP_SOURCE_X, CLIMB_TOP_Y, 0)
            .build();
    Diorama covered = climb.block(glass, CLIMB_COVER_X, 2, 0).build();
    Diorama lever =
        Diorama.builder(width, height, 1)
            .box(FLOOR, 0, 0, 0, width - 1, 0, 0)
            .block(glass, LEVER_GLASS_X, 1, 0)
            .block("lever[face=floor,facing=east,powered=true]", LEVER_GLASS_X, 2, 0)
            .block("redstone_lamp", LEVER_LAMP_X, 1, 0)
            .build();
    return List.of(up, down, covered, lever);
  }

  /**
   * Slime and honey: the slime's column (x), the normal block that always goes along (x), the block
   * each step tries beside the slime (x), the machine's row (z) and the lever's (z).
   */
  public static final int STICKY_SLIME_X = 1;

  public static final int STICKY_CONTROL_X = 0;
  public static final int STICKY_TRIED_X = 2;
  public static final int STICKY_ROW = 1;
  public static final int STICKY_LEVER_Z = 0;

  /** The piston's height; the slime sits on it and the blocks beside the slime on supports. */
  public static final int STICKY_PISTON_Y = 1;

  /** The block that shows what slime drags: a normal block, wood to stand out from the floor. */
  public static final String STICKY_CONTROL = "oak_planks";

  /**
   * One step of the slime guide: a sticky piston facing up lifts a slime block; beside the slime, a
   * normal block on one side and the block the step tries on the other (or on top), each standing
   * on its own support so a lifted block hangs in the air. Each step shows one effect.
   */
  public enum StickyStep {
    /** The machine before the lever: the slime and, beside it, a normal block. */
    SETUP(Optional.empty(), false),
    /** The slime goes up and takes the normal block along. */
    DRAG(Optional.empty(), false),
    /** Honey beside slime stays: they don't stick to each other. */
    HONEY(Optional.of("honey_block"), false),
    /** Glazed terracotta beside slime stays: only pushed head-on. */
    GLAZED_TERRACOTTA(Optional.of("white_glazed_terracotta[facing=north]"), false),
    /** A pumpkin beside slime stays, whole: it breaks only when something runs into it. */
    PUMPKIN(Optional.of("pumpkin"), false),
    /** Obsidian on top of the slime: nothing moves, not even the normal block. */
    OBSIDIAN(Optional.of("obsidian"), true);

    private final Optional<String> tried;
    private final boolean onTop;

    StickyStep(Optional<String> tried, boolean onTop) {
      this.tried = tried;
      this.onTop = onTop;
    }

    /** The machine as built, the lever off. */
    public Diorama before() {
      return sticky(this, false);
    }

    /** The machine once the lever is on: what the step's picture shows ({@link #SETUP}: before). */
    public Diorama after() {
      return this == SETUP ? before() : sticky(this, true);
    }

    /** Whether the slime moves when the lever is pulled. */
    public boolean moves() {
      return !onTop;
    }
  }

  /** One picture per {@link StickyStep}: the setup, then what each change does. */
  static List<Diorama> stickyBlocks() {
    List<Diorama> steps = new ArrayList<>();
    for (StickyStep step : StickyStep.values()) {
      steps.add(step.after());
    }
    return steps;
  }

  /** {@code step}'s machine, with the lever off or on ({@code pulled}). */
  private static Diorama sticky(StickyStep step, boolean pulled) {
    int row = STICKY_ROW;
    int slime = STICKY_SLIME_X;
    int pistonY = STICKY_PISTON_Y;
    boolean lifted = pulled && step.moves();
    int liftedBy = lifted ? 1 : 0;
    Diorama.Builder builder =
        Diorama.builder(3, 4, 2)
            .box(FLOOR, 0, 0, 0, 2, 0, 1)
            .block(FLOOR, STICKY_CONTROL_X, pistonY, row)
            .block(
                "lever[face=floor,facing=north,powered=" + pulled + "]",
                slime,
                pistonY,
                STICKY_LEVER_Z)
            .block("sticky_piston[facing=up,extended=" + lifted + "]", slime, pistonY, row)
            .block("slime_block", slime, pistonY + 1 + liftedBy, row)
            .block(STICKY_CONTROL, STICKY_CONTROL_X, pistonY + 1 + liftedBy, row);
    if (lifted) {
      builder.block("piston_head[facing=up,type=sticky]", slime, pistonY + 1, row);
    }
    step.tried.ifPresent(
        tried -> {
          if (step.onTop) {
            builder.block(tried, slime, pistonY + 2, row);
          } else {
            builder
                .block(FLOOR, STICKY_TRIED_X, pistonY, row)
                .block(tried, STICKY_TRIED_X, pistonY + 1, row);
          }
        });
    return builder.build();
  }

  /** Vertical signal: the lever (x) every method starts from, and the column the signal climbs. */
  public static final int VERTICAL_LEVER_X = 3;

  public static final int VERTICAL_COLUMN_X = 1;

  /** Upside-down slabs in the zigzag: the top dust stands this many steps up. */
  public static final int ZIGZAG_SLABS = 4;

  /** The torch tower's and the observers' output lamp, and the sculk sensor's. */
  public static final int TORCH_TOWER_LAMP_Y = 5;

  public static final int OBSERVER_LAMP_Y = 4;
  public static final int SCULK_SENSOR_Y = 4;

  /**
   * Five ways to take a signal up or down, one picture each, each started by a lever: a dust
   * staircase on solid blocks (up and down), a zigzag of upside-down slabs (up only), a torch tower
   * (up, each torch flips it), observers in a column (pulses, up or down) and a sculk sensor that
   * hears the lever (no wire at all).
   */
  static List<Diorama> verticalSignal() {
    int width = 4;
    int height = 6;
    int max = RedstoneRules.MAX_SIGNAL;
    int column = VERTICAL_COLUMN_X;
    String lever = "lever[face=floor,facing=east,powered=true]";
    Diorama staircase =
        base(width, height)
            .block(lever, VERTICAL_LEVER_X, 1, 0)
            .block(wire(max, "side", "up", "none", "none"), 2, 1, 0)
            .block(SOLID, 1, 1, 0)
            .block(wire(max - 1, "side", "up", "none", "none"), 1, 2, 0)
            .box(SOLID, 0, 1, 0, 0, 2, 0)
            .block(wire(max - 2, "side", "none", "none", "none"), 0, 3, 0)
            .build();
    String slab = "smooth_stone_slab[type=top]";
    Diorama.Builder zigzag = base(width, height).block(lever, VERTICAL_LEVER_X, 1, 0);
    zigzag.block(wire(max, "side", "side", "none", "none"), column + 1, 1, 0);
    for (int step = 0; step < ZIGZAG_SLABS; step++) {
      // Slabs alternate between the column and the one east of it, a step higher each time.
      int x = step % 2 == 0 ? column : column + 1;
      zigzag
          .block(slab, x, 1 + step, 0)
          .block(
              x == column
                  ? wire(max - 1 - step, "side", "none", "none", "none")
                  : wire(max - 1 - step, "none", "side", "none", "none"),
              x,
              2 + step,
              0);
    }
    Diorama torchTower =
        base(width, height)
            .block(SOLID, column, 1, 0)
            .block("lever[face=wall,facing=east,powered=true]", column + 1, 1, 0)
            .block("redstone_torch[lit=false]", column, 2, 0)
            .block(SOLID, column, 3, 0)
            .block("redstone_torch[lit=true]", column, 4, 0)
            .block("redstone_lamp[lit=true]", column, TORCH_TOWER_LAMP_Y, 0)
            .build();
    Diorama observers =
        base(width, height)
            .block("lever[face=floor,facing=east]", column, 1, 0)
            .block("observer[facing=down]", column, 2, 0)
            .block("observer[facing=down]", column, 3, 0)
            .block("redstone_lamp", column, OBSERVER_LAMP_Y, 0)
            .build();
    Diorama sculk =
        base(width, height)
            .block("lever[face=floor,facing=east]", VERTICAL_LEVER_X, 1, 0)
            .box(SOLID, column, 1, 0, column, SCULK_SENSOR_Y - 1, 0)
            .block("sculk_sensor", column, SCULK_SENSOR_Y, 0)
            .block("redstone_lamp", column, SCULK_SENSOR_Y + 1, 0)
            .build();
    return List.of(staircase, zigzag.build(), torchTower, observers, sculk);
  }

  private static Diorama.Builder base(int width, int height) {
    return Diorama.builder(width, height, 1).box(FLOOR, 0, 0, 0, width - 1, 0, 0);
  }

  /**
   * Crafter: its place (x, row z), the hopper above that fills it, the hopper in front that takes
   * what it makes into a chest, the comparator behind, and the button in front of the row.
   */
  public static final int CRAFTER_X = 2;

  public static final int CRAFTER_ROW = 1;
  public static final int CRAFTER_Y = 1;
  public static final int CRAFTER_BUTTON_Z = 0;
  public static final int CRAFTER_CHEST_X = 0;
  public static final int CRAFTER_OUT_HOPPER_X = 1;
  public static final int CRAFTER_COMPARATOR_X = 3;
  public static final int CRAFTER_DUST_X = 4;

  /** The crafter's card: a hopper fills it from above, it crafts into the chest in front. */
  static Diorama crafterCard() {
    return Diorama.builder(2, 2, 1)
        .block("crafter[orientation=west_up]", 1, 0, 0)
        .block("hopper[facing=down]", 1, 1, 0)
        .block("chest[facing=north]", 0, 0, 0)
        .build();
  }

  /**
   * The crafter at work: a hopper above fills it; it crafts out of its front (west) into a hopper
   * that passes the result to a chest; a comparator behind reads its slots; a button crafts once.
   */
  public static Diorama crafter() {
    int row = CRAFTER_ROW;
    int y = CRAFTER_Y;
    return Diorama.builder(5, 3, 2)
        .box(FLOOR, 0, 0, 0, 4, 0, 1)
        .block("crafter[orientation=west_up]", CRAFTER_X, y, row)
        .block("hopper[facing=down]", CRAFTER_X, y + 1, row)
        .block("hopper[facing=west]", CRAFTER_OUT_HOPPER_X, y, row)
        .block("chest[facing=north]", CRAFTER_CHEST_X, y, row)
        .block("comparator[facing=west]", CRAFTER_COMPARATOR_X, y, row)
        .block(dust(0), CRAFTER_DUST_X, y, row)
        .block("stone_button[face=floor,facing=north]", CRAFTER_X, y, CRAFTER_BUTTON_Z)
        .build();
  }

  // The crafter's guide: machines that craft on their own as soon as the grid is full.

  /** The row (z) and height of the guide's crafters. */
  public static final int GUIDE_ROW = 3;

  public static final int GUIDE_Y = 1;

  /** The iron machine: its crafter, the chest it crafts into, its last dust. */
  public static final int IRON_CRAFTER_X = 1;

  public static final int IRON_CHEST_X = 0;
  public static final int IRON_LAST_DUST_X = 1;
  public static final int IRON_LAST_DUST_Z = 1;

  /** The gold machine: nuggets go into the first crafter, which crafts into the second. */
  public static final int GOLD_FIRST_X = 4;

  public static final int GOLD_SECOND_X = 3;
  public static final int GOLD_CHEST_X = 2;

  /** The compact machine: its row, its crafter and chest, the spare crafter that gives the 9. */
  public static final int COMPACT_ROW = 1;

  public static final int COMPACT_CRAFTER_X = 1;
  public static final int COMPACT_CHEST_X = 0;
  public static final int COMPACT_SPARE_X = 2;
  public static final int COMPACT_SPARE_Z = 3;

  private static final String GUIDE_CRAFTER = "crafter[orientation=west_up]";
  private static final String GUIDE_HOPPER = "hopper[facing=down]";
  private static final String GUIDE_CHEST = "chest[facing=north]";

  /** A cell of a machine's flat dust line. */
  private record Spot(int x, int z) {}

  /**
   * The nine dust of a crafter facing west at x = 1, row {@link #GUIDE_ROW}: out of its comparator
   * (east), round and into the block on its north side. Signal 9 reaches the last with 1 left.
   */
  private static List<Spot> ironDust(int shiftX) {
    return List.of(
        new Spot(3 + shiftX, GUIDE_ROW),
        new Spot(4 + shiftX, GUIDE_ROW),
        new Spot(4 + shiftX, 2),
        new Spot(4 + shiftX, 1),
        new Spot(4 + shiftX, 0),
        new Spot(3 + shiftX, 0),
        new Spot(2 + shiftX, 0),
        new Spot(1 + shiftX, 0),
        new Spot(1 + shiftX, 1));
  }

  /** The nine dust of the gold machine's second crafter, from its comparator round to its block. */
  private static final List<Spot> GOLD_SECOND_DUST =
      List.of(
          new Spot(3, 5),
          new Spot(2, 5),
          new Spot(1, 5),
          new Spot(0, 5),
          new Spot(0, 4),
          new Spot(0, GUIDE_ROW),
          new Spot(0, 2),
          new Spot(1, 2),
          new Spot(2, 2));

  /**
   * A dust line at {@link #GUIDE_Y}, from the output of the comparator at {@code from} into the
   * block at {@code into}, each dust drawn joined to its neighbours; {@code power} is the first
   * dust's, one less each dust after it.
   */
  private static void dustLine(
      Diorama.Builder builder, Spot from, List<Spot> line, Spot into, int power) {
    for (int index = 0; index < line.size(); index++) {
      Spot spot = line.get(index);
      Spot before = index == 0 ? from : line.get(index - 1);
      Spot after = index == line.size() - 1 ? into : line.get(index + 1);
      builder.block(
          wire(
              Math.max(0, power - index),
              joined(spot, before, after, 1, 0),
              joined(spot, before, after, -1, 0),
              joined(spot, before, after, 0, -1),
              joined(spot, before, after, 0, 1)),
          spot.x(),
          GUIDE_Y,
          spot.z());
    }
  }

  /** Whether dust at {@code spot} shows a side toward ({@code dx}, {@code dz}). */
  private static String joined(Spot spot, Spot before, Spot after, int dx, int dz) {
    Spot side = new Spot(spot.x() + dx, spot.z() + dz);
    return side.equals(before) || side.equals(after) ? "side" : "none";
  }

  /**
   * The iron machine, step by step: a hopper fills the crafter, which crafts west into a chest; a
   * comparator reads it; nine dust lead round to a block stuck to the crafter, which passes the
   * signal on (a dust line of nine cannot end on the crafter itself: on a flat floor one from the
   * comparator's output to a cell beside the crafter always has an even number of dust).
   */
  public static List<Diorama> ironMachine() {
    Steps steps = new Steps(Diorama.builder(5, 3, 4).box(FLOOR, 0, 0, 0, 4, 0, 3));
    int y = GUIDE_Y;
    steps
        .next()
        .block(GUIDE_CRAFTER, IRON_CRAFTER_X, y, GUIDE_ROW)
        .block(GUIDE_HOPPER, IRON_CRAFTER_X, y + 1, GUIDE_ROW)
        .block(GUIDE_CHEST, IRON_CHEST_X, y, GUIDE_ROW);
    steps.next().block("comparator[facing=west]", IRON_CRAFTER_X + 1, y, GUIDE_ROW);
    Diorama.Builder wired = steps.next().block(SOLID, IRON_CRAFTER_X, y, GUIDE_ROW - 1);
    dustLine(
        wired,
        new Spot(IRON_CRAFTER_X + 1, GUIDE_ROW),
        ironDust(0),
        new Spot(IRON_CRAFTER_X, GUIDE_ROW - 1),
        0);
    return steps.build();
  }

  /** The iron machine at the moment the grid fills: signal 9, and 1 left at the ninth dust. */
  public static Diorama ironMachineFull() {
    Diorama.Builder builder =
        Diorama.builder(5, 3, 4)
            .box(FLOOR, 0, 0, 0, 4, 0, 3)
            .block(
                "crafter[orientation=west_up,triggered=true]", IRON_CRAFTER_X, GUIDE_Y, GUIDE_ROW)
            .block(GUIDE_HOPPER, IRON_CRAFTER_X, GUIDE_Y + 1, GUIDE_ROW)
            .block(GUIDE_CHEST, IRON_CHEST_X, GUIDE_Y, GUIDE_ROW)
            .block("comparator[facing=west,powered=true]", IRON_CRAFTER_X + 1, GUIDE_Y, GUIDE_ROW)
            .block(SOLID, IRON_CRAFTER_X, GUIDE_Y, GUIDE_ROW - 1);
    dustLine(
        builder,
        new Spot(IRON_CRAFTER_X + 1, GUIDE_ROW),
        ironDust(0),
        new Spot(IRON_CRAFTER_X, GUIDE_ROW - 1),
        CrafterRules.SLOTS);
    return builder.build();
  }

  /**
   * The gold machine, step by step: two crafters in a row, the first (hopper on top) crafting
   * nuggets into the second, which crafts ingots into a chest; then the iron machine's comparator
   * and nine dust for each. The second one's comparator stands on its south side: its back is the
   * first crafter.
   */
  public static List<Diorama> goldMachine() {
    Steps steps = new Steps(Diorama.builder(8, 3, 6).box(FLOOR, 0, 0, 0, 7, 0, 5));
    int y = GUIDE_Y;
    steps
        .next()
        .block(GUIDE_CRAFTER, GOLD_FIRST_X, y, GUIDE_ROW)
        .block(GUIDE_HOPPER, GOLD_FIRST_X, y + 1, GUIDE_ROW)
        .block(GUIDE_CRAFTER, GOLD_SECOND_X, y, GUIDE_ROW)
        .block(GUIDE_CHEST, GOLD_CHEST_X, y, GUIDE_ROW);
    Diorama.Builder first =
        steps
            .next()
            .block("comparator[facing=west]", GOLD_FIRST_X + 1, y, GUIDE_ROW)
            .block(SOLID, GOLD_FIRST_X, y, GUIDE_ROW - 1);
    dustLine(
        first,
        new Spot(GOLD_FIRST_X + 1, GUIDE_ROW),
        ironDust(GOLD_FIRST_X - IRON_CRAFTER_X),
        new Spot(GOLD_FIRST_X, GUIDE_ROW - 1),
        0);
    Diorama.Builder second =
        steps
            .next()
            .block("comparator[facing=north]", GOLD_SECOND_X, y, GUIDE_ROW + 1)
            .block(SOLID, GOLD_SECOND_X, y, GUIDE_ROW - 1);
    dustLine(
        second,
        new Spot(GOLD_SECOND_X, GUIDE_ROW + 1),
        GOLD_SECOND_DUST,
        new Spot(GOLD_SECOND_X, GUIDE_ROW - 1),
        0);
    return steps.build();
  }

  /**
   * The compact machine (7 blocks, measured the most compact): the comparator compares with the 9
   * read from a spare crafter whose nine slots are disabled, so it gives a signal only when the
   * crafter is full (9 is not stronger than 9); three dust carry it round to a target block stuck
   * to the crafter, which takes the dust's signal and powers it.
   */
  public static Diorama compactMachine() {
    int row = COMPACT_ROW;
    int y = GUIDE_Y;
    int comparatorX = COMPACT_CRAFTER_X + 1;
    return Diorama.builder(4, 3, 4)
        .box(FLOOR, 0, 0, 0, 3, 0, 3)
        .block(GUIDE_CRAFTER, COMPACT_CRAFTER_X, y, row)
        .block(GUIDE_HOPPER, COMPACT_CRAFTER_X, y + 1, row)
        .block(GUIDE_CHEST, COMPACT_CHEST_X, y, row)
        .block("comparator[facing=west]", comparatorX, y, row)
        .block("comparator[facing=south]", COMPACT_SPARE_X, y, row + 1)
        .block(GUIDE_CRAFTER, COMPACT_SPARE_X, y, COMPACT_SPARE_Z)
        .block(wire(0, "none", "side", "side", "none"), comparatorX + 1, y, row)
        .block(wire(0, "none", "side", "none", "side"), comparatorX + 1, y, row - 1)
        .block(wire(0, "side", "side", "none", "side"), comparatorX, y, row - 1)
        .block("target", COMPACT_CRAFTER_X, y, row - 1)
        .build();
  }

  /** A comparator on a block, as placed, in compare ({@code subtract} false) or subtract mode. */
  public static Diorama comparatorFromAbove(boolean subtract) {
    return Diorama.builder(1, 2, 1)
        .block(FLOOR, 0, 0, 0)
        .block("comparator[facing=south,mode=" + (subtract ? "subtract" : "compare") + "]", 0, 1, 0)
        .build();
  }

  /** A comparator reading the chest at its input end (the end with two torches). */
  public static Diorama comparatorReadingChest() {
    return Diorama.builder(1, 2, 2)
        .box(FLOOR, 0, 0, 0, 0, 0, 1)
        .block("comparator[facing=south,powered=true]", 0, 1, 0)
        .block("chest[facing=north]", 0, 1, 1)
        .build();
  }

  /**
   * A comparator's worked example seen from above: {@code input} comes along the dust on the left,
   * {@code side} along the dust above the comparator, and the right dust carries what {@link
   * ComparatorSignal#output} gives. The top view has south up and east on the left, so the input is
   * the east end and the side the south.
   */
  public static Diorama comparatorExample(int input, int side, boolean subtract) {
    int output = ComparatorSignal.output(input, side, subtract);
    int east = EXAMPLE_COLUMNS - 1;
    int comparator = east - EXAMPLE_COMPARATOR_COLUMN;
    int south = EXAMPLE_ROWS - 1;
    return Diorama.builder(EXAMPLE_COLUMNS, 2, EXAMPLE_ROWS)
        .box(FLOOR, 0, 0, 0, east, 0, south)
        .block(dust(input), east, 1, 0)
        .block(
            "comparator[facing=east,mode="
                + (subtract ? "subtract" : "compare")
                + ",powered="
                + (output > 0)
                + "]",
            comparator,
            1,
            0)
        .block(dust(output), 0, 1, 0)
        .block(wire(side, "none", "none", "side", "side"), comparator, 1, south)
        .build();
  }

  // Techniques: one diorama per step, all in one box.

  /**
   * Onto the Nether roof: ladders up a wall to the bedrock ceiling ({@link #ROOF_CEILING_Y}); then
   * the pearl (a step told in words).
   */
  static List<Diorama> netherRoof() {
    Steps steps =
        new Steps(
            Diorama.builder(3, ROOF_CEILING_Y + 1, 2)
                .box("netherrack", 0, 0, 0, 2, 0, 1)
                .box("netherrack", 0, 1, 0, 0, ROOF_CEILING_Y - 1, 1)
                .box("bedrock", 0, ROOF_CEILING_Y, 0, 2, ROOF_CEILING_Y, 1));
    steps.next().box("ladder[facing=east]", 1, 1, 1, 1, ROOF_CEILING_Y - 1, 1);
    steps.repeat();
    return steps.build();
  }

  /**
   * Bedrock by instant mining: a piston on the bedrock facing up, powered by a torch (it extends);
   * then torch and piston broken and a piston facing down placed in its place within one tick; its
   * queued retraction removes the bedrock ({@code PistonBaseBlock#triggerEvent}).
   */
  static List<Diorama> instantMining() {
    Steps steps = new Steps(Diorama.builder(3, 3, 1).box("bedrock", 0, 0, 0, 2, 0, 0));
    steps
        .next()
        .block("piston[facing=up,extended=true]", BEDROCK_X, 1, 0)
        .block("piston_head[facing=up]", BEDROCK_X, 2, 0)
        .block("redstone_torch", BEDROCK_X - 1, 1, 0);
    steps
        .next()
        .block("air", BEDROCK_X - 1, 1, 0)
        .block("air", BEDROCK_X, 2, 0)
        .block("piston[facing=down]", BEDROCK_X, 1, 0);
    steps.next().block("air", BEDROCK_X, 0, 0);
    return steps.build();
  }

  /**
   * Bedrock by an exploding piston: a piston on the bedrock facing sideways under obsidian, a
   * trapdoor to crawl beside it, TNT on the far side; a lever powers the piston and one the TNT.
   * The blast breaks lever and piston; a piston placed under the obsidian at once faces down and
   * its queued retraction removes the bedrock.
   */
  static List<Diorama> tntPiston() {
    Steps steps = new Steps(Diorama.builder(5, 3, 2).box("bedrock", 0, 0, 0, 4, 0, 1));
    steps
        .next()
        .block("piston[facing=east]", BEDROCK_X, 1, 0)
        .block("obsidian", BEDROCK_X, 2, 0)
        .block("oak_trapdoor[half=top,facing=east]", BEDROCK_X - 1, 1, 0)
        .block("tnt", TNT_X, 1, 0)
        .block("lever[face=floor,facing=east]", BEDROCK_X, 1, 1)
        .block("lever[face=floor,facing=east]", TNT_X, 1, 1);
    steps
        .next()
        .block("piston[facing=east,extended=true]", BEDROCK_X, 1, 0)
        .block("piston_head[facing=east]", BEDROCK_X + 1, 1, 0)
        .block("lever[face=floor,facing=east,powered=true]", BEDROCK_X, 1, 1)
        .block("lever[face=floor,facing=east,powered=true]", TNT_X, 1, 1);
    steps
        .next()
        .block("air", BEDROCK_X + 1, 1, 0)
        .block("air", TNT_X, 1, 0)
        .block("air", BEDROCK_X, 1, 1)
        .block("air", TNT_X, 1, 1)
        .block("piston[facing=down]", BEDROCK_X, 1, 0)
        .block("air", BEDROCK_X, 0, 0);
    return steps.build();
  }

  // Circuits: one diorama per step, all in one box.

  /**
   * Self-emptying dropper (7 blocks, measured the most compact): a comparator in subtract mode
   * reads the dropper and feeds a repeater; four dust bring the signal back to the comparator's
   * side and into a target block beside the dropper. While the dropper holds items the loop
   * switches on and off and every pulse fires one; empty, it rests.
   */
  static List<Diorama> autoDropper() {
    Steps steps = new Steps(Diorama.builder(5, 3, 2).box(FLOOR, 0, 0, 0, 4, 0, 1));
    steps
        .next()
        .block(GUIDE_CHEST, DROPPER_CHEST_X, 1, DROPPER_ROW)
        .block("dropper[facing=west]", DROPPER_X, 1, DROPPER_ROW)
        .block(GUIDE_HOPPER, DROPPER_X, 2, DROPPER_ROW);
    steps
        .next()
        .block("comparator[facing=west,mode=subtract]", DROPPER_X + 1, 1, DROPPER_ROW)
        .block("repeater[facing=west]", DROPPER_X + 2, 1, DROPPER_ROW);
    steps
        .next()
        .block(wire(0, "none", "side", "none", "side"), DROPPER_X + 3, 1, DROPPER_ROW)
        .block(wire(0, "none", "side", "side", "none"), DROPPER_X + 3, 1, DROPPER_ROW + 1)
        .block(dust(0), DROPPER_X + 2, 1, DROPPER_ROW + 1)
        .block(wire(0, "side", "side", "side", "none"), DROPPER_X + 1, 1, DROPPER_ROW + 1)
        .block("target", DROPPER_X, 1, DROPPER_ROW + 1);
    return steps.build();
  }

  /** Observer clock: two observers facing each other; the back one feeds dust. */
  static List<Diorama> observerClock() {
    Steps steps = new Steps(Diorama.builder(4, 2, 1).box(FLOOR, 0, 0, 0, 3, 0, 0));
    steps.next().block("observer[facing=west]", 2, 1, 0).block(dust(0), 3, 1, 0);
    steps.next().block("observer[facing=east]", 1, 1, 0);
    steps
        .next()
        .block("observer[facing=west,powered=true]", 2, 1, 0)
        .block(dust(RedstoneRules.MAX_SIGNAL), 3, 1, 0);
    return steps.build();
  }

  /** Repeater clock: the button beside the loop (x, z) and a corner of the loop the test reads. */
  public static final int RING_BUTTON_Z = 2;

  public static final int RING_PROBE_X = 3;

  /**
   * Repeater clock: four repeaters set to 4 in a loop with dust at the corners; a button next to
   * the loop starts a pulse that runs round it.
   */
  static List<Diorama> repeaterClock() {
    Steps steps = new Steps(Diorama.builder(4, 2, 3).box(FLOOR, 0, 0, 0, 3, 0, 2));
    steps
        .next()
        .block(wire(0, "side", "none", "none", "side"), 0, 1, 0)
        .block("repeater[facing=west,delay=4]", 1, 1, 0)
        .block("repeater[facing=west,delay=4]", 2, 1, 0)
        .block(wire(0, "none", "side", "none", "side"), RING_PROBE_X, 1, 0)
        .block(wire(0, "side", "none", "side", "none"), 0, 1, 1)
        .block("repeater[facing=east,delay=4]", 1, 1, 1)
        .block("repeater[facing=east,delay=4]", 2, 1, 1)
        .block(wire(0, "none", "side", "side", "none"), 3, 1, 1);
    steps
        .next()
        .block("stone_button[face=floor,facing=north]", 0, 1, RING_BUTTON_Z)
        .block(wire(0, "side", "none", "side", "side"), 0, 1, 1);
    steps
        .next()
        .block("repeater[facing=west,delay=4,powered=true]", 1, 1, 0)
        .block("repeater[facing=west,delay=4,powered=true]", 2, 1, 0)
        .block(wire(RedstoneRules.MAX_SIGNAL, "none", "side", "none", "side"), 3, 1, 0)
        .block(wire(RedstoneRules.MAX_SIGNAL, "none", "side", "side", "none"), 3, 1, 1);
    return steps.build();
  }

  /** Hopper clock: the hoppers (x), their row's height and the pistons' row above. */
  public static final int CLOCK_LEFT_HOPPER_X = 2;

  public static final int CLOCK_RIGHT_HOPPER_X = 3;
  public static final int CLOCK_HOPPER_Y = 1;
  public static final int CLOCK_PISTON_Y = 2;

  /**
   * Hopper clock (Etho's): two hoppers feeding each other, a comparator reading each into a block
   * with dust on it; on the comparators, sticky pistons facing each other; a redstone block over
   * one hopper locks it while the other empties into it, then the pistons swap it over.
   */
  static List<Diorama> hopperClock() {
    int hopperY = CLOCK_HOPPER_Y;
    int pistonY = CLOCK_PISTON_Y;
    Steps steps = new Steps(Diorama.builder(6, 3, 1).box(FLOOR, 0, 0, 0, 5, 0, 0));
    steps
        .next()
        .block(SOLID, 0, hopperY, 0)
        .block("comparator[facing=east]", 1, hopperY, 0)
        .block("hopper[facing=east]", CLOCK_LEFT_HOPPER_X, hopperY, 0)
        .block("hopper[facing=west]", CLOCK_RIGHT_HOPPER_X, hopperY, 0)
        .block("comparator[facing=west]", 4, hopperY, 0)
        .block(SOLID, 5, hopperY, 0);
    steps
        .next()
        .block(DUST_CROSS, 0, pistonY, 0)
        .block("sticky_piston[facing=east]", 1, pistonY, 0)
        .block("redstone_block", CLOCK_LEFT_HOPPER_X, pistonY, 0)
        .block("hopper[facing=east,enabled=false]", CLOCK_LEFT_HOPPER_X, hopperY, 0)
        .block("sticky_piston[facing=west]", 4, pistonY, 0)
        .block(DUST_CROSS, 5, pistonY, 0);
    steps
        .next()
        .block("comparator[facing=west,powered=true]", 4, hopperY, 0)
        .block("sticky_piston[facing=west,extended=true]", 4, pistonY, 0)
        .block("piston_head[facing=west,type=sticky]", CLOCK_RIGHT_HOPPER_X, pistonY, 0)
        .block("redstone_wire[east=side,west=side,north=side,south=side,power=3]", 5, pistonY, 0);
    return steps.build();
  }

  /** T flip-flop: a button toggles a copper bulb; a comparator reads the bulb into dust. */
  static List<Diorama> copperBulbFlipFlop() {
    Steps steps = new Steps(Diorama.builder(4, 2, 1).box(FLOOR, 0, 0, 0, 3, 0, 0));
    steps
        .next()
        .block("copper_bulb[lit=false]", 2, 1, 0)
        .block("stone_button[face=floor,facing=north]", 3, 1, 0);
    steps.next().block("comparator[facing=east]", 1, 1, 0).block(dust(0), 0, 1, 0);
    steps
        .next()
        .block("copper_bulb[lit=true]", 2, 1, 0)
        .block("comparator[facing=east,powered=true]", 1, 1, 0)
        .block(dust(RedstoneRules.MAX_SIGNAL), 0, 1, 0);
    return steps.build();
  }

  /** Pulse limiter: the lever, the sticky piston and where its block stands, then is dropped. */
  public static final int LIMITER_LEVER_X = 0;

  public static final int LIMITER_BLOCK_X = 3;
  public static final int LIMITER_DROPPED_X = 4;

  /**
   * Pulse limiter: an observer watching a lever gives a 0.1 s pulse at each flip; a sticky piston
   * behind it, given such a pulse, drops its block (the next flip takes it back).
   */
  static List<Diorama> pulseLimiter() {
    Steps steps = new Steps(Diorama.builder(5, 2, 1).box(FLOOR, 0, 0, 0, 4, 0, 0));
    steps
        .next()
        .block("lever[face=floor,facing=east]", LIMITER_LEVER_X, 1, 0)
        .block("observer[facing=west]", 1, 1, 0);
    steps.next().block("sticky_piston[facing=east]", 2, 1, 0).block(SOLID, LIMITER_BLOCK_X, 1, 0);
    steps
        .next()
        .block("lever[face=floor,facing=east,powered=true]", LIMITER_LEVER_X, 1, 0)
        .clear(LIMITER_BLOCK_X, 1, 0)
        .block(SOLID, LIMITER_DROPPED_X, 1, 0);
    return steps.build();
  }

  /** Pulse extender: the button (x, z) and the lamp in the floor under the loop's side. */
  public static final int EXTENDER_BUTTON_X = 0;

  public static final int EXTENDER_LAMP_X = 1;
  public static final int EXTENDER_LAMP_Z = 1;

  /**
   * Pulse extender: three comparators in a loop, dust at the corners and along one side; each lap
   * the signal fades on that side, so it lasts a while after the button. A lamp under the side dust
   * shows it.
   */
  static List<Diorama> pulseExtender() {
    Steps steps = new Steps(Diorama.builder(4, 2, 3).box(FLOOR, 0, 0, 0, 3, 0, 2));
    steps
        .next()
        .block("comparator[facing=west]", 2, 1, 0)
        .block(wire(0, "none", "side", "none", "side"), 3, 1, 0)
        .block("comparator[facing=north]", 3, 1, 1)
        .block(wire(0, "none", "side", "side", "none"), 3, 1, 2)
        .block("comparator[facing=east]", 2, 1, 2)
        .block(wire(0, "side", "none", "side", "none"), 1, 1, 2)
        .block(wire(0, "none", "none", "side", "side"), 1, 1, 1)
        .block(wire(0, "side", "none", "none", "side"), 1, 1, 0);
    steps
        .next()
        .block("stone_button[face=floor,facing=north]", EXTENDER_BUTTON_X, 1, 0)
        .block(wire(0, "side", "side", "none", "side"), 1, 1, 0)
        .block("redstone_lamp", EXTENDER_LAMP_X, 0, EXTENDER_LAMP_Z);
    int strong = RedstoneRules.MAX_SIGNAL - 2;
    steps
        .next()
        .block("comparator[facing=west,powered=true]", 2, 1, 0)
        .block(wire(strong, "none", "side", "none", "side"), 3, 1, 0)
        .block("comparator[facing=north,powered=true]", 3, 1, 1)
        .block(wire(strong, "none", "side", "side", "none"), 3, 1, 2)
        .block("comparator[facing=east,powered=true]", 2, 1, 2)
        .block(wire(strong, "side", "none", "side", "none"), 1, 1, 2)
        .block(wire(strong - 1, "none", "none", "side", "side"), 1, 1, 1)
        .block(wire(strong - 2, "side", "side", "none", "side"), 1, 1, 0)
        .block("redstone_lamp[lit=true]", EXTENDER_LAMP_X, 0, EXTENDER_LAMP_Z);
    return steps.build();
  }

  /** Item sorter: the filter hopper, the locked hopper under it and the chest it fills. */
  public static final int SORTER_FILTER_X = 4;

  public static final int SORTER_FILTER_Y = 3;
  public static final int SORTER_CHEST_X = 5;
  public static final int SORTER_LOCKED_Y = 2;

  /**
   * Hopper item sorter (the wiki's basic overflow-safe one, Java Edition): a filter hopper over a
   * locked hopper that feeds a chest; a comparator reads the filter, two dust and a lower dust
   * carry its signal to a repeater, which turns off the torch that locks the bottom hopper.
   */
  static List<Diorama> itemSorter() {
    Steps steps = new Steps(Diorama.builder(6, 5, 1));
    steps
        .next()
        .block("hopper[facing=south]", SORTER_FILTER_X, SORTER_FILTER_Y + 1, 0)
        .block("hopper[facing=west]", SORTER_FILTER_X, SORTER_FILTER_Y, 0)
        .block("hopper[facing=east]", SORTER_FILTER_X, SORTER_LOCKED_Y, 0)
        .block("chest[facing=north]", SORTER_CHEST_X, SORTER_LOCKED_Y, 0);
    steps
        .next()
        .box(SOLID, 1, 2, 0, 3, 2, 0)
        .block(SOLID, 0, 1, 0)
        .block("comparator[facing=east,powered=true]", 3, 3, 0)
        .block(dust(2), 2, 3, 0)
        .block(dust(1), 1, 3, 0)
        .block(wire(0, "up", "side", "none", "none"), 0, 2, 0);
    steps
        .next()
        .block(SOLID, 1, 0, 0)
        .block("repeater[facing=west,delay=1]", 1, 1, 0)
        .block(SOLID, 2, 1, 0)
        .block("redstone_wall_torch[facing=east,lit=true]", 3, 1, 0)
        .block("hopper[facing=east,enabled=false]", SORTER_FILTER_X, SORTER_LOCKED_Y, 0);
    steps.repeat();
    return steps.build();
  }

  /** Records a picture after each step of one builder: each step adds to the last. */
  private static final class Steps {
    private final Diorama.Builder builder;
    private final List<Diorama> pictures = new ArrayList<>();
    private boolean started;

    Steps(Diorama.Builder builder) {
      this.builder = builder;
    }

    /** Ends the step before (if any) and starts the next. */
    Diorama.Builder next() {
      if (started) {
        pictures.add(builder.build());
      }
      started = true;
      return builder;
    }

    /** A step that changes nothing: the last picture again (a step told in words). */
    void repeat() {
      next();
    }

    List<Diorama> build() {
      next();
      return List.copyOf(pictures);
    }
  }
}
