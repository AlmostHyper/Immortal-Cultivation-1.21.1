package net.hyper.immortalcultivation.content.item.crops;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class ICModCropItem extends Item {
    private final Supplier<Block> cropBlockSupplier;

    public ICModCropItem(Supplier<Block> cropBlockSupplier, Properties properties) {
        super(properties);
        this.cropBlockSupplier = cropBlockSupplier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // Check if clicking on farmland
        if (state.is(Blocks.FARMLAND)) {
            BlockPos abovePos = pos.above();

            // Check if the space above farmland is empty
            if (level.getBlockState(abovePos).isAir()) {
                // Place the crop block
                level.setBlock(abovePos, this.cropBlockSupplier.get().defaultBlockState(), 3);

                // Consume the item if not in creative mode
                if (!context.getPlayer().getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }
}
