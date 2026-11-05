package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.Cast;
import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.Table;
import com.einsteinmods.cheatsheet.client.sheet.TileGrid;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.mobfarms.MobFarm;
import com.einsteinmods.cheatsheet.core.pictures.Diorama;
import com.einsteinmods.cheatsheet.core.redstone.NetherRoof;
import com.einsteinmods.cheatsheet.core.redstone.PistonRules;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneBuild;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneCard;
import com.einsteinmods.cheatsheet.core.redstone.RedstoneRules;
import com.einsteinmods.cheatsheet.core.redstone.SculkSensors;
import com.einsteinmods.cheatsheet.core.redstone.SorterFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Redstone tab, the minimum on each card: a picture, a name and one key fact. Reference cards
 * (pressure plates, comparator, sculk sensors, ticks...) zoom into all their numbers ({@link
 * RedstoneDetails}); mechanics and circuits zoom into a numbered step-by-step build ({@link
 * RedstoneGuide}); farms zoom into every condition they need ({@link FarmDetails}).
 *
 * <p>Numbers come from {@code core.redstone}, which copies Minecraft 26.3 and is checked by the
 * redstone GameTests: the builds are placed and run in a world, the tables against the game.
 */
public final class RedstoneSheet implements SheetSource {
  public static final String ID = "redstone";

  /** Eight cards fit a row of a 960-pixel sheet. */
  private static final int CARD_WIDTH = 106;

  private static final int REFERENCE_PICTURE_WIDTH = 88;
  private static final int REFERENCE_PICTURE_HEIGHT = 48;
  private static final int BUILD_PICTURE_WIDTH = 90;
  private static final int BUILD_PICTURE_HEIGHT = 60;

  /**
   * Builds are seen from the north, turned a little east (a game yaw: 180 is north), from a little
   * above.
   */
  private static final float BUILD_YAW = 190.0F;

  private static final float BUILD_PITCH = 22.0F;
  private static final String NAMESPACE = "minecraft:";

  /** Picture, title, key fact: every card of a kind has them in the same place. */
  private static final List<Table.Align> TILE_SLOTS = Collections.nCopies(3, Table.Align.CENTER);

