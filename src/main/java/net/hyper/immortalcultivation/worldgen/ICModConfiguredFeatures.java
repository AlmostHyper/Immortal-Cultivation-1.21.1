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
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_EARTH_ASPECTED_ORE_KEY = registerKey("earth_aspected_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_FIRE_ASPECTED_ORE_KEY = registerKey("fire_aspected_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_METAL_ASPECTED_ORE_KEY = registerKey("metal_aspected_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_WATER_ASPECTED_ORE_KEY = registerKey("water_aspected_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_WOOD_ASPECTED_ORE_KEY = registerKey("wood_aspected_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_SKY_IRON_ORE_KEY = registerKey("sky_iron_ore");


    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrackReplaceables = new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest endReplaceables = new BlockMatchTest(Blocks.END_STONE);

        List<OreConfiguration.TargetBlockState> overworldLowQualitySpiritStoneOres = List.of(
            OreConfiguration.target(stoneReplaceables, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> overworldMediumQualitySpiritStoneOres = List.of(
            OreConfiguration.target(stoneReplaceables, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> overworldHighQualitySpiritStoneOres = List.of(
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> overworldTopQualitySpiritStoneOres = List.of(
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> overworldEarthOres = List.of(
            OreConfiguration.target(stoneReplaceables, ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> overworldFireOres = List.of(
            OreConfiguration.target(stoneReplaceables, ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> overworldMetalOres = List.of(
            OreConfiguration.target(stoneReplaceables, ICModBlocks.METAL_ASPECTED_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> overworldWaterOres = List.of(
            OreConfiguration.target(stoneReplaceables, ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> overworldWoodOres = List.of(
            OreConfiguration.target(stoneReplaceables, ICModBlocks.WOOD_ASPECTED_ORE_BLOCK.get().defaultBlockState()),
            OreConfiguration.target(deepslateReplaceables, ICModBlocks.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> overworldSkyIronOres = List.of(
            OreConfiguration.target(stoneReplaceables, ICModBlocks.SKY_IRON_ORE_BLOCK.get().defaultBlockState())

        );

        register(context, OVERWORLD_LOW_QUALITY_SPIRIT_STONE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldLowQualitySpiritStoneOres, 19));
        register(context, OVERWORLD_MEDIUM_QUALITY_SPIRIT_STONE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldMediumQualitySpiritStoneOres, 6));
        register(context, OVERWORLD_HIGH_QUALITY_SPIRIT_STONE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldHighQualitySpiritStoneOres, 4));
        register(context, OVERWORLD_TOP_QUALITY_SPIRIT_STONE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldTopQualitySpiritStoneOres, 3));
        register(context, OVERWORLD_EARTH_ASPECTED_ORE_KEY, Feature.ORE, new OreConfiguration(overworldEarthOres, 6));
        register(context, OVERWORLD_FIRE_ASPECTED_ORE_KEY, Feature.ORE, new OreConfiguration(overworldFireOres, 6));
        register(context, OVERWORLD_METAL_ASPECTED_ORE_KEY, Feature.ORE, new OreConfiguration(overworldMetalOres, 6));
        register(context, OVERWORLD_WATER_ASPECTED_ORE_KEY, Feature.ORE, new OreConfiguration(overworldWaterOres, 6));
        register(context, OVERWORLD_WOOD_ASPECTED_ORE_KEY, Feature.ORE, new OreConfiguration(overworldWoodOres, 6));
        register(context, OVERWORLD_SKY_IRON_ORE_KEY, Feature.ORE, new OreConfiguration(overworldSkyIronOres, 9));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
