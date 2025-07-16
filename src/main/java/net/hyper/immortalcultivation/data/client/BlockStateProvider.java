package net.hyper.immortalcultivation.data.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.content.block.crops.*;
import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

public class BlockStateProvider extends net.neoforged.neoforge.client.model.generators.BlockStateProvider {
    public BlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, ImmortalCultivation.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Spirit Stones
        blockWithItem(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK);
        blockWithItem(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        blockWithItem(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
        blockWithItem(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK);
        blockWithItem(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        blockWithItem(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
        blockWithItem(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        blockWithItem(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
        blockWithItem(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        blockWithItem(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);

        // Ores
        blockWithItem(BlockRegistry.EARTH_ASPECTED_ORE_BLOCK);
        blockWithItem(BlockRegistry.FIRE_ASPECTED_ORE_BLOCK);
        blockWithItem(BlockRegistry.METAL_ASPECTED_ORE_BLOCK);
        blockWithItem(BlockRegistry.WATER_ASPECTED_ORE_BLOCK);
        blockWithItem(BlockRegistry.WOOD_ASPECTED_ORE_BLOCK);
        blockWithItem(BlockRegistry.SKY_IRON_ORE_BLOCK);

        // Deepslate Ores
        blockWithItem(BlockRegistry.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK);
        blockWithItem(BlockRegistry.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK);
        blockWithItem(BlockRegistry.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK);
        blockWithItem(BlockRegistry.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK);
        blockWithItem(BlockRegistry.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK);

        // === PLANTS === //

        // Earth Plants
        makeCrop((CropBlock) BlockRegistry.YELLOW_EARTH_ROOTS_CROP_BLOCK.get(), EarthCropBlock.AGE, "yellow_earth_roots_stage", "yellow_earth_roots_stage");

        // Fire Plants
        makeCrop((CropBlock) BlockRegistry.RED_FIRE_ROOTS_CROP_BLOCK.get(), FireCropBlock.AGE, "red_fire_roots_stage", "red_fire_roots_stage");

        // Earth Plants
        makeCrop((CropBlock) BlockRegistry.WHITE_METAL_ROOTS_CROP_BLOCK.get(), MetalCropBlock.AGE, "white_metal_roots_stage", "white_metal_roots_stage");

        // Earth Plants
        makeCrop((CropBlock) BlockRegistry.BLACK_WATER_ROOTS_CROP_BLOCK.get(), WaterCropBlock.AGE, "black_water_roots_stage", "black_water_roots_stage");

        // Earth Plants
        makeCrop((CropBlock) BlockRegistry.BLUE_WOOD_ROOTS_CROP_BLOCK.get(), WoodCropBlock.AGE, "blue_wood_roots_stage", "blue_wood_roots_stage");

        // === CRAFTING === //

        // Pill Furnaces
        pillFurnaceBlock(BlockRegistry.PILL_FURNACE_BLOCK);
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    public void makeCrop(CropBlock block, IntegerProperty ageProperty, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> states(state, ageProperty, modelName, textureName);
        getVariantBuilder(block).forAllStates(function);
    }

    private ConfiguredModel[] states(BlockState state, IntegerProperty ageProperty, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        int age = state.getValue(ageProperty);

        models[0] = new ConfiguredModel(models().crop(modelName + age,
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "block/" + textureName + age))
            .renderType("cutout"));

        return models;
    }

    private void pillFurnaceBlock(DeferredBlock<?> deferredBlock) {
        Block block = deferredBlock.get();
        String blockName = getBlockName(block);

        // Create models for off and on states
        ModelFile offModel = models().orientable(
            blockName + "_off",
            resourceLocation("block/" + blockName + "_side"),
            resourceLocation("block/" + blockName + "_front_off"),
            resourceLocation("block/" + blockName + "_top")
        );

        ModelFile onModel = models().orientable(
            blockName + "_on",
            resourceLocation("block/" + blockName + "_side"),
            resourceLocation("block/" + blockName + "_front_on"),
            resourceLocation("block/" + blockName + "_top")
        );

        // Create blockstate variants
        getVariantBuilder(block)
            .forAllStates(state -> {
                // Replace "POWERED" with your actual boolean property name
                boolean powered = state.getValue(BlockStateProperties.LIT); // or whatever your property is
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

                return ConfiguredModel.builder()
                    .modelFile(powered ? onModel : offModel)
                    .rotationY(getRotationY(facing))
                    .build();
            });

        // Generate item model
        itemModels().getBuilder(blockName)
            .parent(offModel);
    }

    private String getBlockName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    private int getRotationY(Direction facing) {
        return switch (facing) {
            case NORTH -> 0;
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    private ResourceLocation resourceLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, path);
    }
}
