package net.hyper.immortal_cultivation.item.metals;

import net.hyper.immortal_cultivation.item.tools.ICModToolTiers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

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
            (ICModToolTiers.EARTH_ASPECTED, 3.5F, -2.4F)));
    }
    public static Supplier<PickaxeItem> EARTH_ASPECTED_PICKAXE() {
        return () -> new PickaxeItem(ICModToolTiers.EARTH_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(PickaxeItem.createAttributes
            (ICModToolTiers.EARTH_ASPECTED, 1.5F, -2.8F)));
    }
    public static Supplier<AxeItem> EARTH_ASPECTED_AXE() {
        return () -> new AxeItem(ICModToolTiers.EARTH_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(AxeItem.createAttributes
            (ICModToolTiers.EARTH_ASPECTED, 5.5F, -3.0F)));
    }
    public static Supplier<ShovelItem> EARTH_ASPECTED_SHOVEL() {
        return () -> new ShovelItem(ICModToolTiers.EARTH_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(ShovelItem.createAttributes
            (ICModToolTiers.EARTH_ASPECTED, 2.0F, -3.0F)));
    }
    public static Supplier<HoeItem> EARTH_ASPECTED_HOE() {
        return () -> new HoeItem(ICModToolTiers.EARTH_ASPECTED, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(HoeItem.createAttributes
            (ICModToolTiers.EARTH_ASPECTED, -3.0F, 0.0F)));
    }
}