package net.hyper.immortal_cultivation.block.spirit_stones;


import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class R1SpiritStoneBlocks extends SpiritStoneBlock {
    // Low Quality
    public static final Block LOW_QUALITY_SPIRIT_STONE_ORE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.0F, 5.0F)
        .requiresCorrectToolForDrops());

    public static final Block LOW_QUALITY_SPIRIT_STONE_DEEPSLATE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.0F, 5.0F)
        .requiresCorrectToolForDrops());

    public static final Block LOW_QUALITY_SPIRIT_STONE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.0F, 5.0F)
        .requiresCorrectToolForDrops());

    public static final Block LOW_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.0F, 5.0F)
        .requiresCorrectToolForDrops());

    // Medium Quality
    public static final Block MEDIUM_QUALITY_SPIRIT_STONE_ORE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.25F, 10.0F)
        .requiresCorrectToolForDrops());

    public static final Block MEDIUM_QUALITY_SPIRIT_STONE_DEEPSLATE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.25F, 10.0F)
        .requiresCorrectToolForDrops());

    public static final Block MEDIUM_QUALITY_SPIRIT_STONE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.25F, 10.0F)
        .requiresCorrectToolForDrops());

    public static final Block MEDIUM_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.25F, 10.0F)
        .requiresCorrectToolForDrops());

    // High Quality
    public static final Block HIGH_QUALITY_SPIRIT_STONE_ORE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.5F, 15.0F)
        .requiresCorrectToolForDrops());

    public static final Block HIGH_QUALITY_SPIRIT_STONE_DEEPSLATE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.5F, 15.0F)
        .requiresCorrectToolForDrops());

    public static final Block HIGH_QUALITY_SPIRIT_STONE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.5F, 15.0F)
        .requiresCorrectToolForDrops());

    public static final Block HIGH_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(1.5F, 15.0F)
        .requiresCorrectToolForDrops());

    // Top Quality
    public static final Block TOP_QUALITY_SPIRIT_STONE_ORE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(2.0F, 20.0F)
        .requiresCorrectToolForDrops());

    public static final Block TOP_QUALITY_SPIRIT_STONE_DEEPSLATE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(2.0F, 20.0F)
        .requiresCorrectToolForDrops());

    public static final Block TOP_QUALITY_SPIRIT_STONE_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(2.0F, 20.0F)
        .requiresCorrectToolForDrops());

    public static final Block TOP_QUALITY_SPIRIT_STONE_COMPRESSED_BLOCK = new Block(BlockBehaviour.Properties.of()
        .sound(SoundType.AMETHYST)
        .strength(2.0F, 20.0F)
        .requiresCorrectToolForDrops());

    public R1SpiritStoneBlocks(Properties properties) {
        super(properties);
    }
}
