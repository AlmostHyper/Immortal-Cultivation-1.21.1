package net.hyper.immortal_cultivation.block.realm_1;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SpiritStone1LQBlock extends Block {
    public SpiritStone1LQBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.AMETHYST).strength(1f).explosionResistance(5f).requiresCorrectToolForDrops());

    }

    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos){return 5;}
}
