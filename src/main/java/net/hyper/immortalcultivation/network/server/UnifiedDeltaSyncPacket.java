package net.hyper.immortalcultivation.network.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Unified delta sync packet that efficiently syncs only changed data across all attachment types.
 * Uses bitmasking, compression, and selective field syncing for optimal performance.
 */
public record UnifiedDeltaSyncPacket(
    // Bitmask indicating which attachment types have updates
    byte attachmentMask,

    // Bitmasks for which fields are included for each attachment type
    short cultivationFieldMask,
    short resourceFieldMask,
    byte combatStatsFieldMask,

    // Cultivation data (only included if bits are set)
    Optional<String> currentQiRealmId,
    Optional<String> currentBodyRealmId,
    Optional<Long> currentQiCultivation,
    Optional<Long> maxQiCultivation,
    Optional<Long> currentBodyCultivation,
    Optional<Long> maxBodyCultivation,
    Optional<PlayerCultivationAttachment.CultivationType> currentFocus,
    Optional<PackedBonuses> bonuses,
    Optional<PackedBreakthroughData> breakthroughData,

    // Resource data (only included if bits are set)
    Optional<Long> currentHealth,
    Optional<Long> currentQi,
    Optional<Long> currentBody,
    Optional<PackedPermanentBonuses> permanentBonuses,

    // Combat stats data (only included if bits are set)
    Optional<PackedCombatStats> combatStats
) implements CustomPacketPayload {

    public static final Type<UnifiedDeltaSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "unified_delta_sync")
    );

    // Attachment type masks
    public static final byte CULTIVATION_MASK = 1;
    public static final byte RESOURCE_MASK = 2;
    public static final byte COMBAT_STATS_MASK = 4;

    // Cultivation field masks
    public static final short CULT_QI_REALM = 1;
    public static final short CULT_BODY_REALM = 2;
    public static final short CULT_QI_CURRENT = 4;
    public static final short CULT_QI_MAX = 8;
    public static final short CULT_BODY_CURRENT = 16;
    public static final short CULT_BODY_MAX = 32;
    public static final short CULT_FOCUS = 64;
    public static final short CULT_BONUSES = 128;
    public static final short CULT_BREAKTHROUGH = 256;

    // Resource field masks
    public static final short RES_HEALTH = 1;
    public static final short RES_QI = 2;
    public static final short RES_BODY = 4;
    public static final short RES_PERMANENT = 8;

    // Combat stats field mask
    public static final byte COMBAT_ALL = 1;

    /**
     * Packed cultivation bonuses to reduce network overhead
     */
    public record PackedBonuses(
        double qiAdditiveBonus,
        double qiMultiplicativeBonus,
        double bodyAdditiveBonus,
        double bodyMultiplicativeBonus
    ) {}

    /**
     * Packed breakthrough data using bitmasking for booleans
     */
    public record PackedBreakthroughData(
        long qiBreakthroughAttempts,
        long bodyBreakthroughAttempts,
        byte cooldownFlags, // bit 0: qi cooldown, bit 1: body cooldown
        long lastQiBreakthroughAttempt,
        long lastBodyBreakthroughAttempt
    ) {
        public boolean hasQiCooldown() { return (cooldownFlags & 1) != 0; }
        public boolean hasBodyCooldown() { return (cooldownFlags & 2) != 0; }

        public static PackedBreakthroughData create(long qiAttempts, long bodyAttempts,
                                                    boolean qiCooldown, boolean bodyCooldown,
                                                    long lastQi, long lastBody) {
            byte flags = 0;
            if (qiCooldown) flags |= 1;
            if (bodyCooldown) flags |= 2;
            return new PackedBreakthroughData(qiAttempts, bodyAttempts, flags, lastQi, lastBody);
        }
    }

    /**
     * Packed permanent bonuses
     */
    public record PackedPermanentBonuses(
        long permanentHealthBonus,
        double permanentHealthRegenBonus,
        long permanentMaxQiBonus,
        double permanentQiRegenBonus,
        long permanentMaxBodyBonus,
        double permanentBodyRegenBonus
    ) {}

    /**
     * Packed combat stats
     */
    public record PackedCombatStats(
        double attackBonus,
        double speedBonus,
        double jumpBonus,
        double permanentAttackBonus,
        double permanentSpeedBonus,
        double permanentJumpBonus
    ) {}

    // Custom codec implementation for optimal performance
    public static final StreamCodec<ByteBuf, UnifiedDeltaSyncPacket> STREAM_CODEC = StreamCodec.of(
        UnifiedDeltaSyncPacket::encode,
        UnifiedDeltaSyncPacket::decode
    );

    private static void encode(ByteBuf buf, UnifiedDeltaSyncPacket packet) {
        // Write bitmasks
        ByteBufCodecs.BYTE.encode(buf, packet.attachmentMask);

        if ((packet.attachmentMask & CULTIVATION_MASK) != 0) {
            ByteBufCodecs.SHORT.encode(buf, packet.cultivationFieldMask);

            // Encode cultivation fields based on mask
            if ((packet.cultivationFieldMask & CULT_QI_REALM) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.currentQiRealmId.orElse(""));
            }
            if ((packet.cultivationFieldMask & CULT_BODY_REALM) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.currentBodyRealmId.orElse(""));
            }
            if ((packet.cultivationFieldMask & CULT_QI_CURRENT) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentQiCultivation.orElse(0L));
            }
            if ((packet.cultivationFieldMask & CULT_QI_MAX) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.maxQiCultivation.orElse(0L));
            }
            if ((packet.cultivationFieldMask & CULT_BODY_CURRENT) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentBodyCultivation.orElse(0L));
            }
            if ((packet.cultivationFieldMask & CULT_BODY_MAX) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.maxBodyCultivation.orElse(0L));
            }
            if ((packet.cultivationFieldMask & CULT_FOCUS) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.currentFocus.map(Enum::name).orElse("QI"));
            }
            if ((packet.cultivationFieldMask & CULT_BONUSES) != 0) {
                PackedBonuses bonuses = packet.bonuses.orElse(new PackedBonuses(0, 0, 0, 0));
                ByteBufCodecs.DOUBLE.encode(buf, bonuses.qiAdditiveBonus);
                ByteBufCodecs.DOUBLE.encode(buf, bonuses.qiMultiplicativeBonus);
                ByteBufCodecs.DOUBLE.encode(buf, bonuses.bodyAdditiveBonus);
                ByteBufCodecs.DOUBLE.encode(buf, bonuses.bodyMultiplicativeBonus);
            }
            if ((packet.cultivationFieldMask & CULT_BREAKTHROUGH) != 0) {
                PackedBreakthroughData bt = packet.breakthroughData.orElse(
                    new PackedBreakthroughData(0, 0, (byte)0, 0, 0));
                ByteBufCodecs.VAR_LONG.encode(buf, bt.qiBreakthroughAttempts);
                ByteBufCodecs.VAR_LONG.encode(buf, bt.bodyBreakthroughAttempts);
                ByteBufCodecs.BYTE.encode(buf, bt.cooldownFlags);
                ByteBufCodecs.VAR_LONG.encode(buf, bt.lastQiBreakthroughAttempt);
                ByteBufCodecs.VAR_LONG.encode(buf, bt.lastBodyBreakthroughAttempt);
            }
        }

        if ((packet.attachmentMask & RESOURCE_MASK) != 0) {
            ByteBufCodecs.SHORT.encode(buf, packet.resourceFieldMask);

            if ((packet.resourceFieldMask & RES_HEALTH) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentHealth.orElse(0L));
            }
            if ((packet.resourceFieldMask & RES_QI) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentQi.orElse(0L));
            }
            if ((packet.resourceFieldMask & RES_BODY) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentBody.orElse(0L));
            }
            if ((packet.resourceFieldMask & RES_PERMANENT) != 0) {
                PackedPermanentBonuses pb = packet.permanentBonuses.orElse(
                    new PackedPermanentBonuses(0, 0, 0, 0, 0, 0));
                ByteBufCodecs.VAR_LONG.encode(buf, pb.permanentHealthBonus);
                ByteBufCodecs.DOUBLE.encode(buf, pb.permanentHealthRegenBonus);
                ByteBufCodecs.VAR_LONG.encode(buf, pb.permanentMaxQiBonus);
                ByteBufCodecs.DOUBLE.encode(buf, pb.permanentQiRegenBonus);
                ByteBufCodecs.VAR_LONG.encode(buf, pb.permanentMaxBodyBonus);
                ByteBufCodecs.DOUBLE.encode(buf, pb.permanentBodyRegenBonus);
            }
        }

        if ((packet.attachmentMask & COMBAT_STATS_MASK) != 0) {
            ByteBufCodecs.BYTE.encode(buf, packet.combatStatsFieldMask);

            if ((packet.combatStatsFieldMask & COMBAT_ALL) != 0) {
                PackedCombatStats cs = packet.combatStats.orElse(
                    new PackedCombatStats(0, 0, 0, 0, 0, 0));
                ByteBufCodecs.DOUBLE.encode(buf, cs.attackBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.speedBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.jumpBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.permanentAttackBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.permanentSpeedBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.permanentJumpBonus);
            }
        }
    }

    private static UnifiedDeltaSyncPacket decode(ByteBuf buf) {
        byte attachmentMask = ByteBufCodecs.BYTE.decode(buf);

        short cultivationFieldMask = 0;
        short resourceFieldMask = 0;
        byte combatStatsFieldMask = 0;

        // Initialize all optionals as empty
        Optional<String> currentQiRealmId = Optional.empty();
        Optional<String> currentBodyRealmId = Optional.empty();
        Optional<Long> currentQiCultivation = Optional.empty();
        Optional<Long> maxQiCultivation = Optional.empty();
        Optional<Long> currentBodyCultivation = Optional.empty();
        Optional<Long> maxBodyCultivation = Optional.empty();
        Optional<PlayerCultivationAttachment.CultivationType> currentFocus = Optional.empty();
        Optional<PackedBonuses> bonuses = Optional.empty();
        Optional<PackedBreakthroughData> breakthroughData = Optional.empty();

        Optional<Long> currentHealth = Optional.empty();
        Optional<Long> currentQi = Optional.empty();
        Optional<Long> currentBody = Optional.empty();
        Optional<PackedPermanentBonuses> permanentBonuses = Optional.empty();

        Optional<PackedCombatStats> combatStats = Optional.empty();

        // Decode cultivation data
        if ((attachmentMask & CULTIVATION_MASK) != 0) {
            cultivationFieldMask = ByteBufCodecs.SHORT.decode(buf);

            if ((cultivationFieldMask & CULT_QI_REALM) != 0) {
                currentQiRealmId = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            if ((cultivationFieldMask & CULT_BODY_REALM) != 0) {
                currentBodyRealmId = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            if ((cultivationFieldMask & CULT_QI_CURRENT) != 0) {
                currentQiCultivation = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((cultivationFieldMask & CULT_QI_MAX) != 0) {
                maxQiCultivation = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((cultivationFieldMask & CULT_BODY_CURRENT) != 0) {
                currentBodyCultivation = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((cultivationFieldMask & CULT_BODY_MAX) != 0) {
                maxBodyCultivation = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((cultivationFieldMask & CULT_FOCUS) != 0) {
                try {
                    currentFocus = Optional.of(PlayerCultivationAttachment.CultivationType.valueOf(
                        ByteBufCodecs.STRING_UTF8.decode(buf)));
                } catch (IllegalArgumentException e) {
                    currentFocus = Optional.of(PlayerCultivationAttachment.CultivationType.QI);
                }
            }
            if ((cultivationFieldMask & CULT_BONUSES) != 0) {
                bonuses = Optional.of(new PackedBonuses(
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf)
                ));
            }
            if ((cultivationFieldMask & CULT_BREAKTHROUGH) != 0) {
                breakthroughData = Optional.of(new PackedBreakthroughData(
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.BYTE.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf)
                ));
            }
        }

        // Decode resource data
        if ((attachmentMask & RESOURCE_MASK) != 0) {
            resourceFieldMask = ByteBufCodecs.SHORT.decode(buf);

            if ((resourceFieldMask & RES_HEALTH) != 0) {
                currentHealth = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((resourceFieldMask & RES_QI) != 0) {
                currentQi = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((resourceFieldMask & RES_BODY) != 0) {
                currentBody = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((resourceFieldMask & RES_PERMANENT) != 0) {
                permanentBonuses = Optional.of(new PackedPermanentBonuses(
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf)
                ));
            }
        }

        // Decode combat stats data
        if ((attachmentMask & COMBAT_STATS_MASK) != 0) {
            combatStatsFieldMask = ByteBufCodecs.BYTE.decode(buf);

            if ((combatStatsFieldMask & COMBAT_ALL) != 0) {
                combatStats = Optional.of(new PackedCombatStats(
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf)
                ));
            }
        }

        return new UnifiedDeltaSyncPacket(
            attachmentMask, cultivationFieldMask, resourceFieldMask, combatStatsFieldMask,
            currentQiRealmId, currentBodyRealmId, currentQiCultivation, maxQiCultivation,
            currentBodyCultivation, maxBodyCultivation, currentFocus, bonuses, breakthroughData,
            currentHealth, currentQi, currentBody, permanentBonuses, combatStats
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(UnifiedDeltaSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            // Update cultivation data if present
            if ((packet.attachmentMask & CULTIVATION_MASK) != 0) {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null) {
                    cultivation.applyDeltaSync(packet);
                }
            }

            // Update resource data if present
            if ((packet.attachmentMask & RESOURCE_MASK) != 0) {
                PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
                if (resources != null) {
                    resources.applyDeltaSync(packet);
                }
            }

            // Update combat stats data if present
            if ((packet.attachmentMask & COMBAT_STATS_MASK) != 0) {
                PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                if (combatStats != null) {
                    combatStats.applyDeltaSync(packet);
                }
            }
        });
    }
}