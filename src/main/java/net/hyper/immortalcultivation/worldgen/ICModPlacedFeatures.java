package net.hyper.immortalcultivation.worldgen;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.registry.ICModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ICModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> LOW_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY = registerKey("low_quality_spirit_stone_ore_placed");
    public static final ResourceKey<PlacedFeature> MEDIUM_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY = registerKey("medium_quality_spirit_stone_ore_placed");
    public static final ResourceKey<PlacedFeature> HIGH_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY = registerKey("high_quality_spirit_stone_ore_placed");
    public static final ResourceKey<PlacedFeature> TOP_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY = registerKey("top_quality_spirit_stone_ore_placed");

    public static final ResourceKey<PlacedFeature> EARTH_ASPECTED_ORE_PLACED_KEY = registerKey("earth_aspected_ore_placed");
    public static final ResourceKey<PlacedFeature> FIRE_ASPECTED_ORE_PLACED_KEY = registerKey("fire_aspected_ore_placed");
    public static final ResourceKey<PlacedFeature> METAL_ASPECTED_ORE_PLACED_KEY = registerKey("metal_aspected_ore_placed");
    public static final ResourceKey<PlacedFeature> WATER_ASPECTED_ORE_PLACED_KEY = registerKey("water_aspected_ore_placed");
    public static final ResourceKey<PlacedFeature> WOOD_ASPECTED_ORE_PLACED_KEY = registerKey("wood_aspected_ore_placed");
    public static final ResourceKey<PlacedFeature> SKY_IRON_ORE_PLACED_KEY = registerKey("sky_iron_ore_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        // === Spirit Stones === //

        register(context, LOW_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_LOW_QUALITY_SPIRIT_STONE_ORE_KEY),
            ICModOrePlacements.commonOrePlacement(9,
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(480))));

        register(context, MEDIUM_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_MEDIUM_QUALITY_SPIRIT_STONE_ORE_KEY),
            ICModOrePlacements.commonOrePlacement(3,
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(31))));

        register(context, HIGH_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_HIGH_QUALITY_SPIRIT_STONE_ORE_KEY),
            ICModOrePlacements.commonOrePlacement(1,
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(0))));

        register(context, TOP_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_TOP_QUALITY_SPIRIT_STONE_ORE_KEY),
            ICModOrePlacements.rareOrePlacement(50,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-54))));

        // === Elemental === //

        register(context, EARTH_ASPECTED_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_EARTH_ASPECTED_ORE_KEY),
            ICModOrePlacements.commonOrePlacement(3,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(31))));

        register(context, FIRE_ASPECTED_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_FIRE_ASPECTED_ORE_KEY),
            ICModOrePlacements.commonOrePlacement(3,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(31))));

        register(context, METAL_ASPECTED_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_METAL_ASPECTED_ORE_KEY),
            ICModOrePlacements.commonOrePlacement(3,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(31))));

        register(context, WATER_ASPECTED_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_WATER_ASPECTED_ORE_KEY),
            ICModOrePlacements.commonOrePlacement(3,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(31))));

        register(context, WOOD_ASPECTED_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_WOOD_ASPECTED_ORE_KEY),
            ICModOrePlacements.commonOrePlacement(3,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(31))));

        register(context, SKY_IRON_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ICModConfiguredFeatures.OVERWORLD_SKY_IRON_ORE_KEY),
            ICModOrePlacements.commonOrePlacement(9,
                HeightRangePlacement.uniform(VerticalAnchor.absolute(150), VerticalAnchor.absolute(384))));

    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}
