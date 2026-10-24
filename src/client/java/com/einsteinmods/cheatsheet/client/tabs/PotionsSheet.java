package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardTint;
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
import com.einsteinmods.cheatsheet.core.potions.BrewingGuide;
import com.einsteinmods.cheatsheet.core.potions.BrewingTable;
import com.einsteinmods.cheatsheet.core.potions.PotionRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jspecify.annotations.Nullable;

/**
 * Potions tab, a brewing schematic in equal tiles, in rows, so the whole tab fits one 960x540
 * screen: how to start (water + nether wart = awkward potion) and what the modifiers do, then what
 * each ingredient brews from it (helpful and harmful tiles: the potion big, its name beside its
 * effects' status icons, tiny redstone and glowstone markers where a longer or stronger version
 * exists, and what a fermented spider eye corrupts it into). Mixes come from the fixed table in
 * {@code core.potions} (brewing recipes are server data); effects, colours and durations from the
 * potion registry, in vanilla tooltip format.
 */
public final class PotionsSheet implements SheetSource {
  public static final String ID = "potions";

  /** Every tile: padding, gaps, the big result, and the mark slots below the name. */
  private static final int TILE_PADDING = 4;

  /**
   * Eight tiles fill a row of a 960-wide sheet; names wrap and marks are cut to it, so a long
   * translation never widens the tiles into more rows.
   */
  private static final int TILE_WIDTH = 114;

  private static final int INNER_WIDTH = TILE_WIDTH - TILE_PADDING * 2;

  private static final int TILE_GAP = 4;
  private static final int PART_GAP = 3;
  private static final int RECIPE_GAP = 2;
  private static final int BASE_GAP = 1;

  /** The brewed potion big, so it stands out from its ingredients. */
  private static final int RESULT_SCALE = SheetContext.BIG;

  private static final int RESULT_SIZE = ItemIcon.ITEM_SIZE * RESULT_SCALE;
  private static final int MARKS = 3;
  private static final int MARK_PITCH = 11;

  /**
   * A mob effect's status icon (as in the inventory and the HUD) beside the potion's name, at the
   * sprite's own size.
   */
  private static final int EFFECT_SIZE = 18;

  private static final int EFFECT_GAP = 1;
  private static final int NAME_GAP = 2;
  private static final int MARK_GAP = 2;
  private static final int MARK_INDENT = 2;
  private static final String MARK_SEPARATOR = " ";
  private static final String POTENCY_KEY = "potion.potency.";

  /** The potion every modifier tile is shown on. */
  private static final String EXAMPLE = "swiftness";

  private static final float NORMAL_DURATION = 1.0F;

  /** Effects ending within this many ticks show no duration ({@code addPotionTooltip}). */
  private static final int INSTANT_EFFECT_TICKS = 20;

  /** Vanilla's default tick rate, used when no level is loaded. */
  private static final float DEFAULT_TICK_RATE = 20.0F;

