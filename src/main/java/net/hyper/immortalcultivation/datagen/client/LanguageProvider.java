package net.hyper.immortalcultivation.datagen.client;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.hyper.immortalcultivation.content.item.ItemRegistry;
import net.minecraft.data.PackOutput;

public class LanguageProvider extends net.neoforged.neoforge.common.data.LanguageProvider {
    private final String locale;

    public LanguageProvider(PackOutput output, String locale) {
        super(output, ImmortalCultivation.MOD_ID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        switch (this.locale) {
            case "en_us" -> addEnglishTranslations();
            case "zh_cn" -> addChineseTranslations();
            default -> addEnglishTranslations(); // Fallback to English
        }
    }

    private void addEnglishTranslations() {
        // === CREATIVE TABS === //
        addCreativeTabTranslations();

        // === KEY BINDINGS === //
        addKeyBindingTranslations();

        // === RACES === //
        addRaceTranslations();

        // === BLOCKS === //
        addBlockTranslations();

        // === ITEMS === //
        addItemTranslations();
    }

    private void addChineseTranslations() {
        // === CREATIVE TABS === //
        add("creativetab.immortalcultivation.immortalcultivation_blocks", "仙修方块");
        add("creativetab.immortalcultivation.immortalcultivation_items", "仙修物品");
        add("creativetab.immortalcultivation.immortalcultivation_tools", "仙修工具");
        add("creativetab.immortalcultivation.immortalcultivation_weapons", "仙修武器");
        add("creativetab.immortalcultivation.immortalcultivation_armors", "仙修防具");

        // === KEY BINDINGS === //
        add("key.categories.immortalcultivation.cultivation", "修炼系统");
        add("key.immortalcultivation.cultivation_stats", "打开修炼状态");
        add("key.immortalcultivation.breakthrough_screen", "打开突破分析");

        // === RACES === //
        add("immortalcultivation.race.human", "人族");
        add("immortalcultivation.race.spirit", "灵族");
        add("immortalcultivation.race.demonic_beast", "妖兽");

        // === SPIRIT STONES === //
        add(ItemRegistry.LOW_QUALITY_SPIRIT_STONE.get(), "低品灵石");
        add(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE.get(), "中品灵石");
        add(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE.get(), "高品灵石");
        add(ItemRegistry.TOP_QUALITY_SPIRIT_STONE.get(), "极品灵石");

        // === BASIC SWORD TRANSLATIONS === //
        add(ItemRegistry.EARTH_ASPECTED_SWORD.get(), "土属性剑");
        add(ItemRegistry.FIRE_ASPECTED_SWORD.get(), "火属性剑");
        add(ItemRegistry.METAL_ASPECTED_SWORD.get(), "金属性剑");
        add(ItemRegistry.WATER_ASPECTED_SWORD.get(), "水属性剑");
        add(ItemRegistry.WOOD_ASPECTED_SWORD.get(), "木属性剑");
        add(ItemRegistry.SKY_IRON_SWORD.get(), "天铁剑");

        // === PILLS === //
        add(ItemRegistry.LOW_QUALITY_QI_CULTIVATION_PILL.get(), "低品练气丹");
        add(ItemRegistry.MEDIUM_QUALITY_QI_CULTIVATION_PILL.get(), "中品练气丹");
        add(ItemRegistry.HIGH_QUALITY_QI_CULTIVATION_PILL.get(), "高品练气丹");
        add(ItemRegistry.TOP_QUALITY_QI_CULTIVATION_PILL.get(), "极品练气丹");

        add(ItemRegistry.LOW_QUALITY_BODY_CULTIVATION_PILL.get(), "低品炼体丹");
        add(ItemRegistry.MEDIUM_QUALITY_BODY_CULTIVATION_PILL.get(), "中品炼体丹");
        add(ItemRegistry.HIGH_QUALITY_BODY_CULTIVATION_PILL.get(), "高品炼体丹");
        add(ItemRegistry.TOP_QUALITY_BODY_CULTIVATION_PILL.get(), "极品炼体丹");

        // Note: For a full Chinese localization, you'd want to translate all items
        // For now, this provides essential Chinese translations for key items
    }

    private void addCreativeTabTranslations() {
        add("creativetab.immortalcultivation.immortalcultivation_blocks", "Immortal Cultivation Blocks");
        add("creativetab.immortalcultivation.immortalcultivation_items", "Immortal Cultivation Items");
        add("creativetab.immortalcultivation.immortalcultivation_tools", "Immortal Cultivation Tools");
        add("creativetab.immortalcultivation.immortalcultivation_weapons", "Immortal Cultivation Weapons");
        add("creativetab.immortalcultivation.immortalcultivation_armors", "Immortal Cultivation Armors");
    }

    private void addKeyBindingTranslations() {
        add("key.categories.immortalcultivation.cultivation", "Cultivation System");
        add("key.immortalcultivation.cultivation_stats", "Open Cultivation Stats");
        add("key.immortalcultivation.breakthrough_screen", "Open Breakthrough Analysis");
    }

    private void addRaceTranslations() {
        add("immortalcultivation.race.human", "Human");
        add("immortalcultivation.race.spirit", "Spirit");
        add("immortalcultivation.race.demonic_beast", "Demonic Beast");
    }

    private void addBlockTranslations() {
        // === SPIRIT STONE BLOCKS === //
        addSpiritStoneBlockTranslations();

        // === ORE BLOCKS === //
        addOreBlockTranslations();

        // === CROP BLOCKS === //
        addCropBlockTranslations();

        // === CRAFTING BLOCKS === //
        addCraftingBlockTranslations();
    }

    private void addSpiritStoneBlockTranslations() {
        // Low Quality Spirit Stone Blocks
        add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK.get(), "Low Quality Spirit Stone Block");
        add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get(), "Compressed Low Quality Spirit Stone Block");

