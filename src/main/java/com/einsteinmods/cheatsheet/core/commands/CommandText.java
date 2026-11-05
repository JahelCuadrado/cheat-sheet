package com.einsteinmods.cheatsheet.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cleans text the player typed (or hand-edited in the file) before it is kept: lines of allowed
 * characters, trimmed, within the game's limits. Whitelist first: any character the game would not
 * accept in chat (control characters, the section sign) is dropped, never escaped.
 */
public final class CommandText {
  /**
   * Longest command, without its slash: the chat box holds 256 characters, slash included, so a
   * saved command always fits when opened in the chat.
   */
  public static final int MAX_COMMAND_LENGTH = 255;

  /** Longest command name: it is shown on one line of a card. */
  public static final int MAX_NAME_LENGTH = 32;

  /** Most commands one entry (a built-in combo, a saved command) sends in a row. */
  public static final int MAX_LINES = 32;

  /** Most commands the player can save. */
  public static final int MAX_CUSTOM_COMMANDS = 100;

  private static final char SLASH = '/';

  /** The game's formatting code prefix; vanilla never accepts it in chat. */
  private static final char SECTION_SIGN = '§';

  private static final char DELETE = '\u007f';
  private static final char SPACE = ' ';

  private CommandText() {}

  /**
   * The command to keep: line breaks become spaces, other disallowed characters are dropped, the
   * text is trimmed and one leading slash removed; empty when nothing is left or it is longer than
   * {@link #MAX_COMMAND_LENGTH}.
   */
  public static Optional<String> sanitizeCommand(String raw) {
    String clean = cleanCommand(raw);
    if (clean.isEmpty() || clean.length() > MAX_COMMAND_LENGTH) {
      return Optional.empty();
    }
    return Optional.of(clean);
  }

  /**
   * The commands to keep, one per line of {@code raw}, each cleaned like {@link #sanitizeCommand};
   * blank lines (or a lone slash) are skipped. Empty when no command is left, when one is longer
   * than {@link #MAX_COMMAND_LENGTH} or when there are more than {@link #MAX_LINES}: nothing is cut
   * or dropped silently.
   */
  public static Optional<List<String>> sanitizeLines(String raw) {
    List<String> lines = new ArrayList<>();
    int start = 0;
    while (start <= raw.length()) {
      int end = nextLineBreak(raw, start);
      String clean = cleanCommand(raw.substring(start, end));
      if (clean.length() > MAX_COMMAND_LENGTH) {
        return Optional.empty();
      }
      if (!clean.isEmpty()) {
        if (lines.size() == MAX_LINES) {
          return Optional.empty();
        }
        lines.add(clean);
      }
      start = end + 1;
    }
    return lines.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(lines));
  }

  /**
   * The name to keep: cleaned like a command (no slash removed); empty when nothing is left or it
   * is longer than {@link #MAX_NAME_LENGTH}.
   */
  public static Optional<String> sanitizeName(String raw) {
    String clean = clean(raw);
    if (clean.isEmpty() || clean.length() > MAX_NAME_LENGTH) {
      return Optional.empty();
    }
    return Optional.of(clean);
  }

  /** Whether the game accepts {@code character} in chat. */
  public static boolean isAllowed(char character) {
    return character >= SPACE
        && character != DELETE
        && character != SECTION_SIGN
        && !Character.isISOControl(character);
  }

  /** Index of the first line break at or after {@code from}, or the length when there is none. */
  private static int nextLineBreak(String text, int from) {
    for (int index = from; index < text.length(); index++) {
      char character = text.charAt(index);
      if (character == '\n' || character == '\r') {
        return index;
      }
    }
    return text.length();
  }

  /** Cleaned, with one leading slash removed; may be empty or too long. */
  private static String cleanCommand(String raw) {
    String clean = clean(raw);
    if (!clean.isEmpty() && clean.charAt(0) == SLASH) {
      clean = clean.substring(1).strip();
    }
    return clean;
  }

  private static String clean(String raw) {
    StringBuilder clean = new StringBuilder(raw.length());
    for (int index = 0; index < raw.length(); index++) {
      char character = raw.charAt(index);
      if (character == '\n' || character == '\r' || character == '\t') {
        clean.append(SPACE);
      } else if (isAllowed(character)) {
        clean.append(character);
      }
    }
    return clean.toString().strip();
  }
}
