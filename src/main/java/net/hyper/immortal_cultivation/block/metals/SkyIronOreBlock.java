package net.hyper.immortal_cultivation.block.metals;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class SkyIronOreBlock extends Block {
    public SkyIronOreBlock() {
        super(Properties.of().sound(SoundType.STONE).strength(1.0F,5.0F).requiresCorrectToolForDrops());
    }
}
