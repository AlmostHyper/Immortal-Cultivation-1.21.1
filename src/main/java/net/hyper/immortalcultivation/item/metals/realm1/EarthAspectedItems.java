package net.hyper.immortalcultivation.item.metals.realm1;

import net.hyper.immortalcultivation.item.tools.ICModToolTiers;
import net.hyper.immortalcultivation.registry.ICModArmorMaterials;
import net.minecraft.world.item.*;

import java.util.function.Supplier;

public class EarthAspectedItems {

    public static Supplier<Item> EARTH_ASPECTED_ORE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }
    public static Supplier<Item> EARTH_ASPECTED_INGOT() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }

    public static Supplier<SwordItem> EARTH_ASPECTED_SWORD() {
        return () -> new SwordItem(ICModToolTiers.EARTH_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes
            (ICModToolTiers.EARTH_ASPECTED, 2.5f, -2.4f)));
    }
    public static Supplier<PickaxeItem> EARTH_ASPECTED_PICKAXE() {
        return () -> new PickaxeItem(ICModToolTiers.EARTH_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(PickaxeItem.createAttributes
            (ICModToolTiers.EARTH_ASPECTED, 0.5f, -2.8f)));
    }
    public static Supplier<AxeItem> EARTH_ASPECTED_AXE() {
        return () -> new AxeItem(ICModToolTiers.EARTH_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(AxeItem.createAttributes
            (ICModToolTiers.EARTH_ASPECTED, 5.0f, -3.0f)));
    }
    public static Supplier<ShovelItem> EARTH_ASPECTED_SHOVEL() {
        return () -> new ShovelItem(ICModToolTiers.EARTH_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(ShovelItem.createAttributes
            (ICModToolTiers.EARTH_ASPECTED, 1.0f, -3.0f)));
    }
    public static Supplier<HoeItem> EARTH_ASPECTED_HOE() {
        return () -> new HoeItem(ICModToolTiers.EARTH_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(HoeItem.createAttributes
            (ICModToolTiers.EARTH_ASPECTED, -3.0f, 0.0f)));
    }

    public static Supplier<ArmorItem> EARTH_ASPECTED_HELMET() {
        return () -> new ArmorItem(ICModArmorMaterials.EARTH_ASPECTED, ArmorItem.Type.HELMET,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.HELMET.getDurability(35)));
    }
    public static Supplier<ArmorItem> EARTH_ASPECTED_CHESTPLATE() {
        return () -> new ArmorItem(ICModArmorMaterials.EARTH_ASPECTED, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.CHESTPLATE.getDurability(35)));
    }
    public static Supplier<ArmorItem> EARTH_ASPECTED_LEGGINGS() {
        return () -> new ArmorItem(ICModArmorMaterials.EARTH_ASPECTED, ArmorItem.Type.LEGGINGS,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.LEGGINGS.getDurability(35)));
    }
    public static Supplier<ArmorItem> EARTH_ASPECTED_BOOTS() {
        return () -> new ArmorItem(ICModArmorMaterials.EARTH_ASPECTED, ArmorItem.Type.BOOTS,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.BOOTS.getDurability(35)));
    }
}