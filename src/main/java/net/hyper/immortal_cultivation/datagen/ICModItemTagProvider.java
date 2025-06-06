package net.hyper.immortal_cultivation.datagen;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.hyper.immortal_cultivation.registry.ICModItems;
import net.hyper.immortal_cultivation.util.ICModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ICModItemTagProvider extends ItemTagsProvider {
    public ICModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
                                super(output, lookupProvider, blockTags, ImmortalCultivation.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ICModTags.Items.SPIRIT_STONES)
                .add(ICModItems.LOW_QUALITY_SPIRIT_STONE.get())
                .add(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get())
                .add(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get())
                .add(ICModItems.TOP_QUALITY_SPIRIT_STONE.get());

    }
}
