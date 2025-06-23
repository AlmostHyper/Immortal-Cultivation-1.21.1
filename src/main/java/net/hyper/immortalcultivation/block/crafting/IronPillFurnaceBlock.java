package net.hyper.immortalcultivation.block.crafting;

import com.mojang.serialization.MapCodec;
import net.hyper.immortalcultivation.block.entities.PillFurnaceBlockEntity;
import net.hyper.immortalcultivation.registry.ICModBlockEntities;
import net.hyper.immortalcultivation.registry.ICModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class IronPillFurnaceBlock extends PillFurnaceBlock {

    public static final MapCodec<IronPillFurnaceBlock> CODEC = simpleCodec(IronPillFurnaceBlock::new);

    // Furnace tier for different processing speeds and success rates
    private static final int FURNACE_TIER = 1; // Iron is tier 1

    public static Supplier<Block> IRON_PILL_FURNACE_BLOCK() {
        return () -> new IronPillFurnaceBlock(BlockBehaviour.Properties.of()
            .sound(SoundType.STONE)
            .strength(2.0F, 12.0F)
            .requiresCorrectToolForDrops());
    }

    public IronPillFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends IronPillFurnaceBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PillFurnaceBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ICModBlockEntities.PILL_FURNACE_BLOCK_ENTITY.get(), PillFurnaceBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PillFurnaceBlockEntity pillFurnace) {
                player.openMenu(pillFurnace);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // Inner class for Iron-tier furnace entity
    public static class IronPillFurnaceBlockEntity extends PillFurnaceBlockEntity {

        public IronPillFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
            super(pos, blockState);
        }

        public int getFurnaceTier() {
            return FURNACE_TIER;
        }

        protected double getTierSuccessModifier() {
            return 1.1; // 10% better success rate than basic furnace
        }

        protected int getTierSpeedModifier() {
            return 2; // 2x processing speed
        }

        protected int getTierFuelEfficiency() {
            return 120; // 20% more efficient fuel usage
        }
    }
}