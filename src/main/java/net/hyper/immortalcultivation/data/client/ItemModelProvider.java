package net.hyper.immortalcultivation.data.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.content.item.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ItemModelProvider extends net.neoforged.neoforge.client.model.generators.ItemModelProvider {
    public ItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ImmortalCultivation.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Spirit Stones
        basicItem(ItemRegistry.LOW_QUALITY_SPIRIT_STONE.get());
        basicItem(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE.get());
        basicItem(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE.get());
        basicItem(ItemRegistry.TOP_QUALITY_SPIRIT_STONE.get());

        // Ores
        basicItem(ItemRegistry.EARTH_ASPECTED_ORE.get());
        basicItem(ItemRegistry.FIRE_ASPECTED_ORE.get());
        basicItem(ItemRegistry.METAL_ASPECTED_ORE.get());
        basicItem(ItemRegistry.WATER_ASPECTED_ORE.get());
        basicItem(ItemRegistry.WOOD_ASPECTED_ORE.get());
        basicItem(ItemRegistry.SKY_IRON_ORE.get());

        // Ingots
        basicItem(ItemRegistry.EARTH_ASPECTED_INGOT.get());
        basicItem(ItemRegistry.FIRE_ASPECTED_INGOT.get());
        basicItem(ItemRegistry.METAL_ASPECTED_INGOT.get());
        basicItem(ItemRegistry.SKY_IRON_INGOT.get());
        basicItem(ItemRegistry.WATER_ASPECTED_INGOT.get());
        basicItem(ItemRegistry.WOOD_ASPECTED_INGOT.get());

        // Swords
        handheldItem(ItemRegistry.EARTH_ASPECTED_SWORD.get());
        handheldItem(ItemRegistry.FIRE_ASPECTED_SWORD.get());
        handheldItem(ItemRegistry.METAL_ASPECTED_SWORD.get());
        handheldItem(ItemRegistry.WATER_ASPECTED_SWORD.get());
        handheldItem(ItemRegistry.WOOD_ASPECTED_SWORD.get());
        handheldItem(ItemRegistry.SKY_IRON_SWORD.get());

        // Pickaxes
        handheldItem(ItemRegistry.EARTH_ASPECTED_PICKAXE.get());
        handheldItem(ItemRegistry.FIRE_ASPECTED_PICKAXE.get());
        handheldItem(ItemRegistry.METAL_ASPECTED_PICKAXE.get());
        handheldItem(ItemRegistry.WATER_ASPECTED_PICKAXE.get());
        handheldItem(ItemRegistry.WOOD_ASPECTED_PICKAXE.get());
        handheldItem(ItemRegistry.SKY_IRON_PICKAXE.get());

        // Axes
        handheldItem(ItemRegistry.EARTH_ASPECTED_AXE.get());
        handheldItem(ItemRegistry.FIRE_ASPECTED_AXE.get());
        handheldItem(ItemRegistry.METAL_ASPECTED_AXE.get());
        handheldItem(ItemRegistry.WATER_ASPECTED_AXE.get());
        handheldItem(ItemRegistry.WOOD_ASPECTED_AXE.get());
        handheldItem(ItemRegistry.SKY_IRON_AXE.get());

        // Shovels
        handheldItem(ItemRegistry.EARTH_ASPECTED_SHOVEL.get());
        handheldItem(ItemRegistry.FIRE_ASPECTED_SHOVEL.get());
        handheldItem(ItemRegistry.METAL_ASPECTED_SHOVEL.get());
        handheldItem(ItemRegistry.WATER_ASPECTED_SHOVEL.get());
        handheldItem(ItemRegistry.WOOD_ASPECTED_SHOVEL.get());
        handheldItem(ItemRegistry.SKY_IRON_SHOVEL.get());

        // Hoes
        handheldItem(ItemRegistry.EARTH_ASPECTED_HOE.get());
        handheldItem(ItemRegistry.FIRE_ASPECTED_HOE.get());
        handheldItem(ItemRegistry.METAL_ASPECTED_HOE.get());
        handheldItem(ItemRegistry.WATER_ASPECTED_HOE.get());
        handheldItem(ItemRegistry.WOOD_ASPECTED_HOE.get());
        handheldItem(ItemRegistry.SKY_IRON_HOE.get());

        // Helmets
        basicItem(ItemRegistry.EARTH_ASPECTED_HELMET.get());
        basicItem(ItemRegistry.FIRE_ASPECTED_HELMET.get());
        basicItem(ItemRegistry.METAL_ASPECTED_HELMET.get());
        basicItem(ItemRegistry.WATER_ASPECTED_HELMET.get());
        basicItem(ItemRegistry.WOOD_ASPECTED_HELMET.get());
        basicItem(ItemRegistry.SKY_IRON_HELMET.get());

        // Chestplates
        basicItem(ItemRegistry.EARTH_ASPECTED_CHESTPLATE.get());
        basicItem(ItemRegistry.FIRE_ASPECTED_CHESTPLATE.get());
        basicItem(ItemRegistry.METAL_ASPECTED_CHESTPLATE.get());
        basicItem(ItemRegistry.WATER_ASPECTED_CHESTPLATE.get());
        basicItem(ItemRegistry.WOOD_ASPECTED_CHESTPLATE.get());
        basicItem(ItemRegistry.SKY_IRON_CHESTPLATE.get());

        // Leggings
        basicItem(ItemRegistry.EARTH_ASPECTED_LEGGINGS.get());
        basicItem(ItemRegistry.FIRE_ASPECTED_LEGGINGS.get());
        basicItem(ItemRegistry.METAL_ASPECTED_LEGGINGS.get());
        basicItem(ItemRegistry.WATER_ASPECTED_LEGGINGS.get());
        basicItem(ItemRegistry.WOOD_ASPECTED_LEGGINGS.get());
        basicItem(ItemRegistry.SKY_IRON_LEGGINGS.get());

        // Boots
        basicItem(ItemRegistry.EARTH_ASPECTED_BOOTS.get());
        basicItem(ItemRegistry.FIRE_ASPECTED_BOOTS.get());
        basicItem(ItemRegistry.METAL_ASPECTED_BOOTS.get());
        basicItem(ItemRegistry.WATER_ASPECTED_BOOTS.get());
        basicItem(ItemRegistry.WOOD_ASPECTED_BOOTS.get());
        basicItem(ItemRegistry.SKY_IRON_BOOTS.get());

        // === Plants === //

        // Earth Plants
        basicItem(ItemRegistry.YELLOW_EARTH_ROOTS.get());

        // Fire Plants
        basicItem(ItemRegistry.RED_FIRE_ROOTS.get());

        // Metal Plants
        basicItem(ItemRegistry.WHITE_METAL_ROOTS.get());

        // Water Plants
        basicItem(ItemRegistry.BLACK_WATER_ROOTS.get());

        // Wood Plants
        basicItem(ItemRegistry.BLUE_WOOD_ROOTS.get());

        // === PILLS === //

        // Qi Cultivation Pills
        basicItem(ItemRegistry.LOW_QUALITY_QI_CULTIVATION_PILL.get());
        basicItem(ItemRegistry.MEDIUM_QUALITY_QI_CULTIVATION_PILL.get());
        basicItem(ItemRegistry.HIGH_QUALITY_QI_CULTIVATION_PILL.get());
        basicItem(ItemRegistry.TOP_QUALITY_QI_CULTIVATION_PILL.get());

        // Body Cultivation Pills
        basicItem(ItemRegistry.LOW_QUALITY_BODY_CULTIVATION_PILL.get());
        basicItem(ItemRegistry.MEDIUM_QUALITY_BODY_CULTIVATION_PILL.get());
        basicItem(ItemRegistry.HIGH_QUALITY_BODY_CULTIVATION_PILL.get());
        basicItem(ItemRegistry.TOP_QUALITY_BODY_CULTIVATION_PILL.get());
    }
}
