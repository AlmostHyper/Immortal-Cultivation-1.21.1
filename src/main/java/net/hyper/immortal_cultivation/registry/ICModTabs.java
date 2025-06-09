package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ICModTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmortalCultivation.MOD_ID);

    public static final Supplier<CreativeModeTab> IMMORTAL_CULTIVATION_BLOCKS_TAB = REGISTRY.register("immortal_cultivation_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get()))
                    .title(Component.translatable("creativetab.immortal_cultivation.immortal_cultivation_blocks"))
                    .displayItems((parameters, output) -> {
                        output.accept(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK);
                        output.accept(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
                        output.accept(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK);
                        output.accept(ICModBlocks.LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
                        output.accept(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK);
                        output.accept(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
                        output.accept(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK);
                        output.accept(ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
                        output.accept(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
                        output.accept(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK);
                        output.accept(ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
                        output.accept(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK);
                        output.accept(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK);
                        output.accept(ICModBlocks.TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK);
                        output.accept(ICModBlocks.EARTH_ASPECTED_ORE_BLOCK);
                        output.accept(ICModBlocks.FIRE_ASPECTED_ORE_BLOCK);
                        output.accept(ICModBlocks.FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK);
                        output.accept(ICModBlocks.METAL_ASPECTED_DEEPSLATE_ORE_BLOCK);
                        output.accept(ICModBlocks.SKY_IRON_ORE_BLOCK);
                        output.accept(ICModBlocks.WATER_ASPECTED_ORE_BLOCK);
                        output.accept(ICModBlocks.WATER_ASPECTED_DEEPSLATE_ORE_BLOCK);
                    }).build());

    public static final Supplier<CreativeModeTab> IMMORTAL_CULTIVATION_ITEMS_TAB = REGISTRY.register("immortal_cultivation_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ICModItems.TOP_QUALITY_SPIRIT_STONE.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "immortal_cultivation_blocks_tab"))
                    .title(Component.translatable("creativetab.immortal_cultivation.immortal_cultivation_items"))
                    .displayItems((parameters, output) -> {
                        output.accept(ICModItems.LOW_QUALITY_SPIRIT_STONE);
                        output.accept(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE);
                        output.accept(ICModItems.HIGH_QUALITY_SPIRIT_STONE);
                        output.accept(ICModItems.TOP_QUALITY_SPIRIT_STONE);
                        output.accept(ICModItems.EARTH_ASPECTED_ORE);
                        output.accept(ICModItems.EARTH_ASPECTED_INGOT);
                        output.accept(ICModItems.FIRE_ASPECTED_ORE);
                        output.accept(ICModItems.FIRE_ASPECTED_INGOT);
                        output.accept(ICModItems.METAL_ASPECTED_ORE);
                        output.accept(ICModItems.METAL_ASPECTED_INGOT);
                        output.accept(ICModItems.SKY_IRON_ORE);
                        output.accept(ICModItems.SKY_IRON_INGOT);
                        output.accept(ICModItems.WATER_ASPECTED_ORE);
                        output.accept(ICModItems.WATER_ASPECTED_INGOT);
                        output.accept(ICModItems.WOOD_ASPECTED_ORE);
                        output.accept(ICModItems.WOOD_ASPECTED_INGOT);
                    }).build());

    public static final Supplier<CreativeModeTab> IMMORTAL_CULTIVATION_TOOLS_TAB = REGISTRY.register("immortal_cultivation_tools_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ICModItems.SKY_IRON_PICKAXE.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "immortal_cultivation_items_tab"))
                    .title(Component.translatable("creativetab.immortal_cultivation.immortal_cultivation_tools"))
                    .displayItems((parameters, output) -> {
                        output.accept(ICModItems.EARTH_ASPECTED_PICKAXE);
                        output.accept(ICModItems.EARTH_ASPECTED_AXE);
                        output.accept(ICModItems.EARTH_ASPECTED_SHOVEL);
                        output.accept(ICModItems.EARTH_ASPECTED_HOE);
                        output.accept(ICModItems.FIRE_ASPECTED_PICKAXE);
                        output.accept(ICModItems.FIRE_ASPECTED_AXE);
                        output.accept(ICModItems.FIRE_ASPECTED_SHOVEL);
                        output.accept(ICModItems.FIRE_ASPECTED_HOE);
                        output.accept(ICModItems.METAL_ASPECTED_PICKAXE);
                        output.accept(ICModItems.METAL_ASPECTED_AXE);
                        output.accept(ICModItems.METAL_ASPECTED_SHOVEL);
                        output.accept(ICModItems.METAL_ASPECTED_HOE);
                        output.accept(ICModItems.WATER_ASPECTED_PICKAXE);
                        output.accept(ICModItems.WATER_ASPECTED_AXE);
                        output.accept(ICModItems.WATER_ASPECTED_SHOVEL);
                        output.accept(ICModItems.WATER_ASPECTED_HOE);
                        output.accept(ICModItems.WOOD_ASPECTED_PICKAXE);
                        output.accept(ICModItems.WOOD_ASPECTED_AXE);
                        output.accept(ICModItems.WOOD_ASPECTED_SHOVEL);
                        output.accept(ICModItems.WOOD_ASPECTED_HOE);
                        output.accept(ICModItems.SKY_IRON_PICKAXE);
                        output.accept(ICModItems.SKY_IRON_AXE);
                        output.accept(ICModItems.SKY_IRON_SHOVEL);
                        output.accept(ICModItems.SKY_IRON_HOE);
                    }).build());

    public static final Supplier<CreativeModeTab> IMMORTAL_CULTIVATION_WEAPONS_TAB = REGISTRY.register("immortal_cultivation_weapons_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ICModItems.SKY_IRON_SWORD.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "immortal_cultivation_tools_tab"))
                    .title(Component.translatable("creativetab.immortal_cultivation.immortal_cultivation_weapons"))
                    .displayItems((parameters, output) -> {
                        output.accept(ICModItems.EARTH_ASPECTED_SWORD);
                        output.accept(ICModItems.FIRE_ASPECTED_SWORD);
                        output.accept(ICModItems.METAL_ASPECTED_SWORD);
                        output.accept(ICModItems.WATER_ASPECTED_SWORD);
                        output.accept(ICModItems.WOOD_ASPECTED_SWORD);
                        output.accept(ICModItems.SKY_IRON_SWORD);
                    }).build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}

