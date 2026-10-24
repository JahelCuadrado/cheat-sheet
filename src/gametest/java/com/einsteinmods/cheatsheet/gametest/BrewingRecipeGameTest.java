package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.potions.BrewingMix;
import com.einsteinmods.cheatsheet.core.potions.BrewingTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.PotionsPredicate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The cheat sheet's fixed brewing table ({@link BrewingTable}) against the server's brewing recipes
 * ({@code BrewingRecipe}: input, reagent, output): a Minecraft update that adds, removes or changes
 * a mix fails the build here, so the table (and the Potions tab) gets updated with it.
 *
 * <p>Vanilla writes every mix three times (drinkable, splash, lingering potion); each copy must
 * match the table. Gunpowder must turn every potion of the table into its splash version and
 * dragon's breath every splash potion into its lingering version.
 */
public final class BrewingRecipeGameTest {
  @GameTest
  public void brewingTableMatchesTheGame(GameTestHelper helper) {
    Set<String> expectedMixes = new TreeSet<>();
    for (BrewingMix mix : BrewingTable.MIXES) {
      expectedMixes.add(describe(mix.input(), mix.reagent(), mix.output()));
    }
    Map<String, Set<String>> mixesByContainer = new HashMap<>();
    Set<String> splashed = new TreeSet<>();
    Set<String> lingered = new TreeSet<>();
    List<String> mismatches = new ArrayList<>();
    for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
      Recipe<?> recipe = holder.value();
      if (!(recipe instanceof BrewingRecipe brewing)) {
        continue;
      }
      String recipeId = holder.id().identifier().toString();
      String inputItem = singleItem(brewing.getInput(), recipeId, mismatches);
      String reagent = singleItem(brewing.getReagent(), recipeId, mismatches);
      String outputItem = path(brewing.getOutput().item());
      String output = outputPotion(brewing);
      for (String input : inputPotions(brewing.getInput(), recipeId, mismatches)) {
        if (inputItem.equals(outputItem)) {
          mixesByContainer
              .computeIfAbsent(inputItem, container -> new TreeSet<>())
              .add(describe(input, reagent, output));
        } else if (inputItem.equals(BrewingTable.POTION_ITEM)
            && outputItem.equals(BrewingTable.SPLASH_POTION_ITEM)
            && reagent.equals(BrewingTable.GUNPOWDER)
            && input.equals(output)) {
          splashed.add(input);
        } else if (inputItem.equals(BrewingTable.SPLASH_POTION_ITEM)
            && outputItem.equals(BrewingTable.LINGERING_POTION_ITEM)
            && reagent.equals(BrewingTable.DRAGON_BREATH)
            && input.equals(output)) {
          lingered.add(input);
        } else {
          mismatches.add(
              recipeId + ": unexpected container change " + inputItem + " -> " + outputItem);
        }
      }
    }
    for (String container :
        List.of(
            BrewingTable.POTION_ITEM,
            BrewingTable.SPLASH_POTION_ITEM,
            BrewingTable.LINGERING_POTION_ITEM)) {
      compare(container + " mixes", expectedMixes, mixesByContainer.get(container), mismatches);
    }
    Set<String> potions = new TreeSet<>(BrewingTable.potions());
    compare("gunpowder (splash)", potions, splashed, mismatches);
    compare("dragon's breath (lingering)", potions, lingered, mismatches);
    helper.assertTrue(
        mismatches.isEmpty(),
        "brewing table differs from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  private static void compare(
      String what, Set<String> table, Set<String> game, List<String> mismatches) {
    Set<String> found = game == null ? Set.of() : game;
    Set<String> missing = new TreeSet<>(found);
    missing.removeAll(table);
    Set<String> extra = new TreeSet<>(table);
    extra.removeAll(found);
    if (!missing.isEmpty()) {
      mismatches.add(what + ": in the game, not in the table: " + missing);
    }
    if (!extra.isEmpty()) {
      mismatches.add(what + ": in the table, not in the game: " + extra);
    }
  }

  private static String singleItem(
      PotionIngredient ingredient, String recipeId, List<String> mismatches) {
    Optional<Holder<Item>> item = ingredient.ingredient().getSingleItem();
    if (item.isEmpty()) {
      mismatches.add(recipeId + ": expected one item");
      return "none";
    }
    return path(item.get());
  }

  private static Set<String> inputPotions(
      PotionIngredient ingredient, String recipeId, List<String> mismatches) {
    Optional<HolderSet<Potion>> potions = ingredient.potions().flatMap(PotionsPredicate::potions);
    if (potions.isEmpty()) {
      mismatches.add(recipeId + ": input without potion");
      return Set.of();
    }
    Set<String> ids = new HashSet<>();
    for (Holder<Potion> potion : potions.get()) {
      ids.add(path(potion));
    }
    return ids;
  }

  private static String outputPotion(BrewingRecipe recipe) {
    PotionContents contents =
        recipe
            .getOutput()
            .create()
            .getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    return contents.potion().map(BrewingRecipeGameTest::path).orElse("none");
  }

  private static String describe(String input, String reagent, String output) {
    return input + " + " + reagent + " -> " + output;
  }

  private static String path(Holder<?> holder) {
    return holder.unwrapKey().orElseThrow().identifier().getPath();
  }
}
