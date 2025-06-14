package net.hyper.immortalcultivation.datagen;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.block.crops.EarthCropBlock;
import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

public class ICModBlockStateProvider extends BlockStateProvider {
    public ICModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, ImmortalCultivation.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Spirit Stones
        blockWithItem(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK);
        blockWithItem(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        blockWithItem(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
        blockWithItem(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK);
        blockWithItem(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        blockWithItem(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
        blockWithItem(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        blockWithItem(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
        blockWithItem(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
        blockWithItem(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);

        // Ores
        blockWithItem(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK);
        blockWithItem(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK);
        blockWithItem(ICModBlocks.WATER_ASPECTED_ORE_BLOCK);
        blockWithItem(ICModBlocks.SKY_IRON_ORE_BLOCK);

        // Deepslate Ores
        blockWithItem(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK);
        blockWithItem(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK);
        blockWithItem(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK);

        // Earth Plants
        makeCrop((CropBlock) ICModBlocks.YELLOW_EARTH_ROOT_BLOCK.get(), EarthCropBlock.AGE, "yellow_earth_root_stage", "yellow_earth_root_stage");
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
}
