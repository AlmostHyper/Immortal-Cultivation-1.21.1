package net.hyper.immortalcultivation.data.server;

import net.hyper.immortalcultivation.content.block.crops.*;
import net.hyper.immortalcultivation.common.registry.ICModBlocks;
import net.hyper.immortalcultivation.common.registry.ICModItems;
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

public class ICModBlockLootTableProvider extends BlockLootSubProvider {
    public ICModBlockLootTableProvider(HolderLookup.Provider registries) {
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
        add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), block -> createMultipleOreDrops(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), ICModItems.LOW_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), block -> createMultipleOreDrops(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK.get(), ICModItems.EARTH_ASPECTED_ORE.get()));
        add(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK.get(), ICModItems.FIRE_ASPECTED_ORE.get()));
        add(ICModBlocks.METAL_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.METAL_ASPECTED_ORE_BLOCK.get(), ICModItems.METAL_ASPECTED_ORE.get()));
        add(ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.WATER_ASPECTED_ORE_BLOCK.get(), ICModItems.WATER_ASPECTED_ORE.get()));
        add(ICModBlocks.WOOD_ASPECTED_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.WOOD_ASPECTED_ORE_BLOCK.get(), ICModItems.WOOD_ASPECTED_ORE.get()));
        add(ICModBlocks.SKY_IRON_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.SKY_IRON_ORE_BLOCK.get(), ICModItems.SKY_IRON_ORE.get()));

        // Deepslate Ores
        add(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createMultipleOreDrops(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ICModItems.LOW_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createMultipleOreDrops(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createMultipleOreDrops(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ICModItems.HIGH_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), block -> createMultipleOreDrops(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), ICModItems.TOP_QUALITY_SPIRIT_STONE.get(), 1, 3));
        add(ICModBlocks.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ICModItems.EARTH_ASPECTED_ORE.get()));
        add(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ICModItems.FIRE_ASPECTED_ORE.get()));
        add(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ICModItems.METAL_ASPECTED_ORE.get()));
        add(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ICModItems.WATER_ASPECTED_ORE.get()));
        add(ICModBlocks.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), block -> createOreDrop(ICModBlocks.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), ICModItems.WOOD_ASPECTED_ORE.get()));

        // === PLANTS === //

        // Earth Plants
        add(ICModBlocks.YELLOW_EARTH_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(ICModBlocks.YELLOW_EARTH_ROOTS_CROP_BLOCK.get(), ICModItems.YELLOW_EARTH_ROOTS.get(), ICModItems.YELLOW_EARTH_ROOTS.get(), createMatureCropCondition(ICModBlocks.YELLOW_EARTH_ROOTS_CROP_BLOCK.get(), EarthCropBlock.AGE)));

        // Fire Plants
        add(ICModBlocks.RED_FIRE_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(ICModBlocks.RED_FIRE_ROOTS_CROP_BLOCK.get(), ICModItems.RED_FIRE_ROOTS.get(), ICModItems.RED_FIRE_ROOTS.get(), createMatureCropCondition(ICModBlocks.RED_FIRE_ROOTS_CROP_BLOCK.get(), FireCropBlock.AGE)));

        // Metal Plants
        add(ICModBlocks.WHITE_METAL_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(ICModBlocks.WHITE_METAL_ROOTS_CROP_BLOCK.get(), ICModItems.WHITE_METAL_ROOTS.get(), ICModItems.WHITE_METAL_ROOTS.get(), createMatureCropCondition(ICModBlocks.WHITE_METAL_ROOTS_CROP_BLOCK.get(), MetalCropBlock.AGE)));

        // Water Plants
        add(ICModBlocks.BLACK_WATER_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(ICModBlocks.BLACK_WATER_ROOTS_CROP_BLOCK.get(), ICModItems.BLACK_WATER_ROOTS.get(), ICModItems.BLACK_WATER_ROOTS.get(), createMatureCropCondition(ICModBlocks.BLACK_WATER_ROOTS_CROP_BLOCK.get(), WaterCropBlock.AGE)));

        // Wood Plants
        add(ICModBlocks.BLUE_WOOD_ROOTS_CROP_BLOCK.get(), block -> createCropDrops(ICModBlocks.BLUE_WOOD_ROOTS_CROP_BLOCK.get(), ICModItems.BLUE_WOOD_ROOTS.get(), ICModItems.BLUE_WOOD_ROOTS.get(), createMatureCropCondition(ICModBlocks.BLUE_WOOD_ROOTS_CROP_BLOCK.get(), WoodCropBlock.AGE)));

        // === CRAFTING === //

        // Pill Furnaces
        dropSelf(ICModBlocks.PILL_FURNACE_BLOCK.get());
    }

    // == Helper Methods == //

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ICModBlocks.REGISTRY.getEntries().stream().map(Holder::value)::iterator;
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