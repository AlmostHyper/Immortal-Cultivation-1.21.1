package net.hyper.immortal_cultivation.item.metals;

import net.hyper.immortal_cultivation.item.tools.ICModToolTiers;
import net.minecraft.world.item.*;

public class SkyIronItems {

    public static final Item SKY_IRON_ORE = new Item(new Item.Properties().stacksTo(64).fireResistant().rarity(Rarity.RARE));
    public static final Item SKY_IRON_INGOT = new Item(new Item.Properties().stacksTo(64).fireResistant().rarity(Rarity.RARE));

    public static final Item SKY_IRON_SWORD = new SwordItem(ICModToolTiers.SKY_IRON, new Item.Properties().attributes(SwordItem.createAttributes(ICModToolTiers.SKY_IRON, 9.0f, -2.2f)));
    public static final Item SKY_IRON_PICKAXE = new PickaxeItem(ICModToolTiers.SKY_IRON, new Item.Properties().attributes(PickaxeItem.createAttributes(ICModToolTiers.SKY_IRON, 2.0f, -2.8f)));
    public static final Item SKY_IRON_AXE = new AxeItem(ICModToolTiers.SKY_IRON, new Item.Properties().attributes(AxeItem.createAttributes(ICModToolTiers.SKY_IRON, 10.0f, -3.2f)));
    public static final Item SKY_IRON_SHOVEL = new ShovelItem(ICModToolTiers.SKY_IRON, new Item.Properties().attributes(ShovelItem.createAttributes(ICModToolTiers.SKY_IRON, 3.0f, -3.0f)));
    public static final Item SKY_IRON_HOE = new HoeItem(ICModToolTiers.SKY_IRON, new Item.Properties().attributes(HoeItem.createAttributes(ICModToolTiers.SKY_IRON, 0.0f, -3.0f)));

}
