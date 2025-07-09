package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.util.DirtyTrackerUtil;
import net.hyper.immortalcultivation.common.interfaces.IDirtyTrackable;
import net.hyper.immortalcultivation.common.interfaces.ITickable;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;

/**
 * CORRECTED: PlayerCombatStatsAttachment using ONLY permanent additive bonuses.
 * This system never calculates stats from "current realm" - only from accumulated permanent bonuses.
 * Each JSON realm defines the permanent bonus you GET when reaching it, not base stats while in it.
 */
public class PlayerCombatStatsAttachment implements IDirtyTrackable, ITickable {

    // === PERMANENT BONUSES (accumulated from realm breakthroughs) ===
    private double permanentAttackDamageBonus = 0.0;
    private double permanentMovementSpeedBonus = 0.0;
    private double permanentJumpHeightBonus = 0.0;

    // === TEMPORARY/DYNAMIC BONUSES (from items, effects, etc.) ===
    private double attackDamageBonus = 0.0;
    private double movementSpeedBonus = 0.0;
    private double jumpHeightBonus = 0.0;

    // === DISPLAY VALUES (for client synchronization) ===
    private double displayAttackBonus = 0.0;
    private double displaySpeedBonus = 0.0;
    private double displayJumpBonus = 0.0;

    // === INTERNAL TRACKING ===
    @Nullable
    private Player player;

    // === INITIALIZATION ===

    public PlayerCombatStatsAttachment() {}

    /**
     * CRITICAL: Set player reference for sync and calculation purposes.
     * Must be called after dimension changes and during initialization.
     */
    public void setPlayer(Player player) {
        this.player = player;
        ImmortalCultivation.LOGGER.debug("Set player reference for {}: {}",
            this.getClass().getSimpleName(),
            player != null ? player.getName().getString() : "null");
    }

    @Nullable
    public Player getPlayerSafe() {
        if (player != null && player.isAlive()) {
            return player;
        }
        return null;
    }

    // === PERMANENT BONUS SYSTEM ===

    /**
     * Batch multiple permanent bonus changes to reduce sync overhead.
     * Recommended for realm breakthroughs that grant multiple bonuses.
     */
    public void addPermanentBonuses(double attackBonus, double speedBonus, double jumpBonus) {
        // Validate all inputs first
        if (attackBonus < 0 || speedBonus < 0 || jumpBonus < 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add negative permanent bonuses: ATK={}, SPD={}, JUMP={}",
                attackBonus, speedBonus, jumpBonus);
            return;
        }

        boolean changed = false;
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Player ").append(player != null ? player.getName().getString() : "unknown")
            .append(" gained permanent bonuses: ");

        if (attackBonus > 0) {
            this.permanentAttackDamageBonus += attackBonus;
            logMessage.append(String.format("ATK+%.1f%% (total: %.1f%%), ",
                attackBonus * 100, this.permanentAttackDamageBonus * 100));
            changed = true;
        }

        if (speedBonus > 0) {
            this.permanentMovementSpeedBonus += speedBonus;
            logMessage.append(String.format("SPD+%.1f%% (total: %.1f%%), ",
                speedBonus * 100, this.permanentMovementSpeedBonus * 100));
            changed = true;
        }

        if (jumpBonus > 0) {
            this.permanentJumpHeightBonus += jumpBonus;
            logMessage.append(String.format("JUMP+%.1f%% (total: %.1f%%)",
                jumpBonus * 100, this.permanentJumpHeightBonus * 100));
            changed = true;
        }

