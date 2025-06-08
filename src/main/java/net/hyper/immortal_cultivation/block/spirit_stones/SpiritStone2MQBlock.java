package net.hyper.immortal_cultivation.block.spirit_stones;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class SpiritStone2MQBlock extends R1SpiritStoneBlock {
    public SpiritStone2MQBlock() {
        super(Properties.of().sound(SoundType.AMETHYST).strength(1.0F,10.0F).requiresCorrectToolForDrops());
    }

    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos){return 8;}
}
