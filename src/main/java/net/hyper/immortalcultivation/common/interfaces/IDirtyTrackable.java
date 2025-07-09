package net.hyper.immortalcultivation.common.interfaces;

/**
 * Universal dirty flag interface for all attachments.
 *
 * This provides a consistent way to track when attachment data has changed
 * and needs to be synchronized to the client.
 *
 * Usage:
 * 1. Implement this interface in your attachment class
 * 2. Call markDirty() whenever data changes
 * 3. Use isDirty() to check if sync is needed
 * 4. Call clearDirty() after successful sync
 */
public interface IDirtyTrackable {

    /**
     * Check if this attachment should sync based on its dirty flag and internal criteria
     * @return true if sync should happen now
     */
    public boolean shouldSyncNow();

    /**
     * Force synchronization of this attachment to the client.
     * This should handle creating the appropriate packet, sending it,
     * and clearing the dirty flag upon success.
     */
    void forceSync();

    /**
     * Mark this attachment as dirty (needs sync)
     */
    void markDirty();

    /**
     * Check if this attachment is dirty (needs sync)
     * @return true if sync is needed
     */
    boolean isDirty();

    /**
     * Clear the dirty flag (call after successful sync)
     */
    void clearDirty();

    /**
     * Mark as dirty and increment change counter for advanced tracking
     */
    default void markDirtyWithCounter() {
        markDirty();
        incrementChangeCounter();
    }

    /**
     * Get the number of changes since last major sync
     * @return change count
     */
    default long getChangeCounter() {
        return 0;
    }

    /**
     * Increment change counter (for batching optimizations)
     */
    default void incrementChangeCounter() {
    }

    /**
     * Reset change counter (after major sync)
     */
    default void resetChangeCounter() {
    }
}