package net.hyper.immortalcultivation.common.util;

import net.hyper.immortalcultivation.common.interfaces.IDirtyTrackable;

/**
 * Concrete dirty flag tracker that can be composed into attachment classes.
 *
 * This provides thread-safe dirty flag tracking with optional change counting
 * for advanced sync optimization.
 */
public abstract class DirtyTrackerUtil implements IDirtyTrackable {

    private volatile boolean dirty = false;
    private volatile long changeCounter = 0;
    private volatile long lastSyncTime = 0;

    /**
     * Check if this attachment should sync based on its dirty flag and internal criteria
     * @return true if sync should happen now
     */
    @Override
    public abstract boolean shouldSyncNow();

    /**
     * Force synchronization of this attachment to the client.
     * This should handle creating the appropriate packet, sending it,
     * and clearing the dirty flag upon success.
     */
    @Override
    public abstract void forceSync();

    /**
     * Mark as dirty (needs sync)
     */
    @Override
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * Check if dirty (needs sync)
     */
    @Override
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Clear dirty flag after successful sync
     */
    @Override
    public void clearDirty() {
        this.dirty = false;
        this.lastSyncTime = System.currentTimeMillis();
    }

    /**
     * Get change counter for batching optimizations
     */
    @Override
    public long getChangeCounter() {
        return changeCounter;
    }

    /**
     * Increment change counter
     */
    @Override
    public void incrementChangeCounter() {
        this.changeCounter++;
    }

    /**
     * Reset change counter after major sync
     */
    @Override
    public void resetChangeCounter() {
        this.changeCounter = 0;
    }

    /**
     * Get time of last sync in milliseconds
     */
    public long getLastSyncTime() {
        return lastSyncTime;
    }

    /**
     * Check if enough time has passed since last sync
     */
    public boolean shouldSyncByTime(long intervalMs) {
        return System.currentTimeMillis() - lastSyncTime >= intervalMs;
    }

    /**
     * Check if flag should sync based on change count
     */
    public boolean shouldSyncByCount(long changeThreshold) {
        return changeCounter >= changeThreshold;
    }

    /**
     * Combined check: sync if dirty AND (time passed OR change threshold reached)
     */
    public boolean shouldSync(long timeIntervalMs, long changeThreshold) {
        return isDirty() && (shouldSyncByTime(timeIntervalMs) || shouldSyncByCount(changeThreshold));
    }

    /**
     * Force dirty and increment counter
     */
    public void markDirtyAndIncrement() {
        markDirty();
        incrementChangeCounter();
    }

    @Override
    public String toString() {
        return String.format("DirtyTrackerUtil{dirty=%s, changes=%d, lastSync=%d}",
            dirty, changeCounter, lastSyncTime);
    }

    // === CODEC === //

    /**
     * Restore state from CODEC deserialization
     */
    public void restoreFromCodec(long changeCounter, long lastSyncTime) {
        this.changeCounter = changeCounter;
        this.lastSyncTime = lastSyncTime;
        this.dirty = true; // Always dirty after loading
    }
}