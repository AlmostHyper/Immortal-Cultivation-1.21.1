package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.network.SpiritualRootSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class PlayerSpiritualRootAttachment {
    private SpiritualRootType rootType;
    private boolean hasBeenAssigned;

    public PlayerSpiritualRootAttachment() {
        this.rootType = SpiritualRootType.MORTAL_ROOT;
        this.hasBeenAssigned = false;
    }

    public PlayerSpiritualRootAttachment(SpiritualRootType rootType) {
        this.rootType = rootType;
        this.hasBeenAssigned = true;
    }

    // === Player Reference Management (same pattern as other attachments) ===

    private Player findPlayer() {
        // Check client-side first
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get()) == this) {
                return clientPlayer;
            }
        }

        // Check server-side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (serverPlayer.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get()) == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("Could not find player for SpiritualRootAttachment");
        }
        return player;
    }

    // Getters
    public SpiritualRootType getRootType() { return rootType; }
    public boolean hasBeenAssigned() { return hasBeenAssigned; }

    // Modifier methods
    public float getCultivationMultiplier() {
        return rootType.getCultivationMultiplier();
    }

    public boolean canCultivate() {
        return true; // All players can cultivate with any spiritual root
    }

    // Assignment
    public void assignRootType(SpiritualRootType type) {
        this.rootType = type;
        this.hasBeenAssigned = true;

        // Sync to client after assignment
        Player player = getPlayerSafe();
        if (player != null && !player.level().isClientSide()) {
            forceSync();
        }
    }

    // === NEW: Synchronization Methods ===

    /**
     * Force sync spiritual root data to client
     */
    public void forceSync() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            try {
                // You'd need to create this packet class
                // It would send the rootType ordinal and hasBeenAssigned flag
                ICModNetwork.sendToPlayer(new SpiritualRootSyncPacket(
                    rootType.ordinal(),
                    hasBeenAssigned
                ), serverPlayer);

                ImmortalCultivation.LOGGER.debug("Synced spiritual root data to client for player: {}",
                    serverPlayer.getName().getString());
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing spiritual root data to client", e);
            }
        }
    }

    /**
     * Update from server sync packet
     */
    public void updateFromServer(int rootTypeOrdinal, boolean assigned) {
        try {
            this.rootType = SpiritualRootType.values()[rootTypeOrdinal];
            this.hasBeenAssigned = assigned;

            ImmortalCultivation.LOGGER.debug("Client updated spiritual root: {} (assigned: {})",
                rootType.getDisplayName(), hasBeenAssigned);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating spiritual root from server", e);
            this.rootType = SpiritualRootType.MORTAL_ROOT;
            this.hasBeenAssigned = false;
        }
    }

    // Serialization
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("rootType", rootType.name());
        tag.putBoolean("hasBeenAssigned", hasBeenAssigned);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("rootType")) {
            try {
                this.rootType = SpiritualRootType.valueOf(tag.getString("rootType"));
            } catch (IllegalArgumentException e) {
                this.rootType = SpiritualRootType.MORTAL_ROOT;
            }
        }
        this.hasBeenAssigned = tag.getBoolean("hasBeenAssigned");
    }

    public static PlayerSpiritualRootAttachment fromTag(CompoundTag tag) {
        PlayerSpiritualRootAttachment attachment = new PlayerSpiritualRootAttachment();
        attachment.load(tag);
        return attachment;
    }

    public PlayerSpiritualRootAttachment copy() {
        PlayerSpiritualRootAttachment copy = new PlayerSpiritualRootAttachment();
        copy.rootType = this.rootType;
        copy.hasBeenAssigned = this.hasBeenAssigned;
        return copy;
    }


    // === Codec === //

    public static final Codec<PlayerSpiritualRootAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("rootType", "MORTAL_ROOT").forGetter(comp -> comp.rootType.name()),
        Codec.BOOL.optionalFieldOf("hasBeenAssigned", false).forGetter(PlayerSpiritualRootAttachment::hasBeenAssigned)
    ).apply(instance, (rootTypeString, assigned) -> {
        PlayerSpiritualRootAttachment comp = new PlayerSpiritualRootAttachment();
        try {
            comp.rootType = SpiritualRootType.valueOf(rootTypeString);
        } catch (IllegalArgumentException e) {
            comp.rootType = SpiritualRootType.MORTAL_ROOT;
        }
        comp.hasBeenAssigned = assigned;
        return comp;
    }));
}