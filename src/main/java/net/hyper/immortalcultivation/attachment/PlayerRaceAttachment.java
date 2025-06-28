package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.RaceType;
import net.hyper.immortalcultivation.network.server.RaceSyncPacket;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Manages player race data for the cultivation system.
 * Races provide permanent multipliers to cultivation paths.
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

    // === Player Reference Management ===

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Nullable
    protected Player getPlayerSafe() {
        if (player != null && player.isAlive()) {
            return player;
        }
        return null;
    }

    // === Race Methods ===

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

    // === Utility Methods ===

    /**
     * Reset race (for debug/admin commands)
     */
    public void resetRace() {
        this.raceType = RaceType.HUMAN;
        this.raceAssigned = false;
        this.needsSync = true;
        forceSync();
    }

    /**
     * Force reassignment of race (for special items/events)
     */
    public void forceReassignRace(RaceType type) {
        this.raceType = type;
        this.raceAssigned = true;
        this.needsSync = true;
        forceSync();
    }

    // === Sync Methods ===

    public void forceSync() {
        Player currentPlayer = getPlayerSafe();
        if (currentPlayer instanceof ServerPlayer serverPlayer) {
            RaceSyncPacket packet = new RaceSyncPacket(
                raceType.name(),
                raceAssigned
            );
            ICModNetwork.sendToPlayer(packet, serverPlayer);
            this.needsSync = false;
        }
    }

    public void tick() {
        if (needsSync) {
            forceSync();
        }
    }

    // === Serialization ===

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
            data.raceType = RaceType.HUMAN;
        }
        data.raceAssigned = raceAssigned;

        return data;
    }

    // === Debug Methods ===

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
    }
}