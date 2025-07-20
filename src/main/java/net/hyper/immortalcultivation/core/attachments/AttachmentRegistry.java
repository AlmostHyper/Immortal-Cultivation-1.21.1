package net.hyper.immortalcultivation.core.attachments;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AttachmentRegistry {
    public static final DeferredRegister<AttachmentType<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ImmortalCultivation.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CultivationPlayerAttachment>> CULTIVATION;

    static {
        CULTIVATION = REGISTRY.register("cultivation", () ->
            AttachmentType.builder(() -> new CultivationPlayerAttachment())
                .serialize(new CultivationPlayerAttachment.Serializer())
                .copyOnDeath()
                .build());

    }

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}