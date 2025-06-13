package net.hyper.immortal_cultivation.item.metals.realm1;

import net.hyper.immortal_cultivation.item.armors.ICModArmorItems;
import net.hyper.immortal_cultivation.item.tools.ICModToolTiers;
import net.hyper.immortal_cultivation.registry.ICModArmorMaterials;
import net.minecraft.world.item.*;

import java.util.function.Supplier;

public class WaterAspectedItems {

    public static Supplier<Item> WATER_ASPECTED_ORE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }
    public static Supplier<Item> WATER_ASPECTED_INGOT() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }

    public static Supplier<SwordItem> WATER_ASPECTED_SWORD() {
        return () -> new SwordItem(ICModToolTiers.WATER_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes
            (ICModToolTiers.WATER_ASPECTED, 3.5F, -2.4F)));
    }
    public static Supplier<PickaxeItem> WATER_ASPECTED_PICKAXE() {
        return () -> new PickaxeItem(ICModToolTiers.WATER_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(PickaxeItem.createAttributes
            (ICModToolTiers.WATER_ASPECTED, 1.5F, -2.8F)));
    }
    public static Supplier<AxeItem> WATER_ASPECTED_AXE() {
        return () -> new AxeItem(ICModToolTiers.WATER_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(AxeItem.createAttributes
            (ICModToolTiers.WATER_ASPECTED, 5.5F, -3.0F)));
    }
    public static Supplier<ShovelItem> WATER_ASPECTED_SHOVEL() {
        return () -> new ShovelItem(ICModToolTiers.WATER_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(ShovelItem.createAttributes
            (ICModToolTiers.WATER_ASPECTED, 1.5F, -3.0F)));
    }
    public static Supplier<HoeItem> WATER_ASPECTED_HOE() {
        return () -> new HoeItem(ICModToolTiers.WATER_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(HoeItem.createAttributes
            (ICModToolTiers.WATER_ASPECTED, -3.0F, 0.0F)));
    }

    public static Supplier<ArmorItem> WATER_ASPECTED_HELMET() {
        return () -> new ICModArmorItems(ICModArmorMaterials.WATER_ASPECTED, ArmorItem.Type.HELMET,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.HELMET.getDurability(35)));
    }
    public static Supplier<ArmorItem> WATER_ASPECTED_CHESTPLATE() {
        return () -> new ICModArmorItems(ICModArmorMaterials.WATER_ASPECTED, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.CHESTPLATE.getDurability(35)));
    }
    public static Supplier<ArmorItem> WATER_ASPECTED_LEGGINGS() {
        return () -> new ICModArmorItems(ICModArmorMaterials.WATER_ASPECTED, ArmorItem.Type.LEGGINGS,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.LEGGINGS.getDurability(35)));
    }
    public static Supplier<ArmorItem> WATER_ASPECTED_BOOTS() {
        return () -> new ICModArmorItems(ICModArmorMaterials.WATER_ASPECTED, ArmorItem.Type.BOOTS,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.BOOTS.getDurability(35)));
    }
}
