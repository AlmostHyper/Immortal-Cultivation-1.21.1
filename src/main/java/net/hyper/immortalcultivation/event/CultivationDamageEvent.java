package net.hyper.immortalcultivation.event;

import net.hyper.immortalcultivation.cultivation.combat.CultivationDamage;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

import javax.annotation.Nullable;

public class CultivationDamageEvent extends Event {

    private final LivingEntity target;
    private CultivationDamage damage;

    public CultivationDamageEvent(LivingEntity target, CultivationDamage damage) {
        this.target = target;
        this.damage = damage;
    }

    public LivingEntity getTarget() { return target; }
    public CultivationDamage getDamage() { return damage; }
    public void setDamage(CultivationDamage damage) { this.damage = damage; }

    // === PRE-DAMAGE EVENT (Cancellable) ===
    @Nullable
    public static class Pre extends CultivationDamageEvent {
        public Pre(LivingEntity target, CultivationDamage damage) {
            super(target, damage);
        }
    }

    // === POST-DAMAGE EVENT (Information Only) ===
    public static class Post extends CultivationDamageEvent {
        private final boolean handledByMod;
        private final boolean handledByVanilla;

        public Post(LivingEntity target, CultivationDamage damage,
                    boolean handledByMod, boolean handledByVanilla) {
            super(target, damage);
            this.handledByMod = handledByMod;
            this.handledByVanilla = handledByVanilla;
        }

        public boolean wasHandledByMod() { return handledByMod; }
        public boolean wasHandledByVanilla() { return handledByVanilla; }
        public boolean wasHandled() { return handledByMod || handledByVanilla; }
    }
}
