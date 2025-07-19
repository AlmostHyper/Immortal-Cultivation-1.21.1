package net.hyper.immortalcultivation.datagen.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

public class DataMapProvider extends net.neoforged.neoforge.common.data.DataMapProvider {
    protected DataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }
}
