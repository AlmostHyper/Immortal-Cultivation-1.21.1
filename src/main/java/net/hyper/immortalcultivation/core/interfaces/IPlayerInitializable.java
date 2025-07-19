package net.hyper.immortalcultivation.core.interfaces;

import net.minecraft.server.level.ServerPlayer;

/**
 * An interface for attachments that need to perform logic when a player logs in or is initialized.
 * This allows for a generic, decoupled event system where attachments manage their own setup.
 */
public interface IPlayerInitializable {

    /**
     * Called when a player logs into the server. Use this for initial setup,
     * assigning default values, or sending welcome messages.
     *
     * @param player The player who has just logged in.
     */
    void onPlayerLogin(ServerPlayer player);
}
