package net.hyper.immortalcultivation.core.attachments;

/**
 * Defines the contract for any object that can synchronize its data
 * from the server to the client in a self-contained manner.
 */
public interface ISyncable {

    /**
     * Checks if the object has changes that need to be sent to the client.
     *
     * @return True if the object is "dirty" and needs to be synced.
     */
    boolean needSync();

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
