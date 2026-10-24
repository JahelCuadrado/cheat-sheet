package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.Casts;
import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.picture.Scene;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementCard;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
import com.einsteinmods.cheatsheet.client.sheet.Paragraph;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.TextLabel;
import com.einsteinmods.cheatsheet.client.sheet.TileGrid;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.client.sheet.ZoomCard;
import com.einsteinmods.cheatsheet.core.ZoomSize;
import com.einsteinmods.cheatsheet.core.trades.BarterRow;
import com.einsteinmods.cheatsheet.core.trades.BarterTable;
import com.einsteinmods.cheatsheet.core.trades.CountRange;
import com.einsteinmods.cheatsheet.core.trades.MerchantTrades;
import com.einsteinmods.cheatsheet.core.trades.RecipeIngredient;
import com.einsteinmods.cheatsheet.core.trades.TradeGroups;
import com.einsteinmods.cheatsheet.core.trades.TradeItem;
import com.einsteinmods.cheatsheet.core.trades.TradeKind;
import com.einsteinmods.cheatsheet.core.trades.TradePrices;
import com.einsteinmods.cheatsheet.core.trades.TradeRow;
import com.einsteinmods.cheatsheet.core.trades.TradeTier;
import com.einsteinmods.cheatsheet.core.trades.VillagerTradeTable;
import com.einsteinmods.cheatsheet.core.trades.WorkstationRecipe;
import com.einsteinmods.cheatsheet.core.trades.WorkstationRecipeTable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Villagers tab, like the well-known trading infographic: one card per profession, all the same
 * size so five fit side by side and three rows fill a 960x540 sheet, after a first card (top left)
 * of the level badges. A card's header shows the villager, its name, its workstation and that
 * block's crafting recipe in a tiny grid; below, one row per level with the level's real trades in
 * fixed slots ("pay ▶ get", inventory-style counts, a random price in yellow on the paid item). A
 * trade with a second item or a random price takes two slots; trades that do not fit are listed in
 * the level badge's tooltip (its "+" mark). Trades that only differ by colour or biome share one
 * slot. Below the professions, two cards of one size: the wandering trader's three offer sets and
 * piglin bartering (every item, how many, its chance per gold ingot). Trades and recipes are server
 * data, so they come from the fixed tables in {@code core.trades}; names, workstations and icons
 * come from the client's registries.
 *
 * <p>Zoomed, a profession card puts the villager, its workstation and that block's crafting recipe
 * big in a column on the left, and every trade of every level (none hidden) beside them at the
 * sheet's size; the details stay in the tooltips.
 */
public final class VillagersSheet implements SheetSource {
  public static final String ID = "villagers";

  private static final int CARD_GAP = 4;
  private static final int PADDING = 4;
  private static final int LEVELS = 5;

  /** Item sprite side. */
  private static final int ITEM = ItemIcon.ITEM_SIZE;

  /** The "gives" mark between paid and given items: a small solid triangle, one fill a column. */
  private static final int[] ARROW_HEIGHTS = {5, 3, 1};

  private static final int ARROW_WIDTH = ARROW_HEIGHTS.length;
  private static final int ARROW_GAP = 2;

  /** A trade slot: pay ▶ get, two pixels of room above and below the icons. */
  private static final int SLOT_WIDTH = ITEM + ARROW_GAP + ARROW_WIDTH + ARROW_GAP + ITEM;

  private static final int SLOT_HEIGHT = ITEM + 4;
  private static final int ICON_TOP = 2;
  private static final int SLOT_GAP = 3;
  private static final int ROW_GAP = 2;
  private static final int PIECE_GAP = 1;
  private static final int SLOT_FILL = 0x30000000;

  /** Trade slots per villager level; a trade with a second item or a random price takes two. */
  private static final int SLOT_COLUMNS = 4;

  /** Count text sits on an item's bottom-right corner, as in inventories. */
  private static final int COUNT_RIGHT = 17;

  private static final int COUNT_TOP = 9;

  /**
   * Level badges, a tall tab left of each level's slots: its number, a "+" if trades are hidden.
   */
  private static final int BADGE_WIDTH = 10;

  private static final int BADGE_GAP = 2;
  private static final int BADGE_TEXT = 0xFF202020;
  private static final String MORE_MARK = "+";
  private static final int LEVEL_LEFT = PADDING + BADGE_WIDTH + BADGE_GAP;

  /** Every profession card: badges and four slots wide, a header and five level rows tall. */
  private static final int CARD_WIDTH =
      LEVEL_LEFT + SLOT_COLUMNS * SLOT_WIDTH + (SLOT_COLUMNS - 1) * SLOT_GAP + PADDING;

  /** The 3x3 crafting grid in a profession card's header: half-size items, 1-pixel lines. */
  private static final int RECIPE_CELL = SmallItemIcon.SIZE + 1;

  private static final int RECIPE_SIZE = WorkstationRecipe.SIZE * RECIPE_CELL + 1;
  private static final int RECIPE_CELL_FILL = 0x26FFFFFF;

  /** Mob pictures, in sheet pixels. */
  private static final int VILLAGER_WIDTH = 20;

  private static final int VILLAGER_HEIGHT = 34;
  private static final int PORTRAIT_GAP = 3;
  private static final int TRADER_WIDTH = 44;
  private static final int TRADER_HEIGHT = 30;
  private static final int PIGLIN_WIDTH = 18;
  private static final int PIGLIN_HEIGHT = 30;

  /** Header of every card: as tall as the pictures and the recipe grid. */
  private static final int HEADER_HEIGHT = Math.max(VILLAGER_HEIGHT, RECIPE_SIZE);

