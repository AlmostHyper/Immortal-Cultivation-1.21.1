package net.hyper.immortalcultivation.item.metals.realm1;

import net.hyper.immortalcultivation.item.tools.ICModToolTiers;
import net.hyper.immortalcultivation.registry.ICModArmorMaterials;
import net.minecraft.world.item.*;

import java.util.function.Supplier;

public class FireAspectedItems {

    public static Supplier<Item> FIRE_ASPECTED_ORE() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }
    public static Supplier<Item> FIRE_ASPECTED_INGOT() {
        return () -> new Item(new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.UNCOMMON));
    }

    public static Supplier<SwordItem> FIRE_ASPECTED_SWORD() {
        return () -> new SwordItem(ICModToolTiers.FIRE_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes
            (ICModToolTiers.FIRE_ASPECTED, 2.5f, -2.4f)));
    }
    public static Supplier<PickaxeItem> FIRE_ASPECTED_PICKAXE() {
        return () -> new PickaxeItem(ICModToolTiers.FIRE_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(PickaxeItem.createAttributes
            (ICModToolTiers.FIRE_ASPECTED, 0.5f, -2.8f)));
    }
    public static Supplier<AxeItem> FIRE_ASPECTED_AXE() {
        return () -> new AxeItem(ICModToolTiers.FIRE_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(AxeItem.createAttributes
            (ICModToolTiers.FIRE_ASPECTED, 5.0f, -3.0f)));
    }
    public static Supplier<ShovelItem> FIRE_ASPECTED_SHOVEL() {
        return () -> new ShovelItem(ICModToolTiers.FIRE_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(ShovelItem.createAttributes
            (ICModToolTiers.FIRE_ASPECTED, 1.0f, -3.0f)));
    }
    public static Supplier<HoeItem> FIRE_ASPECTED_HOE() {
        return () -> new HoeItem(ICModToolTiers.FIRE_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(HoeItem.createAttributes
            (ICModToolTiers.FIRE_ASPECTED, -3.0f, 0.0f)));
    }

    public static Supplier<ArmorItem> FIRE_ASPECTED_HELMET() {
        return () -> new ArmorItem(ICModArmorMaterials.FIRE_ASPECTED, ArmorItem.Type.HELMET,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.HELMET.getDurability(35)));
    }
    public static Supplier<ArmorItem> FIRE_ASPECTED_CHESTPLATE() {
        return () -> new ArmorItem(ICModArmorMaterials.FIRE_ASPECTED, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.CHESTPLATE.getDurability(35)));
    }
    public static Supplier<ArmorItem> FIRE_ASPECTED_LEGGINGS() {
        return () -> new ArmorItem(ICModArmorMaterials.FIRE_ASPECTED, ArmorItem.Type.LEGGINGS,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.LEGGINGS.getDurability(35)));
    }
    public static Supplier<ArmorItem> FIRE_ASPECTED_BOOTS() {
        return () -> new ArmorItem(ICModArmorMaterials.FIRE_ASPECTED, ArmorItem.Type.BOOTS,
            new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.BOOTS.getDurability(35)));
    }
}
