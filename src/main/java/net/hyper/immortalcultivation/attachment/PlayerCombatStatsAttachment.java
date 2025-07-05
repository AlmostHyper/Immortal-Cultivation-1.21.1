package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.network.server.CombatStatsSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * CORRECTED: PlayerCombatStatsAttachment using ONLY permanent additive bonuses.
 * This system never calculates stats from "current realm" - only from accumulated permanent bonuses.
 * Each JSON realm defines the permanent bonus you GET when reaching it, not base stats while in it.
 */
public class PlayerCombatStatsAttachment implements INBTSerializable<CompoundTag> {

    // === CONSTANTS ===
    private static final String TAG_PERMANENT_ATTACK_BONUS = "PermanentAttackDamageBonus";
    private static final String TAG_PERMANENT_SPEED_BONUS = "PermanentMovementSpeedBonus";
    private static final String TAG_PERMANENT_JUMP_BONUS = "PermanentJumpHeightBonus";
    private static final String TAG_ATTACK_DAMAGE_BONUS = "AttackDamageBonus";
    private static final String TAG_MOVEMENT_SPEED_BONUS = "MovementSpeedBonus";
    private static final String TAG_JUMP_HEIGHT_BONUS = "JumpHeightBonus";
    private static final String TAG_LAST_SYNC_TIME = "LastSyncTime";

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
    private long lastSyncTime = 0;
    private boolean needsSync = false;
    private int tickCounter = 0;

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
    protected Player getPlayerSafe() {
        if (player != null && player.isAlive()) {
            return player;
        }
        return null;
    }

    // === PERMANENT BONUS SYSTEM ===

    /**
     * Adds a permanent attack damage bonus from realm breakthrough.
     * This is the ONLY way attack damage increases in your system.
     */
    public void addPermanentAttackDamageBonus(double attackBonus) {
        if (attackBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive attack damage bonus: {}", attackBonus);
            return;
        }

        this.permanentAttackDamageBonus += attackBonus;
        markForSync();

        ImmortalCultivation.LOGGER.info("Player {} gained +{}% permanent attack damage from realm breakthrough (total: +{}%)",
            player != null ? player.getName().getString() : "unknown",
            String.format("%.1f", attackBonus * 100),
            String.format("%.1f", this.permanentAttackDamageBonus * 100));
    }

    /**
     * Adds a permanent movement speed bonus from realm breakthrough.
     * This is the ONLY way movement speed increases in your system.
     */
    public void addPermanentMovementSpeedBonus(double speedBonus) {
        if (speedBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive movement speed bonus: {}", speedBonus);
            return;
        }

        this.permanentMovementSpeedBonus += speedBonus;
        markForSync();

        ImmortalCultivation.LOGGER.info("Player {} gained +{}% permanent movement speed from realm breakthrough (total: +{}%)",
            player != null ? player.getName().getString() : "unknown",
            String.format("%.1f", speedBonus * 100),
            String.format("%.1f", this.permanentMovementSpeedBonus * 100));
    }

