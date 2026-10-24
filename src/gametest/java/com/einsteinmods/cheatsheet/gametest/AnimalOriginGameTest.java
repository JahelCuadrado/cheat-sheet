package com.einsteinmods.cheatsheet.gametest;

import com.einsteinmods.cheatsheet.core.farming.AnimalOrigin;
import com.einsteinmods.cheatsheet.core.farming.AnimalOrigins;
import com.einsteinmods.cheatsheet.core.farming.BreedingFood;
import com.einsteinmods.cheatsheet.core.farming.BreedingTable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/**
 * The farming tab's animal origins ({@link AnimalOrigins}) against the game: natural spawns equal
 * each biome's {@code gameplay/natural_mob_spawns} attribute (and no card animal spawning by itself
 * is left out), structure templates hold their animal, structure spawn overrides name it, the
 * sniffer egg is brushed from its loot table, bee nests grow on the trees of exactly the listed
 * biomes, saddles and sticks match the {@code can_equip_saddle} tag and {@link ItemSteerable}, and
 * tag taming items are what an untamed animal takes.
 */
public final class AnimalOriginGameTest {
  /**
   * Cave biomes that carry the plains' tree features ({@code
   * BiomeDefaultFeatures#addPlainVegetation}): trees and their nests only grow where they reach the
   * surface, so the table leaves them out of the bee biomes.
   */
  private static final Set<String> CAVE_BIOMES_WITH_SURFACE_TREES =
      Set.of("minecraft:deep_dark", "minecraft:dripstone_caves");

  private static final String ENTITIES = "entities";
  private static final String ENTITY_NBT = "nbt";
  private static final String ENTITY_ID = "id";

  @GameTest
  public void animalOriginsMatchTheGame(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    MinecraftServer server = level.getServer();
    Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
    LootIndex loot = LootIndex.of(server);
    List<String> mismatches = new ArrayList<>();
    for (BreedingFood animal : BreedingTable.ANIMALS) {
      String cardId = animal.entityIds().getFirst();
      Optional<AnimalOrigin> found = AnimalOrigins.of(cardId);
      if (found.isEmpty()) {
        mismatches.add(cardId + ": no origin");
        continue;
      }
      AnimalOrigin origin = found.get();
      Set<String> natural = new HashSet<>();
      for (AnimalOrigin.Source source : origin.sources()) {
        switch (source) {
          case AnimalOrigin.NaturalSpawn spawn -> {
            natural.add(spawn.entityId());
            compare(
                spawn.entityId() + " spawns",
                spawn.biomeIds(),
                spawningBiomes(biomes, spawn.entityId()),
                mismatches);
          }
          case AnimalOrigin.StructureTemplates templates ->
              checkTemplates(server, templates, mismatches);
          case AnimalOrigin.StructureSpawn spawn -> checkStructureSpawn(level, spawn, mismatches);
          case AnimalOrigin.Archaeology archaeology -> {
            if (!loot.tablesOf(archaeology.itemId()).contains(archaeology.lootTableId())) {
              mismatches.add(archaeology.itemId() + ": not in " + archaeology.lootTableId());
            }
          }
          case AnimalOrigin.BeeNests nests -> {
            Set<String> expected = new TreeSet<>(nests.biomeIds());
            expected.addAll(CAVE_BIOMES_WITH_SURFACE_TREES);
            compare("bee nests", List.copyOf(expected), beeNestBiomes(biomes), mismatches);
          }
          case AnimalOrigin.Special special -> {
            // Told only: the table cites the vanilla code.
          }
        }
      }
      for (String id : animal.entityIds()) {
        if (!natural.contains(id) && !spawningBiomes(biomes, id).isEmpty()) {
          mismatches.add(id + ": spawns naturally but the table has no biomes for it");
        }
      }
      checkRiding(level, origin, mismatches);
      checkTaming(level, origin, mismatches);
    }
    helper.assertTrue(
        mismatches.isEmpty(),
        "animal origins differ from the game:\n" + String.join("\n", mismatches));
    helper.succeed();
  }

  /** Biomes whose natural mob spawns, in any category, list {@code entityId}. */
  private static Set<String> spawningBiomes(Registry<Biome> biomes, String entityId) {
    Set<String> found = new TreeSet<>();
    for (Holder.Reference<Biome> biome : biomes.listElements().toList()) {
      MobSpawnSettings spawns =
          biome
              .value()
              .getAttributes()
              .applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
      for (MobCategory category : MobCategory.values()) {
        for (Weighted<MobSpawnSettings.SpawnerData> spawn :
            spawns.getMobsToSpawn(category).unwrap()) {
          if (BuiltInRegistries.ENTITY_TYPE
              .getKey(spawn.value().type())
              .toString()
              .equals(entityId)) {
            found.add(biome.key().identifier().toString());
          }
        }
      }
    }
    return found;
  }

