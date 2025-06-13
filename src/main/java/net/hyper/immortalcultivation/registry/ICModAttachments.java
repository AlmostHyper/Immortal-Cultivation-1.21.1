package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.component.PlayerQiComponent;
import net.hyper.immortalcultivation.component.PlayerRealmComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ICModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTRY =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ImmortalCultivation.MOD_ID);

    // Use DeferredHolder (the return type of REGISTRY.register)
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerQiComponent>> PLAYER_QI =
        REGISTRY.register("player_qi", () ->
            AttachmentType.builder(() -> new PlayerQiComponent())
                .serialize(PlayerQiComponent.CODEC)
                .copyOnDeath()
                .build()
        );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerRealmComponent>> PLAYER_REALM =
        REGISTRY.register("player_realm", () ->
            AttachmentType.builder(() -> new PlayerRealmComponent())
                .serialize(PlayerRealmComponent.CODEC)
                .copyOnDeath()
                .build()
        );

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}


