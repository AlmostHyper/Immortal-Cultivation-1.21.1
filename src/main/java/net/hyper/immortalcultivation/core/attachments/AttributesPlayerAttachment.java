package net.hyper.immortalcultivation.core.attachments;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

public class AttributesPlayerAttachment implements IAttachmentSerializer, IAttributesData {
    @Override
    public long getCurrentStat(StatType type) {
        return 0;
    }

    @Override
    public void setCurrentStat(StatType type, long value) {

    }

    @Override
    public long getMaxStat(StatType type) {
        return 0;
    }

    @Override
    public void setMaxStat(StatType type, long value) {

    }

    @Override
    public long addStat(StatType type, long amount) {
        return IAttributesData.super.addStat(type, amount);
    }

    @Override
    public long consumeStat(StatType type, long amount) {
        return IAttributesData.super.consumeStat(type, amount);
    }

    @Override
    public long tryConsumeStat(StatType type, long amount) {
        return IAttributesData.super.tryConsumeStat(type, amount);
    }

    @Override
    public boolean hasStat(StatType type, long amount) {
        return IAttributesData.super.hasStat(type, amount);
    }

    @Override
    public double getStatPercentage(StatType type) {
        return IAttributesData.super.getStatPercentage(type);
    }

    @Override
    public double getStatRegenRate(StatType type) {
        return 0;
    }

    @Override
    public double getEffectiveStatRegenerationRate(StatType type) {
        return IAttributesData.super.getEffectiveStatRegenerationRate(type);
    }

    @Override
    public boolean canRegenerateStat(StatType type) {
        return IAttributesData.super.canRegenerateStat(type);
    }

    @Override
    public double getRegenFraction(StatType type) {
        return 0;
    }

    @Override
    public void setRegenFraction(StatType type, double fraction) {

    }

    @Override
    public void addRegenFraction(StatType type, double amount) {
        IAttributesData.super.addRegenFraction(type, amount);
    }

    @Override
    public long getPermanentStatBonus(StatType type) {
        return 0;
    }

    @Override
    public void setPermanentStatBonus(StatType type, long bonus) {

    }

    @Override
    public double getPermanentStatRegenBonus(StatType type) {
        return 0;
    }

    @Override
    public void setPermanentStatRegenBonus(StatType type, double bonus) {

    }

    @Override
    public void onStatChanged(StatType type, long oldValue, long newValue) {
        IAttributesData.super.onStatChanged(type, oldValue, newValue);
    }

    @Override
    public void onStatConsumed(StatType type, long amount) {
        IAttributesData.super.onStatConsumed(type, amount);
    }

    @Override
    public void onStatAdded(StatType type, long amount) {
        IAttributesData.super.onStatAdded(type, amount);
    }

    @Override
    public Object read(IAttachmentHolder iAttachmentHolder, Tag tag, HolderLookup.Provider provider) {
        return null;
    }

    @Override
    public @Nullable Tag write(Object o, HolderLookup.Provider provider) {
        return null;
    }
}
