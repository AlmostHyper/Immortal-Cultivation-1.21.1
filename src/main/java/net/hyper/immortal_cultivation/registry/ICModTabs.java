package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@EventBusSubscriber(
        bus = EventBusSubscriber.Bus.MOD
)
public class ICModTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY;
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ICCREATIVETAB;

    @SubscribeEvent
    public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
        if (tabData.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            tabData.accept((Block)ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get());
            tabData.accept((Block)ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get());
            tabData.accept((Block)ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get());
            tabData.accept((Block)ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get());

        }

    }

    static {
        REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "xianxia");
        ICCREATIVETAB = REGISTRY.register("immortal_cultivation_blocks_tab",
                () -> CreativeModeTab.builder()
                        .title(Component.translatable("item_group.xianxia.xianxiacreativetab"))
                        .icon(() -> new ItemStack((ItemLike) ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get()))
                        .displayItems((parameters, tabData) -> {
                            tabData.accept((Block) ICModBlocks.LOW_QUALITY_SPIRIT_STONE_BLOCK.get());
                            tabData.accept((Block) ICModBlocks.MEDIUM_QUALITY_SPIRIT_STONE_BLOCK.get());
                            tabData.accept((Block) ICModBlocks.HIGH_QUALITY_SPIRIT_STONE_BLOCK.get());
                            tabData.accept((Block) ICModBlocks.TOP_QUALITY_SPIRIT_STONE_BLOCK.get());
                            tabData.accept((ItemLike) ICModItems.LOW_QUALITY_SPIRIT_STONE.get());
                            tabData.accept((ItemLike) ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get());
                            tabData.accept((ItemLike) ICModItems.HIGH_QUALITY_SPIRIT_STONE.get());
                            tabData.accept((ItemLike) ICModItems.TOP_QUALITY_SPIRIT_STONE.get());

                        }).build());

    }
}