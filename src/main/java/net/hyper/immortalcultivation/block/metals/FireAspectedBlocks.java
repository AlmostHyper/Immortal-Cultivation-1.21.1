package net.hyper.immortalcultivation.block.metals;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class FireAspectedBlocks {

    public static Supplier<Block> FIRE_ASPECTED_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.STONE)
            .strength(1.5F, 10.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> FIRE_ASPECTED_DEEPSLATE_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.STONE)
            .strength(1.5F, 10.0F)
            .requiresCorrectToolForDrops());
    }
}
