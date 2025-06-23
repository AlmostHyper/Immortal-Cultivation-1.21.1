package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.block.crafting.IronPillFurnaceBlock;
import net.hyper.immortalcultivation.block.crafting.PillFurnaceBlock;
import net.hyper.immortalcultivation.block.entities.PillFurnaceBlockEntity;
import net.hyper.immortalcultivation.block.spirit_stones.R1SpiritStoneBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

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
