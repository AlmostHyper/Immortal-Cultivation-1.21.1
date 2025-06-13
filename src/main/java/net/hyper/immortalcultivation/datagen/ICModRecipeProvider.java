package net.hyper.immortalcultivation.datagen;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.hyper.immortalcultivation.registry.ICModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ICModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ICModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        //Shaped
        //Spirit Stones
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("LLL")
            .pattern("LLL")
            .pattern("LLL")
            .define('L', ICModItems.LOW_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_low_quality_spirit_stone", has(ICModItems.LOW_QUALITY_SPIRIT_STONE)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("LLL")
            .pattern("LLL")
            .pattern("LLL")
            .define('L', ICModItems.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_low_quality_spirit_stone_block", has(ICModItems.LOW_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("MMM")
            .pattern("MMM")
            .pattern("MMM")
            .define('M', ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_medium_quality_spirit_stone", has(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("MMM")
            .pattern("MMM")
            .pattern("MMM")
            .define('M', ICModItems.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_medium_quality_spirit_stone_block", has(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("HHH")
            .pattern("HHH")
            .pattern("HHH")
            .define('H', ICModItems.HIGH_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_high_quality_spirit_stone", has(ICModItems.HIGH_QUALITY_SPIRIT_STONE)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("HHH")
            .pattern("HHH")
            .pattern("HHH")
            .define('H', ICModItems.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_high_quality_spirit_stone_block", has(ICModItems.HIGH_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .pattern("TTT")
            .pattern("TTT")
            .pattern("TTT")
            .define('T', ICModItems.TOP_QUALITY_SPIRIT_STONE.get())
            .unlockedBy("has_top_quality_spirit_stone", has(ICModItems.TOP_QUALITY_SPIRIT_STONE)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .pattern("TTT")
            .pattern("TTT")
            .pattern("TTT")
            .define('T', ICModItems.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .unlockedBy("has_top_quality_spirit_stone_block", has(ICModItems.TOP_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        // Metals



        // Swords
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.EARTH_ASPECTED_SWORD.get())
            .pattern("E")
            .pattern("E")
            .pattern("I")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", has(ICModItems.EARTH_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.FIRE_ASPECTED_SWORD.get())
            .pattern("F")
            .pattern("F")
            .pattern("I")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", has(ICModItems.FIRE_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.METAL_ASPECTED_SWORD.get())
            .pattern("M")
            .pattern("M")
            .pattern("I")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", has(ICModItems.METAL_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.WATER_ASPECTED_SWORD.get())
            .pattern("W")
            .pattern("W")
            .pattern("I")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", has(ICModItems.WATER_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.WOOD_ASPECTED_SWORD.get())
            .pattern("W")
            .pattern("W")
            .pattern("I")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", has(ICModItems.WOOD_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ICModItems.SKY_IRON_SWORD.get())
            .pattern("S")
            .pattern("S")
            .pattern("I")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", has(ICModItems.SKY_IRON_INGOT)).save(recipeOutput);

        // Pickaxes
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.EARTH_ASPECTED_PICKAXE.get())
            .pattern("EEE")
            .pattern(" I ")
            .pattern(" I ")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", has(ICModItems.EARTH_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.FIRE_ASPECTED_PICKAXE.get())
            .pattern("FFF")
            .pattern(" I ")
            .pattern(" I ")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", has(ICModItems.FIRE_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.METAL_ASPECTED_PICKAXE.get())
            .pattern("MMM")
            .pattern(" I ")
            .pattern(" I ")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", has(ICModItems.METAL_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WATER_ASPECTED_PICKAXE.get())
            .pattern("WWW")
            .pattern(" I ")
            .pattern(" I ")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", has(ICModItems.WATER_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WOOD_ASPECTED_PICKAXE.get())
            .pattern("WWW")
            .pattern(" I ")
            .pattern(" I ")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", has(ICModItems.WOOD_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.SKY_IRON_PICKAXE.get())
            .pattern("SSS")
            .pattern(" I ")
            .pattern(" I ")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", has(ICModItems.SKY_IRON_INGOT)).save(recipeOutput);

        // Axes
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.EARTH_ASPECTED_AXE.get())
            .pattern("EE")
            .pattern("EI")
            .pattern(" I")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", has(ICModItems.EARTH_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.FIRE_ASPECTED_AXE.get())
            .pattern("FF")
            .pattern("FI")
            .pattern(" I")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", has(ICModItems.FIRE_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.METAL_ASPECTED_AXE.get())
            .pattern("MM")
            .pattern("MI")
            .pattern(" I")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", has(ICModItems.METAL_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WATER_ASPECTED_AXE.get())
            .pattern("WW")
            .pattern("WI")
            .pattern(" I")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", has(ICModItems.WATER_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WOOD_ASPECTED_AXE.get())
            .pattern("WW")
            .pattern("WI")
            .pattern(" I")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", has(ICModItems.WOOD_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.SKY_IRON_AXE.get())
            .pattern("SS")
            .pattern("SI")
            .pattern(" I")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", has(ICModItems.SKY_IRON_INGOT)).save(recipeOutput);

        // Shovels
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.EARTH_ASPECTED_SHOVEL.get())
            .pattern("E")
            .pattern("I")
            .pattern("I")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", has(ICModItems.EARTH_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.FIRE_ASPECTED_SHOVEL.get())
            .pattern("F")
            .pattern("I")
            .pattern("I")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", has(ICModItems.FIRE_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.METAL_ASPECTED_SHOVEL.get())
            .pattern("M")
            .pattern("I")
            .pattern("I")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", has(ICModItems.METAL_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WATER_ASPECTED_SHOVEL.get())
            .pattern("W")
            .pattern("I")
            .pattern("I")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", has(ICModItems.WATER_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WOOD_ASPECTED_SHOVEL.get())
            .pattern("W")
            .pattern("I")
            .pattern("I")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", has(ICModItems.WOOD_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.SKY_IRON_SHOVEL.get())
            .pattern("S")
            .pattern("I")
            .pattern("I")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", has(ICModItems.SKY_IRON_INGOT)).save(recipeOutput);

        // Hoes
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.EARTH_ASPECTED_HOE.get())
            .pattern("EE")
            .pattern(" I")
            .pattern(" I")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_earth_aspected_ingot", has(ICModItems.EARTH_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.FIRE_ASPECTED_HOE.get())
            .pattern("FF")
            .pattern(" I")
            .pattern(" I")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_fire_aspected_ingot", has(ICModItems.FIRE_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.METAL_ASPECTED_HOE.get())
            .pattern("MM")
            .pattern(" I")
            .pattern(" I")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_metal_aspected_ingot", has(ICModItems.METAL_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WATER_ASPECTED_HOE.get())
            .pattern("WW")
            .pattern(" I")
            .pattern(" I")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_water_aspected_ingot", has(ICModItems.WATER_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.WOOD_ASPECTED_HOE.get())
            .pattern("WW")
            .pattern(" I")
            .pattern(" I")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_wood_aspected_ingot", has(ICModItems.WOOD_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ICModItems.SKY_IRON_HOE.get())
            .pattern("SS")
            .pattern(" I")
            .pattern(" I")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .define('I', Items.STICK)
            .unlockedBy("has_sky_iron_ingot", has(ICModItems.SKY_IRON_INGOT)).save(recipeOutput);

        // Helmets
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_HELMET.get())
            .pattern("EEE")
            .pattern("E E")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", has(ICModItems.EARTH_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_HELMET.get())
            .pattern("FFF")
            .pattern("F F")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", has(ICModItems.FIRE_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.METAL_ASPECTED_HELMET.get())
            .pattern("MMM")
            .pattern("M M")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", has(ICModItems.METAL_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WATER_ASPECTED_HELMET.get())
            .pattern("WWW")
            .pattern("W W")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", has(ICModItems.WATER_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_HELMET.get())
            .pattern("WWW")
            .pattern("W W")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", has(ICModItems.WOOD_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.SKY_IRON_HELMET.get())
            .pattern("SSS")
            .pattern("S S")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", has(ICModItems.SKY_IRON_INGOT)).save(recipeOutput);

        // Chestplates
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_CHESTPLATE.get())
            .pattern("E E")
            .pattern("EEE")
            .pattern("EEE")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", has(ICModItems.EARTH_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_CHESTPLATE.get())
            .pattern("F F")
            .pattern("FFF")
            .pattern("FFF")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", has(ICModItems.FIRE_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.METAL_ASPECTED_CHESTPLATE.get())
            .pattern("M M")
            .pattern("MMM")
            .pattern("MMM")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", has(ICModItems.METAL_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WATER_ASPECTED_CHESTPLATE.get())
            .pattern("W W")
            .pattern("WWW")
            .pattern("WWW")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", has(ICModItems.WATER_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_CHESTPLATE.get())
            .pattern("W W")
            .pattern("WWW")
            .pattern("WWW")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", has(ICModItems.WOOD_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.SKY_IRON_CHESTPLATE.get())
            .pattern("S S")
            .pattern("SSS")
            .pattern("SSS")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", has(ICModItems.SKY_IRON_INGOT)).save(recipeOutput);

        // Leggings
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_LEGGINGS.get())
            .pattern("EEE")
            .pattern("E E")
            .pattern("E E")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", has(ICModItems.EARTH_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_LEGGINGS.get())
            .pattern("FFF")
            .pattern("F F")
            .pattern("F F")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", has(ICModItems.FIRE_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.METAL_ASPECTED_LEGGINGS.get())
            .pattern("MMM")
            .pattern("M M")
            .pattern("M M")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", has(ICModItems.METAL_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WATER_ASPECTED_LEGGINGS.get())
            .pattern("WWW")
            .pattern("W W")
            .pattern("W W")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", has(ICModItems.WATER_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_LEGGINGS.get())
            .pattern("WWW")
            .pattern("W W")
            .pattern("W W")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", has(ICModItems.WOOD_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.SKY_IRON_LEGGINGS.get())
            .pattern("SSS")
            .pattern("S S")
            .pattern("S S")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", has(ICModItems.SKY_IRON_INGOT)).save(recipeOutput);

        // Boots
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_BOOTS.get())
            .pattern("E E")
            .pattern("E E")
            .define('E', ICModItems.EARTH_ASPECTED_INGOT.get())
            .unlockedBy("has_earth_aspected_ingot", has(ICModItems.EARTH_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_BOOTS.get())
            .pattern("F F")
            .pattern("F F")
            .define('F', ICModItems.FIRE_ASPECTED_INGOT.get())
            .unlockedBy("has_fire_aspected_ingot", has(ICModItems.FIRE_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.METAL_ASPECTED_BOOTS.get())
            .pattern("M M")
            .pattern("M M")
            .define('M', ICModItems.METAL_ASPECTED_INGOT.get())
            .unlockedBy("has_metal_aspected_ingot", has(ICModItems.METAL_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WATER_ASPECTED_BOOTS.get())
            .pattern("W W")
            .pattern("W W")
            .define('W', ICModItems.WATER_ASPECTED_INGOT.get())
            .unlockedBy("has_water_aspected_ingot", has(ICModItems.WATER_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_BOOTS.get())
            .pattern("W W")
            .pattern("W W")
            .define('W', ICModItems.WOOD_ASPECTED_INGOT.get())
            .unlockedBy("has_wood_aspected_ingot", has(ICModItems.WOOD_ASPECTED_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ICModItems.SKY_IRON_BOOTS.get())
            .pattern("S S")
            .pattern("S S")
            .define('S', ICModItems.SKY_IRON_INGOT.get())
            .unlockedBy("has_sky_iron_ingot", has(ICModItems.SKY_IRON_INGOT)).save(recipeOutput);

        // Shapeless
        // Spirit Stones
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.LOW_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_low_quality_spirit_stone_block", has(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_medium_quality_spirit_stone_block", has(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.HIGH_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_high_quality_spirit_stone_block", has(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ICModItems.TOP_QUALITY_SPIRIT_STONE.get(), 9)
            .requires(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK)
            .unlockedBy("has_top_quality_spirit_stone_block", has(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK)).save(recipeOutput);

        // Smelting
        List<ItemLike> EARTH_ASPECTED_SMELTABLES = List.of(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK, ICModItems.EARTH_ASPECTED_ORE);
        List<ItemLike> FIRE_ASPECTED_SMELTABLES = List.of(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK, ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK, ICModItems.FIRE_ASPECTED_ORE);
        List<ItemLike> METAL_ASPECTED_SMELTABLES = List.of(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK, ICModItems.METAL_ASPECTED_ORE);
        List<ItemLike> WATER_ASPECTED_SMELTABLES = List.of(ICModBlocks.WATER_ASPECTED_ORE_BLOCK, ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK, ICModItems.WATER_ASPECTED_ORE);
        List<ItemLike> WOOD_ASPECTED_SMELTABLES = List.of(ICModItems.WOOD_ASPECTED_ORE);
        List<ItemLike> SKY_IRON_SMELTABLES = List.of(ICModBlocks.SKY_IRON_ORE_BLOCK, ICModItems.SKY_IRON_ORE);

        oreSmelting(recipeOutput, EARTH_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_INGOT.get(), 0.25f, 200, "earth_aspected_ingot");
        oreBlasting(recipeOutput, EARTH_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.EARTH_ASPECTED_INGOT.get(), 0.25f, 100, "earth_aspected_ingot");

        oreSmelting(recipeOutput, FIRE_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_INGOT.get(), 0.25f, 200, "fire_aspected_ingot");
        oreBlasting(recipeOutput, FIRE_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.FIRE_ASPECTED_INGOT.get(), 0.25f, 100, "fire_aspected_ingot");

        oreSmelting(recipeOutput, METAL_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.METAL_ASPECTED_INGOT.get(), 0.25f, 200, "metal_aspected_ingot");
        oreBlasting(recipeOutput, METAL_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.METAL_ASPECTED_INGOT.get(), 0.25f, 100, "metal_aspected_ingot");

        oreSmelting(recipeOutput, WATER_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WATER_ASPECTED_INGOT.get(), 0.25f, 200, "water_aspected_ingot");
        oreBlasting(recipeOutput, WATER_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WATER_ASPECTED_INGOT.get(), 0.25f, 100, "water_aspected_ingot");

        oreSmelting(recipeOutput, WOOD_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_INGOT.get(), 0.25f, 200, "wood_aspected_ingot");
        oreBlasting(recipeOutput, WOOD_ASPECTED_SMELTABLES, RecipeCategory.MISC, ICModItems.WOOD_ASPECTED_INGOT.get(), 0.25f, 100, "wood_aspected_ingot");

        oreSmelting(recipeOutput, SKY_IRON_SMELTABLES, RecipeCategory.MISC, ICModItems.SKY_IRON_INGOT.get(), 0.5f, 400, "sky_iron_ingot");
        oreBlasting(recipeOutput, SKY_IRON_SMELTABLES, RecipeCategory.MISC, ICModItems.SKY_IRON_INGOT.get(), 0.5f, 200, "sky_iron_ingot");
    }

    // Helper Methods
    protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTime, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory,
                                                                       List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(recipeOutput, ImmortalCultivation.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}
