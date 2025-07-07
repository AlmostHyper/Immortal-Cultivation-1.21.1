package net.hyper.immortalcultivation.helper;

import net.hyper.immortalcultivation.interfaces.IDirtyTrackable;
import net.minecraft.nbt.CompoundTag;

/**
 * Concrete dirty flag tracker that can be composed into attachment classes.
 *
 * This provides thread-safe dirty flag tracking with optional change counting
 * for advanced sync optimization.
 */
public class DirtyTracker implements IDirtyTrackable {

    private volatile boolean dirty = false;
    private volatile long changeCounter = 0;
    private volatile long lastSyncTime = 0;

    // NBT tags for serialization
    private static final String TAG_CHANGE_COUNTER = "ChangeCounter";
    private static final String TAG_LAST_SYNC_TIME = "LastSyncTime";

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

    // === SERIALIZATION ===

    /**
     * Serialize to NBT
     */
    public void writeToNBT(CompoundTag tag) {
        tag.putLong(TAG_CHANGE_COUNTER, changeCounter);
        tag.putLong(TAG_LAST_SYNC_TIME, lastSyncTime);
        // Note: dirty flag is not serialized as it's runtime state
    }

    /**
     * Deserialize from NBT
     */
    public void readFromNBT(CompoundTag tag) {
        this.changeCounter = tag.getLong(TAG_CHANGE_COUNTER);
        this.lastSyncTime = tag.getLong(TAG_LAST_SYNC_TIME);
        this.dirty = true; // Always dirty after loading
    }

    @Override
    public String toString() {
        return String.format("DirtyTracker{dirty=%s, changes=%d, lastSync=%d}",
            dirty, changeCounter, lastSyncTime);
    }
}