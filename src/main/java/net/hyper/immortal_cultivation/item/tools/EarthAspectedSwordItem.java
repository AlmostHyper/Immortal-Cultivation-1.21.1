package net.hyper.immortal_cultivation.item.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.Tool;


public class EarthAspectedSwordItem extends SwordItem{


    public EarthAspectedSwordItem() {
        super(ICModToolTiers.EARTH_ASPECTED, new Properties().attributes(SwordItem.createAttributes(ICModToolTiers.EARTH_ASPECTED, 6, 3f)));
    }
}
