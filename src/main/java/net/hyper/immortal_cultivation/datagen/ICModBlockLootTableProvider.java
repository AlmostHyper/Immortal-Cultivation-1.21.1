package net.hyper.immortal_cultivation.datagen;

import net.hyper.immortal_cultivation.registry.ICModBlocks;
import net.hyper.immortal_cultivation.registry.ICModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class ICModBlockLootTableProvider extends BlockLootSubProvider {
    protected ICModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);

    }

    @Override
    protected void generate() {
        // Spirit Stones
        dropSelf(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get());
        dropSelf(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());
        dropSelf(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get());
        dropSelf(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());
        dropSelf(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get());
        dropSelf(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());
        dropSelf(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get());
        dropSelf(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());

        // Ores
        add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), ICModItems.LOW_QUALITY_SPIRIT_STONE.get()));
        add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get()));
        add(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get(), ICModItems.EARTH_ASPECTED_ORE.get()));
        add(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get(), ICModItems.FIRE_ASPECTED_ORE.get()));
        add(ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get(), ICModItems.WATER_ASPECTED_ORE.get()));
        add(ICModBlocks.SKY_IRON_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.SKY_IRON_ORE_BLOCK.get(), ICModItems.SKY_IRON_ORE.get()));

        // Deepslate Ores
        add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ICModItems.LOW_QUALITY_SPIRIT_STONE.get()));
        add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get()));
        add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ICModItems.HIGH_QUALITY_SPIRIT_STONE.get()));
        add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ICModItems.TOP_QUALITY_SPIRIT_STONE.get()));
        add(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ICModItems.FIRE_ASPECTED_ORE.get()));
        add(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ICModItems.METAL_ASPECTED_ORE.get()));
        add(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ICModItems.WATER_ASPECTED_ORE.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ICModBlocks.REGISTRY.getEntries().stream().map(Holder::value)::iterator;
    }
}