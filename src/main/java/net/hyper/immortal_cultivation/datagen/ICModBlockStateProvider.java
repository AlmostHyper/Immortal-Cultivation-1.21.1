package net.hyper.immortal_cultivation.datagen;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.hyper.immortal_cultivation.registry.ICModBlocks;
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
        blockWithItem(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK);
        blockWithItem(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK);

    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("immortal_cultivation:block/" + deferredBlock.getId().getPath()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock, String appendix) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("immortal_cultivation:block/" + deferredBlock.getId().getPath() + appendix));
    }
}
