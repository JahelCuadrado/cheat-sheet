package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.client.sheet.CardTint;
import com.einsteinmods.cheatsheet.client.sheet.Paragraph;
import com.einsteinmods.cheatsheet.client.sheet.Section;
import com.einsteinmods.cheatsheet.client.sheet.Sheet;
import com.einsteinmods.cheatsheet.client.sheet.SheetContext;
import com.einsteinmods.cheatsheet.client.sheet.SheetElement;
import com.einsteinmods.cheatsheet.client.sheet.SheetPalette;
import com.einsteinmods.cheatsheet.client.sheet.SheetSource;
import com.einsteinmods.cheatsheet.client.sheet.TileGrid;
import com.einsteinmods.cheatsheet.client.sheet.Tooltips;
import com.einsteinmods.cheatsheet.core.commands.BuiltInCommand;
import com.einsteinmods.cheatsheet.core.commands.BuiltInCommands;
import com.einsteinmods.cheatsheet.core.commands.CommandDanger;
import com.einsteinmods.cheatsheet.core.commands.CommandGroup;
import com.einsteinmods.cheatsheet.core.commands.CommandText;
import com.einsteinmods.cheatsheet.core.commands.CustomCommand;
import com.einsteinmods.cheatsheet.core.commands.CustomCommands;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Commands tab, shown only to a player who can use commands: in creative mode, or with game master
 * permission (cheats on in single player, an operator on a server), as vanilla's own game mode
 * switcher checks ({@code Permissions.COMMANDS_GAMEMASTER}). On top, the player's saved commands (a
 * "+" opens a small form; each can be edited or deleted); then the popular combos and the built-in
 * commands, each in four equal cards, one per group. Every row has its name, the command in a dark
 * strip and small buttons: open it in the chat to edit (single commands only: the chat holds one),
 * copy it, run it (one that can destroy things asks first); built-in ones also have a star that
 * adds them, every line, to the saved commands; saved ones edit and delete. Cards do not zoom:
 * clicks go to the buttons.
 *
 * <p>Cost: the saved commands file is read when the tab is first built; building lays out every row
 * once. A frame draws the visible rows (text, a strip, a few small sprites) and reads the mouse
 * once per row for the hover light; no allocation.
 */
public final class CommandsSheet implements SheetSource {
  public static final String ID = "commands";

  /** Four cards fill a 960-wide sheet (6 px margins, 4 px gaps): 4 x 234 + 3 x 4 = 948. */
  private static final int TILE_WIDTH = 234;

  private static final int TILE_GAP = 4;
  private static final int PADDING = 4;
  private static final int INNER_WIDTH = TILE_WIDTH - PADDING * 2;
  private static final int HEADER_GAP = 3;
  private static final int ROW_GAP = 1;
  private static final int ROW_PITCH = CommandRow.HEIGHT + ROW_GAP;

  /** The name column takes its longest name, within these bounds (longer names are cut). */
  private static final int MIN_NAME_WIDTH = 48;

  private static final int MAX_NAME_WIDTH = 100;

  private static final int DANGER_TEXT = 0xFFFF7A6E;

  /** A combo's tooltip wraps wider: its lines are long and there may be many. */
  private static final int COMBO_WRAP_WIDTH = Tooltips.WRAP_WIDTH * 3;

  private static final Map<CommandGroup, CardTint> GROUP_TINTS =
      Map.of(
          CommandGroup.BASICS, CardTint.GREEN,
          CommandGroup.BUILDING, CardTint.ORANGE,
          CommandGroup.ITEMS, CardTint.PURPLE,
          CommandGroup.ADVANCED, CardTint.AQUA,
          CommandGroup.KITS, CardTint.PURPLE,
          CommandGroup.WORLD, CardTint.GREEN,
          CommandGroup.BOSSES, CardTint.ORANGE,
          CommandGroup.PLAYER, CardTint.AQUA);

