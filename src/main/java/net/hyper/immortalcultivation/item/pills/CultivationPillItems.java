package net.hyper.immortalcultivation.item.pills;

import net.hyper.immortalcultivation.attachment.PlayerCultivationBonusAttachment;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Supplier class for Cultivation Pills following the mod's registry pattern.
 * Provides static methods that return suppliers for both Qi and Body cultivation pills.
 */
public class CultivationPillItems {

    // === QI CULTIVATION PILLS ===

    /**
     * Low Quality Qi Cultivation Pill - provides +0.001/sec Qi cultivation rate
     */
    public static Supplier<Item> LOW_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new QiCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.COMMON),
            PlayerCultivationBonusAttachment.PillTier.LOW_QUALITY
        );
    }

    /**
     * Medium Quality Qi Cultivation Pill - provides +0.01/sec Qi cultivation rate
     */
    public static Supplier<Item> MEDIUM_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new QiCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.UNCOMMON),
            PlayerCultivationBonusAttachment.PillTier.MEDIUM_QUALITY
        );
    }

    /**
     * High Quality Qi Cultivation Pill - provides +0.1/sec Qi cultivation rate
     */
    public static Supplier<Item> HIGH_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new QiCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.RARE),
            PlayerCultivationBonusAttachment.PillTier.HIGH_QUALITY
        );
    }

    /**
     * Top Quality Qi Cultivation Pill - provides +1.0/sec Qi cultivation rate
     */
    public static Supplier<Item> TOP_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new QiCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.EPIC),
            PlayerCultivationBonusAttachment.PillTier.TOP_QUALITY
        );
    }

    // === BODY CULTIVATION PILLS ===

    /**
     * Low Quality Body Cultivation Pill - provides +0.001/sec Body cultivation rate
     */
    public static Supplier<Item> LOW_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BodyCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.COMMON),
            PlayerCultivationBonusAttachment.PillTier.LOW_QUALITY
        );
    }

    /**
     * Medium Quality Body Cultivation Pill - provides +0.01/sec Body cultivation rate
     */
    public static Supplier<Item> MEDIUM_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BodyCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.UNCOMMON),
            PlayerCultivationBonusAttachment.PillTier.MEDIUM_QUALITY
        );
    }

    /**
     * High Quality Body Cultivation Pill - provides +0.1/sec Body cultivation rate
     */
    public static Supplier<Item> HIGH_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BodyCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.RARE),
            PlayerCultivationBonusAttachment.PillTier.HIGH_QUALITY
        );
    }

    /**
     * Top Quality Body Cultivation Pill - provides +1.0/sec Body cultivation rate
     */
    public static Supplier<Item> TOP_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BodyCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.EPIC),
            PlayerCultivationBonusAttachment.PillTier.TOP_QUALITY
        );
    }

    // === UTILITY METHODS ===

    /**
     * Get all Qi cultivation pill suppliers
     */
    public static Supplier<Item>[] getAllQiCultivationPills() {
        return new Supplier[]{
            LOW_QUALITY_QI_CULTIVATION_PILL(),
            MEDIUM_QUALITY_QI_CULTIVATION_PILL(),
            HIGH_QUALITY_QI_CULTIVATION_PILL(),
            TOP_QUALITY_QI_CULTIVATION_PILL()
        };
    }

    /**
     * Get all Body cultivation pill suppliers
     */
    public static Supplier<Item>[] getAllBodyCultivationPills() {
        return new Supplier[]{
            LOW_QUALITY_BODY_CULTIVATION_PILL(),
            MEDIUM_QUALITY_BODY_CULTIVATION_PILL(),
            HIGH_QUALITY_BODY_CULTIVATION_PILL(),
            TOP_QUALITY_BODY_CULTIVATION_PILL()
        };
    }

    /**
     * Get all cultivation pill suppliers
     */
    public static Supplier<Item>[] getAllCultivationPills() {
        return new Supplier[]{
            LOW_QUALITY_QI_CULTIVATION_PILL(),
            MEDIUM_QUALITY_QI_CULTIVATION_PILL(),
            HIGH_QUALITY_QI_CULTIVATION_PILL(),
            TOP_QUALITY_QI_CULTIVATION_PILL(),
            LOW_QUALITY_BODY_CULTIVATION_PILL(),
            MEDIUM_QUALITY_BODY_CULTIVATION_PILL(),
            HIGH_QUALITY_BODY_CULTIVATION_PILL(),
            TOP_QUALITY_BODY_CULTIVATION_PILL()
        };
    }
}