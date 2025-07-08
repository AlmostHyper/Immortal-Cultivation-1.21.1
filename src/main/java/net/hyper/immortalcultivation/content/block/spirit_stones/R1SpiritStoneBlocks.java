package net.hyper.immortalcultivation.content.block.spirit_stones;


import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class R1SpiritStoneBlocks extends SpiritStoneBlock {
    public R1SpiritStoneBlocks(Properties properties) {
        super(properties);
    }

    // Low Quality
    public static Supplier<Block> LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.0F, 5.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.0F, 5.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> LOW_QUALITY_SPIRIT_STONE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.0F, 5.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.0F, 5.0F)
            .requiresCorrectToolForDrops());
    }
    // Medium Quality
    public static Supplier<Block> MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.25F, 10.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.25F, 10.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> MEDIUM_QUALITY_SPIRIT_STONE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.25F, 10.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.25F, 10.0F)
            .requiresCorrectToolForDrops());
    }
    // High Quality
    public static Supplier<Block> HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.5F, 15.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> HIGH_QUALITY_SPIRIT_STONE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.5F, 15.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(1.5F, 15.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_ORE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(2.0F, 20.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> TOP_QUALITY_SPIRIT_STONE_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(2.0F, 20.0F)
            .requiresCorrectToolForDrops());
    }
    public static Supplier<Block> TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK() {
        return () -> new Block(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .strength(2.0F, 20.0F)
            .requiresCorrectToolForDrops());
    }
}
