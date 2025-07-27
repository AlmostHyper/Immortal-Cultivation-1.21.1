package net.hyper.immortalcultivation.core.network;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.network.packets.BreakthroughPacket;
import net.hyper.immortalcultivation.core.network.packets.CultivationPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkRegistry {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ImmortalCultivation.MOD_ID);

        registrar.playBidirectional(
            CultivationPacket.TYPE,
            CultivationPacket.STREAM_CODEC,
            NetworkManager::handleCultivationPacket
        );
    }
}
