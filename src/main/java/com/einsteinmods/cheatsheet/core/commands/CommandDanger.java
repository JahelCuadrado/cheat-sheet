package com.einsteinmods.cheatsheet.core.commands;

import java.util.List;
import java.util.Set;

/**
 * Whether a command can destroy or remove things, judged from its text: {@code kill}, {@code
 * clear}, {@code clone} and {@code fill} (except a fill that only replaces air), also at the end of
 * an {@code execute ... run} chain. A safe guess for the player's own commands; built-in ones carry
 * their own flag, which must agree ({@code BuiltInCommandsTest}).
 */
public final class CommandDanger {
  private static final Set<String> DESTROYING = Set.of("kill", "clear", "clone", "fill");
  private static final String RUN = " run ";
  private static final String EXECUTE = "execute ";
  private static final String FILL = "fill";
  private static final String ONLY_AIR = " replace air";
  private static final char SPACE = ' ';

  private CommandDanger() {}

  /** Whether {@code command} (without its slash) can destroy or remove things. */
  public static boolean destroys(String command) {
    String last = command;
    if (last.startsWith(EXECUTE)) {
      int run = last.lastIndexOf(RUN);
      if (run >= 0) {
        last = last.substring(run + RUN.length());
      }
    }
    int space = last.indexOf(SPACE);
    String name = space < 0 ? last : last.substring(0, space);
    if (!DESTROYING.contains(name)) {
      return false;
    }
    return !name.equals(FILL) || !last.endsWith(ONLY_AIR);
  }

  /** Whether any of {@code commands} can destroy or remove things. */
  public static boolean anyDestroys(List<String> commands) {
    for (String command : commands) {
      if (destroys(command)) {
        return true;
      }
    }
    return false;
  }
}