  private static final String PLUS = "+";
  private static final String ARROW = "→";
  private static final String DURATION_KEY = "potion.withDuration";
  private static final String EMPTY_POTION_KEY = "effect.none";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return PotionContents.createItemStack(Items.POTION, Potions.HEALING);
  }

  @Override
  public Sheet build(SheetContext context) {
    ClientLevel level = context.minecraft().level;
    Builder builder =
        new Builder(
            context, level == null ? DEFAULT_TICK_RATE : level.tickRateManager().tickrate());
    return builder.build();
  }

  /** Build state: the context and the tick rate durations are shown at. */
  private record Builder(SheetContext context, float tickRate) {
    Sheet build() {
      List<TileContent> start = new ArrayList<>();
      TileContent awkward = recipeTile(BrewingGuide.start(), CardTint.AQUA, true);
      if (awkward != null) {
        start.add(awkward);
      }
      start.add(modifier(BrewingTable.REDSTONE, "redstone", Items.POTION, Items.POTION));
      start.add(modifier(BrewingTable.GLOWSTONE, "glowstone", Items.POTION, Items.POTION));
      start.add(
          modifier(BrewingTable.FERMENTED_SPIDER_EYE, "spider_eye", Items.POTION, Items.POTION));
      start.add(modifier(BrewingTable.GUNPOWDER, "gunpowder", Items.POTION, Items.SPLASH_POTION));
      start.add(
          modifier(
              BrewingTable.DRAGON_BREATH,
              "dragon_breath",
              Items.SPLASH_POTION,
              Items.LINGERING_POTION));
      List<TileContent> helpful = new ArrayList<>();
      List<TileContent> harmful = new ArrayList<>();
      for (PotionRecipe recipe : BrewingGuide.recipes()) {
        Holder<Potion> potion = potion(recipe.potion());
        TileContent tile = potion == null ? null : recipeTile(recipe, null, false);
        if (tile != null) {
          (isHelpful(potion) ? helpful : harmful).add(tile);
        }
      }
      List<TileContent> all = new ArrayList<>(start);
      all.addAll(helpful);
      all.addAll(harmful);
      Layout layout = layout(all);
      List<Section> sections = new ArrayList<>();
      addSection(sections, "start", CardTint.AQUA, start, layout);
      addSection(sections, "helpful", CardTint.GREEN, helpful, layout);
      addSection(sections, "harmful", CardTint.RED, harmful, layout);
      return sections.isEmpty() ? Sheet.of() : Sheet.ofSections(sections, null);
    }

    /**
     * What one tile shows, before every tile gets the same size: a recipe (bases + reagent → big
     * result), a name, up to {@link #MARKS} marks, its tint and its tooltip.
     */
    private record TileContent(
        List<SheetElement> bases,
        SheetElement reagent,
        SheetElement result,
        Component name,
        int nameColor,
        List<Holder<MobEffect>> effects,
        List<@Nullable SheetElement> marks,
        CardTint tint,
        List<FormattedCharSequence> tooltip) {}

    /**
     * Shared by every tile: the recipe frame (bases right-aligned before the plus, so the reagent,
     * arrow and result sit at the same spot in every tile), name lines and mark slots.
     */
    private record Layout(
        int width,
        int height,
        int inner,
        int frameLeft,
        int frameWidth,
        int nameTop,
        int nameHeight,
        int marksTop,
        TextLabel plus,
        TextLabel arrow) {}

    private Layout layout(List<TileContent> tiles) {
      TextLabel plus = context.label(Component.literal(PLUS), SheetPalette.MUTED_TEXT);
      TextLabel arrow = context.label(Component.literal(ARROW), SheetPalette.MUTED_TEXT);
      int bases = 1;
      for (TileContent tile : tiles) {
        bases = Math.max(bases, tile.bases().size());
      }
      int frameWidth =
          bases * ItemIcon.ITEM_SIZE
              + (bases - 1) * BASE_GAP
              + plus.minWidth()
              + arrow.minWidth()
              + ItemIcon.ITEM_SIZE
              + RESULT_SIZE
              + RECIPE_GAP * 4;
      int inner = Math.max(frameWidth, INNER_WIDTH);
      int nameHeight = context.font().lineHeight;
      for (TileContent tile : tiles) {
        int lines = nameLines(tile, inner).size() * context.font().lineHeight;
        nameHeight =
            Math.max(nameHeight, tile.effects().isEmpty() ? lines : Math.max(lines, EFFECT_SIZE));
      }
      int nameTop = TILE_PADDING + RESULT_SIZE + PART_GAP;
      int marksTop = nameTop + nameHeight + PART_GAP;
      int height = marksTop + MARKS * MARK_PITCH + TILE_PADDING;
      return new Layout(
          inner + TILE_PADDING * 2,
          height,
          inner,
          TILE_PADDING + (inner - frameWidth) / 2,
          frameWidth,
          nameTop,
          nameHeight,
          marksTop,
          plus,
          arrow);
    }

    private void addSection(
        List<Section> sections, String key, CardTint tint, List<TileContent> tiles, Layout layout) {
      if (tiles.isEmpty()) {
        return;
      }
      List<SheetElement> placed = new ArrayList<>(tiles.size());
      for (TileContent tile : tiles) {
        placed.add(tile(tile, layout));
      }
      sections.add(Section.of(text(key), tint, new TileGrid(placed, TILE_GAP, true)));
    }

    /** One tile: every part at the spot the layout gives it. */
    private SpotTile tile(TileContent tile, Layout layout) {
      List<SpotTile.Slot> slots = new ArrayList<>();
      int resultX = layout.frameLeft() + layout.frameWidth() - RESULT_SIZE;
      int arrowX = resultX - RECIPE_GAP - layout.arrow().minWidth();
      int reagentX = arrowX - RECIPE_GAP - ItemIcon.ITEM_SIZE;
      int plusX = reagentX - RECIPE_GAP - layout.plus().minWidth();
      int iconY = TILE_PADDING + (RESULT_SIZE - ItemIcon.ITEM_SIZE) / 2;
      int textY = TILE_PADDING + (RESULT_SIZE - context.font().lineHeight) / 2 + 1;
      slots.add(new SpotTile.Slot(tile.result(), resultX, TILE_PADDING));
      slots.add(new SpotTile.Slot(layout.arrow(), arrowX, textY));
      slots.add(new SpotTile.Slot(tile.reagent(), reagentX, iconY));
      if (!tile.bases().isEmpty()) {
        slots.add(new SpotTile.Slot(layout.plus(), plusX, textY));
        int baseX = plusX - RECIPE_GAP;
        for (int index = tile.bases().size() - 1; index >= 0; index--) {
          baseX -= ItemIcon.ITEM_SIZE;
          slots.add(new SpotTile.Slot(tile.bases().get(index), baseX, iconY));
          baseX -= BASE_GAP;
        }
      }
      // The effects' icons, then the name's lines centred on each other; the pair centred.
      List<TextLabel> lines = nameLines(tile, layout.inner());
      int textWidth = 1;
      for (TextLabel label : lines) {
        textWidth = Math.max(textWidth, label.minWidth());
      }
      int iconsWidth = effectsWidth(tile);
      int left =
          TILE_PADDING
              + (layout.inner() - iconsWidth - (iconsWidth > 0 ? NAME_GAP : 0) - textWidth) / 2;
      for (int effect = 0; effect < tile.effects().size(); effect++) {
        slots.add(
            new SpotTile.Slot(
                new SpotTile.Sprite(
                    Hud.getMobEffectSprite(tile.effects().get(effect)),
                    EFFECT_SIZE,
                    EFFECT_SIZE,
                    null),
                left + effect * (EFFECT_SIZE + EFFECT_GAP),
                layout.nameTop() + (layout.nameHeight() - EFFECT_SIZE) / 2));
      }
      int textLeft = left + iconsWidth + (iconsWidth > 0 ? NAME_GAP : 0);
      int textTop =
          layout.nameTop() + (layout.nameHeight() - lines.size() * context.font().lineHeight) / 2;
      for (int line = 0; line < lines.size(); line++) {
        TextLabel label = lines.get(line);
        slots.add(
            new SpotTile.Slot(
                label,
                textLeft + (textWidth - label.minWidth()) / 2,
                textTop + line * context.font().lineHeight));
      }
      for (int mark = 0; mark < tile.marks().size(); mark++) {
        SheetElement element = tile.marks().get(mark);
        if (element != null) {
          slots.add(
              new SpotTile.Slot(
                  element,
                  TILE_PADDING + MARK_INDENT,
                  layout.marksTop() + mark * MARK_PITCH + (MARK_PITCH - element.height()) / 2));
        }
      }
      return new SpotTile(layout.width(), layout.height(), tile.tint(), slots, tile.tooltip());
    }

    /** The name wrapped beside the effects' icons. */
    private List<TextLabel> nameLines(TileContent tile, int inner) {
      int iconsWidth = effectsWidth(tile);
      int wrap = iconsWidth > 0 ? inner - iconsWidth - NAME_GAP : inner;
      return FittedText.lines(
          context.font(),
          FittedText.hyphenated(context.font(), tile.name(), wrap),
          wrap,
          tile.nameColor(),
          null);
    }

    private static int effectsWidth(TileContent tile) {
      int count = tile.effects().size();
      return count == 0 ? 0 : count * EFFECT_SIZE + (count - 1) * EFFECT_GAP;
    }

    /**
     * A potion's tile: its bases + reagent → the potion in its own colour, its name, and marks for
     * a longer, stronger and opposite version where they exist. The start tile marks the fuel
     * instead. Null if the potion is missing from the registry.
     */
    private @Nullable TileContent recipeTile(
        PotionRecipe recipe, @Nullable CardTint tint, boolean start) {
      Holder<Potion> potion = potion(recipe.potion());
      if (potion == null) {
        return null;
      }
      List<SheetElement> bases = new ArrayList<>(recipe.bases().size());
      for (String base : recipe.bases()) {
        Holder<Potion> basePotion = potion(base);
        if (basePotion != null) {
          bases.add(potionIcon(basePotion, Items.POTION, 1));
        }
      }
      ItemIcon reagent =
          recipe.bases().contains(BrewingTable.AWKWARD)
              ? item(recipe.reagent(), text("into_awkward"))
              : item(recipe.reagent());
      List<@Nullable SheetElement> marks = new ArrayList<>(MARKS);
      if (start) {
        marks.add(
            new ElementRow(
                List.of(
                    new SmallItemIcon(
                        stack(BrewingTable.BLAZE_POWDER),
                        Tooltips.ofItem(
                            context.font(),
                            stack(BrewingTable.BLAZE_POWDER),
                            List.of(text("fuel.tooltip")))),
                    new TextLabel(
                        context.font(),
                        FittedText.fitted(
                            context.font(),
                            text("fuel"),
                            INNER_WIDTH - MARK_INDENT - SmallItemIcon.SIZE - MARK_GAP),
                        SheetPalette.MUTED_TEXT,
                        1,
                        null)),
                MARK_GAP));
      } else {
        marks.add(durationMark(recipe.longer(), BrewingTable.REDSTONE, "longer"));
        marks.add(potencyMark(recipe.stronger()));
        marks.add(oppositeMark(recipe.opposite()));
      }
      ItemStack stack = PotionContents.createItemStack(Items.POTION, potion);
      return new TileContent(
          bases,
          reagent,
          potionIcon(potion, Items.POTION, RESULT_SCALE),
          shortName(potion),
          start ? SheetPalette.HIGHLIGHT_TEXT : SheetPalette.TEXT,
          start ? List.of() : effects(potion),
          marks,
          tint == null ? CardTint.of(new PotionContents(potion).getColor()) : tint,
          potionTooltip(stack, potion));
    }

    /**
     * What a modifier does, shown on {@link #EXAMPLE}: the example (in {@code from}) + the modifier
     * → what the brewing table turns it into, or the same potion (in {@code to}) for a modifier
     * that only changes the bottle.
     */
    private TileContent modifier(String itemId, String key, Item from, Item to) {
      Component tooltip = text("modifier." + key + ".tooltip");
      Component name = text("modifier." + key);
      ItemIcon reagent = item(itemId, tooltip);
      Holder<Potion> base = potion(EXAMPLE);
      Holder<Potion> result = potion(BrewingTable.result(EXAMPLE, itemId).orElse(EXAMPLE));
      List<SheetElement> bases = new ArrayList<>(1);
      SheetElement resultIcon = item(itemId, tooltip);
      if (base != null && result != null) {
        bases.add(potionIcon(base, from, 1));
        resultIcon = potionIcon(result, to, RESULT_SCALE);
      }
      return new TileContent(
          bases,
          reagent,
          resultIcon,
          name,
          SheetPalette.HIGHLIGHT_TEXT,
          List.of(),
          List.of(),
          CardTint.GOLD,
          Tooltips.of(context.font(), name, List.of(tooltip)));
    }

    /** Redstone and the longer version's duration; null when there is none. */
    private @Nullable SheetElement durationMark(
        Optional<String> upgraded, String reagent, String key) {
      Holder<Potion> potion = upgraded.map(PotionsSheet.Builder::potion).orElse(null);
      if (potion == null || potion.value().getEffects().isEmpty()) {
        return null;
      }
      MobEffectInstance effect = potion.value().getEffects().getFirst();
      return mark(
          reagent,
          key,
          potion,
          MobEffectUtil.formatDuration(effect, NORMAL_DURATION, tickRate).copy());
    }

    /** Glowstone and the stronger version's level (and duration); null when there is none. */
    private @Nullable SheetElement potencyMark(Optional<String> upgraded) {
      Holder<Potion> potion = upgraded.map(PotionsSheet.Builder::potion).orElse(null);
      if (potion == null || potion.value().getEffects().isEmpty()) {
        return null;
      }
      MobEffectInstance effect = potion.value().getEffects().getFirst();
      MutableComponent label = Component.translatable(POTENCY_KEY + effect.getAmplifier());
      if (!effect.endsWithin(INSTANT_EFFECT_TICKS)) {
        label
            .append(MARK_SEPARATOR)
            .append(MobEffectUtil.formatDuration(effect, NORMAL_DURATION, tickRate));
      }
      return mark(BrewingTable.GLOWSTONE, "stronger", potion, label);
    }

    /** Fermented spider eye → the opposite's name; null when there is none. */
    private @Nullable SheetElement oppositeMark(Optional<String> opposite) {
      Holder<Potion> potion = opposite.map(PotionsSheet.Builder::potion).orElse(null);
      if (potion == null) {
        return null;
      }
      MutableComponent label =
          Component.literal(ARROW + MARK_SEPARATOR).append(shortName(potion).copy());
      return mark(BrewingTable.FERMENTED_SPIDER_EYE, "corrupted", potion, label);
    }

    /** A half-size reagent and a short label; both explain the upgraded potion. */
    private SheetElement mark(
        String reagent, String key, Holder<Potion> upgraded, MutableComponent label) {
      ItemStack reagentStack = stack(reagent);
      List<Component> lines = new ArrayList<>();
      lines.add(text(key).withStyle(ChatFormatting.GRAY));
      effectLines(upgraded, lines);
      List<FormattedCharSequence> tooltip = lines(reagentStack.getStyledHoverName(), lines);
      return new ElementRow(
          List.of(
              new SmallItemIcon(reagentStack, tooltip),
              new TextLabel(
                  context.font(),
                  FittedText.fitted(
                      context.font(),
                      label,
                      INNER_WIDTH - MARK_INDENT - SmallItemIcon.SIZE - MARK_GAP),
                  SheetPalette.MUTED_TEXT,
                  1,
                  tooltip)),
          MARK_GAP);
    }

    private ItemIcon potionIcon(Holder<Potion> potion, Item container, int scale) {
      ItemStack stack = PotionContents.createItemStack(container, potion);
      return new ItemIcon(stack, scale, potionTooltip(stack, potion));
    }

    /** Vanilla's potion tooltip (effects, durations, attribute changes), then its upgrades. */
    private List<FormattedCharSequence> potionTooltip(ItemStack stack, Holder<Potion> potion) {
      List<Component> lines = new ArrayList<>();
      PotionContents.addPotionTooltip(
          potion.value().getEffects(), lines::add, NORMAL_DURATION, tickRate);
      String id = potion.unwrapKey().orElseThrow().identifier().getPath();
      upgradeLines(id, BrewingTable.REDSTONE, "longer", lines);
      upgradeLines(id, BrewingTable.GLOWSTONE, "stronger", lines);
      upgradeLines(id, BrewingTable.FERMENTED_SPIDER_EYE, "corrupted", lines);
      return lines(stack.getStyledHoverName(), lines);
    }

    private void upgradeLines(String potionId, String reagent, String key, List<Component> lines) {
      Optional<String> upgraded = BrewingTable.result(potionId, reagent);
      if (upgraded.isEmpty()) {
        return;
      }
      Holder<Potion> potion = potion(upgraded.get());
      // Water + redstone or glowstone only gives effect-less potions: nothing worth listing.
      if (potion == null || potion.value().getEffects().isEmpty()) {
        return;
      }
      lines.add(Component.empty());
      lines.add(text(key).withStyle(ChatFormatting.GRAY));
      effectLines(potion, lines);
    }

    /** The effect lines of vanilla's potion tooltip, without the attribute block. */
    private void effectLines(Holder<Potion> potion, List<Component> lines) {
      List<MobEffectInstance> effects = potion.value().getEffects();
      if (effects.isEmpty()) {
        lines.add(Component.translatable(EMPTY_POTION_KEY).withStyle(ChatFormatting.GRAY));
      }
      for (MobEffectInstance effect : effects) {
        MutableComponent line =
            PotionContents.getPotionDescription(effect.getEffect(), effect.getAmplifier());
        if (!effect.endsWithin(INSTANT_EFFECT_TICKS)) {
          line =
              Component.translatable(
                  DURATION_KEY,
                  line,
                  MobEffectUtil.formatDuration(effect, NORMAL_DURATION, tickRate));
        }
        lines.add(line.withStyle(effect.getEffect().value().getCategory().getTooltipFormatting()));
      }
    }

    /** Title, then lines kept in their own colours and wrapped. */
    private List<FormattedCharSequence> lines(Component title, List<Component> lines) {
      Font font = context.font();
      List<FormattedCharSequence> tooltip = new ArrayList<>(lines.size() + 1);
      tooltip.add(title.getVisualOrderText());
      for (Component line : lines) {
        tooltip.addAll(font.split(line, Tooltips.WRAP_WIDTH));
      }
      return List.copyOf(tooltip);
    }

    /** The effect's name for single-effect potions (shorter), else the potion's name. */
    private static Component shortName(Holder<Potion> potion) {
      List<MobEffectInstance> effects = potion.value().getEffects();
      if (effects.size() == 1) {
        return effects.getFirst().getEffect().value().getDisplayName();
      }
      return PotionContents.createItemStack(Items.POTION, potion).getHoverName();
    }

    /** The potion's effects, for their status icons. */
    private static List<Holder<MobEffect>> effects(Holder<Potion> potion) {
      List<Holder<MobEffect>> effects = new ArrayList<>();
      for (MobEffectInstance effect : potion.value().getEffects()) {
        effects.add(effect.getEffect());
      }
      return List.copyOf(effects);
    }

    private static boolean isHelpful(Holder<Potion> potion) {
      for (MobEffectInstance effect : potion.value().getEffects()) {
        if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
          return true;
        }
      }
      return false;
    }

    private ItemIcon item(String itemId, Component... details) {
      return context.item(stack(itemId), 1, details);
    }

    private ItemStack stack(String itemId) {
      return context.stack(Identifier.DEFAULT_NAMESPACE + ":" + itemId);
    }

    private MutableComponent text(String key, Object... args) {
      return context.tabText(ID, key, args).copy();
    }

    private static @Nullable Holder<Potion> potion(String potionId) {
      return BuiltInRegistries.POTION
          .get(Identifier.withDefaultNamespace(potionId))
          .<Holder<Potion>>map(reference -> reference)
          .orElse(null);
    }
  }
}
