package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.sheet.Scissor;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.core.farming.FarmPlan;
import com.einsteinmods.cheatsheet.core.farming.PlanCell;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * A farm seen from above, one block per square drawn with the game's own textures: the ground
 * (water, farmland, sand) and what stands on it (a crop, a cactus' top). Each square's tooltip says
 * the rule it shows. Sprites are resolved when built; drawing is two blits per square, a GUI
 * stratum per row, rows outside the scissor skipped, and allocates nothing.
 */
final class FarmPlanView implements SheetElement {
  /** Side of a block: a texture at its own size. */
  static final int BLOCK = 16;

  /**
   * Side of a block of a big plan: a texture at half size, its own pixels at a sheet scale of 2.
   */
  static final int SMALL_BLOCK = BLOCK / 2;

  /**
   * How one kind of block looks and what its tooltip says.
   *
   * @param ground the texture below
   * @param groundTint its colour multiplier (water is grey until tinted), white for none
   * @param top the texture standing on it, or null
   * @param topTint its colour multiplier
   * @param tooltip shown over the block
   */
  record Look(
      TextureAtlasSprite ground,
      int groundTint,
      @Nullable TextureAtlasSprite top,
      int topTint,
      List<FormattedCharSequence> tooltip) {
    Look {
      tooltip = List.copyOf(tooltip);
    }
  }

  private final Look[] looks;
  private final int block;
  private final int columns;
  private final int rows;

  /** {@code looks} has a look for every kind of block in {@code plan}. */
  FarmPlanView(FarmPlan plan, Map<PlanCell, Look> looks) {
    this(plan, looks, BLOCK);
  }

  /** Each block {@code block} pixels wide. */
  FarmPlanView(FarmPlan plan, Map<PlanCell, Look> looks, int block) {
    this.block = block;
    this.columns = plan.width();
    this.rows = plan.height();
    this.looks = new Look[columns * rows];
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < columns; x++) {
        Look look = looks.get(plan.cell(x, y));
        if (look == null) {
          throw new IllegalArgumentException("no look for " + plan.cell(x, y));
        }
        this.looks[y * columns + x] = look;
      }
    }
  }

  @Override
  public int minWidth() {
    return columns * block;
  }

  @Override
  public int height() {
    return rows * block;
  }

  @Override
  public void extract(GuiGraphicsExtractor graphics, int x, int y, int width) {
    for (int row = 0; row < rows; row++) {
      int top = y + row * block;
      if (!Scissor.touches(graphics, x, top, block)) {
        continue;
      }
      // A stratum per row: vanilla's placement scan stays within one row of blocks.
      graphics.nextStratum();
      for (int column = 0; column < columns; column++) {
        Look look = looks[row * columns + column];
        int left = x + column * block;
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            look.ground(),
            left,
            top,
            block,
            block,
            look.groundTint());
        TextureAtlasSprite standing = look.top();
        if (standing != null) {
          graphics.blitSprite(
              RenderPipelines.GUI_TEXTURED, standing, left, top, block, block, look.topTint());
        }
      }
    }
  }

  @Override
  public @Nullable List<FormattedCharSequence> tooltipAt(int localX, int localY, int width) {
    int column = localX / block;
    int row = localY / block;
    if (column < 0 || column >= columns || row < 0 || row >= rows) {
      return null;
    }
    List<FormattedCharSequence> tooltip = looks[row * columns + column].tooltip();
    return tooltip.isEmpty() ? null : tooltip;
  }
}
