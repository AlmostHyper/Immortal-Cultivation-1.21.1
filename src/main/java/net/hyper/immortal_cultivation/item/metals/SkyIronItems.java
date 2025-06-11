package net.hyper.immortal_cultivation.item.metals;

import net.hyper.immortal_cultivation.item.armors.ICModArmorItems;
import net.hyper.immortal_cultivation.item.tools.ICModToolTiers;
import net.hyper.immortal_cultivation.registry.ICModArmorMaterials;
import net.minecraft.world.item.*;

import java.util.function.Supplier;

public class SkyIronItems {

    public static Supplier<Item> SKY_IRON_ORE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }
    public static Supplier<Item> SKY_IRON_INGOT() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }

    public static Supplier<SwordItem> SKY_IRON_SWORD() {
        return () -> new SwordItem(ICModToolTiers.SKY_IRON, new Item.Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes
            (ICModToolTiers.SKY_IRON, 6.0F, -2.4F)));
    }
    public static Supplier<PickaxeItem> SKY_IRON_PICKAXE() {
        return () -> new PickaxeItem(ICModToolTiers.SKY_IRON, new Item.Properties().rarity(Rarity.RARE).attributes(PickaxeItem.createAttributes
            (ICModToolTiers.SKY_IRON, 4.0F, -2.8F)));
    }
    public static Supplier<AxeItem> SKY_IRON_AXE() {
        return () -> new AxeItem(ICModToolTiers.SKY_IRON, new Item.Properties().rarity(Rarity.RARE).attributes(AxeItem.createAttributes
            (ICModToolTiers.SKY_IRON, 8.0F, -3.0F)));
    }
    public static Supplier<ShovelItem> SKY_IRON_SHOVEL() {
        return () -> new ShovelItem(ICModToolTiers.SKY_IRON, new Item.Properties().rarity(Rarity.RARE).attributes(ShovelItem.createAttributes
            (ICModToolTiers.SKY_IRON, 3.5F, -3.0F)));
    }
    public static Supplier<HoeItem> SKY_IRON_HOE() {
        return () -> new HoeItem(ICModToolTiers.SKY_IRON, new Item.Properties().rarity(Rarity.RARE).attributes(HoeItem.createAttributes
            (ICModToolTiers.SKY_IRON, -4.0F, 0.0F)));
    }

    public static Supplier<ArmorItem> SKY_IRON_HELMET() {
        return () -> new ICModArmorItems(ICModArmorMaterials.SKY_IRON, ArmorItem.Type.HELMET,
            new Item.Properties().rarity(Rarity.RARE).durability(ArmorItem.Type.HELMET.getDurability(35)));
    }
    public static Supplier<ArmorItem> SKY_IRON_CHESTPLATE() {
        return () -> new ICModArmorItems(ICModArmorMaterials.SKY_IRON, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().rarity(Rarity.RARE).durability(ArmorItem.Type.CHESTPLATE.getDurability(35)));
    }
    public static Supplier<ArmorItem> SKY_IRON_LEGGINGS() {
        return () -> new ICModArmorItems(ICModArmorMaterials.SKY_IRON, ArmorItem.Type.LEGGINGS,
            new Item.Properties().rarity(Rarity.RARE).durability(ArmorItem.Type.LEGGINGS.getDurability(35)));
    }
    public static Supplier<ArmorItem> SKY_IRON_BOOTS() {
        return () -> new ICModArmorItems(ICModArmorMaterials.SKY_IRON, ArmorItem.Type.BOOTS,
            new Item.Properties().rarity(Rarity.RARE).durability(ArmorItem.Type.BOOTS.getDurability(35)));
    }
}
