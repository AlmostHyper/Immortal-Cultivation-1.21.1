package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.network.packets.server.RaceSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Manages player race data for the cultivation system.
 * Races provide permanent multipliers to cultivation paths.
 * Enhanced with proper dimension change handling.
 */
public class PlayerRaceAttachment {

    // === Player Reference ===
    @Nullable
    private Player player;

    // === Race Data ===
    private RaceType raceType = RaceType.HUMAN;
    private boolean raceAssigned = false;

    // === Sync Management ===
    private boolean needsSync = false;

    // === CONSTRUCTORS ===
    public PlayerRaceAttachment() {}

    // === PLAYER REFERENCE MANAGEMENT ===

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

    // === RACE METHODS ===

    public RaceType getRaceType() {
        return raceType;
    }

    public void assignRace(RaceType type) {
        if (!raceAssigned) {
            this.raceType = type;
            this.raceAssigned = true;
            this.needsSync = true;

            ImmortalCultivation.LOGGER.info("Player {} assigned race: {} (Qi: {}x, Body: {}x)",
                player != null ? player.getName().getString() : "Unknown",
                type.getDisplayName(),
                type.getQiMultiplier(),
                type.getBodyMultiplier());

            forceSync();
        } else {
            ImmortalCultivation.LOGGER.warn("Attempted to assign race to player {} who already has one assigned: {}",
                player != null ? player.getName().getString() : "Unknown",
                raceType.getDisplayName());
        }
    }

    public boolean hasRaceAssigned() {
        return raceAssigned;
    }

    public float getQiMultiplier() {
        return raceType.getQiMultiplier();
    }

    public float getBodyMultiplier() {
        return raceType.getBodyMultiplier();
    }

    public String getRaceDisplayName() {
        return raceType.getDisplayName();
    }

    public int getRaceColor() {
        return raceType.getColor();
    }

    public String getRaceDescription() {
        return raceType.getDescription();
    }

    public String getRaceBonusDescription() {
        return raceType.getBonusDescription();
    }

    // === UTILITY METHODS ===

    /**
     * Reset race (for debug/admin commands)
     */
    public void resetRace() {
        ImmortalCultivation.LOGGER.warn("Resetting race for player: {}",
            player != null ? player.getName().getString() : "Unknown");

        this.raceType = RaceType.HUMAN;
        this.raceAssigned = false;
        this.needsSync = true;
        forceSync();
    }

    /**
     * Force reassignment of race (for special items/events)
     */
    public void forceReassignRace(RaceType type) {
        ImmortalCultivation.LOGGER.info("Force reassigning race for player {} from {} to {}",
            player != null ? player.getName().getString() : "Unknown",
            this.raceType.getDisplayName(),
            type.getDisplayName());

        this.raceType = type;
        this.raceAssigned = true;
        this.needsSync = true;
        forceSync();
    }

    // === SYNC MANAGEMENT ===

    public void tick() {
        if (needsSync && player instanceof ServerPlayer) {
            syncToClient();
            needsSync = false;
        }
    }

    private void syncToClient() {
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                RaceSyncPacket packet = new RaceSyncPacket(
                    raceType.name(),
                    raceAssigned
                );
                PacketDistributor.sendToPlayer(serverPlayer, packet);

                ImmortalCultivation.LOGGER.debug("Synced race data to client for player: {}",
                    serverPlayer.getName().getString());
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing race data for player {}",
                    player.getName().getString(), e);
            }
        }
    }

    /**
     * CRITICAL: Force sync method for dimension changes and initialization.
     * This ensures the client receives updated data even if internal state hasn't changed.
     */
    public void forceSync() {
        try {
            if (player instanceof ServerPlayer serverPlayer) {
                RaceSyncPacket packet = new RaceSyncPacket(
                    raceType.name(),
                    raceAssigned
                );
                PacketDistributor.sendToPlayer(serverPlayer, packet);

                ImmortalCultivation.LOGGER.debug("Force synced {} for player: {}",
                    this.getClass().getSimpleName(),
                    serverPlayer.getName().getString());

                this.needsSync = false;
            } else {
                ImmortalCultivation.LOGGER.debug("Cannot sync race data - no player reference or not server player");
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing {} for player {}",
                this.getClass().getSimpleName(),
                player != null ? player.getName().getString() : "unknown", e);
        }
    }

    // === SERIALIZATION ===

    public static final Codec<PlayerRaceAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("race_type").forGetter(d -> d.raceType.name()),
            Codec.BOOL.fieldOf("race_assigned").forGetter(d -> d.raceAssigned)
        ).apply(instance, PlayerRaceAttachment::decode)
    );

    private static PlayerRaceAttachment decode(String raceType, boolean raceAssigned) {
        PlayerRaceAttachment data = new PlayerRaceAttachment();

        try {
            data.raceType = RaceType.valueOf(raceType);
        } catch (IllegalArgumentException e) {
            ImmortalCultivation.LOGGER.warn("Invalid race type '{}', defaulting to HUMAN", raceType);
            data.raceType = RaceType.HUMAN;
        }
        data.raceAssigned = raceAssigned;

        return data;
    }

    // === SERIALIZER ===

    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerRaceAttachment> {
        @Override
        public PlayerRaceAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerRaceAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerRaceAttachment: {}", error))
                .map(pair -> pair.getFirst())
                .orElse(new PlayerRaceAttachment());

            // CRITICAL: Set player reference during deserialization
            if (holder instanceof Player player) {
                attachment.setPlayer(player);
                ImmortalCultivation.LOGGER.debug("Set player reference during race attachment deserialization: {}",
                    player.getName().getString());
            }

            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerRaceAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encode(attachment, provider.createSerializationContext(NbtOps.INSTANCE), new CompoundTag())
                .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerRaceAttachment: {}", error))
                .orElse(new CompoundTag());
        }
    }

    // === DEBUG METHODS ===

    public void debugInfo() {
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== Race Data Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Race: {} - Assigned: {}",
            raceType.getDisplayName(),
            raceAssigned);
        ImmortalCultivation.LOGGER.info("Qi Multiplier: {}x", raceType.getQiMultiplier());
        ImmortalCultivation.LOGGER.info("Body Multiplier: {}x", raceType.getBodyMultiplier());
        ImmortalCultivation.LOGGER.info("Description: {}", raceType.getDescription());
        ImmortalCultivation.LOGGER.info("=============================");
    }

    /**
     * Get a concise summary for command output
     */
    public String getSummary() {
        return String.format("Race: %s (Qi: %.1fx, Body: %.1fx) - %s",
            raceType.getDisplayName(),
            raceType.getQiMultiplier(),
            raceType.getBodyMultiplier(),
            raceType.getDescription());
    }
}