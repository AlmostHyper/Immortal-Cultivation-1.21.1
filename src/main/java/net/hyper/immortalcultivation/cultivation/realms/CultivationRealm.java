package net.hyper.immortalcultivation.cultivation.realms;

import net.hyper.immortalcultivation.cultivation.IPlayerQiData;
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

public abstract class CultivationRealm implements IPlayerQiData {

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
     * @return Realm level (higher = more advanced)
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
     * Accelerated qi regeneration rate per second
     * @return Accelerated qi regen per second
     */
    public abstract double getBaseAcceleratedQiRegenerationRate();

    /**
     * Minimum Qi required to attempt breakthrough from this realm
     * @return Qi threshold for breakthrough
     */
    public long getBreakthroughQiThreshold() {
        return (long) (getBaseMaxQi() * 0.8); // Default: 80% of max Qi
    }

    /**
     * Qi cost consumed during breakthrough attempt
     * @return Qi cost for breakthrough
     */
    public long getBreakthroughQiCost() {
        return getBreakthroughQiThreshold() / 2; // Default: half the threshold
    }

    // === Breakthrough Logic ===

    /**
     * Checks if player meets Qi requirements for breakthrough
     * @param qiData Player's Qi data
     * @return true if Qi threshold is met
     */
    public boolean meetsQiThreshold(IPlayerQiData qiData) {
        return qiData.getCurrentQi() >= getBreakthroughQiThreshold();
    }

    /**
     * Checks if player can breakthrough (includes all conditions)
     * @param player The player attempting breakthrough
     * @param qiData Player's Qi data
     * @return true if breakthrough is possible
     */
    public boolean canBreakthrough(Player player, IPlayerQiData qiData) {
        return meetsQiThreshold(qiData) &&
            !getNextCultivationRealms(player).isEmpty() &&
            meetsAdditionalRequirements(player, qiData);
    }

    /**
     * Override for additional breakthrough requirements beyond Qi
     * @param player The player
     * @param qiData Player's Qi data
     * @return true if additional requirements are met
     */
    protected boolean meetsAdditionalRequirements(Player player, IPlayerQiData qiData) {
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

    // === Breakthrough Types ===

    /**
     * Standard breakthrough to next realm
     * @param player The player
     * @param qiData Player's Qi data
     * @return Next realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getStandardBreakthrough(Player player, IPlayerQiData qiData) {
        List<CultivationRealm> nextRealms = getNextCultivationRealms(player);
        return nextRealms.isEmpty() ? null : nextRealms.get(0);
    }

    /**
     * Special breakthrough with unique conditions
     * @param player The player
     * @param qiData Player's Qi data
     * @return Special realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getSpecialBreakthrough(Player player, IPlayerQiData qiData) {
        return null; // Override in specific realms
    }

    /**
     * Boundary breakthrough (crossing major cultivation stages)
     * @param player The player
     * @param qiData Player's Qi data
     * @return Boundary realm, or null if not possible
     */
    @Nullable
    public CultivationRealm getBoundaryBreakthrough(Player player, IPlayerQiData qiData) {
        return null; // Override in specific realms
    }

    /**
     * Determine the best breakthrough option for the player
     * @param player The player
     * @param qiData Player's Qi data
     * @return Best breakthrough realm, or null if none available
     */
    @Nullable
    public CultivationRealm getBestBreakthrough(Player player, IPlayerQiData qiData) {
        // Priority: Special > Boundary > Standard
        CultivationRealm special = getSpecialBreakthrough(player, qiData);
        if (special != null) return special;

        CultivationRealm boundary = getBoundaryBreakthrough(player, qiData);
        if (boundary != null) return boundary;

        return getStandardBreakthrough(player, qiData);
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

    // === Events and Effects ===

    /**
     * Called when player advances to this realm
     * @param player The player advancing
     * @param qiData Player's Qi data
     * @param previousRealm The realm they came from
     */
    public void onRealmAdvancement(Player player, IPlayerQiData qiData, @Nullable CultivationRealm previousRealm) {
        // Default implementation - override for custom effects
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.literal("Advanced to " + getDisplayName() + "!")
                    .withStyle(ChatFormatting.GOLD)
            );
        }
    }

    /**
     * Called when player leaves this realm
     * @param player The player leaving
     * @param qiData Player's Qi data
     * @param nextRealm The realm they're going to
     */
    public void onRealmExit(Player player, IPlayerQiData qiData, @Nullable CultivationRealm nextRealm) {
        // Override for cleanup or farewell effects
    }

    /**
     * Apply realm-specific modifiers to player stats
     * @param player The player
     * @param qiData Player's Qi data
     */
    public void applyRealmModifiers(Player player, IPlayerQiData qiData) {
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
