package net.hyper.immortalcultivation.data.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.server.worldgen.ICModBiomeModifiers;
import net.hyper.immortalcultivation.server.worldgen.ICModConfiguredFeatures;
import net.hyper.immortalcultivation.server.worldgen.ICModPlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ICModWorldGenProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
        .add(Registries.CONFIGURED_FEATURE, ICModConfiguredFeatures::bootstrap)
        .add(Registries.PLACED_FEATURE, ICModPlacedFeatures::bootstrap)
        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ICModBiomeModifiers::bootstrap);

    public ICModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(ImmortalCultivation.MOD_ID));
    }
}
