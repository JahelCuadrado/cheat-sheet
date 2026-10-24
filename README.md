# Cheat Sheet

Everything you keep looking up on the wiki, one click away in the pause menu: ore heights,
enchantments, potions, armor trims, villager trades, farms and more, drawn as at-a-glance sheets.

Alt-tabbing to the wiki to check the best height for diamonds, which potion needs which
ingredient or where to find that trim? Open the cheat sheet instead and get back to playing.

Fabric mod for Minecraft 26.3. Client only: works on any server.

## Features
- **One click away:** a book button in the pause menu opens it, or bind a key to open it while
  playing.
- **Ores:** where every ore generates and the best height to dig at, as height charts for the
  Overworld and the Nether, with a line at your current height. Biome bonuses at a glance, each
  with its biome in miniature: extra gold in badlands, emeralds in mountains, large copper veins in
  dripstone caves, double gold and quartz in basalt deltas, where ancient debris hides.
- **Enchantments:** every enchantment on the gear it goes on (armor, weapons, tools, bow, trident,
  mace…) with its highest level; treasure ones and curses stand out. Hover for what it does, what
  it can't be combined with and where to get it. Plus an anvil guide: how to put many books on one
  tool, step by step, without hitting "Too Expensive!". Datapack and modded enchantments show up
  too.
- **Potions:** a brewing chart: water + nether wart to start, the ingredient for every potion,
  which ones redstone makes longer and glowstone stronger, what a fermented spider eye turns them
  into, and how to make splash and lingering potions. Hover a potion for its effects and durations.
- **Armor trims:** where to find each design, shown on a full netherite set worn by an armor stand
  next to its template and the structure drawn in miniature, plus how to copy it. And a table of
  every design in every material, so you can pick your look before you go hunting.
- **Music discs:** every disc by where you get it, with its length and comparator signal. Each
  place is shown in miniature and each mob drawn: a creeper killed by a skeleton, a ghast hit by
  its own fireball, a chicken jockey.
- **Dyes:** all 16 dyes with the flowers and items they come from and the dye mixes that make them.
- **Villagers:** every trade of every profession, level by level, with the villager in its work
  clothes, its workstation and how to craft it. Plus the wandering trader with his llamas and piglin
  bartering odds.
- **Food:** every food from most to least filling, with drumsticks and saturation, the best picks
  highlighted and side effects marked. Modded foods show up too.
- **Beacon & Conduit:** blocks needed for each pyramid level, range and effects unlocked, what to
  build and pay with. A step-by-step conduit tutorial: its recipe, where its materials come from
  and how to build its frame, from the minimum that turns it on to the full frame.
- **Farming:** the most efficient sugar cane layouts, how far water reaches on farmland, why
  alternate rows grow faster, pumpkins and melons, cactus. What each animal breeds with, where to
  find it and how to get it home, and its variants (wolves, cats, frogs, cows, sheep, horses…) with
  the biomes they come from.
- **Click a card to zoom:** any card opens bigger right where it is, with more to read; hover inside
  for details.
- **Pictures, not lists:** big item icons, big numbers and charts; hover anything for details.
- **Crisp at any GUI scale:** the sheet has its own size, apart from your GUI scale; on Auto it is
  drawn as big as it can be while the whole tab still fits on screen, pixel-sharp.
- **Simple settings screen** with no extra libraries (Mod Menu supported): hide the pause menu
  button, choose the tab it opens on, how big the sheet is drawn and its key.
- **Lightweight:** does nothing while closed; mobs and places are drawn once when a tab opens, not
  every frame. Uses your resource pack's textures and models.
- **Languages:** English, Español (España), Español (México), Português (Brasil), Português (Portugal),
  Français, Deutsch, Italiano, Nederlands, Polski, Türkçe, Русский, Українська, 简体中文, 繁體中文,
  日本語 and 한국어.

## Requirements

Minecraft 26.3 · Fabric Loader 0.19.5+ · Fabric API · Java 25. Optional: Mod Menu, for the
settings screen.

## Development

Requires JDK 25. `./gradlew runClient` · `./gradlew build` (compile, format check, JUnit, GameTests)
· `./gradlew spotlessApply` · `./gradlew runClientGameTest` (screenshots of the sheets, not part of
`build`).

## License

MIT.
