package com.einsteinmods.cheatsheet.core.commands;

import static com.einsteinmods.cheatsheet.core.commands.CommandGroup.ADVANCED;
import static com.einsteinmods.cheatsheet.core.commands.CommandGroup.BASICS;
import static com.einsteinmods.cheatsheet.core.commands.CommandGroup.BOSSES;
import static com.einsteinmods.cheatsheet.core.commands.CommandGroup.BUILDING;
import static com.einsteinmods.cheatsheet.core.commands.CommandGroup.ITEMS;
import static com.einsteinmods.cheatsheet.core.commands.CommandGroup.KITS;
import static com.einsteinmods.cheatsheet.core.commands.CommandGroup.PLAYER;
import static com.einsteinmods.cheatsheet.core.commands.CommandGroup.WORLD;

import java.util.ArrayList;
import java.util.List;

/**
 * Every built-in command and popular combo, grouped and in display order. The combos are the ones
 * players repeat most in creative worlds, videos and tests (kits of maxed gear, a world set up for
 * building, ending the dragon fight, effect sets), gathered from command guides and the
 * r/MinecraftCommands FAQ. The syntax is the game's own (game rule ids, item components, text
 * components) and is checked against the game's command parser by {@code BuiltInCommandsGameTest},
 * part of {@code build}: a version that changes a command's syntax fails the build.
 */
public final class BuiltInCommands {
  private static final boolean DANGEROUS = true;
  private static final boolean SAFE = false;

  // Gear lines, shared by the single items and the kits.
  private static final String HELMET =
      "give @s netherite_helmet[enchantments={protection:4,respiration:3,"
          + "aqua_affinity:1,unbreaking:3,mending:1}]";
  private static final String CHESTPLATE =
      "give @s netherite_chestplate[enchantments={protection:4,unbreaking:3,mending:1}]";
  private static final String LEGGINGS =
      "give @s netherite_leggings[enchantments={protection:4,swift_sneak:3,"
          + "unbreaking:3,mending:1}]";
  private static final String BOOTS =
      "give @s netherite_boots[enchantments={protection:4,feather_falling:4,"
          + "depth_strider:3,soul_speed:3,unbreaking:3,mending:1}]";
  private static final String SWORD =
      "give @s netherite_sword[enchantments={sharpness:5,looting:3,fire_aspect:2,"
          + "knockback:2,sweeping_edge:3,unbreaking:3,mending:1}]";
  private static final String PICKAXE =
      "give @s netherite_pickaxe[enchantments={efficiency:5,fortune:3,unbreaking:3,mending:1}]";
  private static final String SILK_PICKAXE =
      "give @s netherite_pickaxe[enchantments={efficiency:5,silk_touch:1,unbreaking:3,mending:1}]";
  private static final String AXE =
      "give @s netherite_axe[enchantments={efficiency:5,sharpness:5,unbreaking:3,mending:1}]";
  private static final String SHOVEL =
      "give @s netherite_shovel[enchantments={efficiency:5,unbreaking:3,mending:1}]";
  private static final String HOE =
      "give @s netherite_hoe[enchantments={efficiency:5,fortune:3,unbreaking:3,mending:1}]";
  private static final String SHEARS =
      "give @s shears[enchantments={efficiency:5,unbreaking:3,mending:1}]";
  private static final String BOW =
      "give @s bow[enchantments={power:5,punch:2,flame:1,infinity:1,unbreaking:3}]";
  private static final String CROSSBOW =
      "give @s crossbow[enchantments={quick_charge:3,multishot:1,unbreaking:3,mending:1}]";
  private static final String ARROWS = "give @s arrow 64";
  private static final String TRIDENT =
      "give @s trident[enchantments={loyalty:3,impaling:5,channeling:1,unbreaking:3,mending:1}]";
  private static final String MACE =
      "give @s mace[enchantments={density:5,wind_burst:3,unbreaking:3,mending:1}]";
  private static final String SHIELD = "give @s shield[enchantments={unbreaking:3,mending:1}]";
  private static final String ELYTRA = "give @s elytra[enchantments={unbreaking:3,mending:1}]";
  private static final String ROCKETS = "give @s firework_rocket[fireworks={flight_duration:3}] 64";
  private static final String GOLDEN_CARROTS = "give @s golden_carrot 64";
  private static final String STEAK = "give @s cooked_beef 64";
  private static final String GOLDEN_APPLES = "give @s golden_apple 64";
  private static final String ENCHANTED_APPLES = "give @s enchanted_golden_apple 8";
  private static final String TOTEM = "give @s totem_of_undying";
  private static final String DEBUG_STICK = "give @s debug_stick";
  private static final String BARRIERS = "give @s barrier 64";
  private static final String LIGHTS = "give @s light[block_state={level:\"15\"}] 64";
  private static final String STRUCTURE_VOIDS = "give @s structure_void 64";

