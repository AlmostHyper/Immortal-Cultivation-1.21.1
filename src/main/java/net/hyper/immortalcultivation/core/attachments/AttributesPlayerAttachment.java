package net.hyper.immortalcultivation.core.attachments;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

public class AttributesPlayerAttachment implements IAttachmentSerializer, IAttributesData {
    @Override
    public long getCurrentAttribute(AttributeType type) {
        return 0;
    }

    @Override
    public void setCurrentAttribute(AttributeType type, long value) {

    }

    @Override
    public long getMaxAttribute(AttributeType type) {
        return 0;
    }

    @Override
    public void setMaxAttribute(AttributeType type, long value) {

    }

    @Override
    public long addAttribute(AttributeType type, long amount) {
        return IAttributesData.super.addAttribute(type, amount);
    }

    @Override
    public long consumeAttribute(AttributeType type, long amount) {
        return IAttributesData.super.consumeAttribute(type, amount);
    }

    @Override
    public long tryConsumeAttribute(AttributeType type, long amount) {
        return IAttributesData.super.tryConsumeAttribute(type, amount);
    }

    @Override
    public boolean hasAttribute(AttributeType type, long amount) {
        return IAttributesData.super.hasAttribute(type, amount);
    }

    @Override
    public double getAttributePercentage(AttributeType type) {
        return IAttributesData.super.getAttributePercentage(type);
    }

    @Override
    public double getAttributeRegenRate(AttributeType type) {
        return 0;
    }

    @Override
    public double getEffectiveAttributeRegenerationRate(AttributeType type) {
        return IAttributesData.super.getEffectiveAttributeRegenerationRate(type);
    }

    @Override
    public boolean canRegenerateAttribute(AttributeType type) {
        return IAttributesData.super.canRegenerateAttribute(type);
    }

    @Override
    public double getRegenFraction(AttributeType type) {
        return 0;
    }

    @Override
    public void setRegenFraction(AttributeType type, double fraction) {

    }

    @Override
    public void addRegenFraction(AttributeType type, double amount) {
        IAttributesData.super.addRegenFraction(type, amount);
    }

    @Override
    public long getPermanentAttributeBonus(AttributeType type) {
        return 0;
    }

    @Override
    public void setPermanentAttributeBonus(AttributeType type, long bonus) {

    }

    @Override
    public double getPermanentAttributeRegenBonus(AttributeType type) {
        return 0;
    }

    @Override
    public void setPermanentAttributeRegenBonus(AttributeType type, double bonus) {

    }

    @Override
    public void onAttributeChanged(AttributeType type, long oldValue, long newValue) {
        IAttributesData.super.onAttributeChanged(type, oldValue, newValue);
    }

    @Override
    public void onAttributeConsumed(AttributeType type, long amount) {
        IAttributesData.super.onAttributeConsumed(type, amount);
    }

    @Override
    public void onAttributeAdded(AttributeType type, long amount) {
        IAttributesData.super.onAttributeAdded(type, amount);
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
