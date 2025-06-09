package net.hyper.immortal_cultivation.block.metals;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class MetalAspectedBlocks {

    public static final Block METAL_ASPECTED_ORE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.STONE)
        .strength(1.5F, 10.0F)
        .requiresCorrectToolForDrops());

}