        // Medium Quality Spirit Stone Blocks
        add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get(), "Medium Quality Spirit Stone Block");
        add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get(), "Compressed Medium Quality Spirit Stone Block");

        // High Quality Spirit Stone Blocks
        add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get(), "High Quality Spirit Stone Block");
        add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get(), "Compressed High Quality Spirit Stone Block");

        // Top Quality Spirit Stone Blocks
        add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get(), "Top Quality Spirit Stone Block");
        add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK.get(), "Compressed Top Quality Spirit Stone Block");
    }

    private void addOreBlockTranslations() {
        // Spirit Stone Ores
        add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), "Low Quality Spirit Stone Ore");
        add(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), "Low Quality Spirit Stone Deepslate Ore");
        add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK.get(), "Medium Quality Spirit Stone Ore");
        add(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), "Medium Quality Spirit Stone Deepslate Ore");
        add(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), "High Quality Spirit Stone Deepslate Ore");
        add(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK.get(), "Top Quality Spirit Stone Deepslate Ore");

        // Aspected Ores
        add(BlockRegistry.EARTH_ASPECTED_ORE_BLOCK.get(), "Earth-Aspected Ore");
        add(BlockRegistry.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), "Earth-Aspected Deepslate Ore");
        add(BlockRegistry.FIRE_ASPECTED_ORE_BLOCK.get(), "Fire-Aspected Ore");
        add(BlockRegistry.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), "Fire-Aspected Deepslate Ore");
        add(BlockRegistry.METAL_ASPECTED_ORE_BLOCK.get(), "Metal-Aspected Ore");
        add(BlockRegistry.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), "Metal-Aspected Deepslate Ore");
        add(BlockRegistry.WATER_ASPECTED_ORE_BLOCK.get(), "Water-Aspected Ore");
        add(BlockRegistry.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), "Water-Aspected Deepslate Ore");
        add(BlockRegistry.WOOD_ASPECTED_ORE_BLOCK.get(), "Wood-Aspected Ore");
        add(BlockRegistry.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK.get(), "Wood-Aspected Deepslate Ore");

        // Sky Iron Ore
        add(BlockRegistry.SKY_IRON_ORE_BLOCK.get(), "Sky Iron Ore");
    }

    private void addCropBlockTranslations() {
        add(BlockRegistry.YELLOW_EARTH_ROOTS_CROP_BLOCK.get(), "Yellow Earth Roots Crop");
        // Note: Add other crop blocks when they're implemented
    }

    private void addCraftingBlockTranslations() {
        add(BlockRegistry.PILL_FURNACE_BLOCK.get(), "Pill Furnace");
    }

    private void addItemTranslations() {
        // === SPIRIT STONES === //
        addSpiritStoneItemTranslations();

        // === ORES AND INGOTS === //
        addOreAndIngotTranslations();

        // === TOOLS === //
        addToolTranslations();

        // === WEAPONS === //
        addWeaponTranslations();

        // === ARMOR === //
        addArmorTranslations();

        // === PLANTS === //
        addPlantTranslations();

        // === PILLS === //
        addPillTranslations();
    }

    private void addSpiritStoneItemTranslations() {
        add(ItemRegistry.LOW_QUALITY_SPIRIT_STONE.get(), "Low Quality Spirit Stone");
        add(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE.get(), "Medium Quality Spirit Stone");
        add(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE.get(), "High Quality Spirit Stone");
        add(ItemRegistry.TOP_QUALITY_SPIRIT_STONE.get(), "Top Quality Spirit Stone");
    }

    private void addOreAndIngotTranslations() {
        // Earth Aspected
        add(ItemRegistry.EARTH_ASPECTED_ORE.get(), "Raw Earth-Aspected Ore");
        add(ItemRegistry.EARTH_ASPECTED_INGOT.get(), "Earth-Aspected Ingot");

        // Fire Aspected
        add(ItemRegistry.FIRE_ASPECTED_ORE.get(), "Raw Fire-Aspected Ore");
        add(ItemRegistry.FIRE_ASPECTED_INGOT.get(), "Fire-Aspected Ingot");

        // Metal Aspected
        add(ItemRegistry.METAL_ASPECTED_ORE.get(), "Raw Metal-Aspected Ore");
        add(ItemRegistry.METAL_ASPECTED_INGOT.get(), "Metal-Aspected Ingot");

        // Water Aspected
        add(ItemRegistry.WATER_ASPECTED_ORE.get(), "Raw Water-Aspected Ore");
        add(ItemRegistry.WATER_ASPECTED_INGOT.get(), "Water-Aspected Ingot");

        // Wood Aspected
        add(ItemRegistry.WOOD_ASPECTED_ORE.get(), "Raw Wood-Aspected Ore");
        add(ItemRegistry.WOOD_ASPECTED_INGOT.get(), "Wood-Aspected Ingot");

        // Sky Iron
        add(ItemRegistry.SKY_IRON_ORE.get(), "Raw Sky Iron Ore");
        add(ItemRegistry.SKY_IRON_INGOT.get(), "Sky Iron Ingot");
    }

    private void addToolTranslations() {
        // Pickaxes
        add(ItemRegistry.EARTH_ASPECTED_PICKAXE.get(), "Earth-Aspected Pickaxe");
        add(ItemRegistry.FIRE_ASPECTED_PICKAXE.get(), "Fire-Aspected Pickaxe");
        add(ItemRegistry.METAL_ASPECTED_PICKAXE.get(), "Metal-Aspected Pickaxe");
        add(ItemRegistry.WATER_ASPECTED_PICKAXE.get(), "Water-Aspected Pickaxe");
        add(ItemRegistry.WOOD_ASPECTED_PICKAXE.get(), "Wood-Aspected Pickaxe");
        add(ItemRegistry.SKY_IRON_PICKAXE.get(), "Sky Iron Pickaxe");

        // Axes
        add(ItemRegistry.EARTH_ASPECTED_AXE.get(), "Earth-Aspected Axe");
        add(ItemRegistry.FIRE_ASPECTED_AXE.get(), "Fire-Aspected Axe");
        add(ItemRegistry.METAL_ASPECTED_AXE.get(), "Metal-Aspected Axe");
        add(ItemRegistry.WATER_ASPECTED_AXE.get(), "Water-Aspected Axe");
        add(ItemRegistry.WOOD_ASPECTED_AXE.get(), "Wood-Aspected Axe");
        add(ItemRegistry.SKY_IRON_AXE.get(), "Sky Iron Axe");

        // Shovels
        add(ItemRegistry.EARTH_ASPECTED_SHOVEL.get(), "Earth-Aspected Shovel");
        add(ItemRegistry.FIRE_ASPECTED_SHOVEL.get(), "Fire-Aspected Shovel");
        add(ItemRegistry.METAL_ASPECTED_SHOVEL.get(), "Metal-Aspected Shovel");
        add(ItemRegistry.WATER_ASPECTED_SHOVEL.get(), "Water-Aspected Shovel");
        add(ItemRegistry.WOOD_ASPECTED_SHOVEL.get(), "Wood-Aspected Shovel");
        add(ItemRegistry.SKY_IRON_SHOVEL.get(), "Sky Iron Shovel");

        // Hoes
        add(ItemRegistry.EARTH_ASPECTED_HOE.get(), "Earth-Aspected Hoe");
        add(ItemRegistry.FIRE_ASPECTED_HOE.get(), "Fire-Aspected Hoe");
        add(ItemRegistry.METAL_ASPECTED_HOE.get(), "Metal-Aspected Hoe");
        add(ItemRegistry.WATER_ASPECTED_HOE.get(), "Water-Aspected Hoe");
        add(ItemRegistry.WOOD_ASPECTED_HOE.get(), "Wood-Aspected Hoe");
        add(ItemRegistry.SKY_IRON_HOE.get(), "Sky Iron Hoe");
    }

    private void addWeaponTranslations() {
        add(ItemRegistry.EARTH_ASPECTED_SWORD.get(), "Earth-Aspected Sword");
        add(ItemRegistry.FIRE_ASPECTED_SWORD.get(), "Fire-Aspected Sword");
        add(ItemRegistry.METAL_ASPECTED_SWORD.get(), "Metal-Aspected Sword");
        add(ItemRegistry.WATER_ASPECTED_SWORD.get(), "Water-Aspected Sword");
        add(ItemRegistry.WOOD_ASPECTED_SWORD.get(), "Wood-Aspected Sword");
        add(ItemRegistry.SKY_IRON_SWORD.get(), "Sky Iron Sword");
    }

    private void addArmorTranslations() {
        // Helmets
        add(ItemRegistry.EARTH_ASPECTED_HELMET.get(), "Earth-Aspected Helmet");
        add(ItemRegistry.FIRE_ASPECTED_HELMET.get(), "Fire-Aspected Helmet");
        add(ItemRegistry.METAL_ASPECTED_HELMET.get(), "Metal-Aspected Helmet");
        add(ItemRegistry.WATER_ASPECTED_HELMET.get(), "Water-Aspected Helmet");
        add(ItemRegistry.WOOD_ASPECTED_HELMET.get(), "Wood-Aspected Helmet");
        add(ItemRegistry.SKY_IRON_HELMET.get(), "Sky Iron Helmet");

        // Chestplates
        add(ItemRegistry.EARTH_ASPECTED_CHESTPLATE.get(), "Earth-Aspected Chestplate");
        add(ItemRegistry.FIRE_ASPECTED_CHESTPLATE.get(), "Fire-Aspected Chestplate");
        add(ItemRegistry.METAL_ASPECTED_CHESTPLATE.get(), "Metal-Aspected Chestplate");
        add(ItemRegistry.WATER_ASPECTED_CHESTPLATE.get(), "Water-Aspected Chestplate");
        add(ItemRegistry.WOOD_ASPECTED_CHESTPLATE.get(), "Wood-Aspected Chestplate");
        add(ItemRegistry.SKY_IRON_CHESTPLATE.get(), "Sky Iron Chestplate");

        // Leggings
        add(ItemRegistry.EARTH_ASPECTED_LEGGINGS.get(), "Earth-Aspected Leggings");
        add(ItemRegistry.FIRE_ASPECTED_LEGGINGS.get(), "Fire-Aspected Leggings");
        add(ItemRegistry.METAL_ASPECTED_LEGGINGS.get(), "Metal-Aspected Leggings");
        add(ItemRegistry.WATER_ASPECTED_LEGGINGS.get(), "Water-Aspected Leggings");
        add(ItemRegistry.WOOD_ASPECTED_LEGGINGS.get(), "Wood-Aspected Leggings");
        add(ItemRegistry.SKY_IRON_LEGGINGS.get(), "Sky Iron Leggings");

        // Boots
        add(ItemRegistry.EARTH_ASPECTED_BOOTS.get(), "Earth-Aspected Boots");
        add(ItemRegistry.FIRE_ASPECTED_BOOTS.get(), "Fire-Aspected Boots");
        add(ItemRegistry.METAL_ASPECTED_BOOTS.get(), "Metal-Aspected Boots");
        add(ItemRegistry.WATER_ASPECTED_BOOTS.get(), "Water-Aspected Boots");
        add(ItemRegistry.WOOD_ASPECTED_BOOTS.get(), "Wood-Aspected Boots");
        add(ItemRegistry.SKY_IRON_BOOTS.get(), "Sky Iron Boots");
    }

    private void addPlantTranslations() {
        add(ItemRegistry.YELLOW_EARTH_ROOTS.get(), "Yellow Earth Roots");
        add(ItemRegistry.RED_FIRE_ROOTS.get(), "Red Fire Roots");
        add(ItemRegistry.WHITE_METAL_ROOTS.get(), "White Metal Roots");
        add(ItemRegistry.BLACK_WATER_ROOTS.get(), "Black Water Roots");
        add(ItemRegistry.BLUE_WOOD_ROOTS.get(), "Blue Wood Roots");
    }

    private void addPillTranslations() {
        // Qi Cultivation Pills
        add(ItemRegistry.LOW_QUALITY_QI_CULTIVATION_PILL.get(), "Low Quality Qi Cultivation Pill");
        add(ItemRegistry.MEDIUM_QUALITY_QI_CULTIVATION_PILL.get(), "Medium Quality Qi Cultivation Pill");
        add(ItemRegistry.HIGH_QUALITY_QI_CULTIVATION_PILL.get(), "High Quality Qi Cultivation Pill");
        add(ItemRegistry.TOP_QUALITY_QI_CULTIVATION_PILL.get(), "Top Quality Qi Cultivation Pill");

        // Body Cultivation Pills
        add(ItemRegistry.LOW_QUALITY_BODY_CULTIVATION_PILL.get(), "Low Quality Body Cultivation Pill");
        add(ItemRegistry.MEDIUM_QUALITY_BODY_CULTIVATION_PILL.get(), "Medium Quality Body Cultivation Pill");
        add(ItemRegistry.HIGH_QUALITY_BODY_CULTIVATION_PILL.get(), "High Quality Body Cultivation Pill");
        add(ItemRegistry.TOP_QUALITY_BODY_CULTIVATION_PILL.get(), "Top Quality Body Cultivation Pill");
    }
}