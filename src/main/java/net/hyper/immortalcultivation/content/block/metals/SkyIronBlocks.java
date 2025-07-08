package net.hyper.immortalcultivation.content.block.metals;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class SkyIronBlocks {

    public static Supplier<Block> SKY_IRON_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.STONE)
            .strength(2.5F, 10.0F)
            .requiresCorrectToolForDrops());
    }
}
