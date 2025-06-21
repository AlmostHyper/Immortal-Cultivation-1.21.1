package net.hyper.immortalcultivation.cultivation.realms;

import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
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
 * Base class for all cultivation realms in the dual cultivation system.
 * This abstract class provides the framework for both Qi and Body cultivation realms,
 * allowing each realm to define its own progression paths and requirements.
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
    public abstract long getBaseHealth();

    /**
     * Base health regeneration rate per second
     * @return Health regen per second
     */
    public abstract double getBaseHealthRegeneration();

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
     * Meditation Cultivation per second
     * @return Cultivation per second while meditating
     */
    public final double getBaseMeditationCultivationRate() {
        return getBaseCultivationRate() * 9;
    }

    /**
     * Minimum Cultivation required to attempt breakthrough from this realm
     * @return Cultivation threshold for breakthrough
     */
    public final long getBreakthroughCultivationThreshold() {
        return (long) (getBaseMaxCultivation() * 0.7);
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
    public abstract double getBaseQiRegenerationRate();

    /**
     * Circulating qi regeneration rate per second
     * @return Circulating qi regen per second
     */
    public abstract double getBaseCirculatingQiRegenerationRate();

    // === Qi Cultivation Breakthrough Logic ===

    /**
     * Checks if player meets Qi Cultivation requirements for breakthrough
     * @param qiCultivationData Player's Qi Cultivation data
     * @return true if Cultivation threshold is met
     */
    public boolean meetsQiCultivationThreshold(IQiCultivationData qiCultivationData) {
        return qiCultivationData.getCurrentQiCultivation() >= getBreakthroughCultivationThreshold();
    }

    /**
     * Checks if player can breakthrough in Qi cultivation (includes all conditions)
     * @param player The player attempting breakthrough
     * @param qiCultivationData Player's Qi Cultivation data
     * @return true if breakthrough is possible
     */
    public boolean canBreakthrough(Player player, IQiCultivationData qiCultivationData) {
        return meetsQiCultivationThreshold(qiCultivationData) &&
            !getNextCultivationRealms(player).isEmpty() &&
            meetsAdditionalRequirements(player, qiCultivationData);
    }

    /**
     * Override for additional Qi breakthrough requirements beyond cultivation amount
     * @param player The player
     * @param qiData Player's Qi cultivation data
     * @return true if additional requirements are met
     */
    public boolean meetsAdditionalRequirements(Player player, IQiCultivationData qiData) {
        return true; // Default: no additional requirements
    }

    // === Body Cultivation Breakthrough Logic ===

    /**
     * Checks if player meets Body Cultivation requirements for breakthrough
     * @param bodyCultivationData Player's Body Cultivation data
     * @return true if Cultivation threshold is met
     */
    public boolean meetsBodyCultivationThreshold(IBodyCultivationData bodyCultivationData) {
        return bodyCultivationData.getCurrentBodyCultivation() >= getBreakthroughCultivationThreshold();
    }

    /**
     * Checks if player can breakthrough in Body cultivation (includes all conditions)
     * @param player The player attempting breakthrough
     * @param bodyCultivationData Player's Body Cultivation data
     * @return true if breakthrough is possible
     */
    public boolean canBreakthrough(Player player, IBodyCultivationData bodyCultivationData) {
        return meetsBodyCultivationThreshold(bodyCultivationData) &&
            !getNextCultivationRealms(player).isEmpty() &&
            meetsAdditionalRequirements(player, bodyCultivationData);
    }

    /**
     * Override for additional Body breakthrough requirements beyond cultivation amount
     * @param player The player
     * @param bodyData Player's Body cultivation data
     * @return true if additional requirements are met
     */
    public boolean meetsAdditionalRequirements(Player player, IBodyCultivationData bodyData) {
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

    // === Qi Cultivation Breakthrough Types ===

    /**
     * Qi breakthrough to next minor realm (Ex. Early -> Middle Foundation Establishment)
     * @param player The player
     * @param qiCultivationData Player's Qi cultivation data
     * @return Next realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getMinorBreakthrough(Player player, IQiCultivationData qiCultivationData) {
        List<CultivationRealm> nextRealms = getNextCultivationRealms(player);
        return nextRealms.isEmpty() ? null : nextRealms.get(0);
    }

    /**
     * Qi breakthrough to next major realm (Ex. Qi Condensation -> Foundation Establishment)
     * @param player The player
     * @param qiCultivationData Player's Qi cultivation data
     * @return Next realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getMajorBreakthrough(Player player, IQiCultivationData qiCultivationData) {
        List<CultivationRealm> nextRealms = getNextCultivationRealms(player);
        return nextRealms.isEmpty() ? null : nextRealms.get(0);
    }

    /**
     * Qi breakthrough with unique conditions
     * @param player The player
     * @param qiCultivationData Player's Qi cultivation data
     * @return Special realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getSpecialBreakthrough(Player player, IQiCultivationData qiCultivationData) {
        return null; // Override in specific realms
    }

    /**
     * Qi boundary breakthrough (Ex. First Step -> Second Step)
     * @param player The player
     * @param qiCultivationData Player's Qi cultivation data
     * @return Boundary realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getBoundaryBreakthrough(Player player, IQiCultivationData qiCultivationData) {
        return null;
    }

    // === Body Cultivation Breakthrough Types ===

    /**
     * Body breakthrough to next minor realm
     * @param player The player
     * @param bodyCultivationData Player's Body cultivation data
     * @return Next realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getMinorBreakthrough(Player player, IBodyCultivationData bodyCultivationData) {
        List<CultivationRealm> nextRealms = getNextCultivationRealms(player);
        return nextRealms.isEmpty() ? null : nextRealms.get(0);
    }

    /**
     * Body breakthrough to next major realm
     * @param player The player
     * @param bodyCultivationData Player's Body cultivation data
     * @return Next realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getMajorBreakthrough(Player player, IBodyCultivationData bodyCultivationData) {
        List<CultivationRealm> nextRealms = getNextCultivationRealms(player);
        return nextRealms.isEmpty() ? null : nextRealms.get(0);
    }

    /**
     * Body breakthrough with unique conditions
     * @param player The player
     * @param bodyCultivationData Player's Body cultivation data
     * @return Special realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getSpecialBreakthrough(Player player, IBodyCultivationData bodyCultivationData) {
        return null; // Override in specific realms
    }

    /**
     * Body boundary breakthrough
     * @param player The player
     * @param bodyCultivationData Player's Body cultivation data
     * @return Boundary realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getBoundaryBreakthrough(Player player, IBodyCultivationData bodyCultivationData) {
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

    /**
     * Get the cultivation stage/category this realm belongs to
     * @return Stage name (e.g., "Qi Condensation", "Foundation Establishment")
     */
    public String getCultivationStage() {
        return "Unknown Stage";
    }

    // === Events and Effects for Qi Cultivation ===

    /**
     * Called when player advances to this realm via Qi cultivation
     * @param player The player advancing
     * @param qiData Player's Qi cultivation data
     * @param previousRealm The realm they came from
     */
    public void onRealmAdvancement(Player player, IQiCultivationData qiData, @Nullable CultivationRealm previousRealm) {
        // Default implementation - override for custom effects
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.literal("Advanced to " + getDisplayName() + " through Qi cultivation!")
                    .withStyle(ChatFormatting.GOLD)
            );
        }
    }

    /**
     * Called when player leaves this realm via Qi cultivation
     * @param player The player leaving
     * @param qiData Player's Qi cultivation data
     * @param nextRealm The realm they're going to
     */
    public void onRealmExit(Player player, IQiCultivationData qiData, @Nullable CultivationRealm nextRealm) {
        // Override for cleanup or farewell effects
    }

    /**
     * Apply realm-specific modifiers from Qi cultivation
     * @param player The player
     * @param qiData Player's Qi cultivation data
     */
    public void applyRealmModifiers(Player player, IQiCultivationData qiData) {
        // Override for special realm effects
    }

    // === Events and Effects for Body Cultivation ===

    /**
     * Called when player advances to this realm via Body cultivation
     * @param player The player advancing
     * @param bodyData Player's Body cultivation data
     * @param previousRealm The realm they came from
     */
    public void onRealmAdvancement(Player player, IBodyCultivationData bodyData, @Nullable CultivationRealm previousRealm) {
        // Default implementation - override for custom effects
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.literal("Advanced to " + getDisplayName() + " through Body cultivation!")
                    .withStyle(ChatFormatting.GOLD)
            );
        }
    }

    /**
     * Called when player leaves this realm via Body cultivation
     * @param player The player leaving
     * @param bodyData Player's Body cultivation data
     * @param nextRealm The realm they're going to
     */
    public void onRealmExit(Player player, IBodyCultivationData bodyData, @Nullable CultivationRealm nextRealm) {
        // Override for cleanup or farewell effects
    }

    /**
     * Apply realm-specific modifiers from Body cultivation
     * @param player The player
     * @param bodyData Player's Body cultivation data
     */
    public void applyRealmModifiers(Player player, IBodyCultivationData bodyData) {
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