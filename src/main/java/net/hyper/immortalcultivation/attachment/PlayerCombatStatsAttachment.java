package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.handler.CombatStatsEventHandler;
import net.hyper.immortalcultivation.network.server.CombatStatsSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Enhanced PlayerCombatStatsAttachment with permanent additive bonus system.
 * Tracks both permanent bonuses (from realm breakthroughs) and temporary bonuses.
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

    // === PERMANENT BONUSES (from realm breakthroughs) ===
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
    private Player player;
    private long lastSyncTime = 0;
    private boolean needsSync = false;
    private int tickCounter = 0;

    // === INITIALIZATION ===

    public PlayerCombatStatsAttachment() {}

    public void setPlayer(Player player) {
        this.player = player;
    }

    // === NEW: PERMANENT BONUS SYSTEM ===

    /**
     * Adds a permanent attack damage bonus (from realm breakthrough).
     * This bonus persists even if the player's realm changes.
     */
    public void addPermanentAttackDamageBonus(double attackBonus) {
        if (attackBonus <= 0) return;

        this.permanentAttackDamageBonus += attackBonus;
        markForSync();

        ImmortalCultivation.LOGGER.info("Player gained +{} permanent attack damage (total bonus: +{})",
            attackBonus, this.permanentAttackDamageBonus);

        if (player instanceof ServerPlayer serverPlayer) {
            ImmortalCultivation.LOGGER.info("Added {}% permanent attack damage bonus to player {} (total permanent: +{}%)",
                String.format("%.1f", attackBonus * 100),
                serverPlayer.getName().getString(),
                String.format("%.1f", permanentAttackDamageBonus * 100));
        }
    }

    /**
     * Adds a permanent movement speed bonus (from realm breakthrough).
     */
    public void addPermanentMovementSpeedBonus(double speedBonus) {
        if (speedBonus <= 0) return;

        this.permanentMovementSpeedBonus += speedBonus;
        markForSync();

        ImmortalCultivation.LOGGER.info("Player gained +{} permanent movement speed (total bonus: +{})",
            speedBonus, this.permanentMovementSpeedBonus);

        if (player instanceof ServerPlayer serverPlayer) {
            ImmortalCultivation.LOGGER.info("Added {}% permanent movement speed bonus to player {} (total permanent: +{}%)",
                String.format("%.1f", speedBonus * 100),
                serverPlayer.getName().getString(),
                String.format("%.1f", permanentMovementSpeedBonus * 100));
        }
    }

    /**
     * Adds a permanent jump height bonus (from realm breakthrough).
     */
    public void addPermanentJumpHeightBonus(double jumpBonus) {
        if (jumpBonus <= 0) return;

        this.permanentJumpHeightBonus += jumpBonus;
        markForSync();

        ImmortalCultivation.LOGGER.info("Player gained +{} permanent jump height (total bonus: +{})",
            jumpBonus, this.permanentJumpHeightBonus);

        if (player instanceof ServerPlayer serverPlayer) {
            ImmortalCultivation.LOGGER.info("Added {}% permanent jump height bonus to player {} (total permanent: +{}%)",
                String.format("%.1f", jumpBonus * 100),
                serverPlayer.getName().getString(),
                String.format("%.1f", permanentJumpHeightBonus * 100));
        }
    }

    /**
     * Gets the total permanent attack damage bonus accumulated from breakthroughs
     */
    public double getPermanentAttackDamageBonus() {
        return permanentAttackDamageBonus;
    }

    /**
     * Gets the total permanent movement speed bonus accumulated from breakthroughs
     */
    public double getPermanentMovementSpeedBonus() {
        return permanentMovementSpeedBonus;
    }

    /**
     * Gets the total permanent jump height bonus accumulated from breakthroughs
     */
    public double getPermanentJumpHeightBonus() {
        return permanentJumpHeightBonus;
    }

    // === ATTACK DAMAGE BONUSES ===

    /**
     * Get current temporary attack damage bonus
     */
    public double getAttackDamageBonus() {
        return attackDamageBonus;
    }

    /**
     * Add temporary attack damage bonus
     */
    public void addAttackDamageBonus(double bonus) {
        if (bonus > 0) {
            this.attackDamageBonus += bonus;
            markForSync();

            if (player instanceof ServerPlayer serverPlayer) {
                ImmortalCultivation.LOGGER.info("Added {}% temporary attack damage bonus to player {} (total temporary: +{}%)",
                    String.format("%.1f", bonus * 100),
                    serverPlayer.getName().getString(),
                    String.format("%.1f", attackDamageBonus * 100));
            }
        }
    }

    /**
     * Set temporary attack damage bonus directly
     */
    public void setAttackDamageBonus(double bonus) {
        this.attackDamageBonus = Math.max(0, bonus);
        markForSync();
    }

    /**
     * Get effective attack damage multiplier including all bonuses
     */
    public double getEffectiveAttackDamageMultiplier() {
        try {
            // Only use permanent bonuses (which already include all breakthrough bonuses) + temporary bonuses
            // Do NOT add current realm bonuses as that would be double-counting
            double total = 1.0 + permanentAttackDamageBonus + attackDamageBonus;

            if (player != null) {
                ImmortalCultivation.LOGGER.debug("Calculated attack multiplier for {}: permanent={}, temporary={}, total={}",
                    player.getName().getString(), permanentAttackDamageBonus, attackDamageBonus, total);
            }

            return total;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating effective attack damage for player: {}",
                player != null ? player.getName().getString() : "null", e);
            return 1.0;
        }
    }



    // === MOVEMENT SPEED BONUSES ===

    /**
     * Get current temporary movement speed bonus
     */
    public double getMovementSpeedBonus() {
        return movementSpeedBonus;
    }

    /**
     * Add temporary movement speed bonus
     */
    public void addMovementSpeedBonus(double bonus) {
        if (bonus > 0) {
            this.movementSpeedBonus += bonus;
            markForSync();

            if (player instanceof ServerPlayer serverPlayer) {
                ImmortalCultivation.LOGGER.info("Added {}% temporary movement speed bonus to player {} (total temporary: +{}%)",
                    String.format("%.1f", bonus * 100),
                    serverPlayer.getName().getString(),
                    String.format("%.1f", movementSpeedBonus * 100));
            }
        }
    }

    /**
     * Set temporary movement speed bonus directly
     */
    public void setMovementSpeedBonus(double bonus) {
        this.movementSpeedBonus = Math.max(0, bonus);
        markForSync();
    }

    /**
     * Get effective movement speed multiplier including all bonuses
     */
    public double getEffectiveMovementSpeedMultiplier() {
        try {
            if (player == null) {
                return permanentMovementSpeedBonus + movementSpeedBonus;
            }

            PlayerCultivationAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            double realmSpeedBonus = 0.0;

            if (realmAttachment != null) {
                // Combine both realms as requested
                double qiRealmSpeed = realmAttachment.getCurrentQiRealm().getBaseMovementSpeed();
                double bodyRealmSpeed = realmAttachment.getCurrentBodyRealm().getBaseMovementSpeed();
                realmSpeedBonus = qiRealmSpeed + bodyRealmSpeed;
            }

            return permanentMovementSpeedBonus + movementSpeedBonus + realmSpeedBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating effective movement speed for player", e);
            return permanentMovementSpeedBonus + movementSpeedBonus;
        }
    }

    // === JUMP HEIGHT BONUSES ===

    /**
     * Get current temporary jump height bonus
     */
    public double getJumpHeightBonus() {
        return jumpHeightBonus;
    }

    /**
     * Add temporary jump height bonus
     */
    public void addJumpHeightBonus(double bonus) {
        if (bonus > 0) {
            this.jumpHeightBonus += bonus;
            markForSync();

            if (player instanceof ServerPlayer serverPlayer) {
                ImmortalCultivation.LOGGER.info("Added {}% temporary jump height bonus to player {} (total temporary: +{}%)",
                    String.format("%.1f", bonus * 100),
                    serverPlayer.getName().getString(),
                    String.format("%.1f", jumpHeightBonus * 100));
            }
        }
    }

    /**
     * Set temporary jump height bonus directly
     */
    public void setJumpHeightBonus(double bonus) {
        this.jumpHeightBonus = Math.max(0, bonus);
        markForSync();
    }

    /**
     * Get effective jump height including all bonuses
     */
    public double getEffectiveJumpHeightMultiplier() {
        try {
            if (player == null) {
                return permanentJumpHeightBonus + jumpHeightBonus;
            }

            PlayerCultivationAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            double realmJumpBonus = 0.0;

            if (realmAttachment != null) {
                double qiRealmJump = realmAttachment.getCurrentQiRealm().getBaseJumpHeight();
                double bodyRealmJump = realmAttachment.getCurrentBodyRealm().getBaseJumpHeight();
                realmJumpBonus = qiRealmJump + bodyRealmJump;
            }

            return permanentJumpHeightBonus + jumpHeightBonus + realmJumpBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating effective jump height for player", e);
            return permanentJumpHeightBonus + jumpHeightBonus;
        }
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

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Reapply modifiers after dimension change
            CombatStatsEventHandler.refreshCombatStats(player);
        }
    }

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

    public void forceSync() {
        if (player instanceof ServerPlayer serverPlayer) {
            try {
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

                ImmortalCultivation.LOGGER.debug("Combat stats sync packet sent to player: {}",
                    serverPlayer.getName().getString());

            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing combat stats for player {}",
                    player.getName().getString(), e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot sync combat stats - no player reference");
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
            PlayerCombatStatsAttachment attachment = new PlayerCombatStatsAttachment();
            attachment.deserializeNBT(provider, tag);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }

            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerCombatStatsAttachment attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
        }
    }
}