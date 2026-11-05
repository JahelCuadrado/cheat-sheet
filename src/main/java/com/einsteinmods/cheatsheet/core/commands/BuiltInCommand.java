package com.einsteinmods.cheatsheet.core.commands;

import java.util.List;

/**
 * A command the cheat sheet lists ready to use: one command, or a combo of several sent in order.
 *
 * @param key stable lower-case id: names its lang keys (name and one-line description)
 * @param lines the commands without the leading slash, in the order they run, with a sensible
 *     default for every argument (never translated: the game only understands them as written); at
 *     least one, at most {@link CommandText#MAX_LINES}
 * @param group the card it is listed on
 * @param dangerous whether running it can destroy or remove things (a big fill, killing entities,
 *     clearing an inventory): the cheat sheet asks before running it
 */
public record BuiltInCommand(
    String key, List<String> lines, CommandGroup group, boolean dangerous) {
  public BuiltInCommand {
    lines = List.copyOf(lines);
    if (lines.isEmpty() || lines.size() > CommandText.MAX_LINES) {
      throw new IllegalArgumentException("not 1 to " + CommandText.MAX_LINES + " lines: " + key);
    }
    for (String line : lines) {
      if (CommandText.sanitizeCommand(line).filter(line::equals).isEmpty()) {
        throw new IllegalArgumentException("not a clean command: " + line);
      }
    }
  }

  /** A single command. */
  public BuiltInCommand(String key, String command, CommandGroup group, boolean dangerous) {
    this(key, List.of(command), group, dangerous);
  }

  /** Whether it sends several commands: no "open in the chat", which holds one. */
  public boolean isCombo() {
    return lines.size() > 1;
  }
}
