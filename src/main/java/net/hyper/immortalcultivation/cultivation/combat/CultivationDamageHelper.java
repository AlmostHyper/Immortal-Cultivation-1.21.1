package net.hyper.immortalcultivation.cultivation.combat;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

class CultivationDamageHelper {

    /**
     * EASY METHOD: Create player attack damage!
     * Use this for most sword/weapon attacks!
     */
    public static CultivationDamage createPlayerAttack(Player attacker, long damage,
                                                       CultivationDamage.CultivationDamageType type) {
        return new CultivationDamage.Builder()
            .damage(damage)
            .type(type)
            .vanillaSource(attacker.damageSources().playerAttack(attacker)) // ← PERFECT!
            .directCause(attacker)
            .ultimateCause(attacker)
            .build();
    }

    /**
     * EASY METHOD: Create magic/qi damage!
     * Use this for cultivation techniques!
     */
    public static CultivationDamage createMagicAttack(Entity caster, long damage, Level level) {
        DamageSource magicSource;
        if (caster != null && caster.damageSources() != null) {
            magicSource = caster.damageSources().magic();
        } else {
            // Fallback to generic magic damage
            Holder<DamageType> magicType = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.MAGIC);
            magicSource = new DamageSource(magicType, caster, caster);
        }

        return new CultivationDamage.Builder()
            .damage(damage)
            .type(CultivationDamage.CultivationDamageType.QI)
            .vanillaSource(magicSource)
            .directCause(caster)
            .ultimateCause(caster)
            .build();
    }

    /**
     * EASY METHOD: Create generic damage!
     * Use this when you just need simple damage!
     */
    public static CultivationDamage createGenericDamage(long damage, Level level, @Nullable Entity cause) {
        return new CultivationDamage.Builder()
            .damage(damage)
            .type(CultivationDamage.CultivationDamageType.PHYSICAL)
            .level(level) // ← This will auto-create DamageSource
            .directCause(cause)
            .ultimateCause(cause)
            .build();
    }
}
