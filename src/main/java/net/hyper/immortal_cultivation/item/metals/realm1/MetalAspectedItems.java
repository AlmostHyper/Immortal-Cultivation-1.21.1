package net.hyper.immortal_cultivation.item.metals.realm1;

import net.hyper.immortal_cultivation.item.armors.ICModArmorItems;
import net.hyper.immortal_cultivation.item.tools.ICModToolTiers;
import net.hyper.immortal_cultivation.registry.ICModArmorMaterials;
import net.minecraft.world.item.*;

import java.util.function.Supplier;

public class MetalAspectedItems {

    public static Supplier<Item> METAL_ASPECTED_ORE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }
    public static Supplier<Item> METAL_ASPECTED_INGOT() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }

    public static Supplier<SwordItem> METAL_ASPECTED_SWORD() {
        return () -> new SwordItem(ICModToolTiers.METAL_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes
            (ICModToolTiers.METAL_ASPECTED, 3.5F, -2.4F)));
    }
    public static Supplier<PickaxeItem> METAL_ASPECTED_PICKAXE() {
        return () -> new PickaxeItem(ICModToolTiers.METAL_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(PickaxeItem.createAttributes
            (ICModToolTiers.METAL_ASPECTED, 1.5F, -2.8F)));
    }
    public static Supplier<AxeItem> METAL_ASPECTED_AXE() {
        return () -> new AxeItem(ICModToolTiers.METAL_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(AxeItem.createAttributes
            (ICModToolTiers.METAL_ASPECTED, 5.5F, -3.0F)));
    }
    public static Supplier<ShovelItem> METAL_ASPECTED_SHOVEL() {
        return () -> new ShovelItem(ICModToolTiers.METAL_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(ShovelItem.createAttributes
            (ICModToolTiers.METAL_ASPECTED, 1.5F, -3.0F)));
    }
    public static Supplier<HoeItem> METAL_ASPECTED_HOE() {
        return () -> new HoeItem(ICModToolTiers.METAL_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(HoeItem.createAttributes
            (ICModToolTiers.METAL_ASPECTED, -3.0F, 0.0F)));
    }

    public static Supplier<ArmorItem> METAL_ASPECTED_HELMET() {
        return () -> new ICModArmorItems(ICModArmorMaterials.METAL_ASPECTED, ArmorItem.Type.HELMET,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.HELMET.getDurability(35)));
    }
    public static Supplier<ArmorItem> METAL_ASPECTED_CHESTPLATE() {
        return () -> new ICModArmorItems(ICModArmorMaterials.METAL_ASPECTED, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.CHESTPLATE.getDurability(35)));
    }
    public static Supplier<ArmorItem> METAL_ASPECTED_LEGGINGS() {
        return () -> new ICModArmorItems(ICModArmorMaterials.METAL_ASPECTED, ArmorItem.Type.LEGGINGS,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.LEGGINGS.getDurability(35)));
    }
    public static Supplier<ArmorItem> METAL_ASPECTED_BOOTS() {
        return () -> new ICModArmorItems(ICModArmorMaterials.METAL_ASPECTED, ArmorItem.Type.BOOTS,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.BOOTS.getDurability(35)));
    }
}
