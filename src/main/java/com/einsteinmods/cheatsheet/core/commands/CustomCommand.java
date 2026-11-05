package com.einsteinmods.cheatsheet.core.commands;

import java.util.List;
import java.util.Optional;

/**
 * A command the player saved, already cleaned ({@link CommandText}): one command, or several sent
 * in order.
 *
 * @param name what the card shows
 * @param lines the commands without their leading slash, in the order they run; at least one, at
 *     most {@link CommandText#MAX_LINES}
 */
public record CustomCommand(String name, List<String> lines) {
  public CustomCommand {
    lines = List.copyOf(lines);
    if (CommandText.sanitizeName(name).filter(name::equals).isEmpty()
        || CommandText.sanitizeLines(String.join("\n", lines)).filter(lines::equals).isEmpty()) {
      throw new IllegalArgumentException("not a clean command: " + name + " = " + lines);
    }
  }

  /** A single command. */
  public CustomCommand(String name, String command) {
    this(name, List.of(command));
  }

  /**
   * The cleaned command, or empty when either text is unusable once cleaned; {@code rawLines} has
   * one command per line ({@link CommandText#sanitizeLines}).
   */
  public static Optional<CustomCommand> of(String rawName, String rawLines) {
    Optional<String> name = CommandText.sanitizeName(rawName);
    Optional<List<String>> lines = CommandText.sanitizeLines(rawLines);
    if (name.isEmpty() || lines.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new CustomCommand(name.get(), lines.get()));
  }

  /** Whether it sends several commands. */
  public boolean isCombo() {
    return lines.size() > 1;
  }
}
