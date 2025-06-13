package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.component.PlayerQiComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ICModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTRY =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "immortal_cultivation");

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerQiComponent>> PLAYER_QI =
        REGISTRY.register("player_qi", () ->
            AttachmentType.builder(holder -> new PlayerQiComponent((Player) holder))
                .serialize(PlayerQiComponent.CODEC)
                .copyOnDeath()
                .build()
        );

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}


