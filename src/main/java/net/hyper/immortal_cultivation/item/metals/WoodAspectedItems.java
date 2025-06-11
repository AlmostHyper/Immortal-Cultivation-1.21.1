package net.hyper.immortal_cultivation.item.metals;

import net.hyper.immortal_cultivation.item.tools.ICModToolTiers;
import net.hyper.immortal_cultivation.registry.ICModArmorMaterials;
import net.minecraft.world.item.*;

import java.util.function.Supplier;

public class WoodAspectedItems {

    public static Supplier<Item> WOOD_ASPECTED_ORE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }
    public static Supplier<Item> WOOD_ASPECTED_INGOT() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }

    public static Supplier<SwordItem> WOOD_ASPECTED_SWORD() {
        return () -> new SwordItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes
            (ICModToolTiers.WOOD_ASPECTED, 3.5F, -2.4F)));
    }
    public static Supplier<PickaxeItem> WOOD_ASPECTED_PICKAXE() {
        return () -> new PickaxeItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(PickaxeItem.createAttributes
            (ICModToolTiers.WOOD_ASPECTED, 1.5F, -2.8F)));
    }
    public static Supplier<AxeItem> WOOD_ASPECTED_AXE() {
        return () -> new AxeItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(AxeItem.createAttributes
            (ICModToolTiers.WOOD_ASPECTED, 5.5F, -3.0F)));
    }
    public static Supplier<ShovelItem> WOOD_ASPECTED_SHOVEL() {
        return () -> new ShovelItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(ShovelItem.createAttributes
            (ICModToolTiers.WOOD_ASPECTED, 2.0F, -3.0F)));
    }
    public static Supplier<HoeItem> WOOD_ASPECTED_HOE() {
        return () -> new HoeItem(ICModToolTiers.WOOD_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(HoeItem.createAttributes
            (ICModToolTiers.WOOD_ASPECTED, -3.0F, 0.0F)));
    }

    public static Supplier<ArmorItem> WOOD_ASPECTED_HELMET() {
        return () -> new ArmorItem(ICModArmorMaterials.WOOD_ASPECTED, ArmorItem.Type.HELMET,
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(35)));
    }
    public static Supplier<ArmorItem> WOOD_ASPECTED_CHESTPLATE() {
        return () -> new ArmorItem(ICModArmorMaterials.WOOD_ASPECTED, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(35)));
    }
    public static Supplier<ArmorItem> WOOD_ASPECTED_LEGGINGS() {
        return () -> new ArmorItem(ICModArmorMaterials.WOOD_ASPECTED, ArmorItem.Type.LEGGINGS,
            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(35)));
    }
    public static Supplier<ArmorItem> WOOD_ASPECTED_BOOTS() {
        return () -> new ArmorItem(ICModArmorMaterials.WOOD_ASPECTED, ArmorItem.Type.BOOTS,
            new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(35)));
    }
}
