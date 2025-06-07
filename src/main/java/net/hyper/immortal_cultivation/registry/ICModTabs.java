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

                    }).build());

    public static final Supplier<CreativeModeTab> IMMORTAL_CULTIVATION_ITEMS_TAB = REGISTRY.register("immortal_cultivation_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(ImmortalCultivation.MOD_ID, "immortal_cultivation_blocks_tab"))
                    .title(Component.translatable("creativetab.immortal_cultivation.immortal_cultivation_items"))
                    .displayItems((parameters, output) -> {
                        output.accept(ICModItems.LOW_QUALITY_SPIRIT_STONE);
                        output.accept(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE);
                        output.accept(ICModItems.HIGH_QUALITY_SPIRIT_STONE);
                        output.accept(ICModItems.TOP_QUALITY_SPIRIT_STONE);

                    }).build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }


}

