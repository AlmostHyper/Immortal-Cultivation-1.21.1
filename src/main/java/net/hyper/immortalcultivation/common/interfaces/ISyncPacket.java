package net.hyper.immortalcultivation.common.interfaces;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public interface ISyncPacket extends CustomPacketPayload {

    /**
     * Executes the logic of this packet on the client-side.
     * This is where the client-side data should be updated based on the packet's contents.
     *
     * @param player The client-side player entity.
     */
    void handleOnClient(Player player);
}
