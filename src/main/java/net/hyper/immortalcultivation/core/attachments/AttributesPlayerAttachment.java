package net.hyper.immortalcultivation.core.attachments;

import net.hyper.immortalcultivation.core.cultivation.BonusSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

public class AttributesPlayerAttachment implements IAttachmentSerializer, IAttributesData, ICombatData {

    private static final long MORTAL_BASE_HEALTH = 20;
    private static final long MORTAL_BASE_QI = 0;
    private static final long MORTAL_BASE_BODY = 0;
    private static final long MORTAL_BASE_ATTACK_DAMAGE = 1;

    private long permanentHealthBonus = 0;
    private double permanentHealthRegenBonus = 0.0;
    private long currentHealth = MORTAL_BASE_HEALTH;
    private long maxHealth = MORTAL_BASE_HEALTH;
    private double healthRegenFraction = 0.0;

    private long permanentQiBonus = 0;
    private double permanentQiRegenBonus = 0.0;
    private long currentQi = MORTAL_BASE_QI;
    private long maxQi = MORTAL_BASE_QI;
    private double qiRegenFraction = 0.0;

    private long permanentBodyBonus = 0;
    private double permanentBodyRegenBonus = 0.0;
    private long currentBody = MORTAL_BASE_BODY;
    private long maxBody = MORTAL_BASE_BODY;
    private double bodyRegenFraction = 0.0;

    private long permanentAttackDamageBonus = 0;
    private long permanentSpiritualDamageBonus = 0;
    private double permanentCriticalChanceBonus = 0.0;
    private double permanentCriticalDamageBonus = 0.0;
    private double permanentAttackSpeedBonus = 0.0;
    private long attackDamageBonus = 0;

    private double permanentMovementSpeedBonus = 0.0;
    private double permanentJumpHeightBonus = 0.0;
    private double movementSpeedBonus = 0.0;
    private double jumpHeightBonus = 0.0;

    private int regenTickCounter = 0;
    private static final int REGEN_TICK_INTERVAL = 1;

    public AttributesPlayerAttachment() {super();}


    @Override
    public long getCurrentAttribute(AttributeType type) {
        return switch (type) {
            case HEALTH -> currentHealth;
            case QI -> currentQi;
            case BODY -> currentBody;
        };
    }

    @Override
    public void setCurrentAttribute(AttributeType type, long value) {
        long oldValue = getCurrentAttribute(type);
        long maxValue = getMaxAttribute(type);
        long newValue = Math.max(0, Math.min(value, maxValue));

        switch (type) {
            case HEALTH -> this.currentHealth = newValue;
            case QI -> this.currentQi = newValue;
            case BODY -> this.currentBody = newValue;
        }

        onAttributeChanged(type, oldValue, newValue);
    }

    @Override
    public long getMaxAttribute(AttributeType type) {
        return switch (type) {
            case HEALTH -> maxHealth;
            case QI -> maxQi;
            case BODY -> maxBody;
        };
    }

    @Override
    public void setMaxAttribute(AttributeType type, long value) {
        long oldValue = getMaxAttribute(type);
        long newValue = Math.max(0, value);

        switch (type) {
            case HEALTH -> this.maxHealth = newValue;
            case QI -> this.maxQi = newValue;
            case BODY -> this.maxBody = newValue;
        }

        if (oldValue != newValue) {
            long currentValue = getCurrentAttribute(type);

            if (currentValue > newValue) {setCurrentAttribute(type, newValue);}
        }
    }

    @Override
    public long addAttribute(AttributeType type, long amount) {return IAttributesData.super.addAttribute(type, amount);}

    @Override
    public long consumeAttribute(AttributeType type, long amount) {return IAttributesData.super.consumeAttribute(type, amount);}

    @Override
    public long tryConsumeAttribute(AttributeType type, long amount) {return IAttributesData.super.tryConsumeAttribute(type, amount);}

    @Override
    public boolean hasAttribute(AttributeType type, long amount) {return IAttributesData.super.hasAttribute(type, amount);}

    @Override
    public double getAttributePercentage(AttributeType type) {return IAttributesData.super.getAttributePercentage(type);}

    @Override
    public double getAttributeRegenRate(AttributeType type) {
        return switch (type) {
            case HEALTH -> 0.0 + permanentHealthRegenBonus;
            case QI -> 0.0 + permanentQiRegenBonus;
            case BODY -> 0.0 + permanentBodyRegenBonus;
        };
    }

    @Override
    public double getEffectiveAttributeRegenerationRate(AttributeType type) {return IAttributesData.super.getEffectiveAttributeRegenerationRate(type);}

    @Override
    public boolean canRegenerateAttribute(AttributeType type) {return IAttributesData.super.canRegenerateAttribute(type);}

    @Override
    public double getRegenFraction(AttributeType type) {
        return switch (type) {
            case HEALTH -> healthRegenFraction;
            case QI -> qiRegenFraction;
            case BODY -> bodyRegenFraction;
        };
    }

    @Override
    public void setRegenFraction(AttributeType type, double fraction) {
        switch (type) {
            case HEALTH -> this.healthRegenFraction = fraction;
            case QI -> this.qiRegenFraction = fraction;
            case BODY -> this.bodyRegenFraction = fraction;
        }
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
    public long getBaseCombatStat(CombatStatType type) {
        return 0;
    }

    @Override
    public long getBonusCombatStat(CombatStatType type, BonusSource source) {
        return 0;
    }

    @Override
    public double getMultiplier(CombatStatType type) {
        return 0;
    }

    @Override
    public long getFinalCombatStat(CombatStatType type) {
        return 0;
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
