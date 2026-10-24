package com.einsteinmods.cheatsheet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * The settings button at the end of the tab bar: a vanilla button's sprite with a gear on it, the
 * same size on screen as the tabs ({@link BarButton}); "Settings" is its tooltip and narration.
 */
final class SettingsBarButton extends BarButton {
  /** Vanilla's button sprites ({@code AbstractButton.SPRITES}, private there). */
  private static final WidgetSprites SPRITES =
      new WidgetSprites(
          Identifier.withDefaultNamespace("widget/button"),
          Identifier.withDefaultNamespace("widget/button_disabled"),
          Identifier.withDefaultNamespace("widget/button_highlighted"));

  private static final FormattedCharSequence SYMBOL = Component.literal("⚙").getVisualOrderText();
  private static final int SYMBOL_COLOR = 0xFFFFFFFF;

  SettingsBarButton(Component name, OnPress onPress) {
    super(name, onPress, defaultNarration -> name.copy());
    setTooltip(Tooltip.create(name));
  }

  @Override
  protected void extractSquare(GuiGraphicsExtractor graphics, int x, int y) {
    graphics.blitSprite(
        RenderPipelines.GUI_TEXTURED, SPRITES.get(active, isHoveredOrFocused()), x, y, SIZE, SIZE);
    Font font = Minecraft.getInstance().font;
    graphics.centeredText(
        font, SYMBOL, x + SIZE / 2, y + (SIZE - font.lineHeight) / 2 + 1, SYMBOL_COLOR);
  }
}
