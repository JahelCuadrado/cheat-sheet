package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.ElementCard;
import com.einsteinmods.cheatsheet.client.sheet.ElementRow;
import com.einsteinmods.cheatsheet.client.sheet.ItemIcon;
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
import com.einsteinmods.cheatsheet.core.enchantments.AnvilRules;
import com.einsteinmods.cheatsheet.core.enchantments.EnchantmentFacts;
import com.einsteinmods.cheatsheet.core.enchantments.EnchantmentGrouping;
import com.einsteinmods.cheatsheet.core.enchantments.Gear;
import com.einsteinmods.cheatsheet.core.enchantments.GearCard;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/**
 * Enchantments tab: one card per kind of gear (the title says which name colour means what), all
 * the same size, seven to a row, so every card fits one 960x540 screen in any language: the gear's
 * icons, its name, and every enchantment it takes (name, highest level; a long one wraps), from the
 * registry the server synced, so datapack and modded enchantments show up too. Shared enchantments
 * sit on one card (any gear, every armor piece, first) instead of repeating; treasure names are
 * aqua, curses red. Tooltips: what it does, incompatibilities, where to get it, its anvil cost.
 *
 * <p>Below, a guide to putting several books on one tool with the anvil ({@code
 * core.enchantments.AnvilRules}): the hidden counter that makes each use dearer, the trick (join
 * things in pairs, like a tournament's knockout rounds), why never one by one and what each step
 * costs; beside it, numbered steps with icons for four books and a pickaxe (books in pairs, then
 * the tool on the left and the last book on the right). Details (each step's cost, the 8-book plan)
 * in tooltips.
 */
public final class EnchantmentsSheet implements SheetSource {
  public static final String ID = "enchantments";

  /** Every gear card: padding, gaps between its parts, the divider, and one slot per row. */
  private static final int TILE_PADDING = 4;

  /** Seven cards fill a row of a 960-wide sheet; text wraps to it in every language. */
  private static final int TILE_WIDTH = 132;

  private static final int INNER_WIDTH = TILE_WIDTH - TILE_PADDING * 2;

  private static final int HEADER_GAP = 3;
  private static final int DIVIDER = 1;

  /** An enchantment's lines, with room between enchantments; a wrapped line sits indented. */
  private static final int ROW_PITCH = 12;

  private static final int WRAPPED_PITCH = 9;
  private static final int WRAPPED_INDENT = 6;

  /** Gear icons on top of each card, big. */
  private static final int HEADER_ICON_SCALE = SheetContext.BIG;

  /**
   * Big item sprites have 4 px of transparent edge: overlapping them that much keeps the four
   * armour pieces as narrow as the longest enchantment row, so seven cards still fit a row.
   */
  private static final int ICON_GAP = -4;

  private static final int TILE_GAP = 4;

  /** The anvil guide's two sections, side by side, span a row of gear cards. */
  private static final int ANVIL_WIDTH = 7 * TILE_WIDTH + 6 * TILE_GAP;

  /** Space between two sections side by side ({@code Section}'s pair gap). */
  private static final int SECTION_PAIR_GAP = 8;

  private static final int ANVIL_PADDING = 5;
  private static final int ANVIL_PARAGRAPH_GAP = 3;
  private static final int TIP_INDENT = 8;
  private static final CardTint ANVIL_TINT = CardTint.STONE;

  /** The guide's example: four books joined in pairs, then onto a pickaxe. */
  private static final int GUIDE_BOOKS = 4;

  /** The bigger plan in the tooltip. */
  private static final int MORE_BOOKS = 8;

  /** One step tile; the steps go in two columns beside the explanation. */
  private static final int STEP_TILE_WIDTH = 180;

  private static final int STEP_COLUMNS = 2;

  /** The explanation takes the rest of the row beside the steps. */
  private static final int EXPLANATION_WIDTH =
      ANVIL_WIDTH - STEP_COLUMNS * (STEP_TILE_WIDTH + TILE_GAP) + TILE_GAP - SECTION_PAIR_GAP;

  private static final int STEP_BADGE_GAP = 3;
  private static final int STEP_CAPTION_GAP = 3;
  private static final int STEP_BADGE_COLOR = SheetPalette.HIGHLIGHT_TEXT;

