package com.einsteinmods.cheatsheet.core.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CommandDangerTest {
  @Test
  void killClearCloneAndFillDestroy() {
    assertTrue(CommandDanger.destroys("kill @e[type=item]"));
    assertTrue(CommandDanger.destroys("kill"));
    assertTrue(CommandDanger.destroys("clear @s"));
    assertTrue(CommandDanger.destroys("clone ~ ~ ~ ~1 ~1 ~1 ~5 ~ ~"));
    assertTrue(CommandDanger.destroys("fill ~ ~ ~ ~5 ~5 ~5 air"));
    assertTrue(CommandDanger.destroys("execute as @e[type=zombie] run kill @s"));
  }

  @Test
  void otherCommandsAndAirOnlyFillsDoNot() {
    assertFalse(CommandDanger.destroys("fill ~-5 ~-1 ~-5 ~5 ~-1 ~5 barrier replace air"));
    assertFalse(CommandDanger.destroys("give @s diamond 64"));
    assertFalse(CommandDanger.destroys("killall"));
    assertFalse(CommandDanger.destroys("execute if entity @s[type=player]"));
    assertFalse(CommandDanger.destroys("say kill everyone"));
  }

  @Test
  void anyLineMakesTheWholeEntryDestroy() {
    assertTrue(CommandDanger.anyDestroys(List.of("time set day", "kill @e[type=item]")));
    assertFalse(CommandDanger.anyDestroys(List.of("time set day", "weather clear")));
  }
}