  static final CardTint REFERENCE_TINT = CardTint.RED;
  static final CardTint MECHANIC_TINT = CardTint.BLUE;
  static final CardTint CIRCUIT_TINT = CardTint.ORANGE;
  static final CardTint TECHNIQUE_TINT = CardTint.PURPLE;
  static final CardTint FARM_TINT = CardTint.GREEN;

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.REDSTONE);
  }

  @Override
  public Sheet build(SheetContext context) {
    PictureAtlas atlas = new PictureAtlas(ID);
    SlotTiles tiles = new SlotTiles(CARD_WIDTH);
    SlotTiles.Layout reference = tiles.layout(REFERENCE_TINT, TILE_SLOTS);
    List<Supplier<ZoomCard>> referenceZooms = new ArrayList<>();
    for (RedstoneCard card : RedstoneCard.values()) {
      reference.add(referenceTile(context, atlas, card));
      referenceZooms.add(() -> RedstoneDetails.of(context, atlas, card));
    }
    SlotTiles.Layout mechanics = tiles.layout(MECHANIC_TINT, TILE_SLOTS);
    tiles.nextSection();
    SlotTiles.Layout circuits = tiles.layout(CIRCUIT_TINT, TILE_SLOTS);
    tiles.nextSection();
    SlotTiles.Layout techniques = tiles.layout(TECHNIQUE_TINT, TILE_SLOTS);
    List<Supplier<ZoomCard>> mechanicZooms = new ArrayList<>();
    List<Supplier<ZoomCard>> circuitZooms = new ArrayList<>();
    List<Supplier<ZoomCard>> techniqueZooms = new ArrayList<>();
    for (RedstoneBuild build : RedstoneBuild.values()) {
      SlotTiles.Layout layout =
          switch (build.kind()) {
            case MECHANIC -> mechanics;
            case CIRCUIT -> circuits;
            case TECHNIQUE -> techniques;
          };
      List<Supplier<ZoomCard>> zooms =
          switch (build.kind()) {
            case MECHANIC -> mechanicZooms;
            case CIRCUIT -> circuitZooms;
            case TECHNIQUE -> techniqueZooms;
          };
      layout.add(buildTile(context, atlas, build));
      zooms.add(() -> RedstoneGuide.build(context, atlas, build));
    }
    tiles.nextSection();
    SlotTiles.Layout farms = tiles.layout(FARM_TINT, TILE_SLOTS);
    List<Supplier<ZoomCard>> farmZooms = new ArrayList<>();
    for (MobFarm farm : MobFarm.values()) {
      farms.add(farmTile(context, atlas, farm));
      farmZooms.add(() -> FarmDetails.of(context, atlas, farm));
    }
    tiles.build();

    return Sheet.ofSections(
        List.of(
            Section.of(
                text(context, "section.reference"),
                REFERENCE_TINT,
                new TileGrid(zoomed(reference, referenceZooms), SlotTiles.GAP)),
            Section.of(
                text(context, "section.mechanics"),
                MECHANIC_TINT,
                new TileGrid(zoomed(mechanics, mechanicZooms), SlotTiles.GAP)),
            Section.of(
                text(context, "section.circuits"),
                CIRCUIT_TINT,
                new TileGrid(zoomed(circuits, circuitZooms), SlotTiles.GAP)),
            Section.of(
                text(context, "section.techniques"),
                TECHNIQUE_TINT,
                new TileGrid(zoomed(techniques, techniqueZooms), SlotTiles.GAP)),
            Section.of(
                text(context, "section.farms"),
                FARM_TINT,
                new TileGrid(zoomed(farms, farmZooms), SlotTiles.GAP, true))),
        atlas);
  }

  /** A reference card: its picture, name and key fact; the click zooms into its details. */
  private static List<SheetElement> referenceTile(
      SheetContext context, PictureAtlas atlas, RedstoneCard card) {
    Component title = cardTitle(context, card);
    Component key = cardKey(context, card);
    List<FormattedCharSequence> tooltip =
        Tooltips.of(context.font(), title, List.of(key, text(context, "card.zoom")));
    return List.of(
        atlas.add(
            referenceScene(card.picture()),
            REFERENCE_PICTURE_WIDTH,
            REFERENCE_PICTURE_HEIGHT,
            tooltip),
        tooltipped(centredTitle(context, title, REFERENCE_TINT), tooltip),
        tooltipped(highlight(context, key), tooltip));
  }

  static Component cardTitle(SheetContext context, RedstoneCard card) {
    return text(context, "card." + card.key() + ".title");
  }

  /** A reference card's one key fact. */
  static Component cardKey(SheetContext context, RedstoneCard card) {
    String key = "card." + card.key() + ".key";
    return switch (card) {
      case SCULK_SENSORS -> text(context, key, SculkSensors.RANGE, SculkSensors.CALIBRATED_RANGE);
      case TICKS ->
          text(context, key, seconds(1), seconds(RedstoneRules.GAME_TICKS_PER_REDSTONE_TICK));
      default -> text(context, key);
    };
  }

  /** A mechanic's or circuit's build, name and key fact. */
  private static List<SheetElement> buildTile(
      SheetContext context, PictureAtlas atlas, RedstoneBuild build) {
    Component title = text(context, "build." + build.key() + ".title");
    Component key = buildKey(context, build);
    List<FormattedCharSequence> tooltip =
        Tooltips.of(context.font(), title, List.of(key, text(context, "build.zoom")));
    return List.of(
        atlas.add(buildScene(build.cover()), BUILD_PICTURE_WIDTH, BUILD_PICTURE_HEIGHT, tooltip),
        tooltipped(centredTitle(context, title, tint(build)), tooltip),
        tooltipped(highlight(context, key), tooltip));
  }

  /** A build's one-line summary. */
  static Component buildKey(SheetContext context, RedstoneBuild build) {
    String key = "build." + build.key() + ".key";
    return switch (build) {
      case OBSERVER_CLOCK -> text(context, key, seconds(RedstoneRules.OBSERVER_CLOCK_PERIOD_TICKS));
      case REPEATER_CLOCK -> text(context, key, seconds(RedstoneRules.repeaterClockPeriodTicks()));
      case HOPPER_CLOCK -> text(context, key, seconds(RedstoneRules.HOPPER_CLOCK_TICKS_PER_ITEM));
      case PULSE_LIMITER -> text(context, key, seconds(RedstoneRules.OBSERVER_PULSE_TICKS));
      case PULSE_EXTENDER -> text(context, key, seconds(RedstoneRules.PULSE_EXTENDER_HOLD_TICKS));
      case ITEM_SORTER -> text(context, key, SorterFilter.ITEMS, SorterFilter.JUNK_SLOTS);
      case AUTO_DROPPER -> text(context, key, seconds(RedstoneRules.HOPPER_TICKS_PER_ITEM));
      case NETHER_ROOF -> text(context, key, NetherRoof.WALK_Y);
      case STICKY_BLOCKS -> text(context, key, PistonRules.MAX_PUSHED);
      default -> text(context, key);
    };
  }

  /** A farm's card: its place and mob, name and key fact; the click lists its conditions. */
  private static List<SheetElement> farmTile(
      SheetContext context, PictureAtlas atlas, MobFarm farm) {
    Component title = farmTitle(context, farm);
    Component key = farmKey(context, farm);
    List<FormattedCharSequence> tooltip =
        Tooltips.of(context.font(), title, List.of(key, text(context, "card.zoom")));
    return List.of(
        atlas.add(FarmPictures.scene(farm), BUILD_PICTURE_WIDTH, BUILD_PICTURE_HEIGHT, tooltip),
        tooltipped(centredTitle(context, title, FARM_TINT), tooltip),
        tooltipped(highlight(context, key), tooltip));
  }

  static Component farmTitle(SheetContext context, MobFarm farm) {
    return text(context, "farm." + farm.key() + ".title");
  }

  /** A farm's one key fact. */
  static Component farmKey(SheetContext context, MobFarm farm) {
    return text(context, "farm." + farm.key() + ".key", farm.keyArgs().toArray());
  }

  static CardTint tint(RedstoneBuild build) {
    return switch (build.kind()) {
      case MECHANIC -> MECHANIC_TINT;
      case CIRCUIT -> CIRCUIT_TINT;
      case TECHNIQUE -> TECHNIQUE_TINT;
    };
  }

  /** A layout's tiles, each zooming as the view its supplier builds on the first click. */
  private static List<SheetElement> zoomed(
      SlotTiles.Layout layout, List<Supplier<ZoomCard>> views) {
    List<SheetElement> tiles = layout.tiles();
    List<SheetElement> zoomed = new ArrayList<>(tiles.size());
    for (int index = 0; index < tiles.size(); index++) {
      zoomed.add(ZoomViews.zoomedAs(tiles.get(index), views.get(index)));
    }
    return zoomed;
  }

  /** A reference card's diorama, seen like a block in the inventory, framed on its whole box. */
  private static Scene referenceScene(Diorama diorama) {
    return Scene.of(diorama)
        .focusedOn(new AABB(0, 0, 0, diorama.width(), diorama.height(), diorama.depth()));
  }

  /**
   * A build's diorama seen nearly from the front (north), a little from the east and above, so a
   * machine laid out in a plane reads like a diagram; framed on its whole box, so every step of a
   * build keeps one scale.
   */
  static Scene buildScene(Diorama diorama) {
    return new Scene(
        Optional.of(diorama),
        Cast.NONE,
        BUILD_YAW,
        BUILD_PITCH,
        0.0F,
        Optional.of(new AABB(0, 0, 0, diorama.width(), diorama.height(), diorama.depth())));
  }

  /** A card's name in bold; a word too long for the card breaks with a hyphen. */
  private static SheetElement centredTitle(SheetContext context, Component title, CardTint tint) {
    int room = CARD_WIDTH - SlotTiles.PADDING * 2;
    return new CentredText(
        context.font(),
        FittedText.hyphenated(context.font(), title.copy().withStyle(ChatFormatting.BOLD), room),
        room,
        tint.title());
  }

  private static SheetElement highlight(SheetContext context, Component text) {
    return new CentredText(
        context.font(), text, CARD_WIDTH - SlotTiles.PADDING * 2, SheetPalette.HIGHLIGHT_TEXT);
  }

  /** {@code element} showing {@code tooltip} anywhere over it. */
  private static SheetElement tooltipped(
      SheetElement element, List<FormattedCharSequence> tooltip) {
    return new Tooltipped(element, tooltip);
  }

  static String seconds(int gameTicks) {
    return RedstoneRules.seconds(gameTicks);
  }

  static ItemStack stack(SheetContext context, String itemId) {
    return context.stack(NAMESPACE + itemId);
  }

  static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args);
  }

  /** An element whose tooltip is given, wherever the mouse is over it. */
  private record Tooltipped(SheetElement element, List<FormattedCharSequence> tooltip)
      implements SheetElement {
    @Override
    public int minWidth() {
      return element.minWidth();
    }

    @Override
    public int height() {
      return element.height();
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      element.extract(graphics, x, y, width);
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      return tooltip;
    }
  }
}
