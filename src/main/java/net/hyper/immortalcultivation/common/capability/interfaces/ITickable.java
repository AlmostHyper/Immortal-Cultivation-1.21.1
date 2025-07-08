package net.hyper.immortalcultivation.common.capability.interfaces;

/**
 * Interface for attachments that need to be ticked regularly
 */
public interface ITickable {
    /**
     * Called every tick to update this attachment's state
     */
    void tick();
}