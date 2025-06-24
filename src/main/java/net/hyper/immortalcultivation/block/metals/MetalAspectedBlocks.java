package net.hyper.immortalcultivation.block.metals;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class MetalAspectedBlocks {

    public static Supplier<Block> METAL_ASPECTED_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.STONE)
            .strength(2.0f, 10.0f)
            .requiresCorrectToolForDrops());
    }

    public static Supplier<Block> METAL_ASPECTED_DEEPSLATE_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.STONE)
            .strength(2.0f, 10.0f)
            .requiresCorrectToolForDrops());
    }
}
