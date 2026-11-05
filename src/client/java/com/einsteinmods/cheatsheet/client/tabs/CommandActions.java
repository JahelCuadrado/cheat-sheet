package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.client.CheatSheetClient;
import com.einsteinmods.cheatsheet.client.CheatSheetScreen;
import com.einsteinmods.cheatsheet.client.config.ClientSettings;
import com.einsteinmods.cheatsheet.client.config.CustomCommandStore;
import com.einsteinmods.cheatsheet.core.commands.BuiltInCommand;
import com.einsteinmods.cheatsheet.core.commands.CommandText;
import com.einsteinmods.cheatsheet.core.commands.CustomCommand;
import com.einsteinmods.cheatsheet.core.commands.CustomCommands;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * What the Commands tab's buttons do. Screens opened from the cheat sheet (a confirmation, the
 * form) return to it; the chat replaces it, as the chat key would. Commands are sent like typed in
 * the chat ({@code ClientPacketListener#sendCommand}): the server checks the permission, and its
 * answer shows in the chat. Several commands go one after the other in the same tick, and only for
 * a player with game master permission: the server kicks anyone else who sends ten commands within
 * a few seconds ({@code ServerGamePacketListenerImpl#detectCommandRateSpam} spares operators and
 * the single player host only).
 */
final class CommandActions {
  private static final String SLASH = "/";
  private static final String LINE_BREAK = "\n";
  private static final String PARAGRAPH = "\n\n";

  /** How long a star keeps its green tick across the rebuild its save causes. */
  private static final long FAVOURITE_TICK_MILLIS = 1200L;

  private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId();

  /** The built-in entry last added to My commands and until when its star shows the tick. */
  private static @Nullable String favouritedKey;

  private static long favouritedUntil;

  private CommandActions() {}

  /** Opens the chat with the command typed in, to edit before sending. */
  static void openInChat(String command) {
    Minecraft.getInstance().gui.setScreen(new ChatScreen(SLASH + command, false));
  }

  /** Puts the commands, each with its slash, one per line, on the clipboard. */
  static boolean copy(List<String> lines) {
    Minecraft.getInstance().keyboardHandler.setClipboard(withSlashes(lines));
    return true;
  }

  /**
   * Sends the commands to the server in order, as if typed in the chat; several only with game
   * master permission (else a toast says why and nothing is sent). Whether they were sent.
   */
  static boolean run(List<String> lines) {
    if (!maySend(lines)) {
      return false;
    }
    ClientPacketListener connection = Minecraft.getInstance().getConnection();
    if (connection == null) {
      return false;
    }
    for (String line : lines) {
      connection.sendCommand(line);
    }
    return true;
  }

  /** Asks first (vanilla's confirmation screen), then sends; either way back to the cheat sheet. */
  static boolean confirmRun(List<String> lines, Component warning) {
    if (!maySend(lines)) {
      return false;
    }
    Minecraft minecraft = Minecraft.getInstance();
    Screen back = minecraft.gui.screen();
    minecraft.gui.setScreen(
        new ConfirmScreen(
            run -> {
              if (run) {
                run(lines);
              }
              minecraft.gui.setScreen(back);
            },
            text("confirm.run"),
            message(warning, lines)));
    return false;
  }

  /**
   * Adds the built-in entry, all its lines, to My commands under its shown name (cut to the name
   * limit), and shows the tab again with it; a toast instead when it is already there or the list
   * is full. Whether it was added.
   */
  static boolean favourite(BuiltInCommand command, String name) {
    CustomCommandStore store = CheatSheetClient.customCommands();
    CustomCommands saved = store.commands();
    if (saved.containsLines(command.lines())) {
      toast(text("favourite.already"));
      return false;
    }
    if (saved.isFull()) {
      toast(text("favourite.full", CommandText.MAX_CUSTOM_COMMANDS));
      return false;
    }
    String shortName =
        name.length() > CommandText.MAX_NAME_LENGTH
            ? name.substring(0, CommandText.MAX_NAME_LENGTH)
            : name;
    Optional<CustomCommand> entry =
        CustomCommand.of(shortName, String.join(LINE_BREAK, command.lines()));
    if (entry.isEmpty()) {
      return false;
    }
    store.update(saved.with(entry.get()));
    favouritedKey = command.key();
    favouritedUntil = Util.getMillis() + FAVOURITE_TICK_MILLIS;
    rebuild(Minecraft.getInstance().gui.screen());
    return true;
  }

  /** Whether the star of built-in entry {@code key} still shows its tick after a rebuild. */
  static boolean recentlyFavourited(String key) {
    return key.equals(favouritedKey) && Util.getMillis() < favouritedUntil;
  }

  /** Opens the form for a new command. */
  static void add() {
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.gui.setScreen(new CommandFormScreen(minecraft.gui.screen(), -1, null));
  }

  /** Opens the form on saved command {@code index}. */
  static void edit(int index, CustomCommand command) {
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.gui.setScreen(new CommandFormScreen(minecraft.gui.screen(), index, command));
  }

  /**
   * Deletes saved command {@code index} and shows the cheat sheet again, asking first unless the
   * player said not to. "Don't ask me again" counts only together with Yes.
   */
  static void delete(int index, CustomCommand command) {
    Minecraft minecraft = Minecraft.getInstance();
    Screen back = minecraft.gui.screen();
    ClientSettings settings = CheatSheetClient.settings();
    if (!settings.preferences().confirmCommandDelete()) {
      deleteNow(index, back);
      return;
    }
    minecraft.gui.setScreen(
        new DeleteConfirmScreen(
            (delete, dontAskAgain) -> {
              if (delete) {
                if (dontAskAgain) {
                  settings.update(settings.preferences().withConfirmCommandDelete(false));
                }
                deleteNow(index, back);
              }
              minecraft.gui.setScreen(back);
            },
            text("confirm.delete"),
            message(Component.literal(command.name()), command.lines()),
            text("confirm.delete.dont_ask")));
  }

  /** Saves {@code changed} and shows {@code back} again, rebuilt if it is the cheat sheet. */
  static void save(@Nullable Screen back, CustomCommands changed) {
    CheatSheetClient.customCommands().update(changed);
    rebuild(back);
    Minecraft.getInstance().gui.setScreen(back);
  }

  /** A text of the tab: {@code cheatsheet.tab.commands.<key>}. */
  static Component text(String key, Object... args) {
    return Component.translatable("cheatsheet.tab." + CommandsSheet.ID + "." + key, args);
  }

  private static void deleteNow(int index, @Nullable Screen back) {
    CustomCommandStore store = CheatSheetClient.customCommands();
    store.update(store.commands().without(index));
    rebuild(back);
  }

  private static void rebuild(@Nullable Screen back) {
    if (back instanceof CheatSheetScreen cheatSheet) {
      cheatSheet.rebuildTab(CommandsSheet.ID);
    }
  }

  /** Whether {@code lines} may be sent now: one always, several with game master permission. */
  private static boolean maySend(List<String> lines) {
    if (lines.size() == 1) {
      return true;
    }
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
      return true;
    }
    toast(text("combo.permission"));
    return false;
  }

  private static void toast(Component message) {
    SystemToast.addOrUpdate(
        Minecraft.getInstance().gui.toastManager(),
        TOAST,
        Component.translatable("cheatsheet.tab." + CommandsSheet.ID),
        message);
  }

  private static String withSlashes(List<String> lines) {
    StringBuilder text = new StringBuilder();
    for (String line : lines) {
      if (!text.isEmpty()) {
        text.append(LINE_BREAK);
      }
      text.append(SLASH).append(line);
    }
    return text.toString();
  }

  private static Component message(Component lead, List<String> lines) {
    return Component.empty()
        .append(lead)
        .append(PARAGRAPH)
        .append(Component.literal(withSlashes(lines)).withStyle(ChatFormatting.YELLOW));
  }
}
