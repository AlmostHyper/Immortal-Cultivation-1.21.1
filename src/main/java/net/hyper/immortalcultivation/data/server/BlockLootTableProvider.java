package net.hyper.immortalcultivation.data.server;

import net.hyper.immortalcultivation.content.block.crops.*;
import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.hyper.immortalcultivation.content.item.ItemRegistry;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class BlockLootTableProvider extends BlockLootSubProvider {
    public BlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);

    }

    @Override
    protected void generate() {
        // Spirit Stones
        dropSelf(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK.get());
        dropSelf(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());
        dropSelf(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get());
        dropSelf(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());
        dropSelf(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get());
        dropSelf(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());
        dropSelf(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get());
        dropSelf(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get());

        // Ores
        add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), block -> createMultipleOreDrops(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), ItemRegistry.LOW_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), block -> createMultipleOreDrops(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(BlockRegistry.EARTH_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.EARTH_ASPECTED_ORE_BLOCK.get(), ItemRegistry.EARTH_ASPECTED_ORE.get()));
        add(BlockRegistry.FIRE_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.FIRE_ASPECTED_ORE_BLOCK.get(), ItemRegistry.FIRE_ASPECTED_ORE.get()));
        add(BlockRegistry.METAL_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.METAL_ASPECTED_ORE_BLOCK.get(), ItemRegistry.METAL_ASPECTED_ORE.get()));
        add(BlockRegistry.WATER_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.WATER_ASPECTED_ORE_BLOCK.get(), ItemRegistry.WATER_ASPECTED_ORE.get()));
        add(BlockRegistry.WOOD_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.WOOD_ASPECTED_ORE_BLOCK.get(), ItemRegistry.WOOD_ASPECTED_ORE.get()));
        add(BlockRegistry.SKY_IRON_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.SKY_IRON_ORE_BLOCK.get(), ItemRegistry.SKY_IRON_ORE.get()));

        // Deepslate Ores
        add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createMultipleOreDrops(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ItemRegistry.LOW_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createMultipleOreDrops(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createMultipleOreDrops(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ItemRegistry.HIGH_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createMultipleOreDrops(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ItemRegistry.TOP_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(BlockRegistry.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ItemRegistry.EARTH_ASPECTED_ORE.get()));
        add(BlockRegistry.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ItemRegistry.FIRE_ASPECTED_ORE.get()));
        add(BlockRegistry.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ItemRegistry.METAL_ASPECTED_ORE.get()));
        add(BlockRegistry.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ItemRegistry.WATER_ASPECTED_ORE.get()));
        add(BlockRegistry.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(BlockRegistry.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ItemRegistry.WOOD_ASPECTED_ORE.get()));

        // === PLANTS === //

        // Earth Plants
        add(BlockRegistry.YELLOW_EARTH_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(BlockRegistry.YELLOW_EARTH_ROOTS_CROP_BLOCK.get(), ItemRegistry.YELLOW_EARTH_ROOTS.get(), ItemRegistry.YELLOW_EARTH_ROOTS.get(), createMatureCropCondition(BlockRegistry.YELLOW_EARTH_ROOTS_CROP_BLOCK.get(), EarthCropBlock.AGE)));

        // Fire Plants
        add(BlockRegistry.RED_FIRE_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(BlockRegistry.RED_FIRE_ROOTS_CROP_BLOCK.get(), ItemRegistry.RED_FIRE_ROOTS.get(), ItemRegistry.RED_FIRE_ROOTS.get(), createMatureCropCondition(BlockRegistry.RED_FIRE_ROOTS_CROP_BLOCK.get(), FireCropBlock.AGE)));

        // Metal Plants
        add(BlockRegistry.WHITE_METAL_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(BlockRegistry.WHITE_METAL_ROOTS_CROP_BLOCK.get(), ItemRegistry.WHITE_METAL_ROOTS.get(), ItemRegistry.WHITE_METAL_ROOTS.get(), createMatureCropCondition(BlockRegistry.WHITE_METAL_ROOTS_CROP_BLOCK.get(), MetalCropBlock.AGE)));

        // Water Plants
        add(BlockRegistry.BLACK_WATER_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(BlockRegistry.BLACK_WATER_ROOTS_CROP_BLOCK.get(), ItemRegistry.BLACK_WATER_ROOTS.get(), ItemRegistry.BLACK_WATER_ROOTS.get(), createMatureCropCondition(BlockRegistry.BLACK_WATER_ROOTS_CROP_BLOCK.get(), WaterCropBlock.AGE)));

        // Wood Plants
        add(BlockRegistry.BLUE_WOOD_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(BlockRegistry.BLUE_WOOD_ROOTS_CROP_BLOCK.get(), ItemRegistry.BLUE_WOOD_ROOTS.get(), ItemRegistry.BLUE_WOOD_ROOTS.get(), createMatureCropCondition(BlockRegistry.BLUE_WOOD_ROOTS_CROP_BLOCK.get(), WoodCropBlock.AGE)));

        // === CRAFTING === //

        // Pill Furnaces
        dropSelf(BlockRegistry.PILL_FURNACE_BLOCK.get());
    }

    // == Helper Methods == //

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BlockRegistry.REGISTRY.getEntries().stream().map(Holder::value)::iterator;
    }

    protected LootTable.Builder createMultipleOreDrops(Block pBlock, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(pBlock,
            this.applyExplosionDecay(pBlock, LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))));
    }

    public static <T extends Block> LootItemCondition.Builder createMatureCropCondition(
        T cropBlock, IntegerProperty ageProperty) {

        int maxAge = ageProperty.getPossibleValues().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElseThrow(() -> new IllegalArgumentException("Could not determine max age"));

        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(cropBlock)
            .setProperties(StatePropertiesPredicate.Builder.properties()
                .hasProperty(ageProperty, maxAge));
    }
}