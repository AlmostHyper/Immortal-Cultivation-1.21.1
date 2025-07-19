package net.hyper.immortalcultivation.server.events;

import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CultivationTickEvent extends Event {
    private final LivingEntity entity;
    private final ICultivationData.CultivationType type;
    private final double baseRate;
    private final List<CultivationModifier> modifiers = new ArrayList<>();
    private boolean needSync = false;

    public CultivationTickEvent(LivingEntity entity,
                                ICultivationData.CultivationType type,
                                double baseRate) {
        this.entity = entity;
        this.type = type;
        this.baseRate = baseRate;
    }

    public LivingEntity getEntity() {return this.entity;}

    public ICultivationData.CultivationType getType() {return type;}

    public double getBaseRate() {return baseRate;}

    public List<CultivationModifier> getModifiers() {return new ArrayList<>(modifiers);}

    public void addModifier(CultivationModifier modifier) {
        modifiers.add(modifier);
        needSync = true;
    }

    public void removeModifier(CultivationModifier modifier) {
        modifiers.remove(modifier);
        needSync = true;
    }

    public double calculateFinalRate() {
        if (needSync) {
            modifiers.sort(Comparator.comparing(CultivationModifier::getPriority));
            needSync = false;
        }

        double result = baseRate;
        for (CultivationModifier modifier : modifiers) {
            result = modifier.apply(result, this);
        }
        return result;
    }

    public static abstract class CultivationModifier {
        private final String description;
        private final int priority;

        protected CultivationModifier(String description, int priority) {
            this.description = description;
            this.priority = priority;
        }

        public abstract double apply(double currentRate, CultivationTickEvent context);

        public int getPriority() {return priority;}

        public static class Additive extends CultivationModifier {
            private final double bonus;

            public Additive(String description, int priority, double bonus) {
                super(description, priority);
                this.bonus = bonus;
            }

            public double apply(double currentRate, CultivationTickEvent context) {
                return currentRate + bonus;
            }
        }

        public static class Multiplicative extends CultivationModifier {
            private final double multiplier;

            public Multiplicative(String description, int priority, double multiplier) {
                super(description, priority);
                this.multiplier = multiplier;
            }

            public double apply(double currentRate, CultivationTickEvent context) {
                return currentRate * multiplier;
            }
        }
    }
}