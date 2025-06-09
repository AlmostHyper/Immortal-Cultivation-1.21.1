package net.hyper.immortal_cultivation.block.metals;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class WaterAspectedBlocks {

    public static final Block WATER_ASPECTED_ORE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.STONE)
        .strength(1.5F, 10.0F)
        .requiresCorrectToolForDrops());

    public static final Block WATER_ASPECTED_DEEPSLATE_ORE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.STONE)
        .strength(1.5F, 10.0F)
        .requiresCorrectToolForDrops());

}