  private static final CardTint CUSTOM_TINT = CardTint.GOLD;

  @Override
  public String id() {
    return ID;
  }

  @Override
  public ItemStack icon() {
    return new ItemStack(Items.COMMAND_BLOCK);
  }

  /** Clicks are for the buttons: cards neither light up nor zoom. */
  @Override
  public boolean zoomsCards() {
    return false;
  }

  /**
   * In creative mode, or with game master permission ({@code LocalPlayer#permissions}, level 2:
   * cheats on or an operator), as {@code KeyboardHandler} checks for the game mode switcher.
   */
  @Override
  public boolean availableFor(Minecraft minecraft) {
    LocalPlayer player = minecraft.player;
    return player != null
        && (player.isCreative()
            || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
  }

  @Override
  public Sheet build(SheetContext context) {
    Font font = context.font();
    Buttons buttons = new Buttons(font);
    List<SheetElement> comboCards = groupCards(font, buttons, true);
    List<SheetElement> singleCards = groupCards(font, buttons, false);
    CustomCommands saved = CheatSheetClient.customCommands().commands();
    List<SheetElement> customCards = customCards(font, buttons, saved, rowsPerCard(false));
    return Sheet.ofSections(
        List.of(
            Section.of(
                CommandActions.text("section.custom"),
                CUSTOM_TINT,
                new TileGrid(customCards, TILE_GAP, true)),
            Section.of(
                CommandActions.text("section.combos"),
                CardTint.STONE,
                new TileGrid(comboCards, TILE_GAP, true)),
            Section.of(
                CommandActions.text("section.builtin"),
                CardTint.STONE,
                new TileGrid(singleCards, TILE_GAP, true))),
        null);
  }

  /** The cards of the combo groups or of the single command groups, all as tall as the longest. */
  private static List<SheetElement> groupCards(Font font, Buttons buttons, boolean combos) {
    int rows = rowsPerCard(combos);
    List<SheetElement> cards = new ArrayList<>();
    for (CommandGroup group : CommandGroup.values()) {
      if (group.combos() == combos) {
        cards.add(groupCard(font, buttons, group, rows));
      }
    }
    return cards;
  }

  /** Rows of the longest combo group or single command group. */
  private static int rowsPerCard(boolean combos) {
    int rows = 1;
    for (CommandGroup group : CommandGroup.values()) {
      if (group.combos() == combos) {
        rows = Math.max(rows, BuiltInCommands.of(group).size());
      }
    }
    return rows;
  }

  /** One group's card, {@code rows} rows tall so every group card has the same height. */
  private static SheetElement groupCard(Font font, Buttons buttons, CommandGroup group, int rows) {
    CardTint tint = GROUP_TINTS.get(group);
    List<BuiltInCommand> commands = BuiltInCommands.of(group);
    List<String> names = new ArrayList<>(commands.size());
    for (BuiltInCommand command : commands) {
      names.add(CommandActions.text("name." + command.key()).getString());
    }
    int nameWidth = nameWidth(font, names);
    FixedCard.Builder card = new FixedCard.Builder(tint);
    card.add(
        new CommandHeader(
            font,
            INNER_WIDTH,
            CommandActions.text("group." + group.id()),
            tint.title(),
            null,
            null),
        PADDING,
        PADDING);
    int y = PADDING + CommandRow.HEIGHT + HEADER_GAP;
    for (int index = 0; index < commands.size(); index++) {
      BuiltInCommand command = commands.get(index);
      String name = names.get(index);
      List<String> lines = command.lines();
      Component description = CommandActions.text("desc." + command.key());
      List<CommandButton> rowButtons = new ArrayList<>();
      if (!command.isCombo()) {
        rowButtons.add(buttons.chat(lines.getFirst()));
      }
      rowButtons.add(buttons.copy(lines));
      rowButtons.add(command.dangerous() ? buttons.confirmRun(lines) : buttons.run(lines));
      rowButtons.add(buttons.favourite(command, name));
      CommandRow row =
          new CommandRow(
              font,
              INNER_WIDTH,
              name,
              SheetPalette.TEXT,
              nameWidth,
              lines,
              rowButtons,
              tooltip(font, name, lines, description, command.dangerous()));
      if (CommandActions.recentlyFavourited(command.key())) {
        row.confirm(rowButtons.size() - 1);
      }
      card.row().add(row, PADDING, y);
      y += ROW_PITCH;
    }
    return card.build(TILE_WIDTH, cardHeight(rows));
  }

  /**
   * The saved commands, {@code rowsPerCard} per card: the first card has the count and the "+"
   * button (disabled when full), or a short note when there are none yet.
   */
  private static List<SheetElement> customCards(
      Font font, Buttons buttons, CustomCommands saved, int rowsPerCard) {
    List<CustomCommand> entries = saved.entries();
    List<String> names = new ArrayList<>(entries.size());
    for (CustomCommand command : entries) {
      names.add(command.name());
    }
    int nameWidth = nameWidth(font, names);
    List<SheetElement> cards = new ArrayList<>();
    int first = 0;
    do {
      int last = Math.min(entries.size(), first + rowsPerCard);
      FixedCard.Builder card = new FixedCard.Builder(CUSTOM_TINT);
      if (first == 0) {
        card.add(
            new CommandHeader(
                font,
                INNER_WIDTH,
                Component.empty(),
                CUSTOM_TINT.title(),
                CommandActions.text("count", entries.size(), CommandText.MAX_CUSTOM_COMMANDS),
                buttons.add(!saved.isFull())),
            PADDING,
            PADDING);
      }
      int y = PADDING + CommandRow.HEIGHT + HEADER_GAP;
      if (entries.isEmpty()) {
        Paragraph note =
            new Paragraph(font, CommandActions.text("empty"), INNER_WIDTH, SheetPalette.MUTED_TEXT);
        card.row().add(note, PADDING, y);
        y += note.height();
      }
      for (int index = first; index < last; index++) {
        CustomCommand command = entries.get(index);
        List<String> lines = command.lines();
        boolean dangerous = CommandDanger.anyDestroys(lines);
        List<CommandButton> rowButtons = new ArrayList<>();
        if (!command.isCombo()) {
          rowButtons.add(buttons.chat(lines.getFirst()));
        }
        rowButtons.add(buttons.copy(lines));
        rowButtons.add(dangerous ? buttons.confirmRun(lines) : buttons.run(lines));
        rowButtons.add(buttons.edit(index, command));
        rowButtons.add(buttons.delete(index, command));
        card.row()
            .add(
                new CommandRow(
                    font,
                    INNER_WIDTH,
                    command.name(),
                    SheetPalette.TEXT,
                    nameWidth,
                    lines,
                    rowButtons,
                    tooltip(font, command.name(), lines, null, dangerous)),
                PADDING,
                y);
        y += ROW_PITCH;
      }
      int rows = last - first;
      cards.add(card.build(TILE_WIDTH, Math.max(cardHeight(rows), y - ROW_GAP + PADDING)));
      first = last;
    } while (first < entries.size());
    return cards;
  }

  /** Height of a card with a header and {@code rows} rows. */
  private static int cardHeight(int rows) {
    return PADDING * 2 + CommandRow.HEIGHT + HEADER_GAP + rows * ROW_PITCH - ROW_GAP;
  }

  /** Width of the name column: the longest name, within bounds. */
  private static int nameWidth(Font font, List<String> names) {
    int widest = MIN_NAME_WIDTH;
    for (String name : names) {
      widest = Math.max(widest, font.width(name));
    }
    return Math.min(widest, MAX_NAME_WIDTH);
  }

  /**
   * A row's tooltip: name, every command in full, what it does and, if so, that it asks first. A
   * combo's tooltip wraps wider, so its many long commands stay within the screen's height.
   */
  private static List<FormattedCharSequence> tooltip(
      Font font,
      String name,
      List<String> commands,
      @Nullable Component description,
      boolean dangerous) {
    int wrap = commands.size() > 1 ? COMBO_WRAP_WIDTH : Tooltips.WRAP_WIDTH;
    List<FormattedCharSequence> lines = new ArrayList<>();
    lines.add(Component.literal(name).withStyle(ChatFormatting.BOLD).getVisualOrderText());
    for (String command : commands) {
      lines.addAll(
          font.split(Component.literal("/" + command).withColor(CommandRow.CODE_TEXT), wrap));
    }
    if (description != null) {
      lines.addAll(font.split(description.copy().withStyle(ChatFormatting.GRAY), wrap));
    }
    if (dangerous) {
      lines.addAll(
          font.split(CommandActions.text("dangerous").copy().withColor(DANGER_TEXT), wrap));
    }
    return List.copyOf(lines);
  }

  /** The buttons' tooltips, built once per sheet and shared by every row. */
  private static final class Buttons {
    private final List<FormattedCharSequence> chat;
    private final List<FormattedCharSequence> copy;
    private final List<FormattedCharSequence> run;
    private final List<FormattedCharSequence> confirmRun;
    private final List<FormattedCharSequence> edit;
    private final List<FormattedCharSequence> delete;
    private final List<FormattedCharSequence> add;
    private final List<FormattedCharSequence> full;
    private final List<FormattedCharSequence> favourite;
    private final Component warning;

    Buttons(Font font) {
      chat = line(font, "button.chat");
      copy = line(font, "button.copy");
      run = line(font, "button.run");
      confirmRun = line(font, "button.run_confirm");
      edit = line(font, "button.edit");
      delete = line(font, "button.delete");
      add = line(font, "button.add");
      full = line(font, "button.add_full");
      favourite = line(font, "button.favourite");
      warning = CommandActions.text("dangerous");
    }

    private static List<FormattedCharSequence> line(Font font, String key) {
      return List.copyOf(font.split(CommandActions.text(key), Tooltips.WRAP_WIDTH));
    }

    CommandButton chat(String command) {
      return new CommandButton(
          CommandButton.CHAT,
          CommandButton.ICON,
          chat,
          () -> {
            CommandActions.openInChat(command);
            return false;
          },
          true);
    }

    CommandButton copy(List<String> lines) {
      return new CommandButton(
          CommandButton.COPY, CommandButton.ICON, copy, () -> CommandActions.copy(lines), true);
    }

    CommandButton run(List<String> lines) {
      return new CommandButton(
          CommandButton.RUN, CommandButton.ICON, run, () -> CommandActions.run(lines), true);
    }

    CommandButton confirmRun(List<String> lines) {
      return new CommandButton(
          CommandButton.RUN,
          CommandButton.DANGER_ICON,
          confirmRun,
          () -> CommandActions.confirmRun(lines, warning),
          true);
    }

    CommandButton favourite(BuiltInCommand command, String name) {
      return new CommandButton(
          CommandButton.FAVOURITE,
          CommandButton.FAVOURITE_ICON,
          favourite,
          () -> CommandActions.favourite(command, name),
          true);
    }

    CommandButton edit(int index, CustomCommand command) {
      return new CommandButton(
          CommandButton.EDIT,
          CommandButton.ICON,
          edit,
          () -> {
            CommandActions.edit(index, command);
            return false;
          },
          true);
    }

    CommandButton delete(int index, CustomCommand command) {
      return new CommandButton(
          CommandButton.DELETE,
          CommandButton.ICON,
          delete,
          () -> {
            CommandActions.delete(index, command);
            return false;
          },
          true);
    }

    CommandButton add(boolean enabled) {
      return new CommandButton(
          CommandButton.ADD,
          CommandButton.ICON,
          enabled ? add : full,
          () -> {
            CommandActions.add();
            return false;
          },
          enabled);
    }
  }
}
