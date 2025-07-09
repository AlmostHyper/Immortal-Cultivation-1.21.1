package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerRaceAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.hyper.immortalcultivation.core.network.packets.server.OpenRaceSelectionSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * A generic utility class holding reusable logic for player login events.
 * These methods are designed to be called by a central event handler to
 * process login-related actions like first-time setup and data sync.
 */
public class ICModLoginEvent {

    /**
     * Handles the initial setup for a player's race. If the player is new,
     * it sends the race selection screen.
     *
     * @param player The player logging in.
     * @param raceData The player's race data attachment.
     */
    public static void initializePlayerRace(ServerPlayer player, PlayerRaceAttachment raceData) {
        if (player == null || raceData == null) {
            ImmortalCultivation.LOGGER.warn("Attempted to initialize race with null player or data.");
            return;
        }

        if (!raceData.hasRaceAssigned()) {
            // New player: send the race selection packet.
            ICModPacketRegistry.sendToPlayer(new OpenRaceSelectionSyncPacket(), player);

            Component welcomeMessage = Component.literal("Welcome, young cultivator! Choose your race to begin your journey.")
                .withStyle(ChatFormatting.GOLD);
            player.sendSystemMessage(welcomeMessage);
            ImmortalCultivation.LOGGER.info("Sent race selection screen to new player {}", player.getName().getString());
        } else {
            // Returning player: log their race.
            ImmortalCultivation.LOGGER.debug("Player {} logged in with race: {}", player.getName().getString(), raceData.getRaceType().getDisplayName());
        }
    }

    /**
     * Handles the initial setup for a player's talents (Spiritual Root and Physique).
     * If the player is new, it assigns them random talents.
     *
     * @param player The player logging in.
     * @param talentData The player's talent data attachment.
     */
    public static void initializePlayerTalents(ServerPlayer player, PlayerTalentAttachment talentData) {
        if (player == null || talentData == null) {
            ImmortalCultivation.LOGGER.warn("Attempted to initialize talents with null player or data.");
            return;
        }

        if (!talentData.hasSpiritualRootAssigned() || !talentData.hasPhysiqueAssigned()) {
            RandomSource random = player.getRandom();

            // Assign Spiritual Root if missing
            if (!talentData.hasSpiritualRootAssigned()) {
                SpiritualRootType newRoot = SpiritualRootType.generateRandom(random);
                talentData.assignSpiritualRoot(newRoot);
                player.sendSystemMessage(Component.literal("You have awakened the: " + newRoot.getDisplayName()).withStyle(ChatFormatting.AQUA));
                ImmortalCultivation.LOGGER.info("Assigned new spiritual root {} to player {}", newRoot.getDisplayName(), player.getName().getString());
            }

            // Assign Physique if missing
            if (!talentData.hasPhysiqueAssigned()) {
                PhysiqueType newPhysique = PhysiqueType.generateRandom(random);
                talentData.assignPhysique(newPhysique);
                player.sendSystemMessage(Component.literal("You possess the: " + newPhysique.getDisplayName()).withStyle(ChatFormatting.RED));
                ImmortalCultivation.LOGGER.info("Assigned new physique {} to player {}", newPhysique.getDisplayName(), player.getName().getString());
            }
            talentData.markDirty();
        } else {
            // Returning player: log their talents.
            ImmortalCultivation.LOGGER.debug("Player {} logged in with Spiritual Root: {} and Physique: {}", player.getName().getString(), talentData.getSpiritualRootType().getDisplayName(), talentData.getPhysiqueType().getDisplayName());
        }
    }

    /**
     * Schedules a full synchronization of all mod-related data to the client.
     * This is essential to ensure the UI and client-side logic are correct after login.
     *
     * @param player The player whose data needs to be synchronized.
     */
    public static void scheduleInitialSync(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("Attempted to schedule sync for a null player.");
            return;
        }
        // Use the NetworkSyncManager to handle the sync request.
        NetworkSyncManager.scheduleSync(player, NetworkSyncManager.SyncPriority.HIGH);
        ImmortalCultivation.LOGGER.debug("Scheduled a high-priority initial data sync for {}", player.getName().getString());
    }
}
