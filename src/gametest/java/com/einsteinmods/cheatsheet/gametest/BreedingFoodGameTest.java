package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.farming.BreedingFood;
import com.einsteinmods.cheatsheet.core.farming.BreedingTable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The farming tab's breeding table ({@link BreedingTable}) against the game: every listed animal
 * exists and is an {@link Animal}, accepts every item of its food as food ({@code isFood}, a tamed
 * one when it must be tamed), needs taming exactly when it is tamable or an untamed horse, and no
 * animal of the registry is missing from the table or its list of animals never bred by feeding.
 */
public final class BreedingFoodGameTest {
  @GameTest
  public void breedingFoodsMatchTheGame(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    List<String> mismatches = new ArrayList<>();
    Set<String> listed = new HashSet<>(BreedingTable.NOT_BRED_BY_FEEDING);
    for (BreedingFood food : BreedingTable.ANIMALS) {
      for (String id : food.entityIds()) {
        listed.add(id);
        checkAnimal(level, id, food, mismatches);
      }
    }
    for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
      Entity entity = type.create(level, EntitySpawnReason.LOAD);
      String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
      if (entity instanceof Animal && !listed.contains(id)) {
        mismatches.add(id + ": an animal missing from the breeding table");
      }
    }
    helper.assertTrue(
        mismatches.isEmpty(),
        "breeding foods differ from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  private static void checkAnimal(
      ServerLevel level, String id, BreedingFood food, List<String> mismatches) {
    EntityType<?> type =
        BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
    if (type == null) {
      mismatches.add(id + ": no such entity");
      return;
    }
    if (!(type.create(level, EntitySpawnReason.LOAD) instanceof Animal animal)) {
      mismatches.add(id + ": not an animal");
      return;
    }
    // Camels are horses born tamed (Camel#isTamed is always true).
    boolean tamable =
        animal instanceof TamableAnimal
            || (animal instanceof AbstractHorse horse && !horse.isTamed());
    if (food.tamed() != tamable) {
      mismatches.add(id + ": tamed is " + food.tamed() + " but tamable is " + tamable);
    }
    if (animal instanceof TamableAnimal tamableAnimal) {
      tamableAnimal.setTame(true, false);
    }
    List<Item> items = items(food.food());
    if (items.isEmpty()) {
      mismatches.add(id + ": no food items");
    }
    for (Item item : items) {
      if (!animal.isFood(new ItemStack(item))) {
        mismatches.add(id + ": does not eat " + BuiltInRegistries.ITEM.getKey(item));
      }
    }
  }

  private static List<Item> items(BreedingFood.Food food) {
    List<Item> items = new ArrayList<>();
    switch (food) {
      case BreedingFood.TagFood tag -> {
        TagKey<Item> key = TagKey.create(Registries.ITEM, Identifier.parse(tag.tagId()));
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(key)) {
          items.add(item.value());
        }
      }
      case BreedingFood.FixedFood fixed -> {
        for (String id : fixed.itemIds()) {
          BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).ifPresent(items::add);
        }
      }
    }
    return items;
  }
}