  // World lines, shared by the single rules and the world combos.
  private static final String DAY = "time set day";
  private static final String CLEAR_WEATHER = "weather clear";
  private static final String STOP_TIME = "gamerule advance_time false";
  private static final String STOP_WEATHER = "gamerule advance_weather false";
  private static final String KEEP_INVENTORY = "gamerule keep_inventory true";
  private static final String IMMEDIATE_RESPAWN = "gamerule immediate_respawn true";
  private static final String NO_MOB_GRIEFING = "gamerule mob_griefing false";
  private static final String NO_FIRE_SPREAD = "gamerule fire_spread_radius_around_player 0";
  private static final String NO_PHANTOMS = "gamerule spawn_phantoms false";
  private static final String NIGHT_VISION = "effect give @s night_vision infinite 0 true";
  private static final String KILL_ITEMS = "kill @e[type=item]";

  public static final List<BuiltInCommand> ALL =
      List.of(
          // Basics
          new BuiltInCommand("creative", "gamemode creative", BASICS, SAFE),
          new BuiltInCommand("survival", "gamemode survival", BASICS, SAFE),
          new BuiltInCommand("day", DAY, BASICS, SAFE),
          new BuiltInCommand("night", "time set night", BASICS, SAFE),
          new BuiltInCommand("clear_weather", CLEAR_WEATHER, BASICS, SAFE),
          new BuiltInCommand("teleport", "tp @s 0 100 0", BASICS, SAFE),
          new BuiltInCommand("give", "give @s diamond 64", BASICS, SAFE),
          new BuiltInCommand("night_vision", NIGHT_VISION, BASICS, SAFE),
          new BuiltInCommand("regeneration", "effect give @s regeneration 60 4 true", BASICS, SAFE),
          new BuiltInCommand("clear_effects", "effect clear @s", BASICS, SAFE),
          new BuiltInCommand("levels", "xp add @s 30 levels", BASICS, SAFE),
          new BuiltInCommand("difficulty", "difficulty peaceful", BASICS, DANGEROUS),
          new BuiltInCommand("spawnpoint", "spawnpoint @s ~ ~ ~", BASICS, SAFE),
          new BuiltInCommand("keep_inventory", KEEP_INVENTORY, BASICS, SAFE),
          new BuiltInCommand("stop_daylight", STOP_TIME, BASICS, SAFE),
          new BuiltInCommand("stop_weather", STOP_WEATHER, BASICS, SAFE),
          new BuiltInCommand("no_mob_griefing", NO_MOB_GRIEFING, BASICS, SAFE),
          new BuiltInCommand("no_fire_spread", NO_FIRE_SPREAD, BASICS, SAFE),
          new BuiltInCommand("no_phantoms", NO_PHANTOMS, BASICS, SAFE),
          new BuiltInCommand("immediate_respawn", IMMEDIATE_RESPAWN, BASICS, SAFE),
          new BuiltInCommand("clear_inventory", "clear @s", BASICS, DANGEROUS),
          // Building
          new BuiltInCommand("fill", "fill ~1 ~ ~1 ~10 ~4 ~10 stone", BUILDING, DANGEROUS),
          new BuiltInCommand(
              "fill_hollow", "fill ~-4 ~-1 ~-4 ~4 ~4 ~4 glass hollow", BUILDING, DANGEROUS),
          new BuiltInCommand(
              "fill_outline",
              "fill ~-4 ~-1 ~-4 ~4 ~4 ~4 stone_bricks outline",
              BUILDING,
              DANGEROUS),
          new BuiltInCommand(
              "fill_replace",
              "fill ~-10 ~-5 ~-10 ~10 ~5 ~10 glass replace stone",
              BUILDING,
              DANGEROUS),
          new BuiltInCommand(
              "fill_destroy", "fill ~-3 ~ ~-3 ~3 ~3 ~3 air destroy", BUILDING, DANGEROUS),
          new BuiltInCommand("flatten", "fill ~-15 ~ ~-15 ~15 ~20 ~15 air", BUILDING, DANGEROUS),
          new BuiltInCommand(
              "remove_water",
              "fill ~-15 ~-10 ~-15 ~15 ~10 ~15 air replace water",
              BUILDING,
              DANGEROUS),
          new BuiltInCommand(
              "remove_lava",
              "fill ~-15 ~-10 ~-15 ~15 ~10 ~15 air replace lava",
              BUILDING,
              DANGEROUS),
          new BuiltInCommand(
              "clone", "clone ~-4 ~-1 ~-4 ~4 ~4 ~4 ~10 ~-1 ~-4", BUILDING, DANGEROUS),
          new BuiltInCommand("setblock", "setblock ~ ~-1 ~ diamond_block", BUILDING, DANGEROUS),
          new BuiltInCommand(
              "barrier_floor", "fill ~-5 ~-1 ~-5 ~5 ~-1 ~5 barrier replace air", BUILDING, SAFE),
          new BuiltInCommand("world_border", "worldborder set 1000", BUILDING, SAFE),
          // Special items
          new BuiltInCommand("sword", SWORD, ITEMS, SAFE),
          new BuiltInCommand("pickaxe", PICKAXE, ITEMS, SAFE),
          new BuiltInCommand("helmet", HELMET, ITEMS, SAFE),
          new BuiltInCommand("chestplate", CHESTPLATE, ITEMS, SAFE),
          new BuiltInCommand("leggings", LEGGINGS, ITEMS, SAFE),
          new BuiltInCommand("boots", BOOTS, ITEMS, SAFE),
          new BuiltInCommand("mace", MACE, ITEMS, SAFE),
          new BuiltInCommand("rockets", ROCKETS, ITEMS, SAFE),
          new BuiltInCommand(
              "player_head", "give @s player_head[profile={name:\"Notch\"}]", ITEMS, SAFE),
          new BuiltInCommand(
              "potion",
              "give @s potion[potion_contents={custom_color:16733695,custom_effects:["
                  + "{id:\"speed\",amplifier:1,duration:6000},"
                  + "{id:\"jump_boost\",amplifier:1,duration:6000},"
                  + "{id:\"regeneration\",amplifier:0,duration:6000}]}]",
              ITEMS,
              SAFE),
          new BuiltInCommand("command_block", "give @s command_block", ITEMS, SAFE),
          new BuiltInCommand("debug_stick", DEBUG_STICK, ITEMS, SAFE),
          new BuiltInCommand("barrier", BARRIERS, ITEMS, SAFE),
          new BuiltInCommand("light", LIGHTS, ITEMS, SAFE),
          new BuiltInCommand("structure_void", STRUCTURE_VOIDS, ITEMS, SAFE),
          // Advanced
          new BuiltInCommand(
              "locate_village", "locate structure #minecraft:village", ADVANCED, SAFE),
          new BuiltInCommand("locate_biome", "locate biome cherry_grove", ADVANCED, SAFE),
          new BuiltInCommand(
              "go_nether",
              "execute in minecraft:the_nether run spreadplayers ~ ~ 0 16 under 120 false @s",
              ADVANCED,
              SAFE),
          new BuiltInCommand(
              "go_end",
              "execute in minecraft:the_end run spreadplayers 0 20 0 10 false @s",
              ADVANCED,
              SAFE),
          new BuiltInCommand(
              "go_overworld",
              "execute in minecraft:overworld run spreadplayers ~ ~ 0 16 false @s",
              ADVANCED,
              SAFE),
          new BuiltInCommand(
              "gold_under_players",
              "execute at @a run setblock ~ ~-1 ~ gold_block",
              ADVANCED,
              DANGEROUS),
          new BuiltInCommand("kill_items", KILL_ITEMS, ADVANCED, DANGEROUS),
          new BuiltInCommand(
              "kill_monsters",
              "execute as @e[distance=..64] if predicate {type:entity_properties,"
                  + "entity:this,predicate:{entity_type:[zombie,husk,drowned,skeleton,stray,creeper,"
                  + "spider,cave_spider,witch,slime,phantom,pillager]}} unless data entity @s"
                  + " CustomName run kill @s",
              ADVANCED,
              DANGEROUS),
          new BuiltInCommand(
              "frozen_mob", "summon zombie ~ ~ ~ {NoAI:1b,PersistenceRequired:1b}", ADVANCED, SAFE),
          new BuiltInCommand(
              "invisible_stand",
              "summon armor_stand ~ ~ ~ {Invisible:1b,ShowArms:1b,NoBasePlate:1b}",
              ADVANCED,
              SAFE),
          new BuiltInCommand(
              "villager_trades",
              "summon villager ~ ~ ~ {NoAI:1b,Offers:{Recipes:["
                  + "{buy:{id:\"emerald\",count:1},sell:{id:\"diamond\",count:1},maxUses:9999},"
                  + "{buy:{id:\"dirt\",count:1},sell:{id:\"emerald\",count:1},maxUses:9999}]}}",
              ADVANCED,
              SAFE),
          new BuiltInCommand(
              "tellraw", "tellraw @a {text:\"Hello!\",color:\"gold\",bold:true}", ADVANCED, SAFE),
          new BuiltInCommand("seed", "seed", ADVANCED, SAFE),
          new BuiltInCommand(
              "mob_health", "data get entity @n[type=!player,type=!item] Health", ADVANCED, SAFE),
          new BuiltInCommand("particles", "particle heart ~ ~2 ~ 0.5 0.5 0.5 0 20", ADVANCED, SAFE),
          new BuiltInCommand(
              "sound", "playsound minecraft:entity.player.levelup master @s", ADVANCED, SAFE),
          new BuiltInCommand("lightning", "summon lightning_bolt ^ ^ ^5", ADVANCED, DANGEROUS),
          new BuiltInCommand("enchant", "enchant @s sharpness 5", ADVANCED, SAFE),
          new BuiltInCommand("fast_growth", "gamerule random_tick_speed 100", ADVANCED, SAFE),
          // Popular combos: kits
          combo(
              "full_kit",
              KITS,
              SAFE,
              HELMET,
              CHESTPLATE,
              LEGGINGS,
              BOOTS,
              SWORD,
              PICKAXE,
              AXE,
              SHOVEL,
              BOW,
              ARROWS,
              SHIELD,
              GOLDEN_CARROTS,
              GOLDEN_APPLES,
              TOTEM,
              ELYTRA,
              ROCKETS),
          combo("armour_kit", KITS, SAFE, HELMET, CHESTPLATE, LEGGINGS, BOOTS),
          combo("tools_kit", KITS, SAFE, PICKAXE, SILK_PICKAXE, AXE, SHOVEL, HOE, SHEARS),
          combo("weapons_kit", KITS, SAFE, SWORD, BOW, CROSSBOW, ARROWS, TRIDENT, MACE, SHIELD),
          combo(
              "food_kit",
              KITS,
              SAFE,
              GOLDEN_CARROTS,
              STEAK,
              GOLDEN_APPLES,
              ENCHANTED_APPLES,
              TOTEM),
          combo("elytra_kit", KITS, SAFE, ELYTRA, ROCKETS),
          combo("builder_kit", KITS, SAFE, DEBUG_STICK, LIGHTS, BARRIERS, STRUCTURE_VOIDS),
          // Popular combos: world setup
          combo(
              "build_world",
              WORLD,
              SAFE,
              DAY,
              STOP_TIME,
              CLEAR_WEATHER,
              STOP_WEATHER,
              "gamerule spawn_mobs false"),
          combo(
              "friendly_rules",
              WORLD,
              SAFE,
              KEEP_INVENTORY,
              IMMEDIATE_RESPAWN,
              NO_MOB_GRIEFING,
              NO_FIRE_SPREAD,
              NO_PHANTOMS),
          combo(
              "quiet_commands",
              WORLD,
              SAFE,
              "gamerule send_command_feedback false",
              "gamerule command_block_output false",
              "gamerule log_admin_commands false"),
          combo(
              "flat_area",
              WORLD,
              DANGEROUS,
              "fill ~-15 ~ ~-15 ~15 ~20 ~15 air",
              "fill ~-15 ~-1 ~-15 ~15 ~-1 ~15 grass_block"),
          combo(
              "default_rules",
              WORLD,
              SAFE,
              "gamerule advance_time true",
              "gamerule advance_weather true",
              "gamerule spawn_mobs true",
              "gamerule keep_inventory false",
              "gamerule immediate_respawn false",
              "gamerule mob_griefing true",
              "gamerule fire_spread_radius_around_player 128",
              "gamerule spawn_phantoms true",
              "gamerule send_command_feedback true",
              "gamerule command_block_output true",
              "gamerule log_admin_commands true"),
          // Popular combos: bosses and clean-up
          combo(
              "kill_dragon",
              BOSSES,
              DANGEROUS,
              "kill @e[type=end_crystal]",
              "kill @e[type=ender_dragon]"),
          combo(
              "kill_wither",
              BOSSES,
              DANGEROUS,
              "kill @e[type=wither]",
              "kill @e[type=wither_skull]"),
          combo(
              "clean_ground",
              BOSSES,
              DANGEROUS,
              KILL_ITEMS,
              "kill @e[type=experience_orb]",
              "kill @e[type=arrow]",
              "kill @e[type=spectral_arrow]"),
          combo(
              "reset_player",
              BOSSES,
              DANGEROUS,
              "clear @s",
              "effect clear @s",
              "xp set @s 0 levels",
              "xp set @s 0 points",
              "gamemode survival"),
          // Popular combos: player effects
          combo(
              "invincible",
              PLAYER,
              SAFE,
              "effect give @s resistance infinite 4 true",
              "effect give @s regeneration infinite 4 true",
              "effect give @s fire_resistance infinite 0 true",
              "effect give @s water_breathing infinite 0 true",
              "effect give @s saturation infinite 0 true"),
          combo("miner", PLAYER, SAFE, "effect give @s haste infinite 1 true", NIGHT_VISION),
          combo(
              "explorer",
              PLAYER,
              SAFE,
              "effect give @s speed infinite 1 true",
              "effect give @s jump_boost infinite 1 true",
              NIGHT_VISION),
          combo(
              "underwater",
              PLAYER,
              SAFE,
              "effect give @s water_breathing infinite 0 true",
              "effect give @s dolphins_grace infinite 0 true",
              "effect give @s conduit_power infinite 0 true",
              NIGHT_VISION),
          combo(
              "recording",
              PLAYER,
              SAFE,
              "gamemode spectator",
              NIGHT_VISION,
              "time set noon",
              STOP_TIME,
              CLEAR_WEATHER,
              STOP_WEATHER));

  private BuiltInCommands() {}

  private static BuiltInCommand combo(
      String key, CommandGroup group, boolean dangerous, String... lines) {
    return new BuiltInCommand(key, List.of(lines), group, dangerous);
  }

  /** The commands of {@code group}, in display order. */
  public static List<BuiltInCommand> of(CommandGroup group) {
    List<BuiltInCommand> commands = new ArrayList<>();
    for (BuiltInCommand command : ALL) {
      if (command.group() == group) {
        commands.add(command);
      }
    }
    return List.copyOf(commands);
  }
}
