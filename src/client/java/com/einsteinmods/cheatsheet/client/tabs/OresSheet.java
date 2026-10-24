package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.Table;
import com.einsteinmods.cheatsheet.client.sheet.TileGrid;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.core.ores.BestHeight;
import com.einsteinmods.cheatsheet.core.ores.Ore;
import com.einsteinmods.cheatsheet.core.ores.OreDimension;
import com.einsteinmods.cheatsheet.core.ores.OreProfile;
import com.einsteinmods.cheatsheet.core.ores.OreRow;
import com.einsteinmods.cheatsheet.core.ores.OreTable;
import com.einsteinmods.cheatsheet.core.pictures.Place;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Ores tab, one screen at a glance: where each ore generates and the best height to dig at, as two
 * height charts (Overworld, Nether) on dark panels with the legend under the Nether, and beside
 * them one equal small tile per biome bonus (the place in miniature, its name and the bonus; the
 * details on hover). Heights come from the fixed table in {@code core.ores} (placements are server
 * data, never sent to the client).
 */
public final class OresSheet implements SheetSource {
  public static final String ID = "ores";

  /**
   * Bonus tiles are this wide: one column of them beside both charts fits a 640-pixel-wide sheet,
   * two at 960.
   */
  private static final int TILE_WIDTH = 165;

  /** A biome in miniature on its tile, in sheet pixels. */
  private static final int BIOME_WIDTH = 40;

  private static final int BIOME_HEIGHT = 32;

  /** Width the name and the bonus wrap at, right of the picture. */
  private static final int TEXT_WIDTH =
      TILE_WIDTH - SlotTiles.PADDING * 2 - BIOME_WIDTH - ElementRow.GAP;

  private static final int TEXT_LINE_GAP = 2;

  private static final List<Table.Align> BONUS_SLOTS = List.of(Table.Align.START);
  private static final String BIOME_KEY_PREFIX = "biome.minecraft.";
  private static final String LIST_SEPARATOR = ", ";
  private static final String RANGE_SEPARATOR = "–";
  private static final String BADLANDS_GOLD = "ore_gold_extra";
  private static final String DRIPSTONE_COPPER = "ore_copper_large";
  private static final String DELTAS_GOLD = "ore_gold_deltas";
  private static final String DELTAS_QUARTZ = "ore_quartz_deltas";
  private static final String LARGE_DEBRIS = "ore_ancient_debris_large";
  private static final String SMALL_DEBRIS = "ore_debris_small";

  @Override
  public String id() {
    return ID;
  }

