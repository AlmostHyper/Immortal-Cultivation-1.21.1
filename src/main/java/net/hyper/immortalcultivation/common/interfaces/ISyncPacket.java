package net.hyper.immortalcultivation.common.interfaces;

import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.Function;

/**
 * Defines a generic contract for all packets designed to sync an ISyncable object.
 *
 * @param <T> The type of the ISyncable object this packet is for.
 */
public interface ISyncPacket<T extends ISyncable> extends CustomPacketPayload {

    /**
     * The main handler for this packet on the client side.
     * It retrieves the correct ISyncable object and delegates the actual
     * data handling to the object itself.
     *
     * @param context The context of the packet reception.
     * @param syncableProvider A function to get the ISyncable object (e.g., an attachment) from the player.
     */
    default void handleClient(IPayloadContext context, Function<Player, T> syncableProvider) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                T syncable = syncableProvider.apply(player);
                if (syncable != null) {
                    // Delegate the actual data handling to the ISyncable object
                    syncable.handleSyncPacket(this);
                }
            }
        });
    }
}
