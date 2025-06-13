package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class ICModComponentTypes {
    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ImmortalCultivation.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LAST_DAMAGE_TYPE_RECEIVED = register("last_damage_type_received",
        builder -> builder.persistent(BlockPos.CODEC));


    // Helper Methods
    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return REGISTRY.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }
}
