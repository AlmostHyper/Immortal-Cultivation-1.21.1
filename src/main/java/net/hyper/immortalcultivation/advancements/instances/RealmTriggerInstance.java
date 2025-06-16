package net.hyper.immortalcultivation.advancements.instances;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;

import java.util.Optional;

public record RealmTriggerInstance(Optional<ContextAwarePredicate> player, int requiredAmount)
    implements SimpleCriterionTrigger.SimpleInstance {

    public static final Codec<RealmTriggerInstance> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(RealmTriggerInstance::player),
            Codec.INT.fieldOf("amount").forGetter(RealmTriggerInstance::requiredAmount)
        ).apply(instance, RealmTriggerInstance::new)
    );

    public boolean matches(int amount) {
        return amount >= this.requiredAmount;
    }
}
