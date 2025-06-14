package net.hyper.immortalcultivation.util;

import net.hyper.immortalcultivation.cultivation.CultivationDamage;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class CultivationDamageUtils {

    /**
     * Convert vanilla damage to cultivation damage for processing
     * @param vanillaDamage Minecraft's float damage
     * @param source The damage source
     * @return Equivalent cultivation damage
     */
    public static CultivationDamage fromVanillaDamage(float vanillaDamage, DamageSource source) {
        // Scale up to long precision (you can adjust the multiplier!)
        long preciseDamage = Math.round(vanillaDamage * 100); // 1 vanilla = 100 cultivation units

        // Determine cultivation damage type from vanilla source
        CultivationDamage.CultivationDamageType cultType =
            mapVanillaToCultivationType(source);

        return new CultivationDamage.Builder()
            .damage(preciseDamage)
            .type(cultType)
            .vanillaSource(source)
            .directCause(source.getDirectEntity())
            .ultimateCause(source.getEntity())
            .build();
    }

    private static CultivationDamage.CultivationDamageType mapVanillaToCultivationType(DamageSource source) {
        // Map vanilla damage types to cultivation types
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return CultivationDamage.CultivationDamageType.ELEMENTAL;
        } else if (source.is(DamageTypeTags.IS_MAGIC)) {
            return CultivationDamage.CultivationDamageType.QI_BASED;
        } else if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            return CultivationDamage.CultivationDamageType.TRIBULATION;
        } else {
            return CultivationDamage.CultivationDamageType.PHYSICAL;
        }
    }

    /**
     * Create a cultivation-enhanced entity health system
     * @param entity The entity to enhance
     * @param maxCultivationHealth Maximum health in cultivation units
     */
    public static void enhanceEntityWithCultivation(LivingEntity entity, long maxCultivationHealth) {
        entity.getCapability(CultivationDamageCapability.DAMAGE_HANDLER, null).ifPresent(handler -> {
            if (handler instanceof CultivationDamageCapability cap) {
                cap.setMaxHealth(maxCultivationHealth);
                cap.setCultivationHealth(maxCultivationHealth);
            }
        });
    }
}
