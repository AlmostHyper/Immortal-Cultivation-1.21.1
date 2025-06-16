package net.hyper.immortalcultivation.advancements.critereon;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import net.hyper.immortalcultivation.advancements.instances.RealmTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class MortalAdvancementTrigger extends SimpleCriterionTrigger<RealmTriggerInstance> {

    @Override
    public Codec<RealmTriggerInstance> codec() {
        return RealmTriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(1));
    }
}
