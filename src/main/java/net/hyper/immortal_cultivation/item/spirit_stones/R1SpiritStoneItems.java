package net.hyper.immortal_cultivation.item.spirit_stones;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class R1SpiritStoneItems extends SpiritStoneItem{

    public static final Item LOW_QUALITY_SPIRIT_STONE = new Item(new Properties().stacksTo(64).fireResistant().rarity(Rarity.COMMON));
    public static final Item MEDIUM_QUALITY_SPIRIT_STONE = new Item(new Properties().stacksTo(64).fireResistant().rarity(Rarity.UNCOMMON));
    public static final Item HIGH_QUALITY_SPIRIT_STONE = new Item(new Properties().stacksTo(64).fireResistant().rarity(Rarity.RARE));
    public static final Item TOP_QUALITY_SPIRIT_STONE = new Item(new Properties().stacksTo(64).fireResistant().rarity(Rarity.EPIC));

    public R1SpiritStoneItems(Properties properties) {
        super(properties);
    }
}
