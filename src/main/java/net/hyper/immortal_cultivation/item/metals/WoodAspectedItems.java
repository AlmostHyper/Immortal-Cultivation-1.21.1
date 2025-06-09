package net.hyper.immortal_cultivation.item.metals;

import net.hyper.immortal_cultivation.item.tools.ICModToolTiers;
import net.minecraft.world.item.*;

public class WoodAspectedItems {

    public static final Item WOOD_ASPECTED_ORE = new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final Item WOOD_ASPECTED_INGOT = new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));

    public static final Item WOOD_ASPECTED_SWORD = new SwordItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().attributes(SwordItem.createAttributes(ICModToolTiers.WOOD_ASPECTED, 6.0f, -2.2f)));
    public static final Item WOOD_ASPECTED_PICKAXE = new PickaxeItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().attributes(PickaxeItem.createAttributes(ICModToolTiers.WOOD_ASPECTED, 1.0f, -2.8f)));
    public static final Item WOOD_ASPECTED_AXE = new AxeItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().attributes(AxeItem.createAttributes(ICModToolTiers.WOOD_ASPECTED, 7.0f, -3.2f)));
    public static final Item WOOD_ASPECTED_SHOVEL = new ShovelItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().attributes(ShovelItem.createAttributes(ICModToolTiers.WOOD_ASPECTED, 2.0f, -3.0f)));
    public static final Item WOOD_ASPECTED_HOE = new HoeItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().attributes(HoeItem.createAttributes(ICModToolTiers.WOOD_ASPECTED, 0.0f, -3.0f)));

}