        if (changed) {
            markDirty();

            // Trigger immediate refresh for consistent behavior
            if (player instanceof ServerPlayer serverPlayer) {
                CombatStatsEventHandler.refreshCombatStats(serverPlayer);
            }

            // Clean up trailing comma if present
            String finalMessage = logMessage.toString().replaceAll(", $", "");
            ImmortalCultivation.LOGGER.info(finalMessage);
        } else {
            ImmortalCultivation.LOGGER.debug("No valid bonuses to apply for player: {}",
                player != null ? player.getName().getString() : "unknown");
        }
    }

    /**
     * Adds a permanent attack damage bonus from realm breakthrough.
     * For single bonus additions. Use addPermanentBonuses() for multiple bonuses.
     */
    public void addPermanentAttackDamageBonus(double attackBonus) {
        if (attackBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive attack damage bonus: {}", attackBonus);
            return;
        }

        this.permanentAttackDamageBonus += attackBonus;
        markDirty();

        // Consistent refresh behavior
        if (player instanceof ServerPlayer serverPlayer) {
            CombatStatsEventHandler.refreshCombatStats(serverPlayer);
        }

        ImmortalCultivation.LOGGER.info("Player {} gained +{}% permanent attack damage from realm breakthrough (total: +{}%)",
            player != null ? player.getName().getString() : "unknown",
            String.format("%.1f", attackBonus * 100),
            String.format("%.1f", this.permanentAttackDamageBonus * 100));
    }

    /**
     * Adds a permanent movement speed bonus from realm breakthrough.
     * For single bonus additions. Use addPermanentBonuses() for multiple bonuses.
     */
    public void addPermanentMovementSpeedBonus(double speedBonus) {
        if (speedBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive movement speed bonus: {}", speedBonus);
            return;
        }

        this.permanentMovementSpeedBonus += speedBonus;
        markDirty();

        // Consistent refresh behavior
        if (player instanceof ServerPlayer serverPlayer) {
            CombatStatsEventHandler.refreshCombatStats(serverPlayer);
        }

        ImmortalCultivation.LOGGER.info("Player {} gained +{}% permanent movement speed from realm breakthrough (total: +{}%)",
            player != null ? player.getName().getString() : "unknown",
            String.format("%.1f", speedBonus * 100),
            String.format("%.1f", this.permanentMovementSpeedBonus * 100));
    }

    /**
     * Adds a permanent jump height bonus from realm breakthrough.
     * For single bonus additions. Use addPermanentBonuses() for multiple bonuses.
     */
    public void addPermanentJumpHeightBonus(double jumpBonus) {
        if (jumpBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive jump height bonus: {}", jumpBonus);
            return;
        }

        this.permanentJumpHeightBonus += jumpBonus;
        markDirty();

        // Consistent refresh behavior
        if (player instanceof ServerPlayer serverPlayer) {
            CombatStatsEventHandler.refreshCombatStats(serverPlayer);
        }

        ImmortalCultivation.LOGGER.info("Player {} gained +{}% permanent jump height from realm breakthrough (total: +{}%)",
            player != null ? player.getName().getString() : "unknown",
            String.format("%.1f", jumpBonus * 100),
            String.format("%.1f", this.permanentJumpHeightBonus * 100));
    }

    // === GETTERS FOR PERMANENT BONUSES ===

    public double getPermanentAttackDamageBonus() {
        return permanentAttackDamageBonus;
    }

    public double getPermanentMovementSpeedBonus() {
        return permanentMovementSpeedBonus;
    }

    public double getPermanentJumpHeightBonus() {
        return permanentJumpHeightBonus;
    }

    // === SETTERS FOR SYNC PACKETS ===

    public void setPermanentAttackDamageBonus(double bonus) {
        this.permanentAttackDamageBonus = Math.max(0.0, bonus);
    }

    public void setPermanentMovementSpeedBonus(double bonus) {
        this.permanentMovementSpeedBonus = Math.max(0.0, bonus);
    }

    public void setPermanentJumpHeightBonus(double bonus) {
        this.permanentJumpHeightBonus = Math.max(0.0, bonus);
    }

    // === TEMPORARY BONUS METHODS ===

    public double getAttackDamageBonus() {
        return attackDamageBonus;
    }

    public void addAttackDamageBonus(double bonus) {
        if (bonus > 0) {
            this.attackDamageBonus += bonus;
            markDirty();

            ImmortalCultivation.LOGGER.debug("Added {}% temporary attack damage bonus to player {} (total temporary: +{}%)",
                String.format("%.1f", bonus * 100),
                player != null ? player.getName().getString() : "unknown",
                String.format("%.1f", attackDamageBonus * 100));
        }
    }

    public void setAttackDamageBonus(double bonus) {
        this.attackDamageBonus = Math.max(0, bonus);
        markDirty();
    }

    public double getMovementSpeedBonus() {
        return movementSpeedBonus;
    }

    public void addMovementSpeedBonus(double bonus) {
        if (bonus > 0) {
            this.movementSpeedBonus += bonus;
            markDirty();

            ImmortalCultivation.LOGGER.debug("Added {}% temporary movement speed bonus to player {} (total temporary: +{}%)",
                String.format("%.1f", bonus * 100),
                player != null ? player.getName().getString() : "unknown",
                String.format("%.1f", movementSpeedBonus * 100));
        }
    }

    public void setMovementSpeedBonus(double bonus) {
        this.movementSpeedBonus = Math.max(0, bonus);
        markDirty();
    }

    public double getJumpHeightBonus() {
        return jumpHeightBonus;
    }

    public void addJumpHeightBonus(double bonus) {
        if (bonus > 0) {
            this.jumpHeightBonus += bonus;
            markDirty();

            ImmortalCultivation.LOGGER.debug("Added {}% temporary jump height bonus to player {} (total temporary: +{}%)",
                String.format("%.1f", bonus * 100),
                player != null ? player.getName().getString() : "unknown",
                String.format("%.1f", jumpHeightBonus * 100));
        }
    }

    public void setJumpHeightBonus(double bonus) {
        this.jumpHeightBonus = Math.max(0, bonus);
        markDirty();
    }

    // === EFFECTIVE BONUS CALCULATIONS (CORRECTED - PERMANENT ADDITIVE ONLY) ===

    /**
     * CORRECTED: Get effective attack damage multiplier using ONLY permanent + temporary bonuses.
     * Never uses "current realm" values because your system doesn't work that way.
     */
    public double getEffectiveAttackDamageMultiplier() {
        // Base multiplier (1.0) + all accumulated permanent bonuses + temporary bonuses
        double total = 1.0 + permanentAttackDamageBonus + attackDamageBonus;

        ImmortalCultivation.LOGGER.debug("Calculated attack multiplier for {}: permanent={}, temporary={}, total={}",
            player != null ? player.getName().getString() : "unknown",
            permanentAttackDamageBonus, attackDamageBonus, total);

        return total;
    }

    /**
     * CORRECTED: Get effective movement speed multiplier using ONLY permanent + temporary bonuses.
     * Never uses "current realm" values because your system doesn't work that way.
     */
    public double getEffectiveMovementSpeedMultiplier() {
        // All accumulated permanent bonuses + temporary bonuses
        double total = permanentMovementSpeedBonus + movementSpeedBonus;

        ImmortalCultivation.LOGGER.debug("Calculated movement speed for {}: permanent={}, temporary={}, total={}",
            player != null ? player.getName().getString() : "unknown",
            permanentMovementSpeedBonus, movementSpeedBonus, total);

        return total;
    }

    /**
     * CORRECTED: Get effective jump height multiplier using ONLY permanent + temporary bonuses.
     * Never uses "current realm" values because your system doesn't work that way.
     */
    public double getEffectiveJumpHeightMultiplier() {
        // All accumulated permanent bonuses + temporary bonuses
        double total = permanentJumpHeightBonus + jumpHeightBonus;

        ImmortalCultivation.LOGGER.debug("Calculated jump height for {}: permanent={}, temporary={}, total={}",
            player != null ? player.getName().getString() : "unknown",
            permanentJumpHeightBonus, jumpHeightBonus, total);

        return total;
    }

    // === UTILITY METHODS ===

    /**
     * Reset all temporary bonuses (permanent bonuses remain)
     */
    public void resetTemporaryBonuses() {
        this.attackDamageBonus = 0.0;
        this.movementSpeedBonus = 0.0;
        this.jumpHeightBonus = 0.0;
        markDirty();

        ImmortalCultivation.LOGGER.debug("Reset temporary bonuses for player: {}",
            player != null ? player.getName().getString() : "unknown");
    }

    /**
     * Reset all bonuses including permanent ones (for debugging or special events)
     */
    public void resetAllBonuses() {
        this.permanentAttackDamageBonus = 0.0;
        this.permanentMovementSpeedBonus = 0.0;
        this.permanentJumpHeightBonus = 0.0;
        this.attackDamageBonus = 0.0;
        this.movementSpeedBonus = 0.0;
        this.jumpHeightBonus = 0.0;
        markDirty();

        ImmortalCultivation.LOGGER.warn("Reset ALL bonuses (including permanent) for player: {}",
            player != null ? player.getName().getString() : "unknown");
    }

    /**
     * Get total bonus score for display purposes
     */
    public double getTotalBonusScore() {
        return (getEffectiveAttackDamageMultiplier() - 1.0 +
            getEffectiveMovementSpeedMultiplier() +
            getEffectiveJumpHeightMultiplier()) * 100;
    }

    /**
     * Get breakdown of bonuses for debugging
     */
    public String getBonusBreakdown() {
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("=== Combat Stats Breakdown ===\n");
        breakdown.append("Permanent Attack: +").append(String.format("%.3f", permanentAttackDamageBonus)).append("\n");
        breakdown.append("Temporary Attack: +").append(String.format("%.3f", attackDamageBonus)).append("\n");
        breakdown.append("Effective Attack Multiplier: ").append(String.format("%.3f", getEffectiveAttackDamageMultiplier())).append("\n");
        breakdown.append("Permanent Speed: +").append(String.format("%.3f", permanentMovementSpeedBonus)).append("\n");
        breakdown.append("Temporary Speed: +").append(String.format("%.3f", movementSpeedBonus)).append("\n");
        breakdown.append("Effective Speed: +").append(String.format("%.3f", getEffectiveMovementSpeedMultiplier())).append("\n");
        breakdown.append("Permanent Jump: +").append(String.format("%.3f", permanentJumpHeightBonus)).append("\n");
        breakdown.append("Temporary Jump: +").append(String.format("%.3f", jumpHeightBonus)).append("\n");
        breakdown.append("Effective Jump: +").append(String.format("%.3f", getEffectiveJumpHeightMultiplier())).append("\n");
        return breakdown.toString();
    }

    // === DISPLAY VALUES FOR CLIENT ===

    public double getDisplayAttackBonus() {
        // On client side, prefer synced display value
        if (player != null && player.level().isClientSide()) {
            return displayAttackBonus;
        }
        // On server side or if client doesn't have synced value, calculate
        double effective = getEffectiveAttackDamageMultiplier();
        return Math.max(0.0, effective - 1.0);
    }

    public double getDisplaySpeedBonus() {
        return player != null && player.level().isClientSide() ? displaySpeedBonus : getEffectiveMovementSpeedMultiplier();
    }

    public double getDisplayJumpBonus() {
        return player != null && player.level().isClientSide() ? displayJumpBonus : getEffectiveJumpHeightMultiplier();
    }

    public static PlayerCombatStatsAttachment getOrCreate(Player player) {
        PlayerCombatStatsAttachment attachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
        if (attachment.player == null) {
            attachment.setPlayer(player);
        }
        return attachment;
    }

    // === SYNCHRONIZATION ===

    private final DirtyTrackerUtil dirtyTrackerUtil = new DirtyTrackerUtil() {
        @Override
        public void forceSync() {
            PlayerCombatStatsAttachment.this.forceSync();
        }

        @Override
        public boolean shouldSyncNow() {
            return shouldSync(2000, 2); // Combat stats don't need as frequent sync as resources
        }
    };

    /**
     * Update combat stats from server data (called by sync packet)
     */
    public void updateFromServer(double attackBonus, double speedBonus, double jumpBonus,
                                 double permAttack, double permSpeed, double permJump) {
        try {
            // Store display values
            this.displayAttackBonus = attackBonus;
            this.displaySpeedBonus = speedBonus;
            this.displayJumpBonus = jumpBonus;

            // CRITICAL: Also update permanent values on client
            this.permanentAttackDamageBonus = permAttack;
            this.permanentMovementSpeedBonus = permSpeed;
            this.permanentJumpHeightBonus = permJump;

            clearDirty(); // Use DirtyTrackerUtil instead of needsSync

            ImmortalCultivation.LOGGER.debug("Updated combat stats from server: ATK+{}% (perm: {}), SPD+{}% (perm: {}), JUMP+{}% (perm: {})",
                String.format("%.1f", attackBonus * 100), String.format("%.1f", permAttack * 100),
                String.format("%.1f", speedBonus * 100), String.format("%.1f", permSpeed * 100),
                String.format("%.1f", jumpBonus * 100), String.format("%.1f", permJump * 100));

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating combat stats from server", e);
        }
    }

    @Override
    public void tick() {
        // Update display values if needed
        updateDisplayValues();

        // Check if sync is needed
        if (shouldSyncNow()) {
            forceSync();
        }
    }

    private void updateDisplayValues() {
        // Only calculate if this is server-side
        if (player == null || player.level().isClientSide()) {
            return;
        }

        double newAttackBonus = getDisplayAttackBonus();
        double newSpeedBonus = getEffectiveMovementSpeedMultiplier();
        double newJumpBonus = getEffectiveJumpHeightMultiplier();

        boolean changed = false;
        final double threshold = 0.001;

        if (Math.abs(displayAttackBonus - newAttackBonus) > threshold) {
            displayAttackBonus = newAttackBonus;
            changed = true;
        }
        if (Math.abs(displaySpeedBonus - newSpeedBonus) > threshold) {
            displaySpeedBonus = newSpeedBonus;
            changed = true;
        }
        if (Math.abs(displayJumpBonus - newJumpBonus) > threshold) {
            displayJumpBonus = newJumpBonus;
            changed = true;
        }

        if (changed) {
            markDirty();
        }
    }

    // === DIRTY FLAGS === //

    @Override
    public boolean shouldSyncNow() {
        return dirtyTrackerUtil.shouldSync(2000, 2); // 2 second interval, 2 change threshold
    }

    @Override
    public void forceSync() {
        if (getPlayerSafe() instanceof ServerPlayer serverPlayer) {
            try {
                NetworkSyncManager.scheduleSync(serverPlayer, NetworkSyncManager.SyncPriority.CRITICAL);
                clearDirty();
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Failed to force sync attachment data for player: {}",
                    serverPlayer.getName().getString(), e);
            }
        }
    }

    @Override
    public void markDirty() {
        dirtyTrackerUtil.markDirty();
    }

    @Override
    public boolean isDirty() {
        return dirtyTrackerUtil.isDirty();
    }

    @Override
    public void clearDirty() {
        dirtyTrackerUtil.clearDirty();
    }

    @Override
    public long getChangeCounter() {
        return dirtyTrackerUtil.getChangeCounter();
    }

    @Override
    public void incrementChangeCounter() {
        dirtyTrackerUtil.incrementChangeCounter();
    }

    @Override
    public void resetChangeCounter() {
        dirtyTrackerUtil.resetChangeCounter();
    }

    // === SERIALIZER === //

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerCombatStatsAttachment> {
        @Override
        public PlayerCombatStatsAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerCombatStatsAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerCombatStatsAttachment: {}", error))
                .map(pair -> pair.getFirst())
                .orElse(new PlayerCombatStatsAttachment());

            // CRITICAL: Set player reference during deserialization
            if (holder instanceof Player player) {
                attachment.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Set player reference during combat stats attachment deserialization: {}",
                    player.getName().getString());
            }

            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerCombatStatsAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encode(attachment, provider.createSerializationContext(NbtOps.INSTANCE), new CompoundTag())
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerCombatStatsAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }

    // === CODEC === //

    public static final Codec<PlayerCombatStatsAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        // Permanent bonuses
        Codec.DOUBLE.optionalFieldOf("permanentAttackDamageBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getPermanentAttackDamageBonus),
        Codec.DOUBLE.optionalFieldOf("permanentMovementSpeedBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getPermanentMovementSpeedBonus),
        Codec.DOUBLE.optionalFieldOf("permanentJumpHeightBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getPermanentJumpHeightBonus),
        // Temporary bonuses
        Codec.DOUBLE.optionalFieldOf("attackDamageBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getAttackDamageBonus),
        Codec.DOUBLE.optionalFieldOf("movementSpeedBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getMovementSpeedBonus),
        Codec.DOUBLE.optionalFieldOf("jumpHeightBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getJumpHeightBonus),
        // Remove lastSyncTime from here since it's handled by DirtyTrackerUtil
        // DirtyTrackerUtil fields
        Codec.LONG.optionalFieldOf("changeCounter", 0L).forGetter(comp -> comp.dirtyTrackerUtil.getChangeCounter()),
        Codec.LONG.optionalFieldOf("dirtyLastSyncTime", 0L).forGetter(comp -> comp.dirtyTrackerUtil.getLastSyncTime())
    ).apply(instance, (permAttack, permSpeed, permJump, attackBonus, speedBonus, jumpBonus, changeCounter, dirtyLastSync) -> {
        PlayerCombatStatsAttachment comp = new PlayerCombatStatsAttachment();
        comp.permanentAttackDamageBonus = permAttack;
        comp.permanentMovementSpeedBonus = permSpeed;
        comp.permanentJumpHeightBonus = permJump;
        comp.attackDamageBonus = attackBonus;
        comp.movementSpeedBonus = speedBonus;
        comp.jumpHeightBonus = jumpBonus;
        // Remove: comp.lastSyncTime = syncTime;

        // Restore DirtyTrackerUtil state
        comp.dirtyTrackerUtil.restoreFromCodec(changeCounter, dirtyLastSync);
        comp.markDirty(); // Always dirty after loading

        return comp;
    }));
}