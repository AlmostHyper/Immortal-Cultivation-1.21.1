package net.hyper.immortalcultivation.data.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class BlockTagProvider extends BlockTagsProvider {
    public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ImmortalCultivation.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
            // Spirit Stones
            .add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(BlockRegistry.EARTH_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.FIRE_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.METAL_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.WATER_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.WOOD_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.SKY_IRON_ORE_BLOCK.get())

            // Deepslate Ores
            .add(BlockRegistry.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get())

            // === CRAFTING === //

            // Pill Furnaces
            .add(BlockRegistry.PILL_FURNACE_BLOCK.get());



        tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
            // Spirit Stones
            .add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(BlockRegistry.EARTH_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.FIRE_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.METAL_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.WATER_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.WOOD_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.SKY_IRON_ORE_BLOCK.get())

            // Deepslate Ores
            .add(BlockRegistry.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_STONE_TOOL)
            // Spirit Stones
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(BlockRegistry.EARTH_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.FIRE_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.METAL_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.WATER_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.WOOD_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.SKY_IRON_ORE_BLOCK.get())

            // Deepslate Ores
            .add(BlockRegistry.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
            // Spirit Stones
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(BlockRegistry.EARTH_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.FIRE_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.METAL_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.WATER_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.WOOD_ASPECTED_ORE_BLOCK.get())
            .add(BlockRegistry.SKY_IRON_ORE_BLOCK.get())

            // Deepslate Ores
            .add(BlockRegistry.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_IRON_TOOL)
            // Spirit Stones
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())

            // Ores
            .add(BlockRegistry.SKY_IRON_ORE_BLOCK.get());

            // Deepslate Ores


        tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
            // Spirit Stones
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());

            // Ores

        tag(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)
            // Spirit Stones
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get())
            .add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());
    }
}
