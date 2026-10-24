package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.TileGrid;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.core.food.FoodRanking;
import com.einsteinmods.cheatsheet.core.food.FoodValues;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import org.jspecify.annotations.Nullable;

/**
 * Food tab: every food in the item registry (modded ones included), most filling first, as a grid
 * of equal cells: the food very big, the drumsticks it fills and its saturation below it; the most
 * saturating highlighted, a badge for side effects, explained in the tooltip with level, duration
 * and chance. A small legend sits beside the grid (below it on narrow screens). Values come from
 * the items' {@code food} and {@code consumable} components, so data packs and mods are reflected.
 */
public final class FoodSheet implements SheetSource {
  public static final String ID = "food";

  /**
   * Nine cells beside the legend fill the height of a 960x540 sheet; they need a sheet at least 470
   * pixels wide (narrower only at a fixed sheet size on a small window).
   */
  private static final int COLUMNS = 9;

  private static final int COLUMN_GAP = 2;
  private static final int ROW_GAP = 2;
  private static final int LEGEND_GAP = 4;
  private static final int ICON_SCALE = 3;
  private static final int ICON_SIZE = IconGrid.ITEM * ICON_SCALE;
  private static final int CELL_WIDTH = 50;
  private static final int SPRITE_SIZE = 9;
  private static final int VALUE_GAP = 1;
  private static final int LINE_HEIGHT = 9;
  private static final int NOTE_WIDTH = 120;
  private static final int PADDING = 4;

  /** Saturation values shown highlighted: the foods holding the top three. */
  private static final int BEST_SATURATION_PLACES = 3;

  private static final int SATURATION_COLOR = 0xFFFFC94D;
  private static final int BEST_FILL = 0x40FFD23C;
  private static final int TELEPORT_COLOR = 0xFFC77DFF;
  private static final int STEW_COLOR = 0xFFC77DFF;
  private static final int PERCENT = 100;

