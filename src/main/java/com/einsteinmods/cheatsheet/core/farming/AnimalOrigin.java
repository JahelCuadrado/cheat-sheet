package com.einsteinmods.cheatsheet.core.farming;

import java.util.List;
import java.util.Optional;

/**
 * Where a breeding card's animal is found, how it is brought home or tamed, and whether it is
 * ridden: what its zoomed card says.
 *
 * @param entityId the card's animal (the first of its {@link BreedingFood#entityIds()})
 * @param sources where it (or another animal of its card) comes from, in the order shown
 * @param obtainKey names the text on how to bring it home or tame it ({@code get.<key>})
 * @param tamingItems the items it is tamed with or trusts you for, when there are any
 * @param riding whether and how it is ridden
 */
public record AnimalOrigin(
    String entityId,
    List<Source> sources,
    String obtainKey,
    Optional<BreedingFood.Food> tamingItems,
    Riding riding) {
  public AnimalOrigin {
    sources = List.copyOf(sources);
    if (sources.isEmpty()) {
      throw new IllegalArgumentException(entityId + " comes from nowhere");
    }
  }

  /** One place an animal comes from; {@code placeKey} names its text ({@code place.<key>}). */
  public sealed interface Source
      permits NaturalSpawn, StructureTemplates, StructureSpawn, Archaeology, BeeNests, Special {}

  /**
   * Spawns by itself in exactly these biomes (their natural mob spawns, any category).
   *
   * @param entityId the animal
   * @param biomeIds the biomes, most typical first
   */
  public record NaturalSpawn(String entityId, List<String> biomeIds) implements Source {
    public NaturalSpawn {
      biomeIds = List.copyOf(biomeIds);
    }
  }

  /**
   * Placed with a structure when it generates: each template holds the animal.
   *
   * @param entityId the animal
   * @param placeKey names the structure
   * @param templateIds the structure templates that hold it
   */
  public record StructureTemplates(String entityId, String placeKey, List<String> templateIds)
      implements Source {
    public StructureTemplates {
      templateIds = List.copyOf(templateIds);
    }
  }

  /**
   * Spawns inside a structure, which overrides its biome's spawns.
   *
   * @param entityId the animal
   * @param placeKey names the structure
   * @param structureId the structure
   */
  public record StructureSpawn(String entityId, String placeKey, String structureId)
      implements Source {}

  /**
   * Hatches from an item brushed out of suspicious blocks.
   *
   * @param placeKey names where
   * @param lootTableId the only loot table that gives the item
   * @param itemId the item (an egg)
   */
  public record Archaeology(String placeKey, String lootTableId, String itemId) implements Source {}

  /**
   * Lives in bee nests on the trees of these biomes.
   *
   * @param biomeIds the biomes, most typical first
   */
  public record BeeNests(List<String> biomeIds) implements Source {
    public BeeNests {
      biomeIds = List.copyOf(biomeIds);
    }
  }

  /**
   * Comes some other way, only told: written in the vanilla code the table cites.
   *
   * @param placeKey names it
   */
  public record Special(String placeKey) implements Source {}

  /** Whether and how the animal is ridden. */
  public enum Riding {
    NONE(List.of()),
    /** {@code can_equip_saddle}: ridden and steered with a saddle. */
    SADDLE(List.of(Riding.SADDLE_ITEM)),
    /** {@code Pig#getControllingPassenger}: steered with a carrot on a stick. */
    CARROT_ON_A_STICK(List.of(Riding.SADDLE_ITEM, "minecraft:carrot_on_a_stick")),
    /** {@code Strider#getControllingPassenger}: steered with a warped fungus on a stick. */
    WARPED_FUNGUS_ON_A_STICK(List.of(Riding.SADDLE_ITEM, "minecraft:warped_fungus_on_a_stick")),
    /**
     * A tamed llama is ridden but never steered (no saddle: {@code
     * AbstractHorse#getControllingPassenger}); led on a lead, the llamas near it follow; carpets
     * decorate it.
     */
    NOT_STEERED(List.of("minecraft:lead", "minecraft:white_carpet"));

    private static final String SADDLE_ITEM = "minecraft:saddle";

    private final List<String> itemIds;

    Riding(List<String> itemIds) {
      this.itemIds = itemIds;
    }

    /** The items shown with it. */
    public List<String> itemIds() {
      return itemIds;
    }

    /** Whether a saddle goes on it. */
    public boolean saddled() {
      return itemIds.contains(SADDLE_ITEM);
    }
  }
}