  private static final int NAME_TOP = PADDING + 1;
  private static final int SECOND_LINE_TOP = PADDING + 13;
  private static final int DIVIDER_GAP = 2;
  private static final int BODY_TOP = PADDING + HEADER_HEIGHT + DIVIDER_GAP * 2 + 1;
  private static final int CARD_HEIGHT =
      BODY_TOP + LEVELS * SLOT_HEIGHT + (LEVELS - 1) * ROW_GAP + PADDING;
  private static final int BAND_GAP = 3;
  private static final int TITLE_GAP = 6;

  /** Profession cards side by side on a 960-pixel-wide GUI. */
  private static final int WIDE_COLUMNS = 5;

  /** The wandering trader's and the piglin's cards: two side by side as wide as five cards. */
  private static final int WIDE_CARD_WIDTH =
      (CARD_WIDTH * WIDE_COLUMNS + CARD_GAP * (WIDE_COLUMNS - 1) - CARD_GAP) / 2;

  /** Piglin bartering: two columns of lines, one item each, its chance as a bar. */
  private static final int BARTER_LINE = ITEM + 1;

  private static final int BARTER_COLUMNS = 2;
  private static final int BAR_WIDTH = 40;
  private static final int BAR_HEIGHT = 5;
  private static final int COLUMN_GAP = 6;
  private static final int ALTERNATE_LINE = 0x10FFFFFF;

  /** Level legend: one badge and its name per line. */
  private static final int LEGEND_LINE = 12;

  private static final int LEGEND_GAP = 3;

  /** Badge colours of villager levels, as on their badges: stone, iron, gold, emerald, diamond. */
  private static final int[] LEVEL_COLORS = {
    0xFF9C9C9C, 0xFFE6E6E6, 0xFFF2C94A, 0xFF3CD06A, 0xFF5DECF5
  };

  private static final int BUYING_COLOR = CardTint.ORANGE.accent();
  private static final int COMMON_COLOR = CardTint.BLUE.accent();
  private static final int UNCOMMON_COLOR = CardTint.PURPLE.accent();

  private static final String MINECRAFT = "minecraft:";
  private static final String BOOK = "book";
  private static final String LEVEL_KEY = "merchant.level.";
  private static final String BIOME_KEY = "biome.minecraft.";
  private static final String ENCHANTMENT_KEY = "enchantment.minecraft.";
  private static final String ARROW = " → ";
  private static final String TIMES = " × ";
  private static final String PLUS = " + ";
  private static final String RANGE = "–";
  private static final String LIST_SEPARATOR = ", ";
  private static final String NAME_SEPARATOR = ": ";
  private static final String OPEN = " (";
  private static final String CLOSE = ")";
  private static final String PERCENT = "%.1f%%";
  private static final double PERCENT_SCALE = 100.0;

  /** A zoomed profession: the villager twice its size, the workstation at 2x, the recipe at 1x. */
  private static final int ZOOM_VILLAGER_WIDTH = VILLAGER_WIDTH * 2;

  private static final int ZOOM_VILLAGER_HEIGHT = VILLAGER_HEIGHT * 2;
  private static final int ZOOM_RECIPE_CELL = ITEM + 1;
  private static final int ZOOM_RECIPE_SIZE = WorkstationRecipe.SIZE * ZOOM_RECIPE_CELL + 1;

  /** The left column: as wide as the recipe grid or the workstation's name. */
  private static final int ZOOM_COLUMN_WIDTH = 64;

  private static final int ZOOM_COLUMN_GAP = 4;
  private static final int ZOOM_SIDE_GAP = 8;

  /** Trade slots per line of a zoomed level; a level with more trades takes more lines. */
  private static final int ZOOM_SLOT_COLUMNS = 5;

  private static final int ZOOM_TRADES_LEFT =
      PADDING + ZOOM_COLUMN_WIDTH + ZOOM_SIDE_GAP + BADGE_WIDTH + BADGE_GAP;
  private static final int ZOOM_WIDTH =
      ZOOM_TRADES_LEFT
          + ZOOM_SLOT_COLUMNS * SLOT_WIDTH
          + (ZOOM_SLOT_COLUMNS - 1) * SLOT_GAP
          + PADDING;

  /** Villager types to the biome whose name players know them by. */
  private static final Map<String, String> TYPE_BIOMES =
      Map.of(
          "desert", "desert",
          "jungle", "jungle",
          "plains", "plains",
          "savanna", "savanna",
          "snow", "snowy_plains",
          "swamp", "swamp",
          "taiga", "taiga");

  /** Wandering trader tiers in table order: what it buys, its common and uncommon offers. */
  private static final List<String> TRADER_TIER_KEYS = List.of("buying", "common", "uncommon");

