package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.PhysiqueType;
import net.hyper.immortalcultivation.network.PhysiqueSyncPacket; // You'd need to create this
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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

    // === Player Reference Management (same pattern as other attachments) ===

    private Player findPlayer() {
        // Check client-side first
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get()) == this) {
                return clientPlayer;
            }
        }

        // Check server-side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (serverPlayer.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get()) == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("Could not find player for PhysiqueAttachment");
        }
        return player;
    }

    // Getters
    public PhysiqueType getRootType() { return physiqueType; }
    public boolean hasBeenAssigned() { return hasBeenAssigned; }

    // Modifier methods
    public float getCultivationMultiplier() {
        return physiqueType.getCultivationMultiplier();
    }

    public boolean canCultivate() {
        return true; // All players can cultivate with any spiritual physique
    }

    // Assignment
    public void assignRootType(PhysiqueType type) {
        this.physiqueType = type;
        this.hasBeenAssigned = true;

        // Sync to client after assignment
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide()) {
            forceSync();
        }
    }

    // === NEW: Synchronization Methods ===

    /**
     * Force sync spiritual physique data to client
     */
    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                // You'd need to create this packet class
                // It would send the physiqueType ordinal and hasBeenAssigned flag
                ICModNetwork.sendToPlayer(new PhysiqueSyncPacket(
                    physiqueType.ordinal(),
                    hasBeenAssigned
                ), serverPlayer);

                ImmortalCultivation.LOGGER.debug("Synced spiritual physique data to client for player: {}",
                    serverPlayer.getName().getString());
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing spiritual physique data to client", e);
            }
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

    // Serialization
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

    // === Codec === //

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
}