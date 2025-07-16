package net.hyper.immortalcultivation.content.item;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;

import java.util.List;

public class ArmorMaterialsRegistry {
    public static final DeferredRegister<ArmorMaterial> REGISTRY =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, ImmortalCultivation.MOD_ID);

    public static final Holder<ArmorMaterial> EARTH_ASPECTED =
        REGISTRY.register("earth_aspected", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 7);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 4);
            }), 19, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ItemRegistry.EARTH_ASPECTED_INGOT.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "earth_aspected"))),
            1, 0));

    public static final Holder<ArmorMaterial> FIRE_ASPECTED =
        REGISTRY.register("fire_aspected", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 7);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 4);
            }), 19, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ItemRegistry.FIRE_ASPECTED_INGOT.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "fire_aspected"))),
            1, 0));

    public static final Holder<ArmorMaterial> METAL_ASPECTED =
        REGISTRY.register("metal_aspected", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 7);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 4);
            }), 19, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ItemRegistry.METAL_ASPECTED_INGOT.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "metal_aspected"))),
            1, 0));

    public static final Holder<ArmorMaterial> WATER_ASPECTED =
        REGISTRY.register("water_aspected", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 7);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 4);
            }), 19, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ItemRegistry.WATER_ASPECTED_INGOT.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "water_aspected"))),
            1, 0));

    public static final Holder<ArmorMaterial> WOOD_ASPECTED =
        REGISTRY.register("wood_aspected", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 7);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 4);
            }), 19, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ItemRegistry.WOOD_ASPECTED_INGOT.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "wood_aspected"))),
            1, 0));

    public static final Holder<ArmorMaterial> SKY_IRON =
        REGISTRY.register("sky_iron", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 5);
                map.put(ArmorItem.Type.LEGGINGS, 8);
                map.put(ArmorItem.Type.CHESTPLATE, 10);
                map.put(ArmorItem.Type.HELMET, 5);
                map.put(ArmorItem.Type.BODY, 5);
            }), 25, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(ItemRegistry.SKY_IRON_INGOT.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "sky_iron"))),
            3, 0.1F));

    // Helper Methods
    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }
}
