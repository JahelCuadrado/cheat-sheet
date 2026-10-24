package com.einsteinmods.cheatsheet.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The running game's recipes as JSON ({@code Recipe.DIRECT_CODEC}), the same shape as vanilla's
 * data files: {@code type}, {@code ingredients} / {@code ingredient} / {@code key} + {@code
 * pattern}, and a {@code result} with {@code id} and {@code count} (omitted when 1).
 */
final class RecipeJson {
  static final String TYPE = "type";
  static final String SHAPELESS = "minecraft:crafting_shapeless";
  static final String SHAPED = "minecraft:crafting_shaped";
  static final String SMELTING = "minecraft:smelting";
  private static final String RESULT = "result";
  private static final String ID = "id";
  private static final String COUNT = "count";

  private RecipeJson() {}

  static List<JsonObject> all(MinecraftServer server) {
    RegistryOps<JsonElement> ops =
        server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
    List<JsonObject> recipes = new ArrayList<>();
    for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
      recipes.add(
          Recipe.DIRECT_CODEC.encodeStart(ops, holder.value()).getOrThrow().getAsJsonObject());
    }
    return recipes;
  }

  /** Item id of the result, or "" for recipes without a plain item result (special crafting). */
  static String resultId(JsonObject recipe) {
    if (!recipe.has(RESULT)) {
      return "";
    }
    JsonElement result = recipe.get(RESULT);
    if (result.isJsonPrimitive()) {
      return result.getAsString();
    }
    JsonObject object = result.getAsJsonObject();
    return object.has(ID) ? object.get(ID).getAsString() : "";
  }

  static int resultCount(JsonObject recipe) {
    JsonElement result = recipe.get(RESULT);
    if (result == null || !result.isJsonObject() || !result.getAsJsonObject().has(COUNT)) {
      return 1;
    }
    return result.getAsJsonObject().get(COUNT).getAsInt();
  }

  static String type(JsonObject recipe) {
    return recipe.get(TYPE).getAsString();
  }
}
