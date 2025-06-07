package net.hyper.immortal_cultivation.block.realm_1.spirit_stones;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class SpiritStone2MQOreBlock extends Block {
    public SpiritStone2MQOreBlock() {
        super(Properties.of().sound(SoundType.STONE).strength(1f).explosionResistance(5f).requiresCorrectToolForDrops());

    }

    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos){return 5;}
}
