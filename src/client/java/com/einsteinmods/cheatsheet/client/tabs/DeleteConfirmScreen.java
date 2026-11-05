package com.einsteinmods.cheatsheet.client.tabs;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

/**
 * Vanilla's confirmation with a tick box between the text and the buttons. The box joins vanilla's
 * layout in {@code ConfirmScreen#addAdditionalText}, which {@code ConfirmScreen#init} calls before
 * it registers the layout's widgets. The tick lives outside the widget, so the box starts ticked
 * again if the widgets are ever built anew, and the answer (Yes, No or Escape) reads it.
 */
public final class DeleteConfirmScreen extends ConfirmScreen {
  /** Room left at the sides, as {@code ConfirmScreen#addMessage}. */
  private static final int SIDE_ROOM = 50;

  /** What the player answered, and whether the box was ticked at that moment. */
  @FunctionalInterface
  interface Answer {
    void answered(boolean confirmed, boolean dontAskAgain);
  }

  private final AtomicBoolean dontAskAgain;
  private final Component dontAskAgainLabel;

  DeleteConfirmScreen(
      Answer answer, Component title, Component message, Component dontAskAgainLabel) {
    AtomicBoolean ticked = new AtomicBoolean();
    super(confirmed -> answer.answered(confirmed, ticked.get()), title, message);
    this.dontAskAgain = ticked;
    this.dontAskAgainLabel = dontAskAgainLabel;
  }

  @Override
  protected void addAdditionalText() {
    layout.addChild(
        Checkbox.builder(dontAskAgainLabel, font)
            .maxWidth(width - SIDE_ROOM)
            .selected(dontAskAgain.get())
            .onValueChange((box, ticked) -> dontAskAgain.set(ticked))
            .build());
  }
}
