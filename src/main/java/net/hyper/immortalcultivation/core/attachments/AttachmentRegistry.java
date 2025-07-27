package net.hyper.immortalcultivation.core.attachments;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.attachments.player.PlayerAttributesAttachment;
import net.hyper.immortalcultivation.core.attachments.player.PlayerCultivationAttachment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AttachmentRegistry {
    public static final DeferredRegister<AttachmentType<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ImmortalCultivation.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCultivationAttachment>> PLAYER_CULTIVATION;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerAttributesAttachment>> PLAYER_ATTRIBUTES;

    static {
        PLAYER_CULTIVATION = REGISTRY.register("cultivation", () ->
            AttachmentType.builder(() -> new PlayerCultivationAttachment())
                .serialize(PlayerCultivationAttachment.SERIALIZER_INSTANCE)
                .copyOnDeath()
                .build());

        PLAYER_ATTRIBUTES = REGISTRY.register("attribute", () ->
            AttachmentType.builder(() -> new PlayerAttributesAttachment())
                .build());
    }

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}