package net.hyper.immortalcultivation.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ICModAttributes {
    public static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(Registries.ATTRIBUTE, "immortalcultivation");

    public static final DeferredHolder<Attribute, Attribute>  MAX_QI;

    public static void register(IEventBus eventBus) {REGISTRY.register(eventBus);}

    static {

        MAX_QI = REGISTRY.register("qi", () -> (new RangedAttribute("immortalcultivation.attribute.max_qi_name", (double)0.0F, (double)0.0F, 1.0E18)).setSyncable(true));

    }
}
