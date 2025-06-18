package net.hyper.immortalcultivation.worldgen;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ICModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_LOW_QUALITY_SPIRIT_STONE_ORE_KEY = registerKey("low_quality_spirit_stone_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_MEDIUM_QUALITY_SPIRIT_STONE_ORE_KEY = registerKey("medium_quality_spirit_stone_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_HIGH_QUALITY_SPIRIT_STONE_ORE_KEY = registerKey("high_quality_spirit_stone_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_TOP_QUALITY_SPIRIT_STONE_ORE_KEY = registerKey("top_quality_spirit_stone_ore");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrackReplaceables = new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest endReplaceables = new BlockMatchTest(Blocks.END_STONE);

        List<OreConfiguration.TargetBlockState> overworldSpiritStoneOres = List.of(
            OreConfiguration.target(stoneReplaceables, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(stoneReplaceables, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get().defaultBlockState()));

        register(context, OVERWORLD_LOW_QUALITY_SPIRIT_STONE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldSpiritStoneOres, 9));
        register(context, OVERWORLD_MEDIUM_QUALITY_SPIRIT_STONE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldSpiritStoneOres, 7));
        register(context, OVERWORLD_HIGH_QUALITY_SPIRIT_STONE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldSpiritStoneOres, 5));
        register(context, OVERWORLD_TOP_QUALITY_SPIRIT_STONE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldSpiritStoneOres, 3));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
