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
    private static final long MORTAL_BASE_SPIRITUAL_ATTACK_DAMAGE = 0;
    private static final double MORTAL_BASE_CRITICAL_CHANCE = 0.0;
    private static final double MORTAL_BASE_CRITICAL_DAMAGE = 0.0;
    private static final double MORTAL_BASE_ATTACK_SPEED = 1.0;

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

    private double permanentAttackDamageBonus = 0;
    private double permanentSpiritualDamageBonus = 0;
    private double permanentCriticalChanceBonus = 0.0;
    private double permanentCriticalDamageBonus = 0.0;
    private double permanentAttackSpeedBonus = 0.0;
    private double attackDamageBonus = 0;
    private double spiritualDamageBonus = 0;
    private double criticalChanceBonus = 0.0;
    private double criticalDamageBonus = 0.0;
    private double attackSpeedBonus = 0.0;
    private double currentAttackDamage = MORTAL_BASE_ATTACK_DAMAGE;
    private double currentSpiritualDamage = MORTAL_BASE_SPIRITUAL_ATTACK_DAMAGE;
    private double currentCriticalChance = MORTAL_BASE_CRITICAL_CHANCE;
    private double currentCriticalDamage = MORTAL_BASE_CRITICAL_DAMAGE;
    private double currentAttackSpeed = MORTAL_BASE_ATTACK_SPEED;

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
        return switch (type) {
            case HEALTH -> permanentHealthBonus;
            case QI -> permanentQiBonus;
            case BODY -> permanentBodyBonus;
        };
    }

    @Override
    public void setPermanentAttributeBonus(AttributeType type, long bonus) {
        long oldBonus = getPermanentAttributeBonus(type);

        switch (type) {
            case HEALTH -> this.permanentHealthBonus = bonus;
            case QI -> this.permanentQiBonus = bonus;
            case BODY -> this.permanentBodyBonus = bonus;
        }

        recalculateAttributeMaximum(type);
    }

    @Override
    public double getPermanentAttributeRegenBonus(AttributeType type) {
        return switch (type) {
            case HEALTH -> permanentHealthRegenBonus;
            case QI -> permanentQiRegenBonus;
            case BODY -> permanentBodyRegenBonus;
        };
    }

    @Override
    public void setPermanentAttributeRegenBonus(AttributeType type, double bonus) {
        double oldBonus = getPermanentAttributeRegenBonus(type);

        switch (type) {
            case HEALTH -> this.permanentHealthRegenBonus = bonus;
            case QI -> this.permanentQiRegenBonus = bonus;
            case BODY -> this.permanentBodyRegenBonus = bonus;
        }
    }

    @Override
    public void onAttributeChanged(AttributeType type, long oldValue, long newValue) {IAttributesData.super.onAttributeChanged(type, oldValue, newValue);}

    @Override
    public void onAttributeConsumed(AttributeType type, long amount) {IAttributesData.super.onAttributeConsumed(type, amount);}

    @Override
    public void onAttributeAdded(AttributeType type, long amount) {IAttributesData.super.onAttributeAdded(type, amount);}

    @Override
    public double getBaseCombatStat(CombatStatType type) {
        return switch (type) {
            case ATTACK_DAMAGE -> this.currentAttackDamage;
            case SPIRITUAL_DAMAGE -> this.currentSpiritualDamage;
            case CRITICAL_CHANCE -> this.currentCriticalChance;
            case CRITICAL_DAMAGE -> this.currentCriticalDamage;
            case ATTACK_SPEED -> this.currentAttackSpeed;
        };
    }

    @Override
    public double getBonusCombatStat(CombatStatType type, BonusSource source) {
        return switch (type) {
            case ATTACK_DAMAGE -> this.attackDamageBonus;
            case SPIRITUAL_DAMAGE -> this.spiritualDamageBonus;
            case CRITICAL_CHANCE -> this.criticalChanceBonus;
            case CRITICAL_DAMAGE -> this.criticalDamageBonus;
            case ATTACK_SPEED -> this.attackSpeedBonus;
        };
    }

    @Override
    public double getPermanentBonusCombatStat(CombatStatType type, BonusSource source) {
        return switch (type) {
            case ATTACK_DAMAGE -> this.permanentAttackDamageBonus;
            case SPIRITUAL_DAMAGE -> this.permanentSpiritualDamageBonus;
            case CRITICAL_CHANCE -> this.permanentCriticalChanceBonus;
            case CRITICAL_DAMAGE -> this.permanentCriticalDamageBonus;
            case ATTACK_SPEED -> this.permanentAttackSpeedBonus;
        };
    }

    @Override
    public double getMultiplier(CombatStatType type) {
        return 0;
    }

    @Override
    public double getFinalCombatStat(CombatStatType type) {
        return switch (type) {
            case ATTACK_DAMAGE -> (currentAttackDamage + attackDamageBonus + permanentAttackDamageBonus);
            case SPIRITUAL_DAMAGE -> (currentSpiritualDamage + spiritualDamageBonus + permanentSpiritualDamageBonus);
            case CRITICAL_CHANCE -> (currentCriticalChance + criticalChanceBonus + permanentCriticalChanceBonus);
            case CRITICAL_DAMAGE -> (currentCriticalDamage + criticalDamageBonus + permanentCriticalDamageBonus);
            case ATTACK_SPEED -> (currentAttackSpeed + attackSpeedBonus + permanentAttackSpeedBonus);
        };
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