  private static final int[] TRADER_TIER_COLORS = {BUYING_COLOR, COMMON_COLOR, UNCOMMON_COLOR};

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.EMERALD);
  }

  @Override
  public Sheet build(SheetContext context) {
    PictureAtlas atlas = new PictureAtlas(ID);
    List<SheetElement> professions = new ArrayList<>();
    for (MerchantTrades profession : VillagerTradeTable.PROFESSIONS) {
      Optional<Holder.Reference<VillagerProfession>> type =
          BuiltInRegistries.VILLAGER_PROFESSION.get(
              Identifier.withDefaultNamespace(profession.merchantId()));
      type.ifPresent(
          found ->
              professions.add(
                  ZoomViews.zoomedAs(
                      professionCard(context, profession, found, atlas),
                      () -> professionZoom(context, profession, found, atlas))));
    }
    List<Section> sections = new ArrayList<>();
    if (!professions.isEmpty()) {
      // The level legend first (top left): it explains the badges every card shows.
      professions.addFirst(legend(context));
      sections.add(
          Section.of(
              text(context, "section.professions"),
              CardTint.GREEN,
              new TileGrid(professions, CARD_GAP)));
    }
    FixedCard.Builder trader = traderCard(context, atlas);
    FixedCard.Builder barter = barterCard(context, atlas);
    int height = Math.max(trader.contentBottom(), barter.contentBottom()) + PADDING;
    sections.add(
        Section.of(
            text(context, "section.others"),
            CardTint.AQUA,
            new TileGrid(
                List.of(
                    trader.build(WIDE_CARD_WIDTH, height), barter.build(WIDE_CARD_WIDTH, height)),
                CARD_GAP)));
    return Sheet.ofSections(sections, atlas);
  }

  /**
   * A card of the professions' size: the five level badges with their names, one per line, and how
   * levels add trades.
   */
  private static SheetElement legend(SheetContext context) {
    Font font = context.font();
    int offers = 0;
    for (MerchantTrades profession : VillagerTradeTable.PROFESSIONS) {
      for (TradeTier tier : profession.tiers()) {
        offers = Math.max(offers, tier.offers());
      }
    }
    Component title = text(context, "levels");
    Component note = text(context, "legend", offers);
    List<FormattedCharSequence> tooltip = Tooltips.of(font, title, List.of(note));
    CardTint tint = CardTint.STONE;
    FixedCard.Builder card = new FixedCard.Builder(tint);
    int room = CARD_WIDTH - PADDING * 2;
    card.add(nameLabel(font, title, tint.title(), room), PADDING, NAME_TOP);
    int top = NAME_TOP + font.lineHeight + LEGEND_GAP;
    for (int level = 1; level <= LEVELS; level++) {
      card.row()
          .add(
              new FixedCard.CellPiece(
                  font,
                  new IconGrid.Flow(font)
                      .badge(LEVEL_COLORS[level - 1], Component.literal(Integer.toString(level)))
                      .gap(BADGE_GAP)
                      .text(Component.translatable(LEVEL_KEY + level), SheetPalette.TEXT)
                      .build(tooltip)),
              PADDING,
              top);
      top += LEGEND_LINE;
    }
    card.row()
        .add(
            Paragraph.wholeWords(font, note, room, SheetPalette.MUTED_TEXT),
            PADDING,
            top + LEGEND_GAP);
    return card.build(CARD_WIDTH, CARD_HEIGHT);
  }

  /**
   * Header (villager, name, workstation, its recipe), then one row per level: its badge and its
   * trades in the fixed slots.
   */
  private static SheetElement professionCard(
      SheetContext context,
      MerchantTrades trades,
      Holder<VillagerProfession> professionHolder,
      PictureAtlas atlas) {
    Font font = context.font();
    VillagerProfession profession = professionHolder.value();
    CardTint tint = CardTint.GREEN;
    FixedCard.Builder card = new FixedCard.Builder(tint);
    ItemStack workstation = workstation(profession);
    Optional<WorkstationRecipe> recipe =
        workstation.isEmpty()
            ? Optional.empty()
            : WorkstationRecipeTable.of(
                BuiltInRegistries.ITEM.getKey(workstation.getItem()).getPath());
    List<Component> about = new ArrayList<>();
    if (!workstation.isEmpty()) {
      about.add(
          text(context, "workstation")
              .copy()
              .append(NAME_SEPARATOR)
              .append(workstation.getHoverName()));
    }
    card.add(
        atlas.add(
            Scene.of(Casts.villager(professionHolder)),
            VILLAGER_WIDTH,
            VILLAGER_HEIGHT,
            Tooltips.of(font, profession.name(), about)),
        PADDING,
        PADDING + (HEADER_HEIGHT - VILLAGER_HEIGHT) / 2);
    int textX = PADDING + VILLAGER_WIDTH + PORTRAIT_GAP;
    int recipeLeft = CARD_WIDTH - PADDING - RECIPE_SIZE;
    int textRoom = (recipe.isPresent() ? recipeLeft - PORTRAIT_GAP : CARD_WIDTH - PADDING) - textX;
    card.add(nameLabel(font, profession.name(), tint.title(), textRoom), textX, NAME_TOP);
    if (!workstation.isEmpty()) {
      Component label = text(context, "workstation");
      card.add(context.item(workstation, 1, label), textX, SECOND_LINE_TOP);
      int nameX = textX + ITEM + BADGE_GAP;
      card.add(
          new TextLabel(
              font,
              FittedText.fitted(font, workstation.getHoverName(), textRoom - ITEM - BADGE_GAP),
              SheetPalette.MUTED_TEXT,
              1,
              Tooltips.ofItem(font, workstation, List.of(label))),
          nameX,
          SECOND_LINE_TOP + (ITEM - font.lineHeight) / 2 + 1);
      recipe.ifPresent(
          found ->
              addRecipe(context, card, found, workstation.getHoverName(), recipeLeft, PADDING));
    }
    card.add(
        new FixedCard.Fill(CARD_WIDTH - PADDING * 2, 1, SheetPalette.GRID_LINE),
        PADDING,
        PADDING + HEADER_HEIGHT + DIVIDER_GAP);
    int top = BODY_TOP;
    for (int level = 1; level <= LEVELS; level++) {
      card.row();
      if (level <= trades.tiers().size()) {
        addLevel(context, card, trades.tiers().get(level - 1), level, top);
      }
      top += SLOT_HEIGHT + ROW_GAP;
    }
    return card.build(CARD_WIDTH, CARD_HEIGHT);
  }

  /**
   * A zoomed profession: the villager, its workstation (name under it) and the workstation's recipe
   * big in a column on the left; on the right the profession's name and, per level, its badge and
   * every trade in rows of {@link #ZOOM_SLOT_COLUMNS} slots.
   */
  private static ZoomCard professionZoom(
      SheetContext context,
      MerchantTrades trades,
      Holder<VillagerProfession> professionHolder,
      PictureAtlas atlas) {
    Font font = context.font();
    VillagerProfession profession = professionHolder.value();
    CardTint tint = CardTint.GREEN;
    FixedCard.Builder card = new FixedCard.Builder(tint);
    ItemStack workstation = workstation(profession);
    Component label = text(context, "workstation");
    List<Component> about = new ArrayList<>();
    if (!workstation.isEmpty()) {
      about.add(label.copy().append(NAME_SEPARATOR).append(workstation.getHoverName()));
    }
    int columnTop = PADDING;
    card.add(
        atlas.addZoomOnly(
            Scene.of(Casts.villager(professionHolder)),
            ZOOM_VILLAGER_WIDTH,
            ZOOM_VILLAGER_HEIGHT,
            Tooltips.of(font, profession.name(), about)),
        PADDING + (ZOOM_COLUMN_WIDTH - ZOOM_VILLAGER_WIDTH) / 2,
        columnTop);
    columnTop += ZOOM_VILLAGER_HEIGHT + ZOOM_COLUMN_GAP;
    if (!workstation.isEmpty()) {
      card.row();
      SheetElement station =
          ZoomViews.captioned(
              font,
              context.item(workstation, SheetContext.BIG, label),
              workstation.getHoverName(),
              ZOOM_COLUMN_WIDTH,
              SheetPalette.MUTED_TEXT,
              1);
      card.add(station, PADDING + (ZOOM_COLUMN_WIDTH - station.minWidth()) / 2, columnTop);
      columnTop += station.height() + ZOOM_COLUMN_GAP;
      Optional<WorkstationRecipe> recipe =
          WorkstationRecipeTable.of(BuiltInRegistries.ITEM.getKey(workstation.getItem()).getPath());
      if (recipe.isPresent()) {
        card.row();
        addZoomRecipe(
            context,
            card,
            recipe.get(),
            workstation.getHoverName(),
            PADDING + (ZOOM_COLUMN_WIDTH - ZOOM_RECIPE_SIZE) / 2,
            columnTop);
        columnTop += ZOOM_RECIPE_SIZE;
      }
    }
    int left = PADDING + ZOOM_COLUMN_WIDTH + ZOOM_SIDE_GAP;
    card.row();
    card.add(
        nameLabel(font, profession.name(), tint.title(), ZOOM_WIDTH - left - PADDING),
        left,
        NAME_TOP);
    int top = NAME_TOP + font.lineHeight + DIVIDER_GAP;
    for (int level = 1; level <= trades.tiers().size(); level++) {
      card.row();
      top = addZoomLevel(context, card, trades.tiers().get(level - 1), level, left, top);
    }
    int height = Math.max(columnTop, top - ROW_GAP) + PADDING;
    return new ElementCard(card.build(ZOOM_WIDTH, height), ZoomSize.MEDIUM);
  }

  /**
   * A zoomed level from {@code top}: its badge as tall as its lines, every trade in lines of {@link
   * #ZOOM_SLOT_COLUMNS} slots; the top of the next level.
   */
  private static int addZoomLevel(
      SheetContext context, FixedCard.Builder card, TradeTier tier, int level, int left, int top) {
    Font font = context.font();
    int slotsLeft = left + BADGE_WIDTH + BADGE_GAP;
    int column = 0;
    int line = 0;
    for (List<TradeRow> group : TradeGroups.group(tier.trades())) {
      IconGrid.Cell slot = tradeSlot(context, group);
      int columns = slotColumns(slot.width());
      if (column + columns > ZOOM_SLOT_COLUMNS) {
        column = 0;
        line++;
      }
      card.add(
          new FixedCard.CellPiece(font, slot),
          slotsLeft + column * (SLOT_WIDTH + SLOT_GAP),
          top + line * (SLOT_HEIGHT + ROW_GAP));
      column += columns;
    }
    int height = (line + 1) * SLOT_HEIGHT + line * ROW_GAP;
    List<IconGrid.Part> parts = new ArrayList<>();
    parts.add(new IconGrid.FillPart(0, 0, BADGE_WIDTH, height, LEVEL_COLORS[level - 1]));
    parts.add(centred(font, Integer.toString(level), 1));
    card.add(
        new FixedCard.CellPiece(
            font,
            new IconGrid.Cell(
                BADGE_WIDTH,
                height,
                parts,
                Tooltips.of(
                    font,
                    Component.translatable(LEVEL_KEY + level),
                    List.of(text(context, "offers", tier.offers()))))),
        left,
        top);
    return top + height + ROW_GAP;
  }

  /** The workstation's 3x3 crafting grid at full item size, for the zoom. */
  private static void addZoomRecipe(
      SheetContext context,
      FixedCard.Builder card,
      WorkstationRecipe recipe,
      Component workstationName,
      int left,
      int top) {
    card.add(
        new FixedCard.Fill(ZOOM_RECIPE_SIZE, ZOOM_RECIPE_SIZE, SheetPalette.CHART_BACKGROUND),
        left,
        top);
    List<Optional<RecipeIngredient>> grid = recipe.grid();
    for (int slot = 0; slot < grid.size(); slot++) {
      int x = left + 1 + (slot % WorkstationRecipe.SIZE) * ZOOM_RECIPE_CELL;
      int y = top + 1 + (slot / WorkstationRecipe.SIZE) * ZOOM_RECIPE_CELL;
      card.add(new FixedCard.Fill(ITEM, ITEM, RECIPE_CELL_FILL), x, y);
      if (grid.get(slot).isEmpty()) {
        continue;
      }
      RecipeIngredient ingredient = grid.get(slot).get();
      ItemStack stack = item(ingredient.itemId());
      List<Component> details = new ArrayList<>();
      ingredient.tagId().ifPresent(tag -> details.add(text(context, "tag." + tag)));
      details.add(text(context, "recipe", workstationName));
      card.add(new ItemIcon(stack, 1, Tooltips.ofItem(context.font(), stack, details)), x, y);
    }
  }

  /**
   * A level row: its badge, then its trades left to right in the fixed slots while they fit; the
   * rest go to the badge's tooltip, and the badge gets a "+".
   */
  private static void addLevel(
      SheetContext context, FixedCard.Builder card, TradeTier tier, int level, int top) {
    Font font = context.font();
    List<Component> hidden = new ArrayList<>();
    int column = 0;
    for (List<TradeRow> group : TradeGroups.group(tier.trades())) {
      IconGrid.Cell slot = tradeSlot(context, group);
      int columns = slotColumns(slot.width());
      if (column + columns > SLOT_COLUMNS) {
        hidden.add(tradeLine(group.getFirst()));
        continue;
      }
      card.add(
          new FixedCard.CellPiece(font, slot), LEVEL_LEFT + column * (SLOT_WIDTH + SLOT_GAP), top);
      column += columns;
    }
    List<Component> details = new ArrayList<>();
    details.add(text(context, "offers", tier.offers()));
    if (!hidden.isEmpty()) {
      details.add(text(context, "more"));
      details.addAll(hidden);
    }
    List<IconGrid.Part> parts = new ArrayList<>();
    int color = LEVEL_COLORS[level - 1];
    parts.add(new IconGrid.FillPart(0, 0, BADGE_WIDTH, SLOT_HEIGHT, color));
    parts.add(centred(font, Integer.toString(level), 1));
    if (!hidden.isEmpty()) {
      parts.add(centred(font, MORE_MARK, SLOT_HEIGHT - font.lineHeight));
    }
    card.add(
        new FixedCard.CellPiece(
            font,
            new IconGrid.Cell(
                BADGE_WIDTH,
                SLOT_HEIGHT,
                parts,
                Tooltips.of(font, Component.translatable(LEVEL_KEY + level), details))),
        PADDING,
        top);
  }

  /** Dark badge text centred across the badge, its top at {@code y}. */
  private static IconGrid.TextPart centred(Font font, String value, int y) {
    FormattedCharSequence text = Component.literal(value).getVisualOrderText();
    return new IconGrid.TextPart(
        text, BADGE_TEXT, (BADGE_WIDTH - font.width(text)) / 2 + 1, y, false);
  }

  /** Width of the one or two slots that {@code content} pixels need. */
  private static int slotWidth(int content) {
    int columns = slotColumns(content);
    return columns * SLOT_WIDTH + (columns - 1) * SLOT_GAP;
  }

  /** Slots a cell this wide takes: one, or two for a second item or a random price. */
  private static int slotColumns(int width) {
    return width <= SLOT_WIDTH ? 1 : 2;
  }

  /** "15 × Coal → Emerald": a trade that has no slot, for the level badge's tooltip. */
  private static Component tradeLine(TradeRow trade) {
    ItemStack gives = given(trade);
    MutableComponent line = cost(trade);
    return line.append(ARROW).append(amount(trade.gives().count(), gives));
  }

  /** What a trade costs: "5–64 × Emerald + Book". */
  private static MutableComponent cost(TradeRow trade) {
    CountRange price = trade.price();
    MutableComponent cost =
        Component.literal(price.isFixed() ? Integer.toString(price.min()) : range(price))
            .append(TIMES)
            .append(stack(trade.wants()).getHoverName());
    trade
        .extraWants()
        .ifPresent(extra -> cost.append(PLUS).append(amount(extra.count(), stack(extra))));
    return cost;
  }

  /** The workstation's 3x3 crafting grid; a tag's item says in its tooltip that any of it works. */
  private static void addRecipe(
      SheetContext context,
      FixedCard.Builder card,
      WorkstationRecipe recipe,
      Component workstationName,
      int left,
      int top) {
    card.add(
        new FixedCard.Fill(RECIPE_SIZE, RECIPE_SIZE, SheetPalette.CHART_BACKGROUND), left, top);
    List<Optional<RecipeIngredient>> grid = recipe.grid();
    for (int slot = 0; slot < grid.size(); slot++) {
      int x = left + 1 + (slot % WorkstationRecipe.SIZE) * RECIPE_CELL;
      int y = top + 1 + (slot / WorkstationRecipe.SIZE) * RECIPE_CELL;
      card.add(new FixedCard.Fill(SmallItemIcon.SIZE, SmallItemIcon.SIZE, RECIPE_CELL_FILL), x, y);
      if (grid.get(slot).isEmpty()) {
        continue;
      }
      RecipeIngredient ingredient = grid.get(slot).get();
      ItemStack stack = item(ingredient.itemId());
      List<Component> details = new ArrayList<>();
      ingredient.tagId().ifPresent(tag -> details.add(text(context, "tag." + tag)));
      details.add(text(context, "recipe", workstationName));
      card.add(new SmallItemIcon(stack, Tooltips.ofItem(context.font(), stack, details)), x, y);
    }
  }

  /** The wandering trader: picture, name and note, then each offer set titled, in rows of slots. */
  private static FixedCard.Builder traderCard(SheetContext context, PictureAtlas atlas) {
    Font font = context.font();
    CardTint tint = CardTint.AQUA;
    FixedCard.Builder card = new FixedCard.Builder(tint);
    Component name = Component.translatable("entity.minecraft.wandering_trader");
    card.add(
        atlas.add(
            Scene.of(Casts.wanderingTrader()),
            TRADER_WIDTH,
            TRADER_HEIGHT,
            Tooltips.of(
                font, name, List.of(Component.translatable("entity.minecraft.trader_llama")))),
        PADDING,
        PADDING + (HEADER_HEIGHT - TRADER_HEIGHT) / 2);
    int textX = PADDING + TRADER_WIDTH + PORTRAIT_GAP;
    card.add(
        nameLabel(font, name, tint.title(), WIDE_CARD_WIDTH - textX - PADDING), textX, NAME_TOP);
    card.add(
        context.paragraph(
            text(context, "trader.note"),
            WIDE_CARD_WIDTH - textX - PADDING,
            SheetPalette.MUTED_TEXT),
        textX,
        NAME_TOP + font.lineHeight + 1);
    card.add(
        new FixedCard.Fill(WIDE_CARD_WIDTH - PADDING * 2, 1, SheetPalette.GRID_LINE),
        PADDING,
        PADDING + HEADER_HEIGHT + DIVIDER_GAP);
    int columns = (WIDE_CARD_WIDTH - PADDING * 2 + SLOT_GAP) / (SLOT_WIDTH + SLOT_GAP);
    int left = (WIDE_CARD_WIDTH - columns * (SLOT_WIDTH + SLOT_GAP) + SLOT_GAP) / 2;
    int top = BODY_TOP;
    List<TradeTier> tiers = VillagerTradeTable.WANDERING_TRADER.tiers();
    for (int index = 0; index < tiers.size(); index++) {
      TradeTier tier = tiers.get(index);
      Component tierName = text(context, "trader." + TRADER_TIER_KEYS.get(index));
      Component offers = text(context, "offers", tier.offers());
      int color = TRADER_TIER_COLORS[index];
      card.row()
          .add(
              new FixedCard.CellPiece(
                  font,
                  new IconGrid.Flow(font)
                      .fill(BADGE_WIDTH, color)
                      .gap(BADGE_GAP)
                      .text(tierName, color)
                      .gap(TITLE_GAP)
                      .text(offers, SheetPalette.MUTED_TEXT)
                      .build(Tooltips.of(font, tierName, List.of(offers)))),
              left,
              top);
      top += ITEM;
      int column = 0;
      card.row();
      for (List<TradeRow> group : TradeGroups.group(tier.trades())) {
        IconGrid.Cell slot = tradeSlot(context, group);
        int taken = slotColumns(slot.width());
        if (column + taken > columns) {
          card.row();
          column = 0;
          top += SLOT_HEIGHT + ROW_GAP;
        }
        card.add(new FixedCard.CellPiece(font, slot), left + column * (SLOT_WIDTH + SLOT_GAP), top);
        column += taken;
      }
      top += SLOT_HEIGHT + BAND_GAP;
    }
    return card;
  }

  /**
   * Piglin bartering: the piglin and what to give it, then one line per item in two columns,
   * likeliest first: icon, name, how many, chance per gold ingot and a bar for it.
   */
  private static FixedCard.Builder barterCard(SheetContext context, PictureAtlas atlas) {
    Font font = context.font();
    CardTint tint = CardTint.GOLD;
    FixedCard.Builder card = new FixedCard.Builder(tint);
    Component title = text(context, "bartering");
    Component pay = text(context, "bartering.pay");
    card.add(
        atlas.add(
            Scene.of(Casts.piglin()),
            PIGLIN_WIDTH,
            PIGLIN_HEIGHT,
            Tooltips.of(font, Component.translatable("entity.minecraft.piglin"), List.of(pay))),
        PADDING,
        PADDING + (HEADER_HEIGHT - PIGLIN_HEIGHT) / 2);
    int textX = PADDING + PIGLIN_WIDTH + PORTRAIT_GAP;
    card.add(
        nameLabel(font, title, tint.title(), WIDE_CARD_WIDTH - textX - PADDING), textX, NAME_TOP);
    card.add(context.item(new ItemStack(PiglinAi.BARTERING_ITEM), 1, pay), textX, SECOND_LINE_TOP);
    int payX = textX + ITEM + BADGE_GAP;
    card.add(
        new TextLabel(
            font,
            FittedText.fitted(font, pay, WIDE_CARD_WIDTH - payX - PADDING),
            SheetPalette.MUTED_TEXT,
            1,
            Tooltips.of(font, title, List.of(pay))),
        payX,
        SECOND_LINE_TOP + (ITEM - font.lineHeight) / 2 + 1);
    card.add(
        new FixedCard.Fill(WIDE_CARD_WIDTH - PADDING * 2, 1, SheetPalette.GRID_LINE),
        PADDING,
        PADDING + HEADER_HEIGHT + DIVIDER_GAP);

    List<BarterRow> rows = new ArrayList<>(BarterTable.ROWS);
    rows.sort(Comparator.comparingInt(BarterRow::weight).reversed());
    List<FormattedCharSequence> counts = new ArrayList<>(rows.size());
    List<FormattedCharSequence> chances = new ArrayList<>(rows.size());
    int countWidth = 0;
    int chanceWidth = 0;
    for (BarterRow row : rows) {
      FormattedCharSequence count = Component.literal(amountText(row.count())).getVisualOrderText();
      FormattedCharSequence chance = Component.literal(percent(row)).getVisualOrderText();
      counts.add(count);
      chances.add(chance);
      countWidth = Math.max(countWidth, font.width(count));
      chanceWidth = Math.max(chanceWidth, font.width(chance));
    }
    int columnWidth = (WIDE_CARD_WIDTH - PADDING * 2 - COLUMN_GAP) / BARTER_COLUMNS;
    int perColumn = Math.ceilDiv(rows.size(), BARTER_COLUMNS);
    double likeliest = BarterTable.chance(rows.getFirst());
    int textOffset = (BARTER_LINE - font.lineHeight) / 2 + 1;
    for (int index = 0; index < rows.size(); index++) {
      int line = index % perColumn;
      int columnLeft = PADDING + (index / perColumn) * (columnWidth + COLUMN_GAP);
      int barLeft = columnLeft + columnWidth - PIECE_GAP - BAR_WIDTH;
      int chanceRight = barLeft - TITLE_GAP;
      int countRight = chanceRight - chanceWidth - COLUMN_GAP;
      int nameX = columnLeft + PIECE_GAP + ITEM + BADGE_GAP;
      int nameRoom = countRight - countWidth - COLUMN_GAP - nameX;
      int top = BODY_TOP + line * BARTER_LINE;
      BarterRow row = rows.get(index);
      ItemStack stack = barterStack(row);
      List<FormattedCharSequence> tooltip = barterTooltip(context, row, stack);
      card.row();
      if (line % 2 == 1) {
        card.add(new FixedCard.Fill(columnWidth, BARTER_LINE, ALTERNATE_LINE), columnLeft, top);
      }
      card.add(new ItemIcon(stack, 1, tooltip), columnLeft + PIECE_GAP, top);
      card.add(
          new TextLabel(
              font,
              FittedText.fitted(font, barterName(row, stack), nameRoom),
              SheetPalette.TEXT,
              1,
              tooltip),
          nameX,
          top + textOffset);
      FormattedCharSequence count = counts.get(index);
      card.add(
          new TextLabel(font, count, SheetPalette.HIGHLIGHT_TEXT, 1, tooltip),
          countRight - font.width(count),
          top + textOffset);
      FormattedCharSequence chance = chances.get(index);
      card.add(
          new TextLabel(font, chance, SheetPalette.MUTED_TEXT, 1, tooltip),
          chanceRight - font.width(chance),
          top + textOffset);
      int barTop = top + (BARTER_LINE - BAR_HEIGHT) / 2;
      card.add(
          new FixedCard.Fill(BAR_WIDTH, BAR_HEIGHT, SheetPalette.CHART_BACKGROUND),
          barLeft,
          barTop);
      int filled = Math.max(1, (int) Math.round(BAR_WIDTH * BarterTable.chance(row) / likeliest));
      card.add(new FixedCard.Fill(filled, BAR_HEIGHT, tint.accent()), barLeft, barTop);
    }
    return card;
  }

  /** A card's name: bold when it fits, else plain, else cut with an ellipsis. */
  private static TextLabel nameLabel(Font font, Component name, int color, int room) {
    FormattedCharSequence bold = name.copy().withStyle(ChatFormatting.BOLD).getVisualOrderText();
    FormattedCharSequence text =
        font.width(bold) <= room ? bold : FittedText.fitted(font, name, room);
    return new TextLabel(font, text, color, 1, null);
  }

  /**
   * One trade (or a group of variants) in a slot: pay (+ second item) ▶ get, inventory-style counts
   * on the icons' corners (a random price in yellow), centred in one slot or, when wider, in two;
   * the details in its tooltip.
   */
  private static IconGrid.Cell tradeSlot(SheetContext context, List<TradeRow> group) {
    Font font = context.font();
    TradeRow trade = group.getFirst();
    CountRange price = trade.price();
    ItemStack wants = stack(trade.wants());
    ItemStack gives = given(trade);
    Optional<TradeItem> extra = trade.extraWants();
    int rest =
        ITEM
            + (extra.isPresent() ? PIECE_GAP + ITEM : 0)
            + ARROW_GAP
            + ARROW_WIDTH
            + ARROW_GAP
            + ITEM;
    List<IconGrid.Part> parts = new ArrayList<>();
    int width;
    int x;
    if (price.isFixed()) {
      // The price as an inventory count on the paid item.
      FormattedCharSequence count = countLine(price.min());
      int overflow = count == null ? 0 : Math.max(0, font.width(count) - COUNT_RIGHT);
      width = slotWidth(overflow + rest);
      x = Math.max(0, (width - overflow - rest) / 2) + overflow;
      parts.add(new IconGrid.FillPart(0, 0, width, SLOT_HEIGHT, SLOT_FILL));
      addItem(parts, font, wants, count, SheetPalette.TEXT, x);
    } else {
      // A random price in yellow: beside the paid item when the two slots have room, else on it.
      FormattedCharSequence range = Component.literal(range(price)).getVisualOrderText();
      int rangeWidth = font.width(range);
      boolean beside = rangeWidth + PIECE_GAP + rest <= slotWidth(SLOT_WIDTH + 1);
      int lead = beside ? rangeWidth + PIECE_GAP : Math.max(0, rangeWidth - COUNT_RIGHT);
      width = slotWidth(lead + rest);
      x = Math.max(0, (width - lead - rest) / 2);
      parts.add(new IconGrid.FillPart(0, 0, width, SLOT_HEIGHT, SLOT_FILL));
      if (beside) {
        parts.add(
            new IconGrid.TextPart(
                range,
                SheetPalette.HIGHLIGHT_TEXT,
                x,
                ICON_TOP + (ITEM - font.lineHeight) / 2 + 1,
                true));
        addItem(parts, font, wants, null, SheetPalette.TEXT, x + lead);
      } else {
        addItem(parts, font, wants, range, SheetPalette.HIGHLIGHT_TEXT, x + lead);
      }
      x += lead;
    }
    x += ITEM;
    ItemStack extraStack = ItemStack.EMPTY;
    if (extra.isPresent()) {
      extraStack = stack(extra.get());
      x += PIECE_GAP;
      addItem(parts, font, extraStack, countLine(extra.get().count()), SheetPalette.TEXT, x);
      x += ITEM;
    }
    x += ARROW_GAP;
    int middle = ICON_TOP + ITEM / 2;
    for (int column = 0; column < ARROW_WIDTH; column++) {
      int height = ARROW_HEIGHTS[column];
      parts.add(
          new IconGrid.FillPart(
              x + column, middle - height / 2, 1, height, SheetPalette.MUTED_TEXT));
    }
    x += ARROW_WIDTH + ARROW_GAP;
    addItem(parts, font, gives, countLine(trade.gives().count()), SheetPalette.TEXT, x);

    MutableComponent title = amount(trade.gives().count(), gives);
    List<Component> details = new ArrayList<>();
    details.add(text(context, "cost", cost(trade)));
    kindNote(context, trade).ifPresent(details::add);
    if (group.size() > 1) {
      if (trade.villagerTypes().isEmpty()) {
        details.add(text(context, "variants", group.size()));
      } else {
        for (TradeRow member : group) {
          details.add(
              given(member)
                  .getHoverName()
                  .copy()
                  .append(NAME_SEPARATOR)
                  .append(biomes(member.villagerTypes())));
        }
      }
    } else if (!trade.villagerTypes().isEmpty()) {
      details.add(text(context, "only_from", biomes(trade.villagerTypes())));
    }
    return new IconGrid.Cell(width, SLOT_HEIGHT, parts, Tooltips.of(font, title, details));
  }

  /** An item at {@code x} on the icon line, {@code count} on its corner when not null. */
  private static void addItem(
      List<IconGrid.Part> parts,
      Font font,
      ItemStack stack,
      @Nullable FormattedCharSequence count,
      int color,
      int x) {
    parts.add(new IconGrid.ItemPart(stack, 1, x, ICON_TOP));
    if (count != null) {
      parts.add(
          new IconGrid.TextPart(
              count, color, x + COUNT_RIGHT - font.width(count), ICON_TOP + COUNT_TOP, true));
    }
  }

  /** Inventory-style count: nothing for one. */
  private static @Nullable FormattedCharSequence countLine(int count) {
    return count == 1 ? null : Component.literal(Integer.toString(count)).getVisualOrderText();
  }

  /** The item's name, with its enchantment when it carries one. */
  private static Component barterName(BarterRow row, ItemStack stack) {
    MutableComponent name = stack.getHoverName().copy();
    row.enchantment()
        .ifPresent(
            enchantment ->
                name.append(OPEN)
                    .append(Component.translatable(ENCHANTMENT_KEY + enchantment))
                    .append(CLOSE));
    return name;
  }

  private static List<FormattedCharSequence> barterTooltip(
      SheetContext context, BarterRow row, ItemStack stack) {
    List<Component> details = new ArrayList<>();
    if (row.enchantment().isPresent()) {
      details.add(Component.translatable(ENCHANTMENT_KEY + row.enchantment().get()));
    }
    details.add(text(context, "bartering.chance", percent(row)));
    if (!row.count().isFixed()) {
      details.add(text(context, "bartering.count", range(row.count())));
    }
    return Tooltips.ofItem(context.font(), stack, details);
  }

  private static String percent(BarterRow row) {
    return String.format(Locale.ROOT, PERCENT, BarterTable.chance(row) * PERCENT_SCALE);
  }

  /** "3" or "3–9". */
  private static String amountText(CountRange count) {
    return count.isFixed() ? Integer.toString(count.min()) : range(count);
  }

  private static Optional<Component> kindNote(SheetContext context, TradeRow trade) {
    return switch (trade.kind()) {
      case PLAIN, POTION -> Optional.empty();
      case ENCHANTED_BOOK -> {
        CountRange treasure = TradePrices.treasureBookPrice();
        yield Optional.of(text(context, "kind.enchanted_book", range(treasure)));
      }
      default -> Optional.of(text(context, "kind." + trade.kind().name().toLowerCase(Locale.ROOT)));
    };
  }

  private static ItemStack barterStack(BarterRow row) {
    // An enchanted book comes out of a plain book entry.
    ItemStack stack =
        row.enchantment().isPresent() && row.itemId().equals(BOOK)
            ? new ItemStack(Items.ENCHANTED_BOOK)
            : item(row.itemId());
    row.potion().ifPresent(potion -> withPotion(stack, potion));
    if (row.enchantment().isPresent()) {
      stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }
    return stack;
  }

  /** The block of the job site a profession works at, as an item; empty if none. */
  private static ItemStack workstation(VillagerProfession profession) {
    for (Holder<PoiType> poi : BuiltInRegistries.POINT_OF_INTEREST_TYPE.listElements().toList()) {
      if (profession.heldJobSite().test(poi) && !poi.value().matchingStates().isEmpty()) {
        BlockState state = poi.value().matchingStates().iterator().next();
        return new ItemStack(state.getBlock());
      }
    }
    return ItemStack.EMPTY;
  }

  private static ItemStack stack(TradeItem item) {
    ItemStack stack = item(item.itemId());
    item.potion().ifPresent(potion -> withPotion(stack, potion));
    return stack;
  }

  /** The given item as the player gets it: its potion, an enchantment glint. */
  private static ItemStack given(TradeRow trade) {
    ItemStack stack = stack(trade.gives());
    if (trade.kind() == TradeKind.ENCHANTED_ITEM || trade.kind() == TradeKind.ENCHANTED_BOOK) {
      stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }
    return stack;
  }

  private static ItemStack item(String itemId) {
    return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(MINECRAFT + itemId)));
  }

  private static void withPotion(ItemStack stack, String potionId) {
    BuiltInRegistries.POTION
        .get(Identifier.withDefaultNamespace(potionId))
        .ifPresent(potion -> stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion)));
  }

  /** "4 × Glass", or just the name for one. */
  private static MutableComponent amount(int count, ItemStack stack) {
    MutableComponent name = stack.getHoverName().copy();
    return count == 1 ? name : Component.literal(count + TIMES).append(name);
  }

  private static String range(CountRange range) {
    return range.min() + RANGE + range.max();
  }

  private static Component biomes(List<String> villagerTypes) {
    MutableComponent list = Component.empty();
    for (int index = 0; index < villagerTypes.size(); index++) {
      if (index > 0) {
        list.append(LIST_SEPARATOR);
      }
      String type = villagerTypes.get(index);
      String biome = TYPE_BIOMES.get(type);
      list.append(
          biome == null ? Component.literal(type) : Component.translatable(BIOME_KEY + biome));
    }
    return list;
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args);
  }
}
