package net.hyper.immortalcultivation.core.cultivation.realms;

import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for all cultivation realms in the unified cultivation system.
 * This abstract class provides the framework for both Qi and Body cultivation realms,
 * using the unified ICultivationData interface and CultivationType enum.
 */
public abstract class CultivationRealm {

    // === Core Identification ===

    /**
     * Unique identifier for this realm
     * @return The realm's ID
     */
    public abstract String getId();

    /**
     * Display name for UI purposes
     * @return Human-readable name
     */
    public String getDisplayName() {
        return getId().replace("_", " ").toUpperCase();
    }

    /**
     * Numerical level for comparison and ordering
     * @return Realm level
     */
    public abstract int getRealmLevel();

    /**
     * Description of this realm
     * @return Descriptive text about the realm
     */
    public String getDescription() {
        return "A cultivation realm.";
    }

    // === Base Physical Stats ===

    /**
     * Base health points provided by this realm
     * @return Health value
     */
    public abstract long getBaseMaxHealth();

    /**
     * Base health regeneration rate per second
     * @return Health regen per second
     */
    public abstract double getBaseHealthRegenRate();

    /**
     * Base attack damage multiplier
     * @return Attack damage value
     */
    public abstract double getBaseAttackDamage();

    /**
     * Base movement speed multiplier
     * @return Movement speed value
     */
    public abstract double getBaseMovementSpeed();

    /**
     * Sprint speed (default: 1.3x movement speed)
     * @return Sprint speed multiplier
     */
    public double getSprintSpeed() {
        return getBaseMovementSpeed() * 1.3;
    }

    /**
     * Base jump height multiplier
     * @return Jump height value
     */
    public abstract double getBaseJumpHeight();

    // === Cultivation-Related Stats ===

    /**
     * Maximum Cultivation capacity for this realm
     * @return Max Cultivation value
     */
    public abstract long getBaseMaxCultivation();

    /**
     * Cultivation rate per second
     * @return Cultivation rate per second
     */
    public abstract double getBaseCultivationRate();

    /**
     * Minimum Cultivation required to attempt breakthrough from this realm
     * @return Cultivation threshold for breakthrough
     */
    public final long getBreakthroughCultivationThreshold() {
        return (long) (getBaseMaxCultivation());
    }

    // === Qi-Related Stats ===

    /**
     * Maximum Qi capacity for this realm
     * @return Max Qi value
     */
    public abstract long getBaseMaxQi();

    /**
     * Qi regeneration rate per second
     * @return Qi regen per second
     */
    public abstract double getBaseQiRegenRate();

    // === Qi-Related Stats ===

    /**
     * Maximum Body capacity for this realm
     * @return Max Body value
     */
    public abstract long getBaseMaxBody();

    /**
     * Body regeneration rate per second
     * @return Body regen per second
     */
    public abstract double getBaseBodyRegenRate();

    // === Unified Cultivation Breakthrough Logic ===

    /**
     * Checks if player meets cultivation requirements for breakthrough
     * @param cultivationData Player's cultivation data
     * @param type The cultivation type being checked
     * @return true if cultivation threshold is met
     */
    public boolean meetsCultivationThreshold(ICultivationData cultivationData, ICultivationData.CultivationType type) {
        return cultivationData.getCurrentCultivation(type) >= getBreakthroughCultivationThreshold();
    }

    /**
     * Checks if player can breakthrough (includes all conditions)
     * @param player The player attempting breakthrough
     * @param cultivationData Player's cultivation data
     * @param type The cultivation type being attempted
     * @return true if breakthrough is possible
     */
    public boolean canBreakthrough(Player player, ICultivationData cultivationData, ICultivationData.CultivationType type) {
        return meetsCultivationThreshold(cultivationData, type) &&
            !getNextCultivationRealms(player).isEmpty() &&
            meetsAdditionalRequirements(player, cultivationData, type);
    }

    /**
     * Override for additional breakthrough requirements beyond cultivation amount
     * @param player The player
     * @param cultivationData Player's cultivation data
     * @param type The cultivation type being attempted
     * @return true if additional requirements are met
     */
    public boolean meetsAdditionalRequirements(Player player, ICultivationData cultivationData, ICultivationData.CultivationType type) {
        return true; // Default: no additional requirements
    }

    // === Realm Navigation ===

    /**
     * Get all realms in the same cultivation stage/group
     * @return List of related realms
     */
    public List<CultivationRealm> getCultivationRealmGroup() {
        return new ArrayList<>();
    }

    /**
     * Get previous realms that can lead to this one
     * @return List of previous realms
     */
    public List<CultivationRealm> getPreviousCultivationRealms() {
        return new ArrayList<>();
    }

    /**
     * Get possible next realms from this one
     * @param player The player (for conditional paths)
     * @return List of possible next realms
     */
    public List<CultivationRealm> getNextCultivationRealms(Player player) {
        return getNextCultivationRealms();
    }

    /**
     * Get default next realms (no player context needed)
     * @return List of default next realms
     */
    public List<CultivationRealm> getNextCultivationRealms() {
        return new ArrayList<>();
    }

    // === Unified Breakthrough Types ===

