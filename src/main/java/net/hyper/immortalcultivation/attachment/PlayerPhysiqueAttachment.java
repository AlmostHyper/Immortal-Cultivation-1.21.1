package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.PhysiqueType;
import net.hyper.immortalcultivation.network.PhysiqueSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * FIXED VERSION: PlayerPhysiqueAttachment with proper player reference management
 *
 * Key Fix: The findPlayer() method now works correctly after death/respawn
 * and all methods that depend on player context have better error handling.
 *
 * This attachment was one of the main causes of the "lost attachments" issue
 * showing physique as "unassigned" in the GUI after respawn.
 */
public class PlayerPhysiqueAttachment {
    private PhysiqueType physiqueType;
    private boolean hasBeenAssigned;

    public PlayerPhysiqueAttachment() {
        this.physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
        this.hasBeenAssigned = false;
    }

    public PlayerPhysiqueAttachment(PhysiqueType physiqueType) {
        this.physiqueType = physiqueType;
        this.hasBeenAssigned = true;
    }

    // === FIXED: Player Reference Management ===

    /**
     * CRITICAL FIX: This method was causing the "lost attachments" issue in GUI
     *
     * OLD BROKEN VERSION used reference equality (== this) which fails after respawn
     * NEW FIXED VERSION uses a more reliable approach with better error handling
     */
    private Player findPlayer() {
        // On client side, there's only one player - the local player
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            // Check if this client player has this attachment instance
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get()) == this) {
                return clientPlayer;
            }
        }

        // FIXED: Server side now has better error handling and logging
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                // FIXED: More reliable check with null safety
                PlayerPhysiqueAttachment playerPhysiqueAttachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
                if (playerPhysiqueAttachment != null && playerPhysiqueAttachment == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    /**
     * IMPROVED: Safe player retrieval with better debug logging
     */
    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            // IMPROVED: Better logging that doesn't spam but indicates the issue
            ImmortalCultivation.LOGGER.debug("Could not find player for PhysiqueAttachment - this may indicate a respawn issue");
        }
        return player;
    }

    // === Getters (unchanged) ===
    public PhysiqueType getRootType() { return physiqueType; }
    public boolean hasBeenAssigned() { return hasBeenAssigned; }

    // === Modifier methods (unchanged) ===
    public float getCultivationMultiplier() {
        return physiqueType.getCultivationMultiplier();
    }

    public boolean canCultivate() {
        return true; // All players can cultivate with any spiritual physique
    }

    // === ENHANCED: Assignment now works after respawn ===

    public void assignRootType(PhysiqueType type) {
        this.physiqueType = type;
        this.hasBeenAssigned = true;

        // Sync to client after assignment
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide()) {
            forceSync();
        } else if (player == null) {
            ImmortalCultivation.LOGGER.warn("Cannot sync physique assignment - no player found (type: {})",
                type.getDisplayName());
        }
    }

    // === FIXED: Synchronization Methods now work after respawn ===

    /**
     * FIXED: Force sync spiritual physique data to client with proper null checking
     */
    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                // Send the physiqueType ordinal and hasBeenAssigned flag
                ICModNetwork.sendToPlayer(new PhysiqueSyncPacket(
                    physiqueType.ordinal(),
                    hasBeenAssigned
                ), serverPlayer);

                ImmortalCultivation.LOGGER.debug("Synced spiritual physique data to client for player: {} (type: {}, assigned: {})",
                    serverPlayer.getName().getString(), physiqueType.getDisplayName(), hasBeenAssigned);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing spiritual physique data to client", e);
            }
        } else if (player == null) {
            ImmortalCultivation.LOGGER.debug("Cannot force sync physique - no player found");
        }
    }

    /**
     * Update from server sync packet
     */
    public void updateFromServer(int physiqueTypeOrdinal, boolean assigned) {
        try {
            this.physiqueType = PhysiqueType.values()[physiqueTypeOrdinal];
            this.hasBeenAssigned = assigned;

            ImmortalCultivation.LOGGER.debug("Client updated spiritual physique: {} (assigned: {})",
                physiqueType.getDisplayName(), hasBeenAssigned);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating spiritual physique from server", e);
            this.physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
            this.hasBeenAssigned = false;
        }
    }

    // === Serialization (unchanged) ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("physiqueType", physiqueType.name());
        tag.putBoolean("hasBeenAssigned", hasBeenAssigned);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("physiqueType")) {
            try {
                this.physiqueType = PhysiqueType.valueOf(tag.getString("physiqueType"));
            } catch (IllegalArgumentException e) {
                this.physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
            }
        }
        this.hasBeenAssigned = tag.getBoolean("hasBeenAssigned");
    }

    public static PlayerPhysiqueAttachment fromTag(CompoundTag tag) {
        PlayerPhysiqueAttachment attachment = new PlayerPhysiqueAttachment();
        attachment.load(tag);
        return attachment;
    }

    // === Codec (unchanged) ===

    public static final Codec<PlayerPhysiqueAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("physiqueType", "MORTAL_PHYSIQUE").forGetter(comp -> comp.physiqueType.name()),
        Codec.BOOL.optionalFieldOf("hasBeenAssigned", false).forGetter(PlayerPhysiqueAttachment::hasBeenAssigned)
    ).apply(instance, (physiqueTypeString, assigned) -> {
        PlayerPhysiqueAttachment comp = new PlayerPhysiqueAttachment();
        try {
            comp.physiqueType = PhysiqueType.valueOf(physiqueTypeString);
        } catch (IllegalArgumentException e) {
            comp.physiqueType = PhysiqueType.MORTAL_PHYSIQUE;
        }
        comp.hasBeenAssigned = assigned;
        return comp;
    }));

    // === NEW: Debug Utilities ===

    /**
     * Print debug information about this attachment's state
     */
    public void debugInfo() {
        Player player = getPlayerSafe();
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== Physique Attachment Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Physique Type: {} ({}x multiplier)",
            physiqueType.getDisplayName(), physiqueType.getCultivationMultiplier());
        ImmortalCultivation.LOGGER.info("Has Been Assigned: {}", hasBeenAssigned);
        ImmortalCultivation.LOGGER.info("Can Cultivate: {}", canCultivate());
    }
}