package net.hyper.immortalcultivation.network.server;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network packet to sync vanilla-style damage feedback from server to client.
 * This ensures the client displays the proper hurt animation and red screen overlay.
 */
public record DamageFeedbackSyncPacket(
    long damageAmount,
    String damageSourceId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DamageFeedbackSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("immortalcultivation", "vanilla_damage_feedback"));

    public static final StreamCodec<FriendlyByteBuf, DamageFeedbackSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DamageFeedbackSyncPacket::damageAmount,
            ByteBufCodecs.STRING_UTF8, DamageFeedbackSyncPacket::damageSourceId,
            DamageFeedbackSyncPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleClient(DamageFeedbackSyncPacket packet, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;

                if (player != null) {
                    // Apply vanilla-style client effects
                    applyClientDamageEffects(player, packet.damageAmount(), packet.damageSourceId());
                }
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyClientDamageEffects(Player player, long damageAmount, String damageSourceId) {
        // Convert damage to vanilla scale for proper feedback intensity
        float vanillaDamage = Math.min(20.0f, damageAmount / 10.0f);

        // Apply hurt animation and red screen overlay (same as vanilla)
        player.hurtTime = 10; // 0.5 seconds of hurt animation
        player.hurtDuration = 10;

        // Set hurt direction using reflection for protected field access
        setHurtDirection(player);

        // Play hurt sound based on damage type
        playDamageSound(player, damageSourceId, vanillaDamage);

        // The red screen overlay is handled automatically by Minecraft when hurtTime > 0
        // No additional overlay rendering needed - vanilla handles this
    }

    @OnlyIn(Dist.CLIENT)
    private static void setHurtDirection(Player player) {
        try {
            // Use reflection to access the protected hurtDir field
            java.lang.reflect.Field hurtDirField = Player.class.getDeclaredField("hurtDir");
            hurtDirField.setAccessible(true);
            hurtDirField.setFloat(player, (float) (Math.random() * Math.PI * 2));
        } catch (Exception e) {
            // If reflection fails, that's okay - hurt animation still works without direction
            // This is normal and expected in some environments
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void playDamageSound(Player player, String damageSourceId, float damageAmount) {
        // Play appropriate hurt sound based on damage source
        if (damageSourceId.contains("fire") || damageSourceId.contains("lava")) {
            player.level().playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT_ON_FIRE,
                SoundSource.PLAYERS,
                1.0f, 1.0f, false
            );
        } else if (damageSourceId.contains("drown")) {
            player.level().playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT_DROWN,
                SoundSource.PLAYERS,
                1.0f, 1.0f, false
            );
        } else if (damageSourceId.contains("freeze")) {
            player.level().playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT_FREEZE,
                SoundSource.PLAYERS,
                1.0f, 1.0f, false
            );
        } else if (damageSourceId.contains("sweet_berry")) {
            player.level().playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH,
                SoundSource.PLAYERS,
                1.0f, 1.0f, false
            );
        } else {
            // Default hurt sound
            player.level().playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS,
                1.0f, (float) (0.8 + Math.random() * 0.4), false
            );
        }
    }
}