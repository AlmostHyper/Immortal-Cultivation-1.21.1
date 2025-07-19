package net.hyper.immortalcultivation.core.interfaces;

import net.minecraft.world.entity.player.Player;

public interface IHeavenlyTribulation {
    /**
     * Start the tribulation for this player
     * @param player The player facing tribulation
     * @return Result of the tribulation
     */


    /**
     * Get the difficulty rating of this tribulation
     * @return Difficulty from 1-10
     */
    int getDifficulty();

    /**
     * Get description for UI
     * @return Human-readable description
     */
    String getDescription();

    /**
     * Can this tribulation be attempted in current conditions?
     * @param player The player
     * @return true if conditions are met
     */
    boolean canAttempt(Player player);
}
