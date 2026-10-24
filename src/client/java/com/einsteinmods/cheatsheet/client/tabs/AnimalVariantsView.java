package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.picture.Cast;
import com.einsteinmods.cheatsheet.client.picture.Casts;
import com.einsteinmods.cheatsheet.client.picture.Picture;
import com.einsteinmods.cheatsheet.client.picture.PictureAtlas;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariant;
import com.einsteinmods.cheatsheet.core.farming.AnimalVariants;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * The zoomed breeding card's "Variants" column: a small portrait of each look of the card's animals
 * (spawned with that look, baked at the zoom's scale on its first draw), its name and where it
 * comes from in a word or two; the tooltip tells every way with its biomes and chances. Notes under
 * the grid say how babies get their look. Built on the card's first click, with the rest of its
 * zoom view.
 */
final class AnimalVariantsView {
  /**
   * Up to nine looks in rows of three, so that beside the zoom's 220 the panel fits a 1080p screen
   * at 4x; more (cats) in rows of four, the zoom then drawn at 3x.
   */
  private static final int FEW_COLUMNS = 3;

  private static final int MANY_COLUMNS = 4;
  private static final int MAX_FEW_CELLS = 9;
  private static final int CELL_WIDTH = 62;
  private static final int CELL_GAP = 3;
  private static final int ROW_GAP = 3;
  private static final int LINE_GAP = 2;
  private static final int PORTRAIT_WIDTH = 40;
  private static final int PORTRAIT_HEIGHT = 30;

  /** Rows of four make the zoom 3x: their portraits take the room it leaves. */
  private static final int MANY_PORTRAIT_WIDTH = 52;

  private static final int MANY_PORTRAIT_HEIGHT = 40;

  /** Biome names before "+N" in a look's short line: the rest are in its tooltip. */
  private static final int SHORT_BIOMES = 1;

  /** A way's chance shows under its look only when the look is this rare (in percent). */
  private static final double RARE_PERCENT = 10.0;

  /** Chances under one percent read "1 in 1200" when they are one in something. */
  private static final double SMALL_PERCENT = 1.0;

  private static final String BIG_CHANCE_PATTERN = "0.#";
  private static final String SMALL_CHANCE_PATTERN = "0.###";
  private static final String PERCENT_SIGN = "%";
  private static final String PLUS = "+";
  private static final String NAME_SEPARATOR = ".";
  private static final String SPACE = " ";
  private static final String LIST_SEPARATOR = ", ";
  private static final String BIOME_KEY_PREFIX = "biome";

  private AnimalVariantsView() {}

  /**
   * The column for {@code set}, or null when none of its looks exists in the game and it has no
   * notes.
   */
  static @Nullable SheetElement of(
      SheetContext context, AnimalVariants.VariantSet set, PictureAtlas atlas) {
    RegistryAccess registries = registries(context);
    List<AnimalVariant> shown = new ArrayList<>();
    List<Consumer<Entity>> looks = new ArrayList<>();
    if (registries != null) {
      for (AnimalVariant variant : set.variants()) {
        Optional<Consumer<Entity>> look = AnimalLooks.of(variant, registries);
        if (look.isPresent()
            && BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(variant.entityId()))) {
          shown.add(variant);
          looks.add(look.get());
        }
      }
    }
    if (shown.isEmpty() && set.noteKeys().isEmpty()) {
      return null;
    }
    boolean many = shown.size() > MAX_FEW_CELLS;
    int columns = many ? MANY_COLUMNS : FEW_COLUMNS;
    int width = columns * CELL_WIDTH + (columns - 1) * CELL_GAP;
    List<Component> summaries = new ArrayList<>(shown.size());
    for (AnimalVariant variant : shown) {
      summaries.add(summary(context, variant));
    }
    // Looks that all come the same way (random coats) say it once, above the grid.
    boolean shared =
        summaries.size() > 1
            && summaries.stream().map(Component::getString).distinct().count() == 1;
    List<SheetElement> parts = new ArrayList<>();
    parts.add(
        context.label(
            text(context, "zoom.variants").copy().withStyle(ChatFormatting.BOLD),
            CardTint.GOLD.title()));
    if (shared) {
      parts.add(
          context.paragraph(
              originLine(context, shown.getFirst().origins().getFirst()),
              width,
              SheetPalette.TEXT));
    }
    if (!shown.isEmpty()) {
      List<SheetElement> cells = new ArrayList<>(shown.size());
      for (int index = 0; index < shown.size(); index++) {
        AnimalVariant variant = shown.get(index);
        cells.add(
            cell(
                context,
                variant,
                BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(variant.entityId())),
                looks.get(index),
                shared ? null : summaries.get(index),
                many ? MANY_PORTRAIT_WIDTH : PORTRAIT_WIDTH,
                many ? MANY_PORTRAIT_HEIGHT : PORTRAIT_HEIGHT,
                atlas));
      }
      parts.add(new CellGrid(cells, columns, CELL_GAP));
    }
    for (String note : set.noteKeys()) {
      parts.add(
          context.paragraph(text(context, "variant.note." + note), width, SheetPalette.MUTED_TEXT));
    }
    return new ElementColumn(parts, ROW_GAP + LINE_GAP);
  }

  private static @Nullable RegistryAccess registries(SheetContext context) {
    return context.minecraft().level == null ? null : context.minecraft().level.registryAccess();
  }

  /** A look's portrait, name and short origin; every origin in the tooltip. */
  private static SheetElement cell(
      SheetContext context,
      AnimalVariant variant,
      EntityType<?> type,
      Consumer<Entity> look,
      @Nullable Component summary,
      int portraitWidth,
      int portraitHeight,
      PictureAtlas atlas) {
    Font font = context.font();
    Component name = Component.translatable(variant.nameKey());
    List<Component> details = new ArrayList<>();
    for (AnimalVariant.Origin origin : variant.origins()) {
      details.add(originLine(context, origin));
    }
    List<FormattedCharSequence> tooltip = Tooltips.of(font, name, details);
    Picture picture =
        atlas.addZoomOnly(
            FarmingSheet.portraitScene(type, dressed(type, look)),
            portraitWidth,
            portraitHeight,
            tooltip);
    List<SheetElement> lines = new ArrayList<>();
    lines.add(picture);
    lines.add(
        new CentredText(
            font,
            FittedText.hyphenated(font, name.copy().withStyle(ChatFormatting.BOLD), CELL_WIDTH),
            CELL_WIDTH,
            CardTint.GOLD.title()));
    if (summary != null) {
      lines.add(new CentredText(font, summary, CELL_WIDTH, SheetPalette.TEXT));
    }
    return new TooltipCell(new ZoomViews.CentredColumn(lines, LINE_GAP), CELL_WIDTH, tooltip);
  }

  /** The animal alone, facing the viewer, in {@code look}. */
  private static Cast dressed(EntityType<?> type, Consumer<Entity> look) {
    return stage -> {
      Optional<? extends Entity> mob = stage.spawn(type, 0, 0, 0, Casts.FACING_VIEWER);
      if (mob.isEmpty()) {
        return List.of();
      }
      look.accept(mob.get());
      return List.of(mob.get());
    };
  }

  /**
   * Where a look comes from in a word or two, from its first way: its biome group (its first biome
   * when the look is named after the group, a cold cow), or the way; with the chance when it spawns
   * by luck in its biomes or is rare.
   */
  private static Component summary(SheetContext context, AnimalVariant variant) {
    AnimalVariant.Origin origin = variant.origins().getFirst();
    MutableComponent place =
        switch (origin) {
          case AnimalVariant.InBiomes biomes ->
              biomes.labelKey().isPresent()
                      && !variant.nameKey().endsWith(NAME_SEPARATOR + biomes.labelKey().get())
                  ? text(context, "variant.climate_short." + biomes.labelKey().get()).copy()
                  : biomeNames(biomes.biomeIds(), SHORT_BIOMES);
          case AnimalVariant.Told told -> text(context, "variant.short." + told.key()).copy();
        };
    origin
        .chance()
        .filter(
            chance -> origin instanceof AnimalVariant.InBiomes || chance.percent() < RARE_PERCENT)
        .ifPresent(chance -> place.append(SPACE).append(chance(context, chance)));
    return place;
  }

  /** A way a look comes, in full: every biome by name, its chance. */
  private static Component originLine(SheetContext context, AnimalVariant.Origin origin) {
    return switch (origin) {
      case AnimalVariant.InBiomes biomes -> {
        Component names = biomeNames(biomes.biomeIds(), biomes.biomeIds().size());
        Component where =
            biomes.labelKey().isPresent()
                ? text(
                    context,
                    "variant.labelled",
                    text(context, "variant.climate." + biomes.labelKey().get()),
                    names)
                : names;
        if (biomes.way() == AnimalVariant.BiomeWay.GROWS) {
          yield text(context, "variant.grows", where);
        }
        yield biomes.chance().isPresent()
            ? text(context, "variant.spawns_chance", chance(context, biomes.chance().get()), where)
            : text(context, "variant.spawns", where);
      }
      case AnimalVariant.Told told -> {
        Component how = text(context, "variant.how." + told.key());
        yield told.chance().isPresent()
            ? text(context, "variant.told_chance", how, chance(context, told.chance().get()))
            : how;
      }
    };
  }

  /** The first {@code shown} biome names, "+N" for the rest. */
  private static MutableComponent biomeNames(List<String> biomeIds, int shown) {
    MutableComponent names = Component.empty();
    for (int index = 0; index < Math.min(shown, biomeIds.size()); index++) {
      if (index > 0) {
        names.append(LIST_SEPARATOR);
      }
      names.append(
          Component.translatable(
              Identifier.parse(biomeIds.get(index)).toLanguageKey(BIOME_KEY_PREFIX)));
    }
    if (biomeIds.size() > shown) {
      names.append(SPACE + PLUS + (biomeIds.size() - shown));
    }
    return names;
  }

  /** "50%", "0.164%" or "1 in 1200", in the game's language. */
  private static Component chance(SheetContext context, AnimalVariant.Chance chance) {
    double percent = chance.percent();
    if (percent < SMALL_PERCENT && chance.numerator() == 1) {
      return text(context, "variant.one_in", Long.toString(chance.denominator()));
    }
    String pattern = percent < SMALL_PERCENT ? SMALL_CHANCE_PATTERN : BIG_CHANCE_PATTERN;
    return Component.literal(
        new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(FarmingSheet.locale(context)))
                .format(percent)
            + PERCENT_SIGN);
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(FarmingSheet.ID, key, args);
  }

  /**
   * {@code details} with {@code variants} beside them, both from the top (a row would centre them).
   */
  static SheetElement beside(SheetElement details, SheetElement variants, int gap) {
    return new SideBySide(details, variants, gap);
  }

  /** Two elements side by side, top-aligned. Drawing allocates nothing. */
  private record SideBySide(SheetElement left, SheetElement right, int gap)
      implements SheetElement {
    @Override
    public int minWidth() {
      return left.minWidth() + gap + right.minWidth();
    }

    @Override
    public int height() {
      return Math.max(left.height(), right.height());
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
      left.extract(graphics, x, y, left.minWidth());
      right.extract(graphics, x + left.minWidth() + gap, y, right.minWidth());
    }

    @Override
    public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
      int rightX = left.minWidth() + gap;
      if (localX < left.minWidth() && localY < left.height()) {
        return left.tooltipAt(localX, localY, left.minWidth());
      }
      if (localX >= rightX && localY < right.height()) {
        return right.tooltipAt(localX - rightX, localY, right.minWidth());
      }
      return null;
    }
  }

  /** A cell whose whole area shows its look's tooltip. Drawing allocates nothing. */
  private record TooltipCell(SheetElement content, int width, List<FormattedCharSequence> tooltip)
      implements SheetElement {
    @Override
    public int minWidth() {
      return width;
    }

    @Override
    public int height() {
      return content.height();
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int x, int y, int available) {
      content.extract(graphics, x + (width - content.minWidth()) / 2, y, content.minWidth());
    }

    @Override
    public List<FormattedCharSequence> tooltipAt(int localX, int localY, int available) {
      return tooltip;
    }
  }
}
