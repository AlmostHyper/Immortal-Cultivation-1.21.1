package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.handler.CombatStatsEventHandler;
import net.hyper.immortalcultivation.network.CombatStatsSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Attachment for tracking persistent combat stats bonuses.
 * Handles attack damage, movement speed, and jump height bonuses from cultivation.
 */
public class PlayerCombatStatsAttachment implements INBTSerializable<CompoundTag> {

    // === CONSTANTS ===
    private static final String TAG_ATTACK_DAMAGE_BONUS = "AttackDamageBonus";
    private static final String TAG_MOVEMENT_SPEED_BONUS = "MovementSpeedBonus";
    private static final String TAG_JUMP_HEIGHT_BONUS = "JumpHeightBonus";
    private static final String TAG_LAST_SYNC_TIME = "LastSyncTime";

    // === PERSISTENT BONUS VALUES ===
    private double attackDamageBonus = 0.0;
    private double movementSpeedBonus = 0.0;
    private double jumpHeightBonus = 0.0;

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

    // === ATTACK DAMAGE BONUSES ===

    /**
     * Get current attack damage bonus
     */
    public double getAttackDamageBonus() {
        return attackDamageBonus;
    }

    /**
     * Add permanent attack damage bonus
     */
    public void addAttackDamageBonus(double bonus) {
        if (bonus > 0) {
            this.attackDamageBonus += bonus;
            markForSync();

            if (player instanceof ServerPlayer serverPlayer) {
                ImmortalCultivation.LOGGER.info("Added {}% attack damage bonus to player {} (total: +{}%)",
                    String.format("%.1f", bonus * 100),
                    serverPlayer.getName().getString(),
                    String.format("%.1f", attackDamageBonus * 100));
            }
        }
    }

    /**
     * Set attack damage bonus directly
     */
    public void setAttackDamageBonus(double bonus) {
        this.attackDamageBonus = Math.max(0, bonus);
        markForSync();
    }

