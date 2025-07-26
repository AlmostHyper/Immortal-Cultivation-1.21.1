package net.hyper.immortalcultivation.client.gui;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.content.block.BlockRegistry;
import net.hyper.immortalcultivation.content.item.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CreativeTabsRegistry {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmortalCultivation.MOD_ID);

    public static final Supplier<CreativeModeTab> immortalcultivation_BLOCKS_TAB = REGISTRY.register("immortalcultivation_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK.get()))
                    .title(Component.translatable("creativetab.immortalcultivation.immortalcultivation_blocks"))
                    .displayItems((parameters, output) -> {
                        output.accept(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK);
                        output.accept(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
                        output.accept(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_BLOCK);
                        output.accept(BlockRegistry.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
                        output.accept(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK);
                        output.accept(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
                        output.accept(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK);
                        output.accept(BlockRegistry.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
                        output.accept(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
                        output.accept(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_BLOCK);
                        output.accept(BlockRegistry.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
                        output.accept(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
                        output.accept(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_BLOCK);
                        output.accept(BlockRegistry.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
                        output.accept(BlockRegistry.EARTH_ASPECTED_ORE_BLOCK);
                        output.accept(BlockRegistry.EARTH_ASPECTED_DEEPSLATE_ORE_BLOCK);
                        output.accept(BlockRegistry.FIRE_ASPECTED_ORE_BLOCK);
                        output.accept(BlockRegistry.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK);
                        output.accept(BlockRegistry.METAL_ASPECTED_ORE_BLOCK);
                        output.accept(BlockRegistry.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK);
                        output.accept(BlockRegistry.WATER_ASPECTED_ORE_BLOCK);
                        output.accept(BlockRegistry.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK);
                        output.accept(BlockRegistry.WOOD_ASPECTED_ORE_BLOCK);
                        output.accept(BlockRegistry.WOOD_ASPECTED_DEEPSLATE_ORE_BLOCK);
                        output.accept(BlockRegistry.SKY_IRON_ORE_BLOCK);
                        output.accept(BlockRegistry.PILL_FURNACE_BLOCK);
                    }).build());

    public static final Supplier<CreativeModeTab> immortalcultivation_ITEMS_TAB = REGISTRY.register("immortalcultivation_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ItemRegistry.TOP_QUALITY_SPIRIT_STONE.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "immortalcultivation_blocks_tab"))
                    .title(Component.translatable("creativetab.immortalcultivation.immortalcultivation_items"))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.LOW_QUALITY_SPIRIT_STONE);
                        output.accept(ItemRegistry.MEDIUM_QUALITY_SPIRIT_STONE);
                        output.accept(ItemRegistry.HIGH_QUALITY_SPIRIT_STONE);
                        output.accept(ItemRegistry.TOP_QUALITY_SPIRIT_STONE);
                        output.accept(ItemRegistry.EARTH_ASPECTED_ORE);
                        output.accept(ItemRegistry.EARTH_ASPECTED_INGOT);
                        output.accept(ItemRegistry.FIRE_ASPECTED_ORE);
                        output.accept(ItemRegistry.FIRE_ASPECTED_INGOT);
                        output.accept(ItemRegistry.METAL_ASPECTED_ORE);
                        output.accept(ItemRegistry.METAL_ASPECTED_INGOT);
                        output.accept(ItemRegistry.SKY_IRON_ORE);
                        output.accept(ItemRegistry.SKY_IRON_INGOT);
                        output.accept(ItemRegistry.WATER_ASPECTED_ORE);
                        output.accept(ItemRegistry.WATER_ASPECTED_INGOT);
                        output.accept(ItemRegistry.WOOD_ASPECTED_ORE);
                        output.accept(ItemRegistry.WOOD_ASPECTED_INGOT);
                        output.accept(ItemRegistry.YELLOW_EARTH_ROOTS);
                        output.accept(ItemRegistry.RED_FIRE_ROOTS);
                        output.accept(ItemRegistry.WHITE_METAL_ROOTS);
                        output.accept(ItemRegistry.BLACK_WATER_ROOTS);
                        output.accept(ItemRegistry.BLUE_WOOD_ROOTS);

                        // === Pills === //
                        /*
                        output.accept(ItemRegistry.LOW_QUALITY_QI_CULTIVATION_PILL);
                        output.accept(ItemRegistry.MEDIUM_QUALITY_QI_CULTIVATION_PILL);
                        output.accept(ItemRegistry.HIGH_QUALITY_QI_CULTIVATION_PILL);
                        output.accept(ItemRegistry.TOP_QUALITY_QI_CULTIVATION_PILL);
                        output.accept(ItemRegistry.LOW_QUALITY_BODY_CULTIVATION_PILL);
                        output.accept(ItemRegistry.MEDIUM_QUALITY_BODY_CULTIVATION_PILL);
                        output.accept(ItemRegistry.HIGH_QUALITY_BODY_CULTIVATION_PILL);
                        output.accept(ItemRegistry.TOP_QUALITY_BODY_CULTIVATION_PILL);
                    */
                    }).build());


    public static final Supplier<CreativeModeTab> immortalcultivation_TOOLS_TAB = REGISTRY.register("immortalcultivation_tools_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ItemRegistry.SKY_IRON_PICKAXE.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "immortalcultivation_items_tab"))
                    .title(Component.translatable("creativetab.immortalcultivation.immortalcultivation_tools"))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.EARTH_ASPECTED_SHOVEL);
                        output.accept(ItemRegistry.EARTH_ASPECTED_PICKAXE);
                        output.accept(ItemRegistry.EARTH_ASPECTED_AXE);
                        output.accept(ItemRegistry.EARTH_ASPECTED_HOE);
                        output.accept(ItemRegistry.FIRE_ASPECTED_SHOVEL);
                        output.accept(ItemRegistry.FIRE_ASPECTED_PICKAXE);
                        output.accept(ItemRegistry.FIRE_ASPECTED_AXE);
                        output.accept(ItemRegistry.FIRE_ASPECTED_HOE);
                        output.accept(ItemRegistry.METAL_ASPECTED_SHOVEL);
                        output.accept(ItemRegistry.METAL_ASPECTED_PICKAXE);
                        output.accept(ItemRegistry.METAL_ASPECTED_AXE);
                        output.accept(ItemRegistry.METAL_ASPECTED_HOE);
                        output.accept(ItemRegistry.WATER_ASPECTED_SHOVEL);
                        output.accept(ItemRegistry.WATER_ASPECTED_PICKAXE);
                        output.accept(ItemRegistry.WATER_ASPECTED_AXE);
                        output.accept(ItemRegistry.WATER_ASPECTED_HOE);
                        output.accept(ItemRegistry.WOOD_ASPECTED_SHOVEL);
                        output.accept(ItemRegistry.WOOD_ASPECTED_PICKAXE);
                        output.accept(ItemRegistry.WOOD_ASPECTED_AXE);
                        output.accept(ItemRegistry.WOOD_ASPECTED_HOE);
                        output.accept(ItemRegistry.SKY_IRON_SHOVEL);
                        output.accept(ItemRegistry.SKY_IRON_PICKAXE);
                        output.accept(ItemRegistry.SKY_IRON_AXE);
                        output.accept(ItemRegistry.SKY_IRON_HOE);

                    }).build());

    public static final Supplier<CreativeModeTab> immortalcultivation_WEAPONS_TAB = REGISTRY.register("immortalcultivation_weapons_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ItemRegistry.SKY_IRON_SWORD.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "immortalcultivation_tools_tab"))
                    .title(Component.translatable("creativetab.immortalcultivation.immortalcultivation_weapons"))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.EARTH_ASPECTED_SWORD);
                        output.accept(ItemRegistry.FIRE_ASPECTED_SWORD);
                        output.accept(ItemRegistry.METAL_ASPECTED_SWORD);
                        output.accept(ItemRegistry.WATER_ASPECTED_SWORD);
                        output.accept(ItemRegistry.WOOD_ASPECTED_SWORD);
                        output.accept(ItemRegistry.SKY_IRON_SWORD);
                        output.accept(ItemRegistry.EARTH_ASPECTED_AXE);
                        output.accept(ItemRegistry.FIRE_ASPECTED_AXE);
                        output.accept(ItemRegistry.METAL_ASPECTED_AXE);
                        output.accept(ItemRegistry.WATER_ASPECTED_AXE);
                        output.accept(ItemRegistry.WOOD_ASPECTED_AXE);
                        output.accept(ItemRegistry.SKY_IRON_AXE);
                    }).build());

    public static final Supplier<CreativeModeTab> immortalcultivation_ARMORS_TAB = REGISTRY.register("immortalcultivation_armors_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ItemRegistry.SKY_IRON_HELMET.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "immortalcultivation_weapons_tab"))
                    .title(Component.translatable("creativetab.immortalcultivation.immortalcultivation_armors"))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.EARTH_ASPECTED_HELMET);
                        output.accept(ItemRegistry.EARTH_ASPECTED_CHESTPLATE);
                        output.accept(ItemRegistry.EARTH_ASPECTED_LEGGINGS);
                        output.accept(ItemRegistry.EARTH_ASPECTED_BOOTS);
                        output.accept(ItemRegistry.FIRE_ASPECTED_HELMET);
                        output.accept(ItemRegistry.FIRE_ASPECTED_CHESTPLATE);
                        output.accept(ItemRegistry.FIRE_ASPECTED_LEGGINGS);
                        output.accept(ItemRegistry.FIRE_ASPECTED_BOOTS);
                        output.accept(ItemRegistry.METAL_ASPECTED_HELMET);
                        output.accept(ItemRegistry.METAL_ASPECTED_CHESTPLATE);
                        output.accept(ItemRegistry.METAL_ASPECTED_LEGGINGS);
                        output.accept(ItemRegistry.METAL_ASPECTED_BOOTS);
                        output.accept(ItemRegistry.WATER_ASPECTED_HELMET);
                        output.accept(ItemRegistry.WATER_ASPECTED_CHESTPLATE);
                        output.accept(ItemRegistry.WATER_ASPECTED_LEGGINGS);
                        output.accept(ItemRegistry.WATER_ASPECTED_BOOTS);
                        output.accept(ItemRegistry.WOOD_ASPECTED_HELMET);
                        output.accept(ItemRegistry.WOOD_ASPECTED_CHESTPLATE);
                        output.accept(ItemRegistry.WOOD_ASPECTED_LEGGINGS);
                        output.accept(ItemRegistry.WOOD_ASPECTED_BOOTS);
                        output.accept(ItemRegistry.SKY_IRON_HELMET);
                        output.accept(ItemRegistry.SKY_IRON_CHESTPLATE);
                        output.accept(ItemRegistry.SKY_IRON_LEGGINGS);
                        output.accept(ItemRegistry.SKY_IRON_BOOTS);

                    }).build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}