  /** Extra costs listed after the first in the counter line: 1, 3, 7, 15, 31. */
  private static final int PENALTIES_SHOWN = 5;

  private static final String TOO_EXPENSIVE_KEY = "container.repair.expensive";
  private static final String BULLET = "•";
  private static final String ARROW = "→";
  private static final String PLUS = "+";
  private static final String TAB_KEY = "cheatsheet.tab." + ID;
  private static final int TREASURE_COLOR = 0xFF55FFFF;
  private static final int CURSE_COLOR = 0xFFFF5555;
  private static final int NOT_IN_TAG = Integer.MAX_VALUE;
  private static final String DESCRIPTION_KEY_PREFIX = TAB_KEY + ".desc.";
  private static final String LEVEL_KEY_PREFIX = "enchantment.level.";
  private static final String LIST_SEPARATOR = ", ";
  private static final String NAMESPACE_SEPARATOR = ".";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.ENCHANTED_BOOK);
  }

  @Override
  public Sheet build(SheetContext context) {
    ClientLevel level = context.minecraft().level;
    if (level == null) {
      return Sheet.of();
    }
    Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    Map<Gear, ItemStack> gearItems = new EnumMap<>(Gear.class);
    for (Gear gear : Gear.values()) {
      ItemStack stack = context.stack(Identifier.DEFAULT_NAMESPACE + ":" + gear.itemId());
      if (!stack.isEmpty()) {
        gearItems.put(gear, stack);
      }
    }
    Map<Holder<Enchantment>, Integer> tooltipOrder = new HashMap<>();
    for (Holder<Enchantment> holder : registry.getTagOrEmpty(EnchantmentTags.TOOLTIP_ORDER)) {
      tooltipOrder.putIfAbsent(holder, tooltipOrder.size());
    }
    Map<String, Holder.Reference<Enchantment>> holders = new HashMap<>();
    List<EnchantmentFacts> facts = new ArrayList<>();
    for (Holder.Reference<Enchantment> holder : registry.listElements().toList()) {
      Set<Gear> supported = EnumSet.noneOf(Gear.class);
      for (Map.Entry<Gear, ItemStack> gear : gearItems.entrySet()) {
        if (holder.value().isSupportedItem(gear.getValue())) {
          supported.add(gear.getKey());
        }
      }
      String id = holder.key().identifier().toString();
      holders.put(id, holder);
      facts.add(
          new EnchantmentFacts(
              id,
              supported,
              holder.is(EnchantmentTags.CURSE),
              !holder.is(EnchantmentTags.IN_ENCHANTING_TABLE),
              tooltipOrder.getOrDefault(holder, NOT_IN_TAG)));
    }

    List<GearTile> gearTiles = new ArrayList<>();
    for (Map.Entry<GearCard, List<EnchantmentFacts>> entry :
        EnchantmentGrouping.group(facts, gearItems.keySet()).entrySet()) {
      GearCard gearCard = entry.getKey();
      List<List<TextLabel>> rows = new ArrayList<>();
      for (EnchantmentFacts enchantment : entry.getValue()) {
        rows.add(row(context, holders.get(enchantment.id()), gearCard, gearItems));
      }
      List<SheetElement> icons = new ArrayList<>();
      for (ItemStack stack : headerItems(gearCard, gearItems)) {
        icons.add(new ItemIcon(stack, HEADER_ICON_SCALE, List.of()));
      }
      gearTiles.add(
          new GearTile(
              new ElementRow(icons, ICON_GAP),
              text(context, "gear." + gearCard.id()).withStyle(ChatFormatting.BOLD),
              tint(gearCard),
              rows));
    }
    if (gearTiles.isEmpty()) {
      return Sheet.of();
    }
    List<Section> anvil = anvilSections(context);
    return Sheet.ofSections(
        List.of(
            Section.of(title(context), CardTint.PURPLE, gearGrid(context, gearTiles)),
            anvil.getFirst(),
            anvil.getLast()),
        null);
  }

  /**
   * The guide, in two sections side by side (stacked when the sheet is narrower): how the anvil's
   * hidden counter makes each use dearer, the trick (join things in pairs), why never one by one
   * and what each step costs (the 8-book plan and the refusal in tooltips); then the example,
   * putting four books on a pickaxe, a tile per step in two columns (a numbered badge, left + right
   * → result as icons, one line saying it; what the step costs in its tooltip). Both end at one
   * height.
   */
  private static List<Section> anvilSections(SheetContext context) {
    Font font = context.font();
    ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
    ItemStack gear = new ItemStack(Items.DIAMOND_PICKAXE);
    ItemStack done = gear.copy();
    done.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

    List<FixedCard.Builder> steps = new ArrayList<>();
    List<AnvilRules.Step> plan = AnvilRules.pyramid(GUIDE_BOOKS);
    for (int index = 0; index < plan.size(); index++) {
      AnvilRules.Step step = plan.get(index);
      boolean last = index == plan.size() - 1;
      Component detail =
          last
              ? text(
                  context,
                  "anvil.final",
                  step.rightPenalty(),
                  step.penaltyCost(),
                  step.resultPenalty())
              : text(
                  context,
                  "anvil.merge",
                  step.leftPenalty(),
                  step.penaltyCost(),
                  step.resultPenalty());
      for (int merge = 0; merge < step.merges(); merge++) {
        int number = steps.size() + 1;
        List<FormattedCharSequence> tooltip =
            Tooltips.of(font, text(context, "anvil.step_title", number), List.of(detail));
        IconGrid.Cell formula =
            new IconGrid.Flow(font)
                .badge(STEP_BADGE_COLOR, Component.literal(Integer.toString(number)))
                .gap(STEP_BADGE_GAP)
                .item(last ? gear : book, null)
                .text(Component.literal(" " + PLUS + " "), SheetPalette.MUTED_TEXT)
                .item(book, null)
                .text(Component.literal(" " + ARROW + " "), SheetPalette.MUTED_TEXT)
                .item(last ? done : book, null)
                .build(tooltip);
        FixedCard.Builder tile = new FixedCard.Builder(ANVIL_TINT);
        tile.row().add(new FixedCard.CellPiece(font, formula), ANVIL_PADDING, ANVIL_PADDING);
        paragraph(
            context,
            tile,
            text(context, "anvil.step." + number),
            STEP_TILE_WIDTH - ANVIL_PADDING * 2,
            ANVIL_PADDING,
            ANVIL_PADDING + ItemIcon.ITEM_SIZE + STEP_CAPTION_GAP,
            SheetPalette.TEXT,
            tooltip);
        steps.add(tile);
      }
    }

    // How: the hidden counter, the trick (pairs), never one by one, what each step costs.
    int lineWidth = EXPLANATION_WIDTH - ANVIL_PADDING * 2 - TIP_INDENT;
    int firstTooExpensive = AnvilRules.firstTooExpensiveBookOneByOne();
    Component tooExpensive = Component.translatable(TOO_EXPENSIVE_KEY).withColor(CURSE_COLOR);
    Component moreBooks =
        text(
            context,
            "anvil.more_books",
            MORE_BOOKS,
            AnvilRules.penaltyCost(AnvilRules.pyramid(MORE_BOOKS)),
            AnvilRules.oneByOnePenaltyCost(MORE_BOOKS));
    Component refused = text(context, "anvil.cost.detail", AnvilRules.TOO_EXPENSIVE, tooExpensive);
    List<Component> lines =
        List.of(
            text(
                context,
                "anvil.counter",
                AnvilRules.penaltyAfterUses(0),
                penaltySequence(),
                AnvilRules.TOO_EXPENSIVE,
                tooExpensive),
            text(context, "anvil.trick"),
            text(
                context,
                "anvil.one_by_one",
                // The book before can already be: its counter alone is most of the limit.
                firstTooExpensive - 1,
                firstTooExpensive,
                tooExpensive),
            text(context, "anvil.cost"));
    List<Component> details = List.of(refused, moreBooks, moreBooks, refused);
    FixedCard.Builder explanation = new FixedCard.Builder(ANVIL_TINT);
    int y = ANVIL_PADDING;
    for (int index = 0; index < lines.size(); index++) {
      List<FormattedCharSequence> tooltip =
          Tooltips.of(font, lines.get(index), List.of(details.get(index)));
      explanation
          .row()
          .add(context.label(Component.literal(BULLET), SheetPalette.MUTED_TEXT), ANVIL_PADDING, y);
      y =
          paragraph(
                  context,
                  explanation,
                  lines.get(index),
                  lineWidth,
                  ANVIL_PADDING + TIP_INDENT,
                  y,
                  SheetPalette.TEXT,
                  tooltip)
              + ANVIL_PARAGRAPH_GAP;
    }

    // The steps go in rows of STEP_COLUMNS beside the explanation, both ending at one height.
    int stepRows = Math.ceilDiv(steps.size(), STEP_COLUMNS);
    int explanationHeight = explanation.contentBottom() - ANVIL_PARAGRAPH_GAP + ANVIL_PADDING;
    int stepHeight = Math.ceilDiv(explanationHeight - (stepRows - 1) * TILE_GAP, stepRows);
    for (FixedCard.Builder step : steps) {
      stepHeight = Math.max(stepHeight, step.contentBottom() + ANVIL_PADDING);
    }
    explanationHeight = stepRows * stepHeight + (stepRows - 1) * TILE_GAP;
    List<SheetElement> stepTiles = new ArrayList<>(steps.size());
    for (FixedCard.Builder step : steps) {
      stepTiles.add(step.build(STEP_TILE_WIDTH, stepHeight));
    }
    // The explanation is the guide's text: its zoom is large, so it reads bigger.
    SheetElement explanationTile = explanation.build(EXPLANATION_WIDTH, explanationHeight);
    ZoomCard explanationZoom = new ElementCard(explanationTile, ZoomSize.LARGE);
    return List.of(
        Section.of(
            text(context, "anvil.title"),
            ANVIL_TINT,
            new TileGrid(
                List.of(ZoomViews.zoomedAs(explanationTile, () -> explanationZoom)), TILE_GAP)),
        Section.of(
                text(context, "anvil.example", GUIDE_BOOKS),
                ANVIL_TINT,
                new TileGrid(stepTiles, TILE_GAP))
            .besidePrevious());
  }

  /** "1, 3, 7, 15, 31": the extra cost of each anvil use after the first (which costs 0). */
  private static String penaltySequence() {
    StringBuilder sequence = new StringBuilder();
    for (int uses = 1; uses <= PENALTIES_SHOWN; uses++) {
      if (uses > 1) {
        sequence.append(LIST_SEPARATOR);
      }
      sequence.append(AnvilRules.penaltyAfterUses(uses));
    }
    return sequence.toString();
  }

  /**
   * {@code text} wrapped at {@code width} from ({@code x}, {@code y}), each line with {@code
   * tooltip}; returns its bottom.
   */
  private static int paragraph(
      SheetContext context,
      FixedCard.Builder card,
      Component text,
      int width,
      int x,
      int y,
      int color,
      List<FormattedCharSequence> tooltip) {
    Font font = context.font();
    card.row();
    int top = y;
    for (TextLabel line : FittedText.lines(font, text, width, color, tooltip)) {
      card.add(line, x, top);
      top += font.lineHeight;
    }
    return top;
  }

  /** A gear card before layout: its icons, bold name, tint and one row per enchantment. */
  private record GearTile(
      SheetElement icons, Component title, CardTint tint, List<List<TextLabel>> rows) {}

  /**
   * Every gear card the same size, set by the largest: the icons on top, the name below (as many
   * lines as the longest name takes), a divider, then as many enchantment slots as the fullest card
   * has; slots past a card's last enchantment stay blank.
   */
  private static TileGrid gearGrid(SheetContext context, List<GearTile> gearTiles) {
    int inner = INNER_WIDTH;
    for (GearTile tile : gearTiles) {
      inner = Math.max(inner, tile.icons().minWidth());
    }
    List<List<TextLabel>> titles = new ArrayList<>(gearTiles.size());
    int titleLines = 1;
    int rowsHeight = 0;
    for (GearTile tile : gearTiles) {
      List<TextLabel> lines =
          FittedText.lines(context.font(), tile.title(), inner, tile.tint().title(), null);
      titles.add(lines);
      titleLines = Math.max(titleLines, lines.size());
      rowsHeight = Math.max(rowsHeight, rowsHeight(tile));
    }
    int lineHeight = context.font().lineHeight;
    int iconHeight = ItemIcon.ITEM_SIZE * HEADER_ICON_SCALE;
    int titleTop = TILE_PADDING + iconHeight + HEADER_GAP;
    int dividerTop = titleTop + titleLines * lineHeight + HEADER_GAP;
    int rowsTop = dividerTop + DIVIDER + HEADER_GAP;
    int width = inner + TILE_PADDING * 2;
    int height = rowsTop + rowsHeight + TILE_PADDING;
    List<SheetElement> tiles = new ArrayList<>(gearTiles.size());
    for (int index = 0; index < gearTiles.size(); index++) {
      GearTile tile = gearTiles.get(index);
      List<SpotTile.Slot> placed = new ArrayList<>();
      placed.add(
          new SpotTile.Slot(
              tile.icons(), TILE_PADDING + (inner - tile.icons().minWidth()) / 2, TILE_PADDING));
      List<TextLabel> lines = titles.get(index);
      for (int line = 0; line < lines.size(); line++) {
        TextLabel label = lines.get(line);
        placed.add(
            new SpotTile.Slot(
                label,
                TILE_PADDING + (inner - label.minWidth()) / 2,
                titleTop + line * lineHeight));
      }
      placed.add(
          new SpotTile.Slot(
              new SpotTile.Fill(inner, DIVIDER, tile.tint().border()), TILE_PADDING, dividerTop));
      int top = rowsTop + (ROW_PITCH - lineHeight) / 2;
      for (List<TextLabel> row : tile.rows()) {
        for (int line = 0; line < row.size(); line++) {
          placed.add(
              new SpotTile.Slot(
                  row.get(line),
                  TILE_PADDING + (line == 0 ? 0 : WRAPPED_INDENT),
                  top + line * WRAPPED_PITCH));
        }
        top += ROW_PITCH + (row.size() - 1) * WRAPPED_PITCH;
      }
      tiles.add(new SpotTile(width, height, tile.tint(), placed, null));
    }
    return new TileGrid(tiles, TILE_GAP, true);
  }

  /** Height of a card's enchantment lines. */
  private static int rowsHeight(GearTile tile) {
    int height = 0;
    for (List<TextLabel> row : tile.rows()) {
      height += ROW_PITCH + (row.size() - 1) * WRAPPED_PITCH;
    }
    return height;
  }

  /** Name and highest level, wrapped to the card; the tooltip explains it. */
  private static List<TextLabel> row(
      SheetContext context,
      Holder.Reference<Enchantment> holder,
      GearCard card,
      Map<Gear, ItemStack> gearItems) {
    Enchantment enchantment = holder.value();
    int maxLevel = enchantment.getMaxLevel();
    boolean curse = holder.is(EnchantmentTags.CURSE);
    boolean inTable = holder.is(EnchantmentTags.IN_ENCHANTING_TABLE);
    int nameColor = curse ? CURSE_COLOR : inTable ? SheetPalette.TEXT : TREASURE_COLOR;
    MutableComponent name = enchantment.description().copy().withColor(nameColor);
    if (maxLevel > 1) {
      name.append(" ")
          .append(
              Component.translatable(LEVEL_KEY_PREFIX + maxLevel)
                  .withColor(SheetPalette.HIGHLIGHT_TEXT));
    }

    List<Component> details = new ArrayList<>();
    String descriptionKey =
        DESCRIPTION_KEY_PREFIX
            + holder.key().identifier().getNamespace()
            + NAMESPACE_SEPARATOR
            + holder.key().identifier().getPath();
    if (Language.getInstance().has(descriptionKey)) {
      details.add(Component.translatable(descriptionKey));
    }
    details.add(text(context, "max_level", Component.translatable(LEVEL_KEY_PREFIX + maxLevel)));
    details.add(
        text(
            context,
            "anvil_cost",
            AnvilRules.feePerLevel(enchantment.getAnvilCost(), true),
            AnvilRules.feePerLevel(enchantment.getAnvilCost(), false)));
    Component incompatible = incompatibleWith(holder);
    if (incompatible != null) {
      details.add(text(context, "incompatible", incompatible));
    }
    if (curse) {
      details.add(text(context, "curse"));
    }
    if (!inTable) {
      details.add(text(context, "treasure"));
    } else {
      Component tableNote = tableNote(context, enchantment, card, gearItems);
      if (tableNote != null) {
        details.add(tableNote);
      }
    }
    if (holder.is(EnchantmentTags.TRADEABLE)) {
      details.add(text(context, "librarians"));
    }

    return FittedText.lines(
        context.font(),
        name,
        INNER_WIDTH - WRAPPED_INDENT,
        SheetPalette.TEXT,
        Tooltips.of(context.font(), name, details));
  }

  /**
   * Where the enchanting table falls short on this card's gear (it only offers an enchantment on
   * its primary items): the anvil with a book is then the way. Null when the table covers it all.
   */
  private static @Nullable Component tableNote(
      SheetContext context,
      Enchantment enchantment,
      GearCard card,
      Map<Gear, ItemStack> gearItems) {
    List<Gear> primary = new ArrayList<>();
    int tested = 0;
    for (Gear gear : card.gears()) {
      ItemStack stack = gearItems.get(gear);
      if (stack == null || !enchantment.isSupportedItem(stack)) {
        continue;
      }
      tested++;
      if (enchantment.isPrimaryItem(stack)) {
        primary.add(gear);
      }
    }
    if (tested == 0 || primary.size() == tested) {
      return null;
    }
    if (primary.isEmpty()) {
      return text(context, "anvil_only");
    }
    MutableComponent names = Component.empty();
    for (int index = 0; index < primary.size(); index++) {
      if (index > 0) {
        names.append(LIST_SEPARATOR);
      }
      names.append(text(context, "gear." + primary.get(index).name().toLowerCase(Locale.ROOT)));
    }
    return text(context, "table_only", names);
  }

  private static @Nullable Component incompatibleWith(Holder<Enchantment> holder) {
    MutableComponent names = Component.empty();
    int count = 0;
    for (Holder<Enchantment> other : holder.value().exclusiveSet()) {
      if (other.equals(holder)) {
        continue;
      }
      if (count > 0) {
        names.append(LIST_SEPARATOR);
      }
      names.append(other.value().description());
      count++;
    }
    return count == 0 ? null : names;
  }

  /** The gear cards' title, with which name colour means what. */
  private static Component title(SheetContext context) {
    return text(
        context,
        "section.title",
        Component.translatable(TAB_KEY),
        text(context, "legend.treasure").withColor(TREASURE_COLOR),
        text(context, "legend.curse").withColor(CURSE_COLOR));
  }

  /**
   * Icons on top of a card: a tool, a bow and a chestplate for any gear, the four pieces for all
   * armour, a book for other items, else the gear itself.
   */
  private static List<ItemStack> headerItems(GearCard card, Map<Gear, ItemStack> gearItems) {
    List<Gear> gears =
        switch (card) {
          case ANY -> List.of(Gear.TOOL, Gear.BOW, Gear.CHESTPLATE);
          case OTHER -> List.of();
          default -> List.copyOf(card.gears());
        };
    List<ItemStack> stacks = new ArrayList<>(gears.size());
    for (Gear gear : gears) {
      ItemStack stack = gearItems.get(gear);
      if (stack != null) {
        stacks.add(stack);
      }
    }
    if (stacks.isEmpty()) {
      stacks.add(new ItemStack(Items.ENCHANTED_BOOK));
    }
    return List.copyOf(stacks);
  }

  private static CardTint tint(GearCard card) {
    return switch (card) {
      case ANY, OTHER -> CardTint.PURPLE;
      case ARMOR, HELMET, CHESTPLATE, LEGGINGS, BOOTS -> CardTint.BLUE;
      case SWORD, SPEAR, AXE, MACE -> CardTint.RED;
      case TOOL -> CardTint.STONE;
      case BOW, CROSSBOW -> CardTint.GREEN;
      case TRIDENT, FISHING_ROD -> CardTint.AQUA;
    };
  }

  private static MutableComponent text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args).copy();
  }
}
