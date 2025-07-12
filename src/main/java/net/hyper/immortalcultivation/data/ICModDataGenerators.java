package net.hyper.immortalcultivation.data;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.data.client.ICModBlockStateProvider;
import net.hyper.immortalcultivation.data.client.ICModItemModelProvider;
import net.hyper.immortalcultivation.data.client.ICModLanguageProvider;
import net.hyper.immortalcultivation.data.recipes.ICModRecipeProvider;
import net.hyper.immortalcultivation.data.server.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ICModDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // === SERVER DATA PROVIDERS === //
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
            List.of(new LootTableProvider.SubProviderEntry(ICModBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider));
        generator.addProvider(event.includeServer(), new ICModRecipeProvider(packOutput, lookupProvider));

        BlockTagsProvider blockTagsProvider = new ICModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(), new ICModItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));

        generator.addProvider(event.includeServer(), new ICModWorldGenProvider(packOutput, lookupProvider));

        // === CLIENT DATA PROVIDERS === //
        generator.addProvider(event.includeClient(), new ICModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ICModBlockStateProvider(packOutput, existingFileHelper));

        // === LANGUAGE PROVIDERS === //
        generator.addProvider(event.includeClient(), new ICModLanguageProvider(packOutput, "en_us"));
        // Add more languages as needed for international support:
        // generator.addProvider(event.includeClient(), new ICModLanguageProvider(packOutput, "zh_cn")); // Chinese (Simplified)
        // generator.addProvider(event.includeClient(), new ICModLanguageProvider(packOutput, "zh_tw")); // Chinese (Traditional)
        // generator.addProvider(event.includeClient(), new ICModLanguageProvider(packOutput, "ja_jp")); // Japanese
        // generator.addProvider(event.includeClient(), new ICModLanguageProvider(packOutput, "ko_kr")); // Korean
    }
}