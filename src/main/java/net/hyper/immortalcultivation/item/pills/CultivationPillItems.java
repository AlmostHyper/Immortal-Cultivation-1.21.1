package net.hyper.immortalcultivation.item.pills;

import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Supplier class for Cultivation Pills following the mod's registry pattern.
 * Provides static methods that return suppliers for both Qi and Body cultivation pills.
 * Updated to use the unified PlayerCultivationAttachment.PillTier enum.
 */
public class CultivationPillItems {

    // === QI CULTIVATION PILLS ===

    /**
     * Low Quality Qi Cultivation Pill - provides +0.005/sec Qi cultivation rate
     */
    public static Supplier<Item> LOW_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new QiCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.COMMON),
            PlayerCultivationAttachment.PillTier.LOW_QUALITY
        );
    }

    /**
     * Medium Quality Qi Cultivation Pill - provides +0.02/sec Qi cultivation rate
     */
    public static Supplier<Item> MEDIUM_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new QiCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.UNCOMMON),
            PlayerCultivationAttachment.PillTier.MEDIUM_QUALITY
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
            PlayerCultivationAttachment.PillTier.HIGH_QUALITY
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
            PlayerCultivationAttachment.PillTier.TOP_QUALITY
        );
    }

    // === BODY CULTIVATION PILLS ===

    /**
     * Low Quality Body Cultivation Pill - provides +0.005/sec Body cultivation rate
     */
    public static Supplier<Item> LOW_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BodyCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.COMMON),
            PlayerCultivationAttachment.PillTier.LOW_QUALITY
        );
    }

    /**
     * Medium Quality Body Cultivation Pill - provides +0.02/sec Body cultivation rate
     */
    public static Supplier<Item> MEDIUM_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BodyCultivationPillItem(
            new Item.Properties()
                .stacksTo(64)
                .rarity(net.minecraft.world.item.Rarity.UNCOMMON),
            PlayerCultivationAttachment.PillTier.MEDIUM_QUALITY
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
            PlayerCultivationAttachment.PillTier.HIGH_QUALITY
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
            PlayerCultivationAttachment.PillTier.TOP_QUALITY
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

    // === PILL TIER CONSTANTS ===

    /**
     * Get all pill tiers for iteration
     */
    public static PlayerCultivationAttachment.PillTier[] getAllPillTiers() {
        return PlayerCultivationAttachment.PillTier.values();
    }

    /**
     * Get pill tier by ordinal with fallback
     */
    public static PlayerCultivationAttachment.PillTier getPillTierByOrdinal(int ordinal) {
        PlayerCultivationAttachment.PillTier[] tiers = PlayerCultivationAttachment.PillTier.values();
        if (ordinal >= 0 && ordinal < tiers.length) {
            return tiers[ordinal];
        }
        return PlayerCultivationAttachment.PillTier.LOW_QUALITY; // Fallback
    }

    /**
     * Get random pill tier weighted by rarity
     */
    public static PlayerCultivationAttachment.PillTier getRandomPillTier(java.util.Random random) {
        double roll = random.nextDouble();

        // Weighted distribution (rarer pills have lower chance)
        if (roll < 0.5) {
            return PlayerCultivationAttachment.PillTier.LOW_QUALITY;    // 50% chance
        } else if (roll < 0.8) {
            return PlayerCultivationAttachment.PillTier.MEDIUM_QUALITY; // 30% chance
        } else if (roll < 0.95) {
            return PlayerCultivationAttachment.PillTier.HIGH_QUALITY;   // 15% chance
        } else {
            return PlayerCultivationAttachment.PillTier.TOP_QUALITY;    // 5% chance
        }
    }
}