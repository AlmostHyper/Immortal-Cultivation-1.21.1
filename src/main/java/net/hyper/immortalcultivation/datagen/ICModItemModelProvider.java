package net.hyper.immortalcultivation.datagen;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ICModItemModelProvider extends ItemModelProvider {
    public ICModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ImmortalCultivation.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Spirit Stones
        basicItem(ICModItems.LOW_QUALITY_SPIRIT_STONE.get());
        basicItem(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get());
        basicItem(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get());
        basicItem(ICModItems.TOP_QUALITY_SPIRIT_STONE.get());

        // Ores
        basicItem(ICModItems.EARTH_ASPECTED_ORE.get());
        basicItem(ICModItems.FIRE_ASPECTED_ORE.get());
        basicItem(ICModItems.METAL_ASPECTED_ORE.get());
        basicItem(ICModItems.WATER_ASPECTED_ORE.get());
        basicItem(ICModItems.WOOD_ASPECTED_ORE.get());
        basicItem(ICModItems.SKY_IRON_ORE.get());

        // Ingots
        basicItem(ICModItems.EARTH_ASPECTED_INGOT.get());
        basicItem(ICModItems.FIRE_ASPECTED_INGOT.get());
        basicItem(ICModItems.METAL_ASPECTED_INGOT.get());
        basicItem(ICModItems.SKY_IRON_INGOT.get());
        basicItem(ICModItems.WATER_ASPECTED_INGOT.get());
        basicItem(ICModItems.WOOD_ASPECTED_INGOT.get());

        // Swords
        handheldItem(ICModItems.EARTH_ASPECTED_SWORD.get());
        handheldItem(ICModItems.FIRE_ASPECTED_SWORD.get());
        handheldItem(ICModItems.METAL_ASPECTED_SWORD.get());
        handheldItem(ICModItems.WATER_ASPECTED_SWORD.get());
        handheldItem(ICModItems.WOOD_ASPECTED_SWORD.get());
        handheldItem(ICModItems.SKY_IRON_SWORD.get());

        // Pickaxes
        handheldItem(ICModItems.EARTH_ASPECTED_PICKAXE.get());
        handheldItem(ICModItems.FIRE_ASPECTED_PICKAXE.get());
        handheldItem(ICModItems.METAL_ASPECTED_PICKAXE.get());
        handheldItem(ICModItems.WATER_ASPECTED_PICKAXE.get());
        handheldItem(ICModItems.WOOD_ASPECTED_PICKAXE.get());
        handheldItem(ICModItems.SKY_IRON_PICKAXE.get());

        // Axes
        handheldItem(ICModItems.EARTH_ASPECTED_AXE.get());
        handheldItem(ICModItems.FIRE_ASPECTED_AXE.get());
        handheldItem(ICModItems.METAL_ASPECTED_AXE.get());
        handheldItem(ICModItems.WATER_ASPECTED_AXE.get());
        handheldItem(ICModItems.WOOD_ASPECTED_AXE.get());
        handheldItem(ICModItems.SKY_IRON_AXE.get());

        // Shovels
        handheldItem(ICModItems.EARTH_ASPECTED_SHOVEL.get());
        handheldItem(ICModItems.FIRE_ASPECTED_SHOVEL.get());
        handheldItem(ICModItems.METAL_ASPECTED_SHOVEL.get());
        handheldItem(ICModItems.WATER_ASPECTED_SHOVEL.get());
        handheldItem(ICModItems.WOOD_ASPECTED_SHOVEL.get());
        handheldItem(ICModItems.SKY_IRON_SHOVEL.get());

        // Hoes
        handheldItem(ICModItems.EARTH_ASPECTED_HOE.get());
        handheldItem(ICModItems.FIRE_ASPECTED_HOE.get());
        handheldItem(ICModItems.METAL_ASPECTED_HOE.get());
        handheldItem(ICModItems.WATER_ASPECTED_HOE.get());
        handheldItem(ICModItems.WOOD_ASPECTED_HOE.get());
        handheldItem(ICModItems.SKY_IRON_HOE.get());

        // Helmets
        basicItem(ICModItems.EARTH_ASPECTED_HELMET.get());
        basicItem(ICModItems.FIRE_ASPECTED_HELMET.get());
        basicItem(ICModItems.METAL_ASPECTED_HELMET.get());
        basicItem(ICModItems.WATER_ASPECTED_HELMET.get());
        basicItem(ICModItems.WOOD_ASPECTED_HELMET.get());
        basicItem(ICModItems.SKY_IRON_HELMET.get());

        // Chestplates
        basicItem(ICModItems.EARTH_ASPECTED_CHESTPLATE.get());
        basicItem(ICModItems.FIRE_ASPECTED_CHESTPLATE.get());
        basicItem(ICModItems.METAL_ASPECTED_CHESTPLATE.get());
        basicItem(ICModItems.WATER_ASPECTED_CHESTPLATE.get());
        basicItem(ICModItems.WOOD_ASPECTED_CHESTPLATE.get());
        basicItem(ICModItems.SKY_IRON_CHESTPLATE.get());

        // Leggings
        basicItem(ICModItems.EARTH_ASPECTED_LEGGINGS.get());
        basicItem(ICModItems.FIRE_ASPECTED_LEGGINGS.get());
        basicItem(ICModItems.METAL_ASPECTED_LEGGINGS.get());
        basicItem(ICModItems.WATER_ASPECTED_LEGGINGS.get());
        basicItem(ICModItems.WOOD_ASPECTED_LEGGINGS.get());
        basicItem(ICModItems.SKY_IRON_LEGGINGS.get());

        // Boots
        basicItem(ICModItems.EARTH_ASPECTED_BOOTS.get());
        basicItem(ICModItems.FIRE_ASPECTED_BOOTS.get());
        basicItem(ICModItems.METAL_ASPECTED_BOOTS.get());
        basicItem(ICModItems.WATER_ASPECTED_BOOTS.get());
        basicItem(ICModItems.WOOD_ASPECTED_BOOTS.get());
        basicItem(ICModItems.SKY_IRON_BOOTS.get());
    }
}
