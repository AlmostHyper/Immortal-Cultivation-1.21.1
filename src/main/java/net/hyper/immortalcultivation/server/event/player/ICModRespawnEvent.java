package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A generic utility class holding reusable logic for player respawn events.
 * These methods are designed to be called by specific attachment implementations
 * or a central event handler to process respawn-related actions.
 */
public class ICModRespawnEvent {

    /**
     * Applies a percentage-based penalty to a player's cultivation progress.
     * This method is generic and operates on the provided cultivation data.
     *
     * @param cultivationData The cultivation data attachment to modify.
     * @param penaltyPercent The percentage of cultivation to lose (e.g., 0.10 for 10%).
     */
    public static void applyCultivationPenalty(PlayerCultivationAttachment cultivationData, double penaltyPercent) {
        if (cultivationData == null) {
            ImmortalCultivation.LOGGER.warn("Attempted to apply penalty to null cultivation data.");
            return;
        }

        // Apply penalty to Qi cultivation
        long qiPenalty = (long) (cultivationData.getCurrentCultivation(ICultivationData.CultivationType.QI) * penaltyPercent);
        cultivationData.subtractCultivation(ICultivationData.CultivationType.QI, qiPenalty);

        // Apply penalty to Body cultivation
        long bodyPenalty = (long) (cultivationData.getCurrentCultivation(ICultivationData.CultivationType.BODY) * penaltyPercent);
        cultivationData.subtractCultivation(ICultivationData.CultivationType.BODY, bodyPenalty);

        cultivationData.markDirty();
        ImmortalCultivation.LOGGER.debug("Applied death penalties. Qi lost: {}, Body lost: {}", qiPenalty, bodyPenalty);
    }

    /**
     * Restores a player's vital resources to their maximum values.
     *
     * @param resourceData The resource data attachment to modify.
     * @param player The player whose vanilla health should also be restored.
     */
    public static void restoreResourcesToMax(PlayerResourceAttachment resourceData, ServerPlayer player) {
        if (resourceData == null || player == null) {
            ImmortalCultivation.LOGGER.warn("Attempted to restore resources with null data or player.");
            return;
        }

        // Restore custom resources to their maximum values.
        resourceData.setCurrentHealth(resourceData.getMaxHealth());
        resourceData.setCurrentQi(resourceData.getMaxQi());
        resourceData.setCurrentBody(resourceData.getMaxBody());

        // Also restore vanilla health to full.
        player.setHealth(player.getMaxHealth());

        resourceData.markDirty();
        ImmortalCultivation.LOGGER.debug("Restored resources to maximum for {}", player.getName().getString());
    }

    /**
     * Forces a full synchronization of all mod-related data to the client.
     * This is a crucial step after respawn to ensure the client UI is up-to-date.
     *
     * @param player The player whose data needs to be synchronized.
     */
    public static void synchronizeAllData(ServerPlayer player) {
        if (player == null) {
            ImmortalCultivation.LOGGER.warn("Attempted to sync data for a null player.");
            return;
        }
        NetworkSyncManager.forceSyncAll(player);
        ImmortalCultivation.LOGGER.debug("Forcing full data sync for {}", player.getName().getString());
    }

    /**
     * Sends a generic, thematic respawn message to the player.
     *
     * @param player The player to receive the message.
     */
    public static void sendThematicRespawnMessage(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Component message = Component.literal("Your soul returns from the void. Your cultivation has weakened, but your journey is not over.")
            .withStyle(ChatFormatting.GOLD);
        player.sendSystemMessage(message);
    }
}
