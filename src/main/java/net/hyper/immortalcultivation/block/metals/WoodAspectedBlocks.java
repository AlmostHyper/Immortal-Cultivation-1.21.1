package net.hyper.immortalcultivation.block.metals;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class WoodAspectedBlocks {

    public static Supplier<Block> WOOD_ASPECTED_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.STONE)
            .strength(1.5F, 10.0F)
            .requiresCorrectToolForDrops());
    }
}
