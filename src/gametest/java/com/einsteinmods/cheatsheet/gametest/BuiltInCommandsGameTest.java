package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.commands.BuiltInCommand;
import com.einsteinmods.cheatsheet.core.commands.BuiltInCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.phys.Vec3;

/**
 * Every line of every built-in command and combo of the cheat sheet ({@link BuiltInCommands})
 * parses with the game's own command dispatcher, as an operator (permission level 4) standing in
 * the test structure: no syntax error, nothing left unread, and it ends on a command that runs.
 * Parsing resolves what the parser checks (game rule names, items and their components, block
 * states, inline predicates, text components); summon data and structure ids are only checked when
 * a command runs.
 */
public final class BuiltInCommandsGameTest {
  @GameTest
  public void everyBuiltInCommandParses(GameTestHelper helper) {
    CommandDispatcher<CommandSourceStack> dispatcher =
        helper.getLevel().getServer().getCommands().getDispatcher();
    CommandSourceStack source =
        helper
            .getLevel()
            .getServer()
            .createCommandSourceStack()
            .withLevel(helper.getLevel())
            .withPosition(Vec3.atBottomCenterOf(helper.absolutePos(BlockPos.ZERO)))
            .withPermission(LevelBasedPermissionSet.OWNER)
            .withSuppressedOutput();
    List<String> failures = new ArrayList<>();
    for (BuiltInCommand command : BuiltInCommands.ALL) {
      for (String line : command.lines()) {
        String problem = problem(dispatcher.parse(line, source));
        if (problem != null) {
          failures.add(command.key() + " (" + line + "): " + problem);
        }
      }
    }
    helper.assertTrue(
        failures.isEmpty(),
        "built-in commands that no longer parse:\n" + String.join("\n", failures));
    helper.succeed();
  }

  /** What is wrong with the parse, as vanilla's own check sees it, or null when it is valid. */
  private static String problem(ParseResults<CommandSourceStack> parse) {
    Map<?, CommandSyntaxException> exceptions = parse.getExceptions();
    if (!exceptions.isEmpty()) {
      return exceptions.values().iterator().next().getMessage();
    }
    if (parse.getReader().canRead()) {
      return "unknown or extra input at '" + parse.getReader().getRemaining() + "'";
    }
    if (parse.getContext().getLastChild().getCommand() == null) {
      return "incomplete command";
    }
    return null;
  }
}
