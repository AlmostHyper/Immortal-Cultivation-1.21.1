package net.hyper.immortal_cultivation.block.metals;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class SkyIronBlocks {

    public static final Block SKY_IRON_ORE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.STONE)
        .strength(2.0F, 10.0F)
        .requiresCorrectToolForDrops());

}
