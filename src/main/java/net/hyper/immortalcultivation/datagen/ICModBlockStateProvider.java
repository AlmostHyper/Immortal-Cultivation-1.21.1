package net.hyper.immortalcultivation.datagen;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

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
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
}