    /**
     * Breakthrough to next minor realm (Ex. Early -> Middle Foundation Establishment)
     * @param player The player
     * @param cultivationData Player's cultivation data
     * @param type The cultivation type
     * @return Next realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getMinorBreakthrough(Player player, ICultivationData cultivationData, ICultivationData.CultivationType type) {
        List<CultivationRealm> nextRealms = getNextCultivationRealms(player);
        return nextRealms.isEmpty() ? null : nextRealms.get(0);
    }

    /**
     * Breakthrough to next major realm (Ex. Qi Condensation -> Foundation Establishment)
     * @param player The player
     * @param cultivationData Player's cultivation data
     * @param type The cultivation type
     * @return Next realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getMajorBreakthrough(Player player, ICultivationData cultivationData, ICultivationData.CultivationType type) {
        List<CultivationRealm> nextRealms = getNextCultivationRealms(player);
        return nextRealms.isEmpty() ? null : nextRealms.get(0);
    }

    /**
     * Breakthrough with unique conditions
     * @param player The player
     * @param cultivationData Player's cultivation data
     * @param type The cultivation type
     * @return Special realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getSpecialBreakthrough(Player player, ICultivationData cultivationData, ICultivationData.CultivationType type) {
        return null; // Override in specific realms
    }

    /**
     * Boundary breakthrough (Ex. First Step -> Second Step)
     * @param player The player
     * @param cultivationData Player's cultivation data
     * @param type The cultivation type
     * @return Boundary realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getBoundaryBreakthrough(Player player, ICultivationData cultivationData, ICultivationData.CultivationType type) {
        return null;
    }

    // === Abilities and Features ===

    /**
     * Abilities unlocked at this realm
     * @return List of ability IDs
     */
    public List<String> getUnlockedAbilities() {
        return new ArrayList<>();
    }

    /**
     * Passive benefits of this realm
     * @return List of benefit descriptions
     */
    public List<String> getRealmBenefits() {
        return new ArrayList<>();
    }

    /**
     * Cultivation techniques available at this realm
     * @return List of technique IDs
     */
    public List<String> getAvailableTechniques() {
        return new ArrayList<>();
    }

    // === Comparison Methods ===

    /**
     * Check if this realm is higher than another
     * @param other Other realm to compare
     * @return true if this realm is higher level
     */
    public boolean isHigherThan(CultivationRealm other) {
        return this.getRealmLevel() > other.getRealmLevel();
    }

    /**
     * Check if this realm is lower than another
     * @param other Other realm to compare
     * @return true if this realm is lower level
     */
    public boolean isLowerThan(CultivationRealm other) {
        return this.getRealmLevel() < other.getRealmLevel();
    }

    /**
     * Check if realms are in the same cultivation stage
     * @param other Other realm to compare
     * @return true if in same stage
     */
    public boolean isSameStageAs(CultivationRealm other) {
        return getCultivationStage().equals(other.getCultivationStage());
    }

    public boolean isMajorRealm() {
        return false;
    }

    /**
     * Get the cultivation stage/category this realm belongs to
     * @return Stage name (e.g., "Qi Condensation", "Foundation Establishment")
     */
    public String getCultivationStage() {
        return "Unknown Stage";
    }

    // === Unified Events and Effects ===

    /**
     * Called when player advances to this realm through any cultivation type
     * @param player The player advancing
     * @param cultivationData Player's cultivation data
     * @param previousRealm The realm they came from
     * @param type The cultivation type used for advancement
     */
    public void onRealmAdvancement(Player player, ICultivationData cultivationData, @Nullable CultivationRealm previousRealm, ICultivationData.CultivationType type) {
        // Default implementation - override for custom effects
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.literal("Advanced to " + getDisplayName() + " through " + type.getDisplayName() + " cultivation!")
                    .withStyle(ChatFormatting.GOLD)
            );
        }
    }

    /**
     * Called when player leaves this realm through any cultivation type
     * @param player The player leaving
     * @param cultivationData Player's cultivation data
     * @param nextRealm The realm they're going to
     * @param type The cultivation type being used
     */
    public void onRealmExit(Player player, ICultivationData cultivationData, @Nullable CultivationRealm nextRealm, ICultivationData.CultivationType type) {
        // Override for cleanup or farewell effects
    }

    /**
     * Apply realm-specific modifiers
     * @param player The player
     * @param cultivationData Player's cultivation data
     * @param type The cultivation type being affected
     * @param silent If true, suppresses advancement messages
     */
    public void applyRealmModifiers(Player player, ICultivationData cultivationData, ICultivationData.CultivationType type, boolean silent) {
        // Override for special realm effects
    }

    // === Visual/UI Properties ===

    /**
     * Color associated with this realm (for UI/effects)
     * @return RGB color value
     */
    public int getRealmColor() {
        return 0xFFFFFF; // Default white
    }

    /**
     * Particle effect for this realm
     * @return Particle type
     */
    public ParticleOptions getRealmParticle() {
        return ParticleTypes.ENCHANT;
    }

    /**
     * Icon/texture for this realm
     * @return Resource location for icon
     */
    public ResourceLocation getRealmIcon() {
        return ResourceLocation.fromNamespaceAndPath("immortalcultivation", "textures/gui/realms/" + getId() + ".png");
    }

    // === Utility Methods ===

    @Override
    public String toString() {
        return String.format("CultivationRealm{id='%s', level=%d, stage='%s'}",
            getId(), getRealmLevel(), getCultivationStage());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CultivationRealm that = (CultivationRealm) obj;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}