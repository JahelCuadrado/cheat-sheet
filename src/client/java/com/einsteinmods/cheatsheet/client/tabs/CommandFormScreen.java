package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.core.commands.CommandText;
import com.einsteinmods.cheatsheet.core.commands.CustomCommand;
import com.einsteinmods.cheatsheet.core.commands.CustomCommands;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * The small form that adds or edits a saved command: a name (a vanilla text box) and its commands,
 * one per line, run in order (vanilla's multi-line box), Save and Cancel. Save is only active while
 * both are usable once cleaned ({@link CommandText#sanitizeLines}: no line too long, not too many;
 * the box itself has no character limit, whose counter would only show a meaningless total); Enter
 * saves from the name box (in the commands box it starts a new line), Escape or Cancel go back to
 * the cheat sheet without saving. Drawn at the player's GUI scale (the cheat sheet puts it back
 * when another screen opens).
 */
public final class CommandFormScreen extends Screen {
  private static final int FIELD_WIDTH = 300;
  private static final int FIELD_HEIGHT = 20;
  private static final int BUTTON_WIDTH = 148;
  private static final int BUTTON_GAP = 4;
  private static final int TITLE_Y = 20;
  private static final int NAME_LABEL_Y = 44;
  private static final int LABEL_GAP = 11;
  private static final int SECTION_GAP = 28;
  private static final int HINT_LINE_HEIGHT = 10;
  private static final int MIN_COMMAND_HEIGHT = 40;
  private static final int MAX_COMMAND_HEIGHT = 100;
  private static final int BOTTOM_MARGIN = 8;
  private static final int HINT_TOP = 4;
  private static final int BUTTONS_GAP = 16;
  private static final int LABEL_COLOR = 0xFFA0A0A0;
  private static final int TITLE_COLOR = 0xFFFFFFFF;

  private static final Component NAME = CommandActions.text("form.name");
  private static final Component COMMAND = CommandActions.text("form.command");
  private static final Component HINT =
      CommandActions.text("form.hint", CommandText.MAX_LINES, CommandText.MAX_COMMAND_LENGTH);
  private static final String LINE_BREAK = "\n";

  private final @Nullable Screen back;
  private final int index;
  private final String initialName;
  private final String initialCommand;
  private @Nullable EditBox nameBox;
  private @Nullable MultiLineEditBox commandBox;
  private @Nullable Button saveButton;
  private List<FormattedCharSequence> hint = List.of();
  private int commandHeight = MAX_COMMAND_HEIGHT;

  /**
   * @param back the screen to return to (the cheat sheet)
   * @param index the saved command edited, or -1 for a new one
   * @param initial its current text, or null for a new one
   */
  CommandFormScreen(@Nullable Screen back, int index, @Nullable CustomCommand initial) {
    super(CommandActions.text(initial == null ? "form.new" : "form.edit"));
    this.back = back;
    this.index = index;
    this.initialName = initial == null ? "" : initial.name();
    this.initialCommand = initial == null ? "" : String.join(LINE_BREAK, initial.lines());
  }

  @Override
  protected void init() {
    int left = (width - FIELD_WIDTH) / 2;
    int nameY = NAME_LABEL_Y + LABEL_GAP;
    int commandY = nameY + FIELD_HEIGHT + SECTION_GAP;
    EditBox name = new EditBox(font, left, nameY, FIELD_WIDTH, FIELD_HEIGHT, nameBox, NAME);
    name.setMaxLength(CommandText.MAX_NAME_LENGTH);
    if (nameBox == null) {
      name.setValue(initialName);
    }
    hint = font.split(HINT, FIELD_WIDTH);
    int hintHeight = HINT_TOP + hint.size() * HINT_LINE_HEIGHT;
    int room = height - commandY - hintHeight - BUTTONS_GAP - FIELD_HEIGHT - BOTTOM_MARGIN;
    commandHeight = Math.clamp(room, MIN_COMMAND_HEIGHT, MAX_COMMAND_HEIGHT);
    String typed = commandBox == null ? initialCommand : commandBox.getValue();
    MultiLineEditBox command =
        MultiLineEditBox.builder()
            .setX(left)
            .setY(commandY)
            .build(font, FIELD_WIDTH, commandHeight, COMMAND);
    command.setValue(typed);
    nameBox = addRenderableWidget(name);
    commandBox = addRenderableWidget(command);
    int buttonsY = commandY + commandHeight + hintHeight + BUTTONS_GAP;
    int buttonsLeft = (width - BUTTON_WIDTH * 2 - BUTTON_GAP) / 2;
    saveButton =
        addRenderableWidget(
            Button.builder(CommandActions.text("form.save"), button -> save())
                .bounds(buttonsLeft, buttonsY, BUTTON_WIDTH, FIELD_HEIGHT)
                .build());
    addRenderableWidget(
        Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(buttonsLeft + BUTTON_WIDTH + BUTTON_GAP, buttonsY, BUTTON_WIDTH, FIELD_HEIGHT)
            .build());
    name.setResponder(text -> updateSaveButton());
    command.setValueListener(text -> updateSaveButton());
    updateSaveButton();
  }

  @Override
  protected void setInitialFocus() {
    EditBox name = nameBox;
    if (name != null) {
      setInitialFocus(name);
    }
  }

  /** The command as typed, cleaned; empty while either box is unusable. */
  private Optional<CustomCommand> typed() {
    EditBox name = nameBox;
    MultiLineEditBox command = commandBox;
    if (name == null || command == null) {
      return Optional.empty();
    }
    return CustomCommand.of(name.getValue(), command.getValue());
  }

  private void updateSaveButton() {
    Button save = saveButton;
    if (save != null) {
      save.active = typed().isPresent();
    }
  }

  private void save() {
    Optional<CustomCommand> typed = typed();
    if (typed.isEmpty()) {
      return;
    }
    CustomCommands saved = CheatSheetClient.customCommands().commands();
    CommandActions.save(
        back, index < 0 ? saved.with(typed.get()) : saved.replaced(index, typed.get()));
  }

  /** Enter saves (in the commands box it starts a new line); Escape (vanilla) closes. */
  @Override
  public boolean keyPressed(KeyEvent event) {
    if (event.isConfirmation() && getFocused() != commandBox && typed().isPresent()) {
      save();
      return true;
    }
    return super.keyPressed(event);
  }

  @Override
  public void onClose() {
    minecraft.gui.setScreen(back);
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    int left = (width - FIELD_WIDTH) / 2;
    int nameY = NAME_LABEL_Y + LABEL_GAP;
    int commandY = nameY + FIELD_HEIGHT + SECTION_GAP;
    graphics.centeredText(font, title, width / 2, TITLE_Y, TITLE_COLOR);
    graphics.text(font, NAME, left, NAME_LABEL_Y, LABEL_COLOR);
    graphics.text(font, COMMAND, left, commandY - LABEL_GAP, LABEL_COLOR);
    int hintY = commandY + commandHeight + HINT_TOP;
    for (FormattedCharSequence line : hint) {
      graphics.text(font, line, left, hintY, LABEL_COLOR);
      hintY += HINT_LINE_HEIGHT;
    }
  }
}
