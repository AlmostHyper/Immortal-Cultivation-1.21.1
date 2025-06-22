package net.hyper.immortalcultivation.worldgen;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ICModBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_LOW_QUALITY_SPIRIT_STONE_ORE = registerKey("add_overworld_low_quality_spirit_stone_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_MEDIUM_QUALITY_SPIRIT_STONE_ORE = registerKey("add_overworld_medium_quality_spirit_stone_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_HIGH_QUALITY_SPIRIT_STONE_ORE = registerKey("add_overworld_high_quality_spirit_stone_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_TOP_QUALITY_SPIRIT_STONE_ORE = registerKey("add_overworld_top_quality_spirit_stone_ore");

    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_EARTH_ASPECTED_ORE = registerKey("add_overworld_earth_aspected_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_FIRE_ASPECTED_ORE = registerKey("add_overworld_fire_aspected_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_METAL_ASPECTED_ORE = registerKey("add_overworld_metal_aspected_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_WATER_ASPECTED_ORE = registerKey("add_overworld_water_aspected_ore");
    public static final ResourceKey<BiomeModifier> ADD_OVERWORLD_SKY_IRON_ORE = registerKey("add_overworld_sky_iron_ore");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        context.register(ADD_OVERWORLD_LOW_QUALITY_SPIRIT_STONE_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(placedFeatures.getOrThrow(ICModPlacedFeatures.LOW_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY)),
            GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_OVERWORLD_MEDIUM_QUALITY_SPIRIT_STONE_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(placedFeatures.getOrThrow(ICModPlacedFeatures.MEDIUM_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY)),
            GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_OVERWORLD_HIGH_QUALITY_SPIRIT_STONE_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(placedFeatures.getOrThrow(ICModPlacedFeatures.HIGH_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY)),
            GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_OVERWORLD_TOP_QUALITY_SPIRIT_STONE_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(placedFeatures.getOrThrow(ICModPlacedFeatures.TOP_QUALITY_SPIRIT_STONE_ORE_PLACED_KEY)),
            GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_OVERWORLD_EARTH_ASPECTED_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(placedFeatures.getOrThrow(ICModPlacedFeatures.EARTH_ASPECTED_ORE_PLACED_KEY)),
            GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_OVERWORLD_FIRE_ASPECTED_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(placedFeatures.getOrThrow(ICModPlacedFeatures.FIRE_ASPECTED_ORE_PLACED_KEY)),
            GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_OVERWORLD_METAL_ASPECTED_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(placedFeatures.getOrThrow(ICModPlacedFeatures.METAL_ASPECTED_ORE_PLACED_KEY)),
            GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_OVERWORLD_WATER_ASPECTED_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(placedFeatures.getOrThrow(ICModPlacedFeatures.WATER_ASPECTED_ORE_PLACED_KEY)),
            GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_OVERWORLD_SKY_IRON_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
            HolderSet.direct(placedFeatures.getOrThrow(ICModPlacedFeatures.SKY_IRON_ORE_PLACED_KEY)),
            GenerationStep.Decoration.UNDERGROUND_ORES));
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, name));
    }
}
