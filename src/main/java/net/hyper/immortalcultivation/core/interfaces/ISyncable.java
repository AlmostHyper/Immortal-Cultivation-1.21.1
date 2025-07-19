package net.hyper.immortalcultivation.core.interfaces;

import java.util.Optional;

/**
 * Defines the contract for any object that can synchronize its data
 * from the server to the client in a self-contained manner.
 */
public interface ISyncable {

    /**
     * Creates a custom network packet payload containing the data that needs to be synced.
     * The object itself knows what data to write.
     *
     * @param isFullSync If true, a complete snapshot of all data should be serialized.
     *                   If false, only changed ("delta") data should be serialized.
     * @return An Optional containing the packet payload if there is data to send, otherwise Optional.empty().
     */
    Optional<ISyncPacket> createPacket(boolean isFullSync);

    /**
     * Checks if the object has changes that need to be sent to the client.
     *
     * @return True if the object is "dirty" and needs to be synced.
     */
    boolean isDirty();

    /**
     * Marks the object as "dirty", indicating it needs a sync on the next tick.
     * This should be called whenever the object's state changes.
     */
    void markDirty();

    /**
     * Resets the dirty state of the object after a sync has been performed.
     */
    void clearDirty();
}
