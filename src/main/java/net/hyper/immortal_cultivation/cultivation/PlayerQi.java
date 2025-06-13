package net.hyper.immortal_cultivation.cultivation;

import net.minecraft.nbt.CompoundTag;

public interface PlayerQi {

    void deserializeNBT(CompoundTag tag);

    int getQi();
    void setQi(int qi);

    int getRegenRate();
    void setRegenRate(int regen);

    int getMaxQi();

    default void tick() {
        if (getQi() < getMaxQi()) {
            setQi(Math.min(getQi() + getRegenRate(), getMaxQi()));
        }
    }
}
