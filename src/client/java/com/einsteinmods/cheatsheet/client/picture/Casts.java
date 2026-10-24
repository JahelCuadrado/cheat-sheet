package com.einsteinmods.cheatsheet.client.picture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The mobs the sheets show, made the way the game makes them, only for one bake. Positions are in
 * block units; yaws in degrees as in the game (0 faces south).
 */
public final class Casts {
  /** Faces the viewer of a picture taken at {@link Scene#MOB_YAW} or {@link Scene#BLOCK_YAW}. */
  public static final float FACING_VIEWER = 215.0F;

  private static final EquipmentSlot[] ARMOR_SLOTS = {
    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
  };

  /** A full set of armour a mannequin wears, helmet to boots. */
  public enum ArmorSet {
    NETHERITE(
        Items.NETHERITE_HELMET,
        Items.NETHERITE_CHESTPLATE,
        Items.NETHERITE_LEGGINGS,
        Items.NETHERITE_BOOTS),
    IRON(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);

    private final Item[] pieces;

    ArmorSet(Item... pieces) {
      this.pieces = pieces;
    }
  }

  /** Turned sideways to a viewer at {@link Scene#BLOCK_YAW}: an elder guardian shows its spikes. */
  private static final float SIDE_ON = 300.0F;

  /** Two mobs side by side: this far from the middle each. */
  private static final double SIDE_BY_SIDE = 0.55;

  /** The wandering trader's llamas: beside and a little behind him, turned towards him. */
  private static final double LLAMA_SIDE = 1.1;

  private static final double LLAMA_BEHIND = -0.9;
  private static final float LLAMA_TURN = 20.0F;

  /**
   * The chest of a {@link #mannequin}, in block units: the chestplate from the belt to the
   * shoulders, arms included. Frame a scene on it to show a trim's pattern up close.
   */
  public static final AABB MANNEQUIN_CHEST = new AABB(-0.5, 0.72, -0.3, 0.5, 1.52, 0.3);

  /**
   * A whole {@link #mannequin}, in block units: helmet to base plate, arms included (its hitbox is
   * narrower than its arms). Frame a scene on it so no part of the armour is cropped.
   */
  public static final AABB MANNEQUIN_BODY = new AABB(-0.55, 0.0, -0.3, 0.55, 2.05, 0.3);

  private Casts() {}

  /** An armour stand with arms, in a full netherite set trimmed with {@code pattern}. */
  public static Cast mannequin(Holder<TrimPattern> pattern, Holder<TrimMaterial> material) {
    return mannequin(pattern, material, ArmorSet.NETHERITE);
  }

  /** An armour stand with arms, in a full {@code set} trimmed with {@code pattern}. */
  public static Cast mannequin(
      Holder<TrimPattern> pattern, Holder<TrimMaterial> material, ArmorSet set) {
    return stage -> dressedStand(stage, set, new ArmorTrim(material, pattern));
  }

  /** An armour stand with arms, in a full netherite set without trim. */
  public static Cast mannequin() {
    return stage -> dressedStand(stage, ArmorSet.NETHERITE, null);
  }

  private static List<Entity> dressedStand(Stage stage, ArmorSet set, @Nullable ArmorTrim trim) {
    Optional<ArmorStand> stand = stage.spawn(EntityTypes.ARMOR_STAND, 0, 0, 0, FACING_VIEWER);
    if (stand.isEmpty()) {
      return List.of();
    }
    stand.get().setShowArms(true);
    for (int index = 0; index < ARMOR_SLOTS.length; index++) {
      ItemStack piece = new ItemStack(set.pieces[index]);
      if (trim != null) {
        piece.set(DataComponents.TRIM, trim);
      }
      stand.get().setItemSlot(ARMOR_SLOTS[index], piece);
    }
    return List.of(stand.get());
  }

  /** A plains villager of {@code profession}, in its work clothes. */
  public static Cast villager(Holder<VillagerProfession> profession) {
    return stage -> {
      Optional<Villager> villager = stage.spawn(EntityTypes.VILLAGER, 0, 0, 0, FACING_VIEWER);
      if (villager.isEmpty()) {
        return List.of();
      }
      Villager found = villager.get();
      found.setVillagerData(
          found
              .getVillagerData()
              .withType(stage.level().registryAccess(), VillagerType.PLAINS)
              .withProfession(profession));
      return List.of(found);
    };
  }

  /** The wandering trader between his two llamas. */
  public static Cast wanderingTrader() {
    return stage -> {
      List<Entity> mobs = new ArrayList<>();
      stage
          .spawn(EntityTypes.TRADER_LLAMA, -LLAMA_SIDE, 0, LLAMA_BEHIND, FACING_VIEWER + LLAMA_TURN)
          .ifPresent(mobs::add);
      stage
          .spawn(EntityTypes.TRADER_LLAMA, LLAMA_SIDE, 0, LLAMA_BEHIND, FACING_VIEWER - LLAMA_TURN)
          .ifPresent(mobs::add);
      stage.spawn(EntityTypes.WANDERING_TRADER, 0, 0, 0, FACING_VIEWER).ifPresent(mobs::add);
      return mobs;
    };
  }

  /** A piglin holding gold, as when it barters. */
  public static Cast piglin() {
    return stage ->
        holding(
            stage.spawn(EntityTypes.PIGLIN, 0, 0, 0, FACING_VIEWER),
            EquipmentSlot.OFFHAND,
            Items.GOLD_INGOT);
  }

  /** A creeper beside a skeleton with its bow. */
  public static Cast creeperAndSkeleton() {
    return stage -> {
      List<Entity> mobs =
          new ArrayList<>(
              holding(
                  stage.spawn(EntityTypes.SKELETON, -SIDE_BY_SIDE, 0, 0, FACING_VIEWER),
                  EquipmentSlot.MAINHAND,
                  Items.BOW));
      stage.spawn(EntityTypes.CREEPER, SIDE_BY_SIDE, 0, 0, FACING_VIEWER).ifPresent(mobs::add);
      return mobs;
    };
  }

  public static Cast ghast() {
    return stage -> only(stage.spawn(EntityTypes.GHAST, 0, 0, 0, FACING_VIEWER));
  }

  /** An elder guardian at (x, y, z), to float over a place. */
  public static Cast elderGuardian(double x, double y, double z) {
    return stage -> only(stage.spawn(EntityTypes.ELDER_GUARDIAN, x, y, z, SIDE_ON));
  }

  /** A baby zombie riding a chicken. */
  public static Cast chickenJockey() {
    return stage -> {
      Optional<Chicken> chicken = stage.spawn(EntityTypes.CHICKEN, 0, 0, 0, FACING_VIEWER);
      Optional<Zombie> zombie = stage.spawn(EntityTypes.ZOMBIE, 0, 0, 0, FACING_VIEWER);
      if (chicken.isEmpty() || zombie.isEmpty()) {
        return List.of();
      }
      zombie.get().setBaby(true);
      zombie.get().startRiding(chicken.get(), true, false);
      chicken.get().positionRider(zombie.get());
      return List.of(chicken.get(), zombie.get());
    };
  }

  private static List<Entity> only(Optional<? extends Entity> mob) {
    return mob.isEmpty() ? List.of() : List.of(mob.get());
  }

  private static List<Entity> holding(
      Optional<? extends LivingEntity> mob, EquipmentSlot slot, Item item) {
    if (mob.isEmpty()) {
      return List.of();
    }
    mob.get().setItemSlot(slot, new ItemStack(item));
    return List.of(mob.get());
  }
}