    /**
     * Get effective attack damage multiplier based on cultivation
     */
    public double getEffectiveAttackDamageMultiplier() {
        try {
            // Ensure we have a valid player reference
            if (player == null) {
                // Try to find the player if we don't have a reference
                // This is a fallback and shouldn't normally be needed
                ImmortalCultivation.LOGGER.debug("Combat stats attachment missing player reference, using fallback");
                return 1.0 + attackDamageBonus;
            }

            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) {
                ImmortalCultivation.LOGGER.warn("Realm attachment is null for player {}!",
                    player.getName().getString());
                return 1.0 + attackDamageBonus;
            }

            // Get base attack damage from both realms
            double qiRealmDamage = realmAttachment.getCurrentQiRealm().getBaseAttackDamage();
            double bodyRealmDamage = realmAttachment.getCurrentBodyRealm().getBaseAttackDamage();
            double baseRealmBonus = qiRealmDamage + bodyRealmDamage;

            // Combine realm bonus with persistent bonuses
            double total = 1.0 + baseRealmBonus + attackDamageBonus;

            ImmortalCultivation.LOGGER.debug("Calculated attack multiplier for {}: realm={}, personal={}, total={}",
                player.getName().getString(), baseRealmBonus, attackDamageBonus, total);

            return total;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating effective attack damage for player: {}",
                player != null ? player.getName().getString() : "NULL", e);
            return 1.0 + attackDamageBonus;
        }
    }

    // === MOVEMENT SPEED BONUSES ===

    /**
     * Get current movement speed bonus
     */
    public double getMovementSpeedBonus() {
        return movementSpeedBonus;
    }

    /**
     * Add permanent movement speed bonus
     */
    public void addMovementSpeedBonus(double bonus) {
        if (bonus > 0) {
            this.movementSpeedBonus += bonus;
            markForSync();

            if (player instanceof ServerPlayer serverPlayer) {
                ImmortalCultivation.LOGGER.info("Added {}% movement speed bonus to player {} (total: +{}%)",
                    String.format("%.1f", bonus * 100),
                    serverPlayer.getName().getString(),
                    String.format("%.1f", movementSpeedBonus * 100));
            }
        }
    }

    /**
     * Set movement speed bonus directly
     */
    public void setMovementSpeedBonus(double bonus) {
        this.movementSpeedBonus = Math.max(0, bonus);
        markForSync();
    }

    /**
     * Get effective movement speed multiplier based on cultivation
     */
    public double getEffectiveMovementSpeedMultiplier() {
        try {
            if (player == null) return movementSpeedBonus;

            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return movementSpeedBonus;

            // Get base movement speed from both realms
            double qiRealmSpeed = realmAttachment.getCurrentQiRealm().getBaseMovementSpeed();
            double bodyRealmSpeed = realmAttachment.getCurrentBodyRealm().getBaseMovementSpeed();
            double baseRealmBonus = qiRealmSpeed + bodyRealmSpeed;

            // Combine realm bonus with persistent bonuses
            return baseRealmBonus + movementSpeedBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating effective movement speed for player", e);
            return movementSpeedBonus;
        }
    }

    // === JUMP HEIGHT BONUSES ===

    /**
     * Get current jump height bonus
     */
    public double getJumpHeightBonus() {
        return jumpHeightBonus;
    }

    /**
     * Add permanent jump height bonus
     */
    public void addJumpHeightBonus(double bonus) {
        if (bonus > 0) {
            this.jumpHeightBonus += bonus;
            markForSync();

            if (player instanceof ServerPlayer serverPlayer) {
                ImmortalCultivation.LOGGER.info("Added {}% jump height bonus to player {} (total: +{}%)",
                    String.format("%.1f", bonus * 100),
                    serverPlayer.getName().getString(),
                    String.format("%.1f", jumpHeightBonus * 100));
            }
        }
    }

    /**
     * Set jump height bonus directly
     */
    public void setJumpHeightBonus(double bonus) {
        this.jumpHeightBonus = Math.max(0, bonus);
        markForSync();
    }

    /**
     * Get effective jump height multiplier based on cultivation
     */
    public double getEffectiveJumpHeightMultiplier() {
        try {
            if (player == null) return jumpHeightBonus;

            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment == null) return jumpHeightBonus;

            // Get base jump height from both realms
            double qiRealmJump = realmAttachment.getCurrentQiRealm().getBaseJumpHeight();
            double bodyRealmJump = realmAttachment.getCurrentBodyRealm().getBaseJumpHeight();
            double baseRealmBonus = qiRealmJump + bodyRealmJump;

            // Combine realm bonus with persistent bonuses
            return baseRealmBonus + jumpHeightBonus;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error calculating effective jump height for player", e);
            return jumpHeightBonus;
        }
    }

    // === UTILITY METHODS ===

    /**
     * Reset all bonuses (for debugging or special events)
     */
    public void resetAllBonuses() {
        this.attackDamageBonus = 0.0;
        this.movementSpeedBonus = 0.0;
        this.jumpHeightBonus = 0.0;
        markForSync();
    }

    /**
     * Get total bonus score for display purposes
     */
    public double getTotalBonusScore() {
        return (attackDamageBonus + movementSpeedBonus + jumpHeightBonus) * 100;
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
                // Ensure we have the latest realm data
                PlayerRealmAttachment realmAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_REALM.get());
                if (realmAttachment == null) {
                    ImmortalCultivation.LOGGER.warn("Cannot sync combat stats - no realm attachment for player: {}",
                        serverPlayer.getName().getString());
                    return;
                }

                // Calculate effective bonuses including realm bonuses
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
        tag.putDouble(TAG_ATTACK_DAMAGE_BONUS, attackDamageBonus);
        tag.putDouble(TAG_MOVEMENT_SPEED_BONUS, movementSpeedBonus);
        tag.putDouble(TAG_JUMP_HEIGHT_BONUS, jumpHeightBonus);
        tag.putLong(TAG_LAST_SYNC_TIME, lastSyncTime);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.attackDamageBonus = tag.getDouble(TAG_ATTACK_DAMAGE_BONUS);
        this.movementSpeedBonus = tag.getDouble(TAG_MOVEMENT_SPEED_BONUS);
        this.jumpHeightBonus = tag.getDouble(TAG_JUMP_HEIGHT_BONUS);
        this.lastSyncTime = tag.getLong(TAG_LAST_SYNC_TIME);
        this.needsSync = true; // Always need sync after loading
    }

    // === CODEC ===

    public static final Codec<PlayerCombatStatsAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.optionalFieldOf("attackDamageBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getAttackDamageBonus),
        Codec.DOUBLE.optionalFieldOf("movementSpeedBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getMovementSpeedBonus),
        Codec.DOUBLE.optionalFieldOf("jumpHeightBonus", 0.0).forGetter(PlayerCombatStatsAttachment::getJumpHeightBonus),
        Codec.LONG.optionalFieldOf("lastSyncTime", 0L).forGetter(comp -> comp.lastSyncTime),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter)
    ).apply(instance, (attackBonus, speedBonus, jumpBonus, syncTime, ticks) -> {
        PlayerCombatStatsAttachment comp = new PlayerCombatStatsAttachment();
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