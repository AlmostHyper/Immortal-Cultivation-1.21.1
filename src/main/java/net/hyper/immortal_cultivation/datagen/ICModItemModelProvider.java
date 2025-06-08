package net.hyper.immortal_cultivation.datagen;

import net.hyper.immortal_cultivation.ImmortalCultivation;
import net.hyper.immortal_cultivation.registry.ICModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ICModItemModelProvider extends ItemModelProvider {
    public ICModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ImmortalCultivation.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ICModItems.LOW_QUALITY_SPIRIT_STONE.get());
        basicItem(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get());
        basicItem(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get());
        basicItem(ICModItems.TOP_QUALITY_SPIRIT_STONE.get());
        basicItem(ICModItems.EARTH_ASPECTED_ORE.get());
        basicItem(ICModItems.EARTH_ASPECTED_INGOT.get());
        basicItem(ICModItems.FIRE_ASPECTED_ORE.get());
        basicItem(ICModItems.FIRE_ASPECTED_INGOT.get());
        basicItem(ICModItems.METAL_ASPECTED_ORE.get());
        basicItem(ICModItems.METAL_ASPECTED_INGOT.get());
        basicItem(ICModItems.SKY_IRON_ORE.get());
        basicItem(ICModItems.SKY_IRON_INGOT.get());
        basicItem(ICModItems.WATER_ASPECTED_ORE.get());
        basicItem(ICModItems.WATER_ASPECTED_INGOT.get());
        basicItem(ICModItems.WOOD_ASPECTED_ORE.get());
        basicItem(ICModItems.WOOD_ASPECTED_INGOT.get());
    }
}