  /** Biomes with a tree feature that places bee nests ({@link BeehiveDecorator}). */
  private static Set<String> beeNestBiomes(Registry<Biome> biomes) {
    Set<String> found = new TreeSet<>();
    for (Holder.Reference<Biome> biome : biomes.listElements().toList()) {
      boolean nests = false;
      for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
        for (Holder<PlacedFeature> placed : step) {
          nests |= placed.value().getFeatures().anyMatch(AnimalOriginGameTest::placesBeeNests);
        }
      }
      if (nests) {
        found.add(biome.key().identifier().toString());
      }
    }
    return found;
  }

  private static boolean placesBeeNests(Holder<Feature> feature) {
    return feature.value() instanceof TreeFeature tree
        && tree.decorators().stream().anyMatch(BeehiveDecorator.class::isInstance);
  }

  private static void checkTemplates(
      MinecraftServer server, AnimalOrigin.StructureTemplates templates, List<String> mismatches) {
    for (String templateId : templates.templateIds()) {
      Optional<StructureTemplate> template =
          server.getStructureTemplateManager().get(Identifier.parse(templateId));
      if (template.isEmpty()) {
        mismatches.add(templateId + ": no such structure template");
        continue;
      }
      Set<String> entities = new TreeSet<>();
      ListTag list = template.get().save(new CompoundTag()).getListOrEmpty(ENTITIES);
      for (int index = 0; index < list.size(); index++) {
        list.getCompoundOrEmpty(index)
            .getCompoundOrEmpty(ENTITY_NBT)
            .getString(ENTITY_ID)
            .ifPresent(entities::add);
      }
      if (!entities.contains(templates.entityId())) {
        mismatches.add(templateId + ": holds " + entities + ", not " + templates.entityId());
      }
    }
  }

  private static void checkStructureSpawn(
      ServerLevel level, AnimalOrigin.StructureSpawn spawn, List<String> mismatches) {
    Optional<Structure> structure =
        level
            .registryAccess()
            .lookupOrThrow(Registries.STRUCTURE)
            .getOptional(Identifier.parse(spawn.structureId()));
    if (structure.isEmpty()) {
      mismatches.add(spawn.structureId() + ": no such structure");
      return;
    }
    boolean found = false;
    for (StructureSpawnOverride override : structure.get().spawnOverrides().values()) {
      for (Weighted<MobSpawnSettings.SpawnerData> entry : override.spawns().unwrap()) {
        found |=
            BuiltInRegistries.ENTITY_TYPE
                .getKey(entry.value().type())
                .toString()
                .equals(spawn.entityId());
      }
    }
    if (!found) {
      mismatches.add(spawn.structureId() + ": does not spawn " + spawn.entityId());
    }
  }

  /**
   * Saddled exactly when the saddle tag takes it, steered by a stick exactly when it is {@link
   * ItemSteerable}, ridden unsteered only as a horse the saddle tag leaves out (a llama).
   */
  private static void checkRiding(ServerLevel level, AnimalOrigin origin, List<String> mismatches) {
    EntityType<?> type = type(origin.entityId());
    if (type == null) {
      mismatches.add(origin.entityId() + ": no such entity");
      return;
    }
    AnimalOrigin.Riding riding = origin.riding();
    boolean saddle =
        BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(EntityTypeTags.CAN_EQUIP_SADDLE);
    if (riding.saddled() != saddle) {
      mismatches.add(origin.entityId() + ": riding " + riding + " but saddle tag " + saddle);
    }
    Entity mob = type.create(level, EntitySpawnReason.LOAD);
    boolean stick =
        riding == AnimalOrigin.Riding.CARROT_ON_A_STICK
            || riding == AnimalOrigin.Riding.WARPED_FUNGUS_ON_A_STICK;
    if (stick != mob instanceof ItemSteerable) {
      mismatches.add(origin.entityId() + ": riding " + riding + " but steerable is " + !stick);
    }
    boolean unsteered = mob instanceof AbstractHorse && !saddle;
    if ((riding == AnimalOrigin.Riding.NOT_STEERED) != unsteered) {
      mismatches.add(origin.entityId() + ": riding " + riding + " but unsteered is " + unsteered);
    }
  }

  /** An untamed animal takes every item of a tag it is tamed or trusts with. */
  private static void checkTaming(ServerLevel level, AnimalOrigin origin, List<String> mismatches) {
    if (!(origin.tamingItems().orElse(null) instanceof BreedingFood.TagFood tag)) {
      return;
    }
    EntityType<?> type = type(origin.entityId());
    if (type == null || !(type.create(level, EntitySpawnReason.LOAD) instanceof Animal animal)) {
      mismatches.add(origin.entityId() + ": not an animal");
      return;
    }
    TagKey<Item> key = TagKey.create(Registries.ITEM, Identifier.parse(tag.tagId()));
    int items = 0;
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(key)) {
      items++;
      if (!animal.isFood(new ItemStack(item.value()))) {
        mismatches.add(origin.entityId() + ": untamed, does not take " + item.getRegisteredName());
      }
    }
    if (items == 0) {
      mismatches.add(origin.entityId() + ": empty taming tag " + tag.tagId());
    }
  }

  private static void compare(
      String what, List<String> expected, Set<String> actual, List<String> mismatches) {
    Set<String> sorted = new TreeSet<>(expected);
    if (!sorted.equals(actual)) {
      Set<String> missing = new TreeSet<>(actual);
      missing.removeAll(sorted);
      Set<String> extra = new TreeSet<>(sorted);
      extra.removeAll(actual);
      mismatches.add(what + ": missing " + missing + ", not in the game " + extra);
    }
  }

  private static @Nullable EntityType<?> type(String id) {
    return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
  }
}