    /**
     * Adds a permanent jump height bonus from realm breakthrough.
     * This is the ONLY way jump height increases in your system.
     */
    public void addPermanentJumpHeightBonus(double jumpBonus) {
        if (jumpBonus <= 0) {
            ImmortalCultivation.LOGGER.warn("Attempted to add non-positive jump height bonus: {}", jumpBonus);
            return;
        }

        this.permanentJumpHeightBonus += jumpBonus;
        markForSync();

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
            markForSync();

            ImmortalCultivation.LOGGER.debug("Added {}% temporary attack damage bonus to player {} (total temporary: +{}%)",
                String.format("%.1f", bonus * 100),
                player != null ? player.getName().getString() : "unknown",
                String.format("%.1f", attackDamageBonus * 100));
        }
    }

    public void setAttackDamageBonus(double bonus) {
        this.attackDamageBonus = Math.max(0, bonus);
        markForSync();
    }

    public double getMovementSpeedBonus() {
        return movementSpeedBonus;
    }

    public void addMovementSpeedBonus(double bonus) {
        if (bonus > 0) {
            this.movementSpeedBonus += bonus;
            markForSync();

            ImmortalCultivation.LOGGER.debug("Added {}% temporary movement speed bonus to player {} (total temporary: +{}%)",
                String.format("%.1f", bonus * 100),
                player != null ? player.getName().getString() : "unknown",
                String.format("%.1f", movementSpeedBonus * 100));
        }
    }

    public void setMovementSpeedBonus(double bonus) {
        this.movementSpeedBonus = Math.max(0, bonus);
        markForSync();
    }

    public double getJumpHeightBonus() {
        return jumpHeightBonus;
    }

    public void addJumpHeightBonus(double bonus) {
        if (bonus > 0) {
            this.jumpHeightBonus += bonus;
            markForSync();

            ImmortalCultivation.LOGGER.debug("Added {}% temporary jump height bonus to player {} (total temporary: +{}%)",
                String.format("%.1f", bonus * 100),
                player != null ? player.getName().getString() : "unknown",
                String.format("%.1f", jumpHeightBonus * 100));
        }
    }

    public void setJumpHeightBonus(double bonus) {
        this.jumpHeightBonus = Math.max(0, bonus);
        markForSync();
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
        markForSync();

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
        markForSync();

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

    /**
     * Update combat stats from server data (called by sync packet)
     */
    public void updateFromServer(double attackBonus, double speedBonus, double jumpBonus) {
        try {
            // Store display values separately
            double oldAttackBonus = this.displayAttackBonus;

            this.displayAttackBonus = attackBonus;
            this.displaySpeedBonus = speedBonus;
            this.displayJumpBonus = jumpBonus;
            this.needsSync = false;

            ImmortalCultivation.LOGGER.debug("Updated combat stats from server: ATK+{}% (was {}%), SPD+{}%, JUMP+{}%",
                String.format("%.1f", attackBonus * 100),
                String.format("%.1f", oldAttackBonus * 100),
                String.format("%.1f", speedBonus * 100),
                String.format("%.1f", jumpBonus * 100));

            // Log if there was a significant change
            if (Math.abs(attackBonus - oldAttackBonus) > 0.01) {
                ImmortalCultivation.LOGGER.info("Combat stats changed on client - Attack bonus: {}% -> {}%",
                    String.format("%.1f", oldAttackBonus * 100),
                    String.format("%.1f", attackBonus * 100));
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating combat stats from server", e);
        }
    }

    public void tick() {
        tickCounter++;

        // Sync every 20 ticks (1 second) if needed
        if (needsSync && tickCounter % 20 == 0) {
            forceSync();
        }

        // Periodic full sync every 5 seconds
        if (tickCounter % 100 == 0) {
            forceSync();
        }
    }

    public void markForSync() {
        this.needsSync = true;
    }

    /**
     * CRITICAL: Force sync method for dimension changes and initialization.
     * This ensures the client receives updated data even if internal state hasn't changed.
     */
    public void forceSync() {
        try {
            if (player instanceof ServerPlayer serverPlayer) {
                // Calculate effective bonuses including all types of bonuses
                double effectiveAttackBonus = getEffectiveAttackDamageMultiplier() - 1.0;
                double effectiveSpeedBonus = getEffectiveMovementSpeedMultiplier();
                double effectiveJumpBonus = getEffectiveJumpHeightMultiplier();

                // Log what we're sending
                ImmortalCultivation.LOGGER.debug("Syncing combat stats to client - Attack: {}%, Speed: {}%, Jump: {}%",
                    String.format("%.1f", effectiveAttackBonus * 100),
                    String.format("%.1f", effectiveSpeedBonus * 100),
                    String.format("%.1f", effectiveJumpBonus * 100));

                // Send sync packet with effective values
                PacketDistributor.sendToPlayer(serverPlayer,
                    new CombatStatsSyncPacket(
                        effectiveAttackBonus, effectiveSpeedBonus, effectiveJumpBonus));

                this.lastSyncTime = System.currentTimeMillis();
                this.needsSync = false;

                ImmortalCultivation.LOGGER.debug("Force synced combat stats for player: {}",
                    serverPlayer.getName().getString());

            } else {
                ImmortalCultivation.LOGGER.debug("Cannot sync combat stats - no player reference or not server player");
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing combat stats for player {}",
                player != null ? player.getName().getString() : "unknown", e);
        }
    }

    // === NBT SERIALIZATION ===

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        // Permanent bonuses
        tag.putDouble(TAG_PERMANENT_ATTACK_BONUS, permanentAttackDamageBonus);
        tag.putDouble(TAG_PERMANENT_SPEED_BONUS, permanentMovementSpeedBonus);
        tag.putDouble(TAG_PERMANENT_JUMP_BONUS, permanentJumpHeightBonus);
        // Temporary bonuses
        tag.putDouble(TAG_ATTACK_DAMAGE_BONUS, attackDamageBonus);
        tag.putDouble(TAG_MOVEMENT_SPEED_BONUS, movementSpeedBonus);
        tag.putDouble(TAG_JUMP_HEIGHT_BONUS, jumpHeightBonus);
        tag.putLong(TAG_LAST_SYNC_TIME, lastSyncTime);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        // Load permanent bonuses
        this.permanentAttackDamageBonus = tag.getDouble(TAG_PERMANENT_ATTACK_BONUS);
        this.permanentMovementSpeedBonus = tag.getDouble(TAG_PERMANENT_SPEED_BONUS);
        this.permanentJumpHeightBonus = tag.getDouble(TAG_PERMANENT_JUMP_BONUS);
        // Load temporary bonuses
        this.attackDamageBonus = tag.getDouble(TAG_ATTACK_DAMAGE_BONUS);
        this.movementSpeedBonus = tag.getDouble(TAG_MOVEMENT_SPEED_BONUS);
        this.jumpHeightBonus = tag.getDouble(TAG_JUMP_HEIGHT_BONUS);
        this.lastSyncTime = tag.getLong(TAG_LAST_SYNC_TIME);
        this.needsSync = true; // Always need sync after loading
    }

    // === CODEC ===

    public static final Codec<PlayerCombatStatsAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        // Permanent bonuses
        Codec.DOUBLE.optionalFieldOf("permanentAttackDamageBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getPermanentAttackDamageBonus),
        Codec.DOUBLE.optionalFieldOf("permanentMovementSpeedBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getPermanentMovementSpeedBonus),
        Codec.DOUBLE.optionalFieldOf("permanentJumpHeightBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getPermanentJumpHeightBonus),
        // Temporary bonuses
        Codec.DOUBLE.optionalFieldOf("attackDamageBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getAttackDamageBonus),
        Codec.DOUBLE.optionalFieldOf("movementSpeedBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getMovementSpeedBonus),
        Codec.DOUBLE.optionalFieldOf("jumpHeightBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getJumpHeightBonus),
        Codec.LONG.optionalFieldOf("lastSyncTime", 0L).forGetter(comp -> comp.lastSyncTime),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter)
    ).apply(instance, (permAttack, permSpeed, permJump, attackBonus, speedBonus, jumpBonus, syncTime, ticks) -> {
        PlayerCombatStatsAttachment comp = new PlayerCombatStatsAttachment();
        comp.permanentAttackDamageBonus = permAttack;
        comp.permanentMovementSpeedBonus = permSpeed;
        comp.permanentJumpHeightBonus = permJump;
        comp.attackDamageBonus = attackBonus;
        comp.movementSpeedBonus = speedBonus;
        comp.jumpHeightBonus = jumpBonus;
        comp.lastSyncTime = syncTime;
        comp.tickCounter = ticks;
        comp.needsSync = true; // Always need sync after loading
        return comp;
    }));

    // === SERIALIZER ===

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
}