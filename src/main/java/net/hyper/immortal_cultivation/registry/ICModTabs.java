package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ICModTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmortalCultivation.MOD_ID);

    public static final Supplier<CreativeModeTab> IMMORTAL_CULTIVATION_ITEMS_TAB = REGISTRY.register("immortal_cultivation_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ICModItems.LOW_QUALITY_SPIRIT_STONE.get()))
                    .title(Component.translatable("creativetab.immortal_cultivation.immortal_cultivation_items_tab"))
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