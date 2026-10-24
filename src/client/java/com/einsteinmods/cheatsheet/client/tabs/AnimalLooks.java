package com.einsteinmods.cheatsheet.client.tabs;

import com.einsteinmods.cheatsheet.core.farming.AnimalVariant;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.DyeColor;

/**
 * Gives a portrait's animal one of its looks ({@link AnimalVariant}) the way the game stores it:
 * the variant component its entity applies ({@code Entity#setComponent}, as a spawn egg or bucket
 * does), both genes for a panda, a name tag for Toast.
 */
final class AnimalLooks {
  private static final String NAMESPACE = "minecraft:";

  private AnimalLooks() {}

  /**
   * What dresses an animal of {@code variant}'s kind in it; empty when the game (or its data packs)
   * has no such look, so the card leaves it out.
   */
  static Optional<Consumer<Entity>> of(AnimalVariant variant, RegistryAccess registries) {
    String id = variant.variantId();
    Optional<Consumer<Entity>> look =
        switch (variant.entityId().substring(NAMESPACE.length())) {
          case "cow" ->
              registered(registries, Registries.COW_VARIANT, DataComponents.COW_VARIANT, id);
          case "pig" ->
              registered(registries, Registries.PIG_VARIANT, DataComponents.PIG_VARIANT, id);
          case "chicken" ->
              registered(
                  registries, Registries.CHICKEN_VARIANT, DataComponents.CHICKEN_VARIANT, id);
          case "wolf" ->
              registered(registries, Registries.WOLF_VARIANT, DataComponents.WOLF_VARIANT, id);
          case "cat" ->
              registered(registries, Registries.CAT_VARIANT, DataComponents.CAT_VARIANT, id);
          case "frog" ->
              registered(registries, Registries.FROG_VARIANT, DataComponents.FROG_VARIANT, id);
          case "mooshroom" ->
              named(MushroomCow.Variant.values(), DataComponents.MOOSHROOM_VARIANT, id);
          case "rabbit" -> named(Rabbit.Variant.values(), DataComponents.RABBIT_VARIANT, id);
          case "fox" -> named(Fox.Variant.values(), DataComponents.FOX_VARIANT, id);
          case "axolotl" -> named(Axolotl.Variant.values(), DataComponents.AXOLOTL_VARIANT, id);
          case "llama" -> named(Llama.Variant.values(), DataComponents.LLAMA_VARIANT, id);
          case "horse" -> named(Variant.values(), DataComponents.HORSE_VARIANT, id);
          case "sheep" -> named(DyeColor.values(), DataComponents.SHEEP_COLOR, id);
          case "panda" -> byName(Panda.Gene.values(), id).map(AnimalLooks::bothGenes);
          default -> Optional.empty();
        };
    return look.map(
        dress ->
            variant
                .customName()
                .<Consumer<Entity>>map(
                    name ->
                        mob -> {
                          dress.accept(mob);
                          mob.setCustomName(Component.literal(name));
                        })
                .orElse(dress));
  }

  private static <T> Optional<Consumer<Entity>> registered(
      RegistryAccess registries,
      ResourceKey<Registry<T>> registry,
      DataComponentType<Holder<T>> component,
      String id) {
    Optional<Holder.Reference<T>> holder =
        registries.lookupOrThrow(registry).get(ResourceKey.create(registry, Identifier.parse(id)));
    return holder.map(found -> mob -> mob.setComponent(component, found));
  }

  private static <E extends Enum<E> & StringRepresentable> Optional<Consumer<Entity>> named(
      E[] values, DataComponentType<E> component, String id) {
    return byName(values, id).map(found -> mob -> mob.setComponent(component, found));
  }

  /** A recessive gene shows only when both genes carry it. */
  private static Consumer<Entity> bothGenes(Panda.Gene gene) {
    return mob -> {
      if (mob instanceof Panda panda) {
        panda.setMainGene(gene);
        panda.setHiddenGene(gene);
      }
    };
  }

  private static <E extends Enum<E> & StringRepresentable> Optional<E> byName(
      E[] values, String name) {
    for (E value : values) {
      if (value.getSerializedName().equals(name)) {
        return Optional.of(value);
      }
    }
    return Optional.empty();
  }
}