  /** The charts and bonus cards are big enough to read as they are. */
  @Override
  public boolean zoomsCards() {
    return false;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.DIAMOND_PICKAXE);
  }

  @Override
  public Sheet build(SheetContext context) {
    Component playerText = Component.empty();
    OptionalInt overworldY = OptionalInt.empty();
    OptionalInt netherY = OptionalInt.empty();
    LocalPlayer player = context.minecraft().player;
    if (player != null) {
      int y = player.getBlockY();
      playerText = text(context, "you", y);
      ResourceKey<Level> dimension = player.level().dimension();
      if (Level.OVERWORLD.equals(dimension)) {
        overworldY = OptionalInt.of(y);
      } else if (Level.NETHER.equals(dimension)) {
        netherY = OptionalInt.of(y);
      }
    }
    OreChartsBody charts =
        new OreChartsBody(
            context.font(),
            chart(context, OreDimension.OVERWORLD, overworldY, playerText),
            text(context, "overworld"),
            CardTint.STONE,
            chart(context, OreDimension.NETHER, netherY, playerText),
            text(context, "nether"),
            CardTint.NETHER,
            text(context, "legend"),
            text(context, "legend.bonus"));
    PictureAtlas atlas = new PictureAtlas(ID);
    return Sheet.ofSections(
        List.of(
            Section.of(text(context, "heights"), CardTint.STONE, charts),
            Section.of(
                    text(context, "notes"),
                    CardTint.GREEN,
                    new TileGrid(bonusTiles(context, atlas), SlotTiles.GAP))
                .besidePrevious()),
        atlas);
  }

  private static OreHeightChart chart(
      SheetContext context, OreDimension dimension, OptionalInt playerY, Component playerText) {
    List<OreHeightChart.Column> columns = new ArrayList<>();
    for (Ore ore : Ore.values()) {
      if (ore.dimension() == dimension) {
        columns.add(column(context, OreProfile.of(ore)));
      }
    }
    return OreHeightChart.of(context, dimension, columns, playerY, playerText);
  }

  private static OreHeightChart.Column column(SheetContext context, OreProfile profile) {
    Ore ore = profile.ore();
    BestHeight best = profile.best();
    ItemStack icon = context.stack(ore.itemAt(best.y()));
    List<Component> details = new ArrayList<>();
    details.add(text(context, "range", profile.minY(), profile.maxY()));
    Component bestText;
    if (best.isFlat()) {
      details.add(text(context, "flat", best.fromY(), best.toY()));
      bestText = Component.literal(best.fromY() + RANGE_SEPARATOR + best.toY());
    } else {
      details.add(text(context, "best", best.y()));
      bestText = Component.literal(Integer.toString(best.y()));
    }
    if (profile.mountainBestY().isPresent()) {
      details.add(text(context, "mountains", profile.mountainBestY().getAsInt()));
    }
    List<Component> bonusLines = new ArrayList<>();
    for (OreRow bonus : profile.bonusRows()) {
      bonusLines.add(
          text(
              context,
              "bonus",
              biomeList(bonus.biomes().biomes()),
              Math.max(bonus.minY(), ore.dimension().minY()),
              Math.min(bonus.maxY(), ore.dimension().maxY())));
    }
    details.addAll(bonusLines);
    ItemStack bonusIcon = ItemStack.EMPTY;
    Component bonusTitle = Component.empty();
    if (!profile.bonusRows().isEmpty()) {
      OreRow bonus = profile.bonusRows().getFirst();
      bonusIcon = context.stack(biomeBlock(bonus.featureId()));
      bonusTitle = biomeName(bonus.biomes().biomes().getFirst());
    }
    List<OreRow> rows = OreTable.rowsOf(ore);
    if (rows.stream().allMatch(row -> row.biomes().isBiomeOnly())) {
      details.add(text(context, "only_in", biomeList(rows.getFirst().biomes().biomes())));
    }
    return OreHeightChart.column(
        context.font(),
        profile,
        icon,
        color(ore),
        bestText,
        Tooltips.ofItem(context.font(), icon, details),
        bonusIcon,
        Tooltips.of(context.font(), bonusTitle, bonusLines));
  }

  /** A block of the bonus biome, the small icon at the top of the ore's striped strip. */
  private static String biomeBlock(String featureId) {
    return switch (featureId) {
      case BADLANDS_GOLD -> "minecraft:red_sand";
      case DRIPSTONE_COPPER -> "minecraft:pointed_dripstone";
      case DELTAS_GOLD, DELTAS_QUARTZ -> "minecraft:basalt";
      default -> "minecraft:grass_block";
    };
  }

  /**
   * One equal tile per biome bonus, each with the place in miniature (the debris: its block) and,
   * beside it, the biome's name and the bonus by its ore; the biomes or heights it applies to are
   * in the picture's and the ore's tooltips.
   */
  private static List<SheetElement> bonusTiles(SheetContext context, PictureAtlas atlas) {
    OreRow badlandsGold = row(BADLANDS_GOLD);
    OreRow dripstoneCopper = row(DRIPSTONE_COPPER);
    OreRow deltasGold = row(DELTAS_GOLD);
    OreRow largeDebris = row(LARGE_DEBRIS);
    OreRow smallDebris = row(SMALL_DEBRIS);
    OreRow emerald = OreTable.rowsOf(Ore.EMERALD).getFirst();
    List<String> badlands = badlandsGold.biomes().biomes();
    List<String> mountains = emerald.biomes().biomes();
    List<String> dripstone = dripstoneCopper.biomes().biomes();
    List<String> deltas = deltasGold.biomes().biomes();
    SlotTiles tiles = new SlotTiles(TILE_WIDTH);
    SlotTiles.Layout layout = tiles.layout(CardTint.GREEN, BONUS_SLOTS);
    layout.add(
        bonusTile(
            context,
            biomePicture(context, atlas, Place.BADLANDS, biomeName(badlands.getFirst()), badlands),
            biomeName(badlands.getFirst()),
            context.stack("minecraft:gold_ore"),
            text(context, "bonus.badlands", badlandsGold.minY(), badlandsGold.maxY()),
            biomeList(badlands)));
    layout.add(
        bonusTile(
            context,
            biomePicture(
                context, atlas, Place.MOUNTAINS, text(context, "bonus.mountains"), mountains),
            text(context, "bonus.mountains"),
            context.stack("minecraft:emerald_ore"),
            text(context, "bonus.emerald", OreProfile.of(Ore.EMERALD).best().y()),
            biomeList(mountains)));
    layout.add(
        bonusTile(
            context,
            biomePicture(
                context, atlas, Place.DRIPSTONE_CAVES, biomeName(dripstone.getFirst()), dripstone),
            biomeName(dripstone.getFirst()),
            context.stack("minecraft:copper_ore"),
            text(context, "bonus.dripstone"),
            text(
                context,
                "bonus.dripstone.detail",
                dripstoneCopper.minY(),
                dripstoneCopper.maxY())));
    layout.add(
        bonusTile(
            context,
            biomePicture(context, atlas, Place.BASALT_DELTAS, biomeName(deltas.getFirst()), deltas),
            biomeName(deltas.getFirst()),
            context.stack("minecraft:nether_gold_ore"),
            text(context, "bonus.deltas"),
            text(context, "bonus.deltas.detail", deltasGold.minY(), deltasGold.maxY())));
    ItemStack debris = context.stack("minecraft:ancient_debris");
    layout.add(
        bonusTile(
            context,
            new BoxedElement(
                context.item(debris, SheetContext.BIG, text(context, "bonus.debris.where")),
                BIOME_WIDTH,
                BIOME_HEIGHT),
            text(context, "bonus.debris.where"),
            debris,
            // The veins' heights are long: on the tile only the best height, the rest on hover.
            text(context, "best", OreProfile.of(Ore.ANCIENT_DEBRIS).best().y()),
            text(
                context,
                "bonus.debris",
                largeDebris.minY(),
                largeDebris.maxY(),
                smallDebris.minY(),
                smallDebris.maxY())));
    tiles.build();
    return layout.tiles();
  }

  /** The picture, and beside it the name over the ore and its bonus; the detail on hover. */
  private static List<SheetElement> bonusTile(
      SheetContext context,
      SheetElement picture,
      Component where,
      ItemStack ore,
      Component bonus,
      Component detail) {
    SheetElement text =
        new LeftColumn(
            List.of(
                context.paragraph(
                    where.copy().withStyle(ChatFormatting.BOLD),
                    TEXT_WIDTH,
                    CardTint.GREEN.title()),
                new ElementRow(
                    List.of(
                        context.item(ore, 1, bonus, detail),
                        context.paragraph(
                            bonus,
                            TEXT_WIDTH - ItemIcon.ITEM_SIZE - ElementRow.GAP,
                            SheetPalette.TEXT)),
                    ElementRow.GAP)),
            TEXT_LINE_GAP);
    return List.of(new ElementRow(List.of(picture, text), ElementRow.GAP));
  }

  /** The biome in miniature; its tooltip names the place and lists the biomes. */
  private static SheetElement biomePicture(
      SheetContext context, PictureAtlas atlas, Place biome, Component name, List<String> ids) {
    return atlas.add(
        Scene.of(biome.diorama()),
        BIOME_WIDTH,
        BIOME_HEIGHT,
        Tooltips.of(context.font(), name, List.of(biomeList(ids))));
  }

  private static OreRow row(String featureId) {
    for (OreRow row : OreTable.ROWS) {
      if (row.featureId().equals(featureId)) {
        return row;
      }
    }
    throw new IllegalStateException("missing ore row " + featureId);
  }

  private static Component biomeName(String biomeId) {
    return Component.translatable(BIOME_KEY_PREFIX + biomeId);
  }

  private static Component biomeList(List<String> biomeIds) {
    MutableComponent list = Component.empty();
    for (int index = 0; index < biomeIds.size(); index++) {
      if (index > 0) {
        list.append(LIST_SEPARATOR);
      }
      list.append(biomeName(biomeIds.get(index)));
    }
    return list;
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args);
  }

  /** Each ore's bar colour, close to its block's colour and readable on the dark chart. */
  private static int color(Ore ore) {
    return switch (ore) {
      case COAL -> 0xFF9A9A9A;
      case COPPER -> 0xFFE77C56;
      case IRON -> 0xFFD8AF93;
      case LAPIS -> 0xFF3F6FE6;
      case GOLD, NETHER_GOLD -> 0xFFFCE14B;
      case REDSTONE -> 0xFFFF3B30;
      case DIAMOND -> 0xFF5DECF5;
      case EMERALD -> 0xFF2BD96B;
      case NETHER_QUARTZ -> 0xFFEDE6DE;
      case ANCIENT_DEBRIS -> 0xFFB5826A;
    };
  }
}