  private static final Identifier DRUMSTICK = Identifier.withDefaultNamespace("hud/food_full");
  private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
  private static final String TELEPORT_MARK = "✦";
  private static final String STEW_MARK = "?";
  private static final String LIST_SEPARATOR = ", ";
  private static final String POTENCY_KEY = "potion.potency.";
  private static final String WITH_AMPLIFIER_KEY = "potion.withAmplifier";
  private static final String WITH_DURATION_KEY = "potion.withDuration";
  private static final String PERCENT_SIGN = "%";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.GOLDEN_CARROT);
  }

  @Override
  public Sheet build(SheetContext context) {
    Map<String, Item> items = new HashMap<>();
    List<FoodValues> foods = new ArrayList<>();
    for (Item item : BuiltInRegistries.ITEM) {
      FoodProperties food = item.components().get(DataComponents.FOOD);
      Consumable consumable = item.components().get(DataComponents.CONSUMABLE);
      if (food == null && !cures(consumable)) {
        continue;
      }
      String id = BuiltInRegistries.ITEM.getKey(item).toString();
      items.put(id, item);
      foods.add(
          food == null
              ? new FoodValues(id, 0, 0.0F)
              : new FoodValues(id, food.nutrition(), food.saturation()));
    }
    Set<String> best = FoodRanking.bestSaturation(foods, BEST_SATURATION_PLACES);
    float topSaturation = 0.0F;
    for (FoodValues food : foods) {
      topSaturation = Math.max(topSaturation, food.saturation());
    }
    if (foods.isEmpty()) {
      return Sheet.of();
    }
    IconGrid.Builder grid = new IconGrid.Builder(context.font(), COLUMNS, COLUMN_GAP, ROW_GAP);
    for (FoodValues food : FoodRanking.sorted(foods)) {
      grid.cell(cell(context, new ItemStack(items.get(food.id())), food, best.contains(food.id())));
    }
    return Sheet.ofSections(
        List.of(
            Section.of(
                text(context, "title"), CardTint.ORANGE, panel(CardTint.ORANGE, grid.build())),
            Section.of(
                    text(context, "legend"),
                    CardTint.STONE,
                    panel(CardTint.STONE, legend(context, topSaturation)))
                .besidePrevious()),
        null);
  }

  /** {@code elements} stacked in one tinted panel, the only tile of a section. */
  private static TileGrid panel(CardTint tint, SheetElement... elements) {
    FixedCard.Builder card = new FixedCard.Builder(tint);
    int width = 0;
    int top = PADDING;
    for (SheetElement element : elements) {
      card.row().add(element, PADDING, top);
      top += element.height() + ROW_GAP;
      width = Math.max(width, element.minWidth());
    }
    return new TileGrid(
        List.of(card.build(width + PADDING * 2, top - ROW_GAP + PADDING)), COLUMN_GAP);
  }

  private static IconGrid.Cell cell(
      SheetContext context, ItemStack stack, FoodValues values, boolean bestSaturation) {
    List<IconGrid.Part> parts = new ArrayList<>();
    List<Component> details = new ArrayList<>();
    int height = ICON_SIZE + VALUE_GAP + LINE_HEIGHT * 2;
    if (bestSaturation) {
      parts.add(new IconGrid.FillPart(0, 0, CELL_WIDTH, height, BEST_FILL));
    }
    int iconX = (CELL_WIDTH - ICON_SIZE) / 2;
    parts.add(new IconGrid.ItemPart(stack, ICON_SCALE, iconX, 0));
    FoodProperties food = stack.get(DataComponents.FOOD);
    if (food != null) {
      int valuesTop = ICON_SIZE + VALUE_GAP;
      parts.add(new IconGrid.SpritePart(DRUMSTICK, 0, valuesTop, SPRITE_SIZE, SPRITE_SIZE));
      parts.add(
          textPart(
              context,
              FoodRanking.drumsticks(values.nutrition()),
              SheetPalette.TEXT,
              SPRITE_SIZE + VALUE_GAP,
              valuesTop + 1));
      parts.add(
          textPart(
              context,
              FoodRanking.saturation(values.saturation()),
              SATURATION_COLOR,
              0,
              valuesTop + LINE_HEIGHT + 1));
      details.add(
          text(context, "hunger", values.nutrition(), FoodRanking.drumsticks(values.nutrition())));
      details.add(text(context, "saturation", FoodRanking.saturation(values.saturation())));
      if (food.canAlwaysEat()) {
        details.add(text(context, "always"));
      }
    }
    if (bestSaturation) {
      details.add(text(context, "best"));
    }
    IconGrid.Part badge = effects(context, stack, details);
    if (badge != null) {
      parts.add(badge);
    }
    return new IconGrid.Cell(
        CELL_WIDTH, height, parts, Tooltips.ofItem(context.font(), stack, details));
  }

  /**
   * Adds one tooltip line per side effect and returns the badge for the first one: the effect's
   * icon, a heart for cures, a mark for teleporting or a random stew effect; null when none.
   */
  private static IconGrid.@Nullable Part effects(
      SheetContext context, ItemStack stack, List<Component> details) {
    int badgeX = CELL_WIDTH - SPRITE_SIZE;
    IconGrid.Part badge = null;
    Consumable consumable = stack.get(DataComponents.CONSUMABLE);
    if (consumable != null) {
      for (ConsumeEffect effect : consumable.onConsumeEffects()) {
        IconGrid.Part mark = null;
        if (effect instanceof ApplyStatusEffectsConsumeEffect apply) {
          for (MobEffectInstance instance : apply.effects()) {
            details.add(applied(context, instance, apply.probability()));
          }
          if (!apply.effects().isEmpty()) {
            mark =
                new IconGrid.SpritePart(
                    Hud.getMobEffectSprite(apply.effects().getFirst().getEffect()),
                    badgeX,
                    0,
                    SPRITE_SIZE,
                    SPRITE_SIZE);
          }
        } else if (effect instanceof RemoveStatusEffectsConsumeEffect remove) {
          details.add(text(context, "cures", effectNames(remove.effects())));
          mark = new IconGrid.SpritePart(HEART, badgeX, 0, SPRITE_SIZE, SPRITE_SIZE);
        } else if (effect instanceof ClearAllStatusEffectsConsumeEffect) {
          details.add(text(context, "clears"));
          mark = new IconGrid.SpritePart(HEART, badgeX, 0, SPRITE_SIZE, SPRITE_SIZE);
        } else if (effect instanceof TeleportRandomlyConsumeEffect teleport) {
          details.add(text(context, "teleports", Math.round(teleport.diameter() / 2.0F)));
          mark = textPart(context, TELEPORT_MARK, TELEPORT_COLOR, badgeX + 1, 0);
        }
        if (badge == null) {
          badge = mark;
        }
      }
    }
    if (stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS)) {
      details.add(text(context, "stew"));
      if (badge == null) {
        badge = textPart(context, STEW_MARK, STEW_COLOR, badgeX + 2, 0);
      }
    }
    return badge;
  }

  /** "Poison II (1:00)", with the chance in front when it is not certain. */
  private static Component applied(
      SheetContext context, MobEffectInstance instance, float probability) {
    MutableComponent line = instance.getEffect().value().getDisplayName().copy();
    if (instance.getAmplifier() > 0) {
      line =
          Component.translatable(
              WITH_AMPLIFIER_KEY,
              line,
              Component.translatable(POTENCY_KEY + instance.getAmplifier()));
    }
    line =
        Component.translatable(
            WITH_DURATION_KEY,
            line,
            MobEffectUtil.formatDuration(instance, 1.0F, SharedConstants.TICKS_PER_SECOND));
    if (probability >= 1.0F) {
      return line;
    }
    String chance = Math.round(probability * PERCENT) + PERCENT_SIGN;
    return text(context, "chance", chance, line);
  }

  private static Component effectNames(Iterable<Holder<MobEffect>> effects) {
    MutableComponent names = Component.empty();
    boolean first = true;
    for (Holder<MobEffect> effect : effects) {
      if (!first) {
        names.append(LIST_SEPARATOR);
      }
      names.append(effect.value().getDisplayName());
      first = false;
    }
    return names;
  }

  /** Non-food consumables worth listing: drinks that cure (milk). */
  private static boolean cures(@Nullable Consumable consumable) {
    if (consumable == null) {
      return false;
    }
    for (ConsumeEffect effect : consumable.onConsumeEffects()) {
      if (effect instanceof ClearAllStatusEffectsConsumeEffect
          || effect instanceof RemoveStatusEffectsConsumeEffect) {
        return true;
      }
    }
    return false;
  }

  private static SheetElement[] legend(SheetContext context, float sampleSaturation) {
    IconGrid.Builder grid = new IconGrid.Builder(context.font(), 1, LEGEND_GAP, ROW_GAP);
    grid.cell(
        new IconGrid.Flow(context.font())
            .sprite(DRUMSTICK, SPRITE_SIZE)
            .gap(LEGEND_GAP)
            .text(text(context, "legend.hunger"), SheetPalette.TEXT)
            .build(List.of()));
    grid.cell(
        new IconGrid.Flow(context.font())
            .text(Component.literal(FoodRanking.saturation(sampleSaturation)), SATURATION_COLOR)
            .gap(LEGEND_GAP)
            .text(text(context, "legend.saturation"), SheetPalette.TEXT)
            .build(List.of()));
    grid.cell(
        new IconGrid.Flow(context.font())
            .fill(SPRITE_SIZE, BEST_FILL)
            .gap(LEGEND_GAP)
            .text(text(context, "legend.best"), SheetPalette.TEXT)
            .build(List.of()));
    grid.cell(
        new IconGrid.Flow(context.font())
            .sprite(Hud.getMobEffectSprite(MobEffects.POISON), SPRITE_SIZE)
            .gap(LEGEND_GAP)
            .text(text(context, "legend.effects"), SheetPalette.TEXT)
            .build(List.of()));
    return new SheetElement[] {
      grid.build(),
      context.paragraph(text(context, "legend.note"), NOTE_WIDTH, SheetPalette.MUTED_TEXT)
    };
  }

  private static IconGrid.TextPart textPart(
      SheetContext context, String value, int color, int x, int y) {
    FormattedCharSequence text = Component.literal(value).getVisualOrderText();
    return new IconGrid.TextPart(text, color, x, y, true);
  }

  private static Component text(SheetContext context, String key, Object... args) {
    return context.tabText(ID, key, args);
  }
}
