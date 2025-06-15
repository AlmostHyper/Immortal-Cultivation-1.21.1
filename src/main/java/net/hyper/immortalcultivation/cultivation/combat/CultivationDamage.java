package net.hyper.immortalcultivation.cultivation.combat;

// ============================================================================
// ADVANCED CULTIVATION DAMAGE SYSTEM
// Compatible with Minecraft + Other Mods | Stores as Long | Zero Precision Loss
// ============================================================================

// ============================================================================
// ADVANCED CULTIVATION DAMAGE SYSTEM
// Compatible with Minecraft + Other Mods | Stores as Long | Zero Precision Loss
// ============================================================================

// === 1. CORE DAMAGE WRAPPER CLASS ===
// This is like the master control system for your damage spacecraft! 🚁

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class CultivationDamage {

    // === Core Storage ===
    private final long preciseDamage;      // Your TRUE damage value!
    private final DamageSource vanillaSource;  // Minecraft compatibility layer
    private final String damageType;       // Custom damage classification
    private final Entity directCause;     // What directly caused damage
    private final Entity ultimateCause;   // Who ultimately caused damage

    // === Damage Categories ===
    public enum CultivationDamageType {
        PHYSICAL("physical"),
        QI("qi"),
        SOUL("soul"),
        EARTH("earth"),
        FIRE("fire"),
        METAL("metal"),
        WATER("water"),
        WOOD("wood"),
        LIGHTNING("lightning"),
        KARMIC("karmic"),
        BLOOD("blood"),
        TRIBULATION("tribulation");


        private final String id;
        CultivationDamageType(String id) { this.id = id; }
        public String getId() { return id; }
    }

    // === Constructor ===
    public CultivationDamage(long damage, CultivationDamageType type,
                             DamageSource vanillaSource, Entity directCause, Entity ultimateCause) {
        this.preciseDamage = Math.max(0, damage);  // No negative damage!
        this.damageType = type.getId();
        this.vanillaSource = vanillaSource;
        this.directCause = directCause;
        this.ultimateCause = ultimateCause;
    }

    // === Builder Pattern for Easy Creation ===
    public static class Builder {
        private long damage = 0;
        private CultivationDamageType type = CultivationDamageType.PHYSICAL;
        private DamageSource vanillaSource = null;  // ← This stays null until build()
        private Entity directCause = null;
        private Entity ultimateCause = null;
        private Level level = null; // ← For registry access

        public Builder damage(long damage) {
            this.damage = damage;
            return this;
        }

        public Builder type(CultivationDamageType type) {
            this.type = type;
            return this;
        }

        public Builder vanillaSource(DamageSource source) {
            this.vanillaSource = source;
            return this;
        }

        public Builder directCause(Entity entity) {
            this.directCause = entity;
            return this;
        }

        public Builder ultimateCause(Entity entity) {
            this.ultimateCause = entity;
            return this;
        }

        public Builder level(Level level) {
            this.level = level;
            return this;
        }

        public CultivationDamage build() {
            // === CRITICAL FIX: Proper DamageSource creation! ===
            if (vanillaSource == null) {
                if (level == null) {
                    throw new IllegalStateException("Either vanillaSource or level must be provided!");
                }

                // Create proper DamageSource from ResourceKey using registry
                Holder<DamageType> genericDamageType = level.registryAccess()
                    .lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.GENERIC);

                vanillaSource = new DamageSource(genericDamageType, directCause, ultimateCause);
            }

            return new CultivationDamage(damage, type, vanillaSource, directCause, ultimateCause);
        }
    }

    // === Getters ===
    public long getPreciseDamage() { return preciseDamage; }
    public String getDamageType() { return damageType; }
    public DamageSource getVanillaSource() { return vanillaSource; }
    public Entity getDirectCause() { return directCause; }
    public Entity getUltimateCause() { return ultimateCause; }

    // === SAFE CONVERSION TO MINECRAFT FLOAT ===
    public float getMinecraftCompatibleDamage() {
        if (preciseDamage > Float.MAX_VALUE) {
            return Float.MAX_VALUE;  // Cap at maximum float
        }
        return (float) preciseDamage;
    }

    // === PRECISION-AWARE SCALING ===
    public float getScaledDamage(float scaleFactor) {
        long scaledPrecise = (long) (preciseDamage * scaleFactor);
        return safeLongToFloat(scaledPrecise);
    }

    // === UTILITY METHODS ===
    private static float safeLongToFloat(long value) {
        if (value > Float.MAX_VALUE) return Float.MAX_VALUE;
        if (value < -Float.MAX_VALUE) return -Float.MAX_VALUE;
        return (float) value;
    }

    @Override
    public String toString() {
        return String.format("CultivationDamage{damage=%d, type='%s', source='%s'}",
            preciseDamage, damageType, vanillaSource);
    }
}