package net.hyper.immortalcultivation.common.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.content.block.entities.PillFurnaceBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ICModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ImmortalCultivation.MOD_ID);

    // Basic Pill Furnace (Stone tier)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PillFurnaceBlockEntity>> PILL_FURNACE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("pill_furnace",
            () -> BlockEntityType.Builder.of(PillFurnaceBlockEntity::new,
                    ICModBlocks.PILL_FURNACE_BLOCK.get())
                .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
