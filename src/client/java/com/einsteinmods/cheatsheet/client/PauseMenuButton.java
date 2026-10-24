package com.einsteinmods.cheatsheet.client;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The book button at the end of the pause menu's row of small icon buttons (bug report, feedback,
 * friends, player reporting, and whatever other mods add there), the row re-centred. Vanilla {@code
 * PauseScreen#createPauseMenu} builds that row as a horizontal {@code LinearLayout} of 20 px
 * buttons with a 4 px gap, centred in the grid below "Back to Game" (a 204 px button labelled
 * {@code menu.returnToGame}). The row is found as the highest line of 20 px square buttons below
 * "Back to Game"; without one (another mod removed it) the book goes right of "Back to Game".
 */
final class PauseMenuButton {
  private static final String RETURN_TO_GAME_KEY = "menu.returnToGame";
  private static final Component NAME = Component.translatable("cheatsheet.button");

  /** Gap between the pause menu's buttons ({@code PauseScreen.BUTTON_PADDING}, the row spacing). */
  private static final int GAP = 4;

  /** Each button of the row moves left by half of what the book adds, keeping the row centred. */
  private static final int ROW_SHIFT = (ItemIconButton.SIZE + GAP) / 2;

  private PauseMenuButton() {}

  /**
   * {@code ScreenEvents.AFTER_INIT} listener: one {@code instanceof} for every other screen. Also
   * runs on resize, when the pause menu rebuilds its widgets.
   */
  static void addTo(Minecraft minecraft, Screen screen, int width, int height) {
    if (!(screen instanceof PauseScreen pauseScreen)
        || !pauseScreen.showsPauseMenu()
        || !CheatSheetClient.settings().preferences().pauseButton()) {
      return;
    }
    List<AbstractWidget> widgets = Screens.getWidgets(screen);
    AbstractWidget returnToGame = returnToGame(widgets);
    if (returnToGame == null) {
      return;
    }
    List<AbstractWidget> row = iconRow(widgets, returnToGame.getBottom());
    int x;
    int y;
    if (row.isEmpty()) {
      x = returnToGame.getRight() + GAP;
      y = returnToGame.getY();
    } else {
      int rowRight = Integer.MIN_VALUE;
      for (AbstractWidget button : row) {
        button.setX(button.getX() - ROW_SHIFT);
        rowRight = Math.max(rowRight, button.getRight());
      }
      x = rowRight + GAP;
      y = row.getFirst().getY();
    }
    widgets.add(
        new ItemIconButton(
            x,
            y,
            new ItemStack(Items.BOOK),
            NAME,
            button -> CheatSheetClient.openCheatSheet(minecraft, screen)));
  }

  private static @Nullable AbstractWidget returnToGame(List<AbstractWidget> widgets) {
    for (AbstractWidget widget : widgets) {
      if (widget.getMessage().getContents() instanceof TranslatableContents translatable
          && RETURN_TO_GAME_KEY.equals(translatable.getKey())) {
        return widget;
      }
    }
    return null;
  }

  /** The highest line of 20 px square buttons starting below {@code top}; empty when none. */
  private static List<AbstractWidget> iconRow(List<AbstractWidget> widgets, int top) {
    int rowY = Integer.MAX_VALUE;
    for (AbstractWidget widget : widgets) {
      if (isIconButton(widget) && widget.getY() >= top) {
        rowY = Math.min(rowY, widget.getY());
      }
    }
    List<AbstractWidget> row = new ArrayList<>();
    for (AbstractWidget widget : widgets) {
      if (isIconButton(widget) && widget.getY() == rowY) {
        row.add(widget);
      }
    }
    return row;
  }

  private static boolean isIconButton(AbstractWidget widget) {
    return widget.getWidth() == ItemIconButton.SIZE && widget.getHeight() == ItemIconButton.SIZE;
  }
}
