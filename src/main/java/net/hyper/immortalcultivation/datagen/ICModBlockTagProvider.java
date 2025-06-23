package net.hyper.immortalcultivation.datagen;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ICModBlockTagProvider extends BlockTagsProvider {
    public ICModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ImmortalCultivation.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
            // Spirit Stones
            .add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.SKY_IRON_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get())

            // Deepslate Ores
            .add(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
            // Spirit Stones
            .add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.SKY_IRON_ORE_BLOCK.get())

            // Deepslate Ores
            .add(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_STONE_TOOL)
            // Spirit Stones
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.SKY_IRON_ORE_BLOCK.get())

            // Deepslate Ores
            .add(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
            // Spirit Stones
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.SKY_IRON_ORE_BLOCK.get())

            // Deepslate Ores
            .add(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_IRON_TOOL)
            // Spirit Stones
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get())
            .add(ICModBlocks.SKY_IRON_ORE_BLOCK.get())

            // Deepslate Ores
            .add(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
            // Spirit Stones
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());

            // Ores

        tag(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)
            // Spirit Stones
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());
    }
}
