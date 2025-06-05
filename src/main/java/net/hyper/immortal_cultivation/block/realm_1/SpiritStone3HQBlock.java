package net.hyper.immortal_cultivation.block.realm_1;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class SpiritStone3HQBlock extends Block {
    public SpiritStone3HQBlock() {
        super(Properties.of().sound(SoundType.AMETHYST).strength(1.0F,15.0F).requiresCorrectToolForDrops());
    }

    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos){return 12;}
}
