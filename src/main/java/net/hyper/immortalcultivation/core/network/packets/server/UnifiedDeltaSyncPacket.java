package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

/**
 * Unified delta sync packet for efficient player attachment synchronization.
 * Supports selective field syncing with bitmasks to minimize network traffic.
 * Uses bitmasking, compression, and selective field syncing for optimal performance.
 */
public record UnifiedDeltaSyncPacket(
    // Bitmask indicating which attachment types have updates
    byte attachmentMask,

    // Bitmasks for which fields are included for each attachment type
    short cultivationFieldMask,
    short resourceFieldMask,
    byte combatStatsFieldMask,
    byte raceFieldMask,
    byte talentFieldMask,
    byte pillConsumptionFieldMask,

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
    Optional<PackedCombatStats> combatStats,

    // Damage feedback data
    Optional<Long> damageAmount,
    Optional<String> damageSourceId,

    // Race data (only included if bits are set)
    Optional<String> raceType,
    Optional<Boolean> raceAssigned,

    // Talent data (only included if bits are set)
    Optional<String> spiritualRootType,
    Optional<Boolean> spiritualRootAssigned,
    Optional<String> physiqueType,
    Optional<Boolean> physiqueAssigned,

    // Pill consumption data (only included if bits are set)
    Optional<CompoundTag> pillConsumptionData

) implements CustomPacketPayload {

    public static final Type<UnifiedDeltaSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "unified_delta_sync")
    );

    // Attachment type masks
    public static final byte CULTIVATION_MASK = 1;
    public static final byte RESOURCE_MASK = 2;
    public static final byte COMBAT_STATS_MASK = 4;
    public static final byte DAMAGE_FEEDBACK_MASK = 8;
    public static final byte RACE_MASK = 16;
    public static final byte TALENT_MASK = 32;
    public static final byte PILL_CONSUMPTION_MASK = 64;

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

    // Damage feedback field masks
    public static final byte DAMAGE_AMOUNT = 1;
    public static final byte DAMAGE_SOURCE = 2;

    // Race field masks
    public static final byte RACE_TYPE = 1;
    public static final byte RACE_ASSIGNED = 2;
    public static final byte RACE_ALL = RACE_TYPE | RACE_ASSIGNED;

    // Talent field masks
    public static final byte TALENT_SPIRITUAL_ROOT_TYPE = 1;
    public static final byte TALENT_SPIRITUAL_ROOT_ASSIGNED = 2;
    public static final byte TALENT_PHYSIQUE_TYPE = 4;
    public static final byte TALENT_PHYSIQUE_ASSIGNED = 8;
    public static final byte TALENT_ALL = TALENT_SPIRITUAL_ROOT_TYPE | TALENT_SPIRITUAL_ROOT_ASSIGNED |
        TALENT_PHYSIQUE_TYPE | TALENT_PHYSIQUE_ASSIGNED;

    // Pill consumption field masks
    public static final byte PILL_CONSUMPTION_DATA = 1;

    // Packed data structures
    public record PackedBonuses(double qiAdditiveBonus, double qiMultiplicativeBonus,
                                double bodyAdditiveBonus, double bodyMultiplicativeBonus) {}

    public record PackedBreakthroughData(long qiBreakthroughAttempts, long bodyBreakthroughAttempts,
                                         boolean qiBreakthroughCooldown, boolean bodyBreakthroughCooldown,
                                         long qiBreakthroughAttempt, long bodyBreakthroughAttempt) {}

    public record PackedPermanentBonuses(long permanentHealthBonus, double permanentHealthRegenBonus,
                                         long permanentMaxQiBonus, double permanentQiRegenBonus,
                                         long permanentMaxBodyBonus, double permanentBodyRegenBonus) {}

    public record PackedCombatStats(double attackBonus, double speedBonus, double jumpBonus,
                                    double permanentAttackBonus, double permanentSpeedBonus, double permanentJumpBonus) {}

    public static final StreamCodec<ByteBuf, UnifiedDeltaSyncPacket> STREAM_CODEC = StreamCodec.of(
        UnifiedDeltaSyncPacket::encode,
        UnifiedDeltaSyncPacket::decode
    );

    private static void encode(ByteBuf buf, UnifiedDeltaSyncPacket packet) {
        // Write main bitmask
        ByteBufCodecs.BYTE.encode(buf, packet.attachmentMask);

        // Encode cultivation data
        if ((packet.attachmentMask & CULTIVATION_MASK) != 0) {
            ByteBufCodecs.SHORT.encode(buf, packet.cultivationFieldMask);

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
                    new PackedBreakthroughData(0, 0, false, false, 0, 0));
                ByteBufCodecs.VAR_LONG.encode(buf, bt.qiBreakthroughAttempts);
                ByteBufCodecs.VAR_LONG.encode(buf, bt.bodyBreakthroughAttempts);
                ByteBufCodecs.BOOL.encode(buf, bt.qiBreakthroughCooldown);
                ByteBufCodecs.BOOL.encode(buf, bt.bodyBreakthroughCooldown);
                ByteBufCodecs.VAR_LONG.encode(buf, bt.qiBreakthroughAttempt);
                ByteBufCodecs.VAR_LONG.encode(buf, bt.bodyBreakthroughAttempt);
            }
        }

        // Encode resource data
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
                PackedPermanentBonuses perm = packet.permanentBonuses.orElse(
                    new PackedPermanentBonuses(0, 0.0, 0, 0.0, 0, 0.0));
                ByteBufCodecs.VAR_LONG.encode(buf, perm.permanentHealthBonus);
                ByteBufCodecs.DOUBLE.encode(buf, perm.permanentHealthRegenBonus);
                ByteBufCodecs.VAR_LONG.encode(buf, perm.permanentMaxQiBonus);
                ByteBufCodecs.DOUBLE.encode(buf, perm.permanentQiRegenBonus);
                ByteBufCodecs.VAR_LONG.encode(buf, perm.permanentMaxBodyBonus);
                ByteBufCodecs.DOUBLE.encode(buf, perm.permanentBodyRegenBonus);
            }
        }

        // Encode combat stats data
        if ((packet.attachmentMask & COMBAT_STATS_MASK) != 0) {
            ByteBufCodecs.BYTE.encode(buf, packet.combatStatsFieldMask);

            if ((packet.combatStatsFieldMask & COMBAT_ALL) != 0) {
                PackedCombatStats combat = packet.combatStats.orElse(
                    new PackedCombatStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
                ByteBufCodecs.DOUBLE.encode(buf, combat.attackBonus);
                ByteBufCodecs.DOUBLE.encode(buf, combat.speedBonus);
                ByteBufCodecs.DOUBLE.encode(buf, combat.jumpBonus);
                ByteBufCodecs.DOUBLE.encode(buf, combat.permanentAttackBonus);
                ByteBufCodecs.DOUBLE.encode(buf, combat.permanentSpeedBonus);
                ByteBufCodecs.DOUBLE.encode(buf, combat.permanentJumpBonus);
            }
        }

        // Encode damage feedback data
        if ((packet.attachmentMask & DAMAGE_FEEDBACK_MASK) != 0) {
            byte damageFieldMask = 0;
            if (packet.damageAmount.isPresent()) damageFieldMask |= DAMAGE_AMOUNT;
            if (packet.damageSourceId.isPresent()) damageFieldMask |= DAMAGE_SOURCE;

            ByteBufCodecs.BYTE.encode(buf, damageFieldMask);

            if ((damageFieldMask & DAMAGE_AMOUNT) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.damageAmount.orElse(0L));
            }
            if ((damageFieldMask & DAMAGE_SOURCE) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.damageSourceId.orElse(""));
            }
        }

        // Encode race data
        if ((packet.attachmentMask & RACE_MASK) != 0) {
            ByteBufCodecs.BYTE.encode(buf, packet.raceFieldMask);

            if ((packet.raceFieldMask & RACE_TYPE) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.raceType.orElse("HUMAN"));
            }
            if ((packet.raceFieldMask & RACE_ASSIGNED) != 0) {
                ByteBufCodecs.BOOL.encode(buf, packet.raceAssigned.orElse(false));
            }
        }

        // Encode talent data
        if ((packet.attachmentMask & TALENT_MASK) != 0) {
            ByteBufCodecs.BYTE.encode(buf, packet.talentFieldMask);

            if ((packet.talentFieldMask & TALENT_SPIRITUAL_ROOT_TYPE) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.spiritualRootType.orElse("MIXED"));
            }
            if ((packet.talentFieldMask & TALENT_SPIRITUAL_ROOT_ASSIGNED) != 0) {
                ByteBufCodecs.BOOL.encode(buf, packet.spiritualRootAssigned.orElse(false));
            }
            if ((packet.talentFieldMask & TALENT_PHYSIQUE_TYPE) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.physiqueType.orElse("MORTAL"));
            }
            if ((packet.talentFieldMask & TALENT_PHYSIQUE_ASSIGNED) != 0) {
                ByteBufCodecs.BOOL.encode(buf, packet.physiqueAssigned.orElse(false));
            }
        }

        // Encode pill consumption data
        if ((packet.attachmentMask & PILL_CONSUMPTION_MASK) != 0) {
            ByteBufCodecs.BYTE.encode(buf, packet.pillConsumptionFieldMask);

            if ((packet.pillConsumptionFieldMask & PILL_CONSUMPTION_DATA) != 0) {
                ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.pillConsumptionData.orElse(new CompoundTag()));
            }
        }
    }

    private static UnifiedDeltaSyncPacket decode(ByteBuf buf) {
        // Read main bitmask
        byte attachmentMask = ByteBufCodecs.BYTE.decode(buf);

        // Initialize all variables
        short cultivationFieldMask = 0;
        short resourceFieldMask = 0;
        byte combatStatsFieldMask = 0;
        byte raceFieldMask = 0;
        byte talentFieldMask = 0;
        byte pillConsumptionFieldMask = 0;

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

        Optional<Long> damageAmount = Optional.empty();
        Optional<String> damageSourceId = Optional.empty();

        Optional<String> raceType = Optional.empty();
        Optional<Boolean> raceAssigned = Optional.empty();

        Optional<String> spiritualRootType = Optional.empty();
        Optional<Boolean> spiritualRootAssigned = Optional.empty();
        Optional<String> physiqueType = Optional.empty();
        Optional<Boolean> physiqueAssigned = Optional.empty();

        Optional<CompoundTag> pillConsumptionData = Optional.empty();

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
                String focusStr = ByteBufCodecs.STRING_UTF8.decode(buf);
                try {
                    currentFocus = Optional.of(PlayerCultivationAttachment.CultivationType.valueOf(focusStr));
                } catch (IllegalArgumentException e) {
                    currentFocus = Optional.of(PlayerCultivationAttachment.CultivationType.QI);
                }
            }
            if ((cultivationFieldMask & CULT_BONUSES) != 0) {
                bonuses = Optional.of(new PackedBonuses(
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf)
                ));
            }
            if ((cultivationFieldMask & CULT_BREAKTHROUGH) != 0) {
                breakthroughData = Optional.of(new PackedBreakthroughData(
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
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
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf)
                ));
            }
        }

        // Decode combat stats data
        if ((attachmentMask & COMBAT_STATS_MASK) != 0) {
            combatStatsFieldMask = ByteBufCodecs.BYTE.decode(buf);

            if ((combatStatsFieldMask & COMBAT_ALL) != 0) {
                combatStats = Optional.of(new PackedCombatStats(
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf)
                ));
            }
        }

        // Decode damage feedback data
        if ((attachmentMask & DAMAGE_FEEDBACK_MASK) != 0) {
            byte damageFieldMask = ByteBufCodecs.BYTE.decode(buf);

            if ((damageFieldMask & DAMAGE_AMOUNT) != 0) {
                damageAmount = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((damageFieldMask & DAMAGE_SOURCE) != 0) {
                damageSourceId = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
        }

        // Decode race data
        if ((attachmentMask & RACE_MASK) != 0) {
            raceFieldMask = ByteBufCodecs.BYTE.decode(buf);

            if ((raceFieldMask & RACE_TYPE) != 0) {
                raceType = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            if ((raceFieldMask & RACE_ASSIGNED) != 0) {
                raceAssigned = Optional.of(ByteBufCodecs.BOOL.decode(buf));
            }
        }

        // Decode talent data
        if ((attachmentMask & TALENT_MASK) != 0) {
            talentFieldMask = ByteBufCodecs.BYTE.decode(buf);

            if ((talentFieldMask & TALENT_SPIRITUAL_ROOT_TYPE) != 0) {
                spiritualRootType = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            if ((talentFieldMask & TALENT_SPIRITUAL_ROOT_ASSIGNED) != 0) {
                spiritualRootAssigned = Optional.of(ByteBufCodecs.BOOL.decode(buf));
            }
            if ((talentFieldMask & TALENT_PHYSIQUE_TYPE) != 0) {
                physiqueType = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            if ((talentFieldMask & TALENT_PHYSIQUE_ASSIGNED) != 0) {
                physiqueAssigned = Optional.of(ByteBufCodecs.BOOL.decode(buf));
            }
        }

        // Decode pill consumption data
        if ((attachmentMask & PILL_CONSUMPTION_MASK) != 0) {
            pillConsumptionFieldMask = ByteBufCodecs.BYTE.decode(buf);

            if ((pillConsumptionFieldMask & PILL_CONSUMPTION_DATA) != 0) {
                pillConsumptionData = Optional.of(ByteBufCodecs.COMPOUND_TAG.decode(buf));
            }
        }

        return new UnifiedDeltaSyncPacket(
            attachmentMask, cultivationFieldMask, resourceFieldMask, combatStatsFieldMask,
            raceFieldMask, talentFieldMask, pillConsumptionFieldMask,
            currentQiRealmId, currentBodyRealmId, currentQiCultivation, maxQiCultivation,
            currentBodyCultivation, maxBodyCultivation, currentFocus, bonuses, breakthroughData,
            currentHealth, currentQi, currentBody, permanentBonuses, combatStats,
            damageAmount, damageSourceId,
            raceType, raceAssigned,
            spiritualRootType, spiritualRootAssigned, physiqueType, physiqueAssigned,
            pillConsumptionData
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(UnifiedDeltaSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                packet.applyDeltaSync(player);
            }
        });
    }

    /**
     * Apply delta sync changes to client-side attachments.
     * Only updates fields that are marked as changed in the bitmasks.
     */
    private void applyDeltaSync(Player player) {
        try {
            // Apply cultivation changes
            if ((attachmentMask & CULTIVATION_MASK) != 0) {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null) {
                    applyCultivationDelta(cultivation);
                }
            }

            // Apply resource changes
            if ((attachmentMask & RESOURCE_MASK) != 0) {
                PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
                if (resources != null) {
                    applyResourceDelta(resources);
                }
            }

            // Apply combat stats changes
            if ((attachmentMask & COMBAT_STATS_MASK) != 0) {
                PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                if (combatStats != null) {
                    applyCombatStatsDelta(combatStats);
                }
            }

            // Apply damage feedback effects
            if ((attachmentMask & DAMAGE_FEEDBACK_MASK) != 0) {
                applyDamageFeedback(player);
            }

            // Apply race changes
            if ((attachmentMask & RACE_MASK) != 0) {
                PlayerRaceAttachment raceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
                if (raceAttachment != null) {
                    applyRaceDelta(raceAttachment);
                }
            }

            // Apply talent changes
            if ((attachmentMask & TALENT_MASK) != 0) {
                PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
                if (talentAttachment != null) {
                    applyTalentDelta(talentAttachment);
                }
            }

            // Apply pill consumption changes
            if ((attachmentMask & PILL_CONSUMPTION_MASK) != 0) {
                PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());
                if (pillAttachment != null) {
                    applyPillConsumptionDelta(pillAttachment);
                }
            }

            ImmortalCultivation.LOGGER.debug("Applied unified delta sync for player: {}", player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying unified delta sync for player {}: {}",
                player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Apply cultivation-specific delta changes
     */
    private void applyCultivationDelta(PlayerCultivationAttachment cultivation) {
        // Realm changes - need to look up realm by ID, not set ID directly
        if ((cultivationFieldMask & CULT_QI_REALM) != 0 && currentQiRealmId.isPresent()) {
            CultivationRealm qiRealm = ICModCultivationRealms.get(currentQiRealmId.get());
            if (qiRealm != null) {
                cultivation.setCurrentRealm(ICultivationData.CultivationType.QI, qiRealm);
            }
        }

        if ((cultivationFieldMask & CULT_BODY_REALM) != 0 && currentBodyRealmId.isPresent()) {
            CultivationRealm bodyRealm = ICModCultivationRealms.get(currentBodyRealmId.get());
            if (bodyRealm != null) {
                cultivation.setCurrentRealm(ICultivationData.CultivationType.BODY, bodyRealm);
            }
        }

        // Cultivation progress changes
        if ((cultivationFieldMask & CULT_QI_CURRENT) != 0 && currentQiCultivation.isPresent()) {
            cultivation.setCurrentCultivation(ICultivationData.CultivationType.QI, currentQiCultivation.get());
        }

        if ((cultivationFieldMask & CULT_QI_MAX) != 0 && maxQiCultivation.isPresent()) {
            cultivation.setMaxCultivation(ICultivationData.CultivationType.QI, maxQiCultivation.get());
        }

        if ((cultivationFieldMask & CULT_BODY_CURRENT) != 0 && currentBodyCultivation.isPresent()) {
            cultivation.setCurrentCultivation(ICultivationData.CultivationType.BODY, currentBodyCultivation.get());
        }

        if ((cultivationFieldMask & CULT_BODY_MAX) != 0 && maxBodyCultivation.isPresent()) {
            cultivation.setMaxCultivation(ICultivationData.CultivationType.BODY, maxBodyCultivation.get());
        }

        // Focus changes
        if ((cultivationFieldMask & CULT_FOCUS) != 0 && currentFocus.isPresent()) {
            cultivation.setCurrentFocus(currentFocus.get());
        }

        // Bonus changes
        if ((cultivationFieldMask & CULT_BONUSES) != 0 && bonuses.isPresent()) {
            PackedBonuses b = bonuses.get();
            cultivation.setRawAdditiveBonus(ICultivationData.CultivationType.QI, b.qiAdditiveBonus);
            cultivation.setRawMultiplicativeBonus(ICultivationData.CultivationType.QI, b.qiMultiplicativeBonus);
            cultivation.setRawAdditiveBonus(ICultivationData.CultivationType.BODY, b.bodyAdditiveBonus);
            cultivation.setRawMultiplicativeBonus(ICultivationData.CultivationType.BODY, b.bodyMultiplicativeBonus);
        }
    }

    /**
     * Apply resource-specific delta changes
     */
    private void applyResourceDelta(PlayerResourceAttachment resources) {
        if ((resourceFieldMask & RES_HEALTH) != 0 && currentHealth.isPresent()) {
            resources.setCurrentHealth(currentHealth.get());
        }
        if ((resourceFieldMask & RES_QI) != 0 && currentQi.isPresent()) {
            resources.setCurrentQi(currentQi.get());
        }
        if ((resourceFieldMask & RES_BODY) != 0 && currentBody.isPresent()) {
            resources.setCurrentBody(currentBody.get());
        }
        if ((resourceFieldMask & RES_PERMANENT) != 0 && permanentBonuses.isPresent()) {
            PackedPermanentBonuses p = permanentBonuses.get();
            resources.setPermanentHealthBonus(p.permanentHealthBonus);
            resources.setPermanentHealthRegenBonus(p.permanentHealthRegenBonus);
            resources.setPermanentMaxQiBonus(p.permanentMaxQiBonus);
            resources.setPermanentQiRegenBonus(p.permanentQiRegenBonus);
            resources.setPermanentMaxBodyBonus(p.permanentMaxBodyBonus);
            resources.setPermanentBodyRegenBonus(p.permanentBodyRegenBonus);
        }
    }

    /**
     * Apply combat stats delta changes
     */
    private void applyCombatStatsDelta(PlayerCombatStatsAttachment combatStats) {
        if ((combatStatsFieldMask & COMBAT_ALL) != 0 && this.combatStats.isPresent()) {
            PackedCombatStats c = this.combatStats.get();
            combatStats.updateFromServer(
                c.attackBonus, c.speedBonus, c.jumpBonus,
                c.permanentAttackBonus, c.permanentSpeedBonus, c.permanentJumpBonus
            );
        }
    }

    /**
     * Apply damage feedback effects
     */
    private void applyDamageFeedback(Player player) {
        // This would need to be implemented based on your damage feedback system
        // The structure is ready for future implementation
    }

    /**
     * Apply race-specific delta changes
     */
    private void applyRaceDelta(PlayerRaceAttachment raceAttachment) {
        if ((raceFieldMask & RACE_TYPE) != 0 && raceType.isPresent()) {
            try {
                RaceType type = RaceType.valueOf(raceType.get());
                if ((raceFieldMask & RACE_ASSIGNED) != 0 && raceAssigned.isPresent() && raceAssigned.get()) {
                    raceAttachment.updateFromServer(type, true);
                }
            } catch (IllegalArgumentException e) {
                ImmortalCultivation.LOGGER.warn("Invalid race type received in sync: {}", raceType.get());
            }
        }
    }

    /**
     * Apply talent-specific delta changes
     */
    private void applyTalentDelta(PlayerTalentAttachment talentAttachment) {
        // Apply spiritual root changes
        if ((talentFieldMask & TALENT_SPIRITUAL_ROOT_TYPE) != 0 && spiritualRootType.isPresent()) {
            try {
                SpiritualRootType rootType = SpiritualRootType.valueOf(spiritualRootType.get());
                if ((talentFieldMask & TALENT_SPIRITUAL_ROOT_ASSIGNED) != 0 &&
                    spiritualRootAssigned.isPresent() && spiritualRootAssigned.get()) {
                    talentAttachment.forceReassignSpiritualRoot(rootType);
                }
            } catch (IllegalArgumentException e) {
                ImmortalCultivation.LOGGER.warn("Invalid spiritual root type received in sync: {}", spiritualRootType.get());
            }
        }

        // Apply physique changes
        if ((talentFieldMask & TALENT_PHYSIQUE_TYPE) != 0 && physiqueType.isPresent()) {
            try {
                PhysiqueType physType = PhysiqueType.valueOf(physiqueType.get());
                if ((talentFieldMask & TALENT_PHYSIQUE_ASSIGNED) != 0 &&
                    physiqueAssigned.isPresent() && physiqueAssigned.get()) {
                    talentAttachment.forceReassignPhysique(physType);
                }
            } catch (IllegalArgumentException e) {
                ImmortalCultivation.LOGGER.warn("Invalid physique type received in sync: {}", physiqueType.get());
            }
        }
    }

    /**
     * Apply pill consumption delta changes
     */
    private void applyPillConsumptionDelta(PlayerPillConsumptionAttachment pillAttachment) {
        if ((pillConsumptionFieldMask & PILL_CONSUMPTION_DATA) != 0 && pillConsumptionData.isPresent()) {
            pillAttachment.applySync(pillConsumptionData.get());
        }
    }

    // === Add these static factory methods to UnifiedDeltaSyncPacket.java ===
// Place these methods inside the UnifiedDeltaSyncPacket class

    /**
     * Creates a damage feedback packet for visual/audio damage effects on the client.
     * This is used for hit indicators, damage numbers, screen effects, etc.
     *
     * @param damageAmount The amount of damage dealt
     * @param damageSourceId Optional identifier for the damage source (for different effects)
     * @return A UnifiedDeltaSyncPacket configured for damage feedback
     */
    public static UnifiedDeltaSyncPacket createDamageFeedbackPacket(long damageAmount, String damageSourceId) {
        byte attachmentMask = DAMAGE_FEEDBACK_MASK;
        byte damageFieldMask = DAMAGE_AMOUNT | DAMAGE_SOURCE;

        return new UnifiedDeltaSyncPacket(
            attachmentMask,
            (short) 0, (short) 0, (byte) 0, // No other field masks
            (byte) 0, (byte) 0, (byte) 0, // No other attachment masks

            // All cultivation data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All resource data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All combat stats data empty
            Optional.empty(),

            // Damage feedback data (populated)
            Optional.of(damageAmount),
            Optional.of(damageSourceId),

            // All race data empty
            Optional.empty(), Optional.empty(),

            // All talent data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // No pill consumption data
            Optional.empty()
        );
    }

    /**
     * Creates a damage feedback packet with only damage amount (no source ID).
     *
     * @param damageAmount The amount of damage dealt
     * @return A UnifiedDeltaSyncPacket configured for damage feedback
     */
    public static UnifiedDeltaSyncPacket createDamageFeedbackPacket(long damageAmount) {
        byte attachmentMask = DAMAGE_FEEDBACK_MASK;
        byte damageFieldMask = DAMAGE_AMOUNT;

        return new UnifiedDeltaSyncPacket(
            attachmentMask,
            (short) 0, (short) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0,

            // All cultivation data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All resource data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All combat stats data empty
            Optional.empty(),

            // Damage feedback data (only amount)
            Optional.of(damageAmount),
            Optional.empty(), // No damage source

            // All race data empty
            Optional.empty(), Optional.empty(),

            // All talent data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // No pill consumption data
            Optional.empty()
        );
    }

    /**
     * Creates a packet that only syncs health data (useful for health changes without full resource sync).
     *
     * @param currentHealth The current health value
     * @return A UnifiedDeltaSyncPacket configured for health only
     */
    public static UnifiedDeltaSyncPacket createHealthOnlyPacket(long currentHealth) {
        byte attachmentMask = RESOURCE_MASK;
        short resourceFieldMask = RES_HEALTH;

        return new UnifiedDeltaSyncPacket(
            attachmentMask,
            (short) 0, resourceFieldMask, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0,

            // All cultivation data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // Only health resource data
            Optional.of(currentHealth),
            Optional.empty(), Optional.empty(), Optional.empty(),

            // All combat stats data empty
            Optional.empty(),

            // No damage feedback data
            Optional.empty(), Optional.empty(),

            // All race data empty
            Optional.empty(), Optional.empty(),

            // All talent data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // No pill consumption data
            Optional.empty()
        );
    }

    /**
     * Creates a packet that only syncs cultivation focus changes.
     *
     * @param newFocus The new cultivation focus
     * @return A UnifiedDeltaSyncPacket configured for focus change only
     */
    public static UnifiedDeltaSyncPacket createFocusChangePacket(PlayerCultivationAttachment.CultivationType newFocus) {
        byte attachmentMask = CULTIVATION_MASK;
        short cultivationFieldMask = CULT_FOCUS;

        return new UnifiedDeltaSyncPacket(
            attachmentMask,
            cultivationFieldMask, (short) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0,

            // Only focus cultivation data
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.of(newFocus), Optional.empty(), Optional.empty(),

            // All resource data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All combat stats data empty
            Optional.empty(),

            // No damage feedback data
            Optional.empty(), Optional.empty(),

            // All race data empty
            Optional.empty(), Optional.empty(),

            // All talent data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // No pill consumption data
            Optional.empty()
        );
    }

    /**
     * Creates a packet for realm breakthrough notifications.
     *
     * @param qiRealmId The new Qi realm ID (if changed)
     * @param bodyRealmId The new Body realm ID (if changed)
     * @return A UnifiedDeltaSyncPacket configured for realm changes
     */
    public static UnifiedDeltaSyncPacket createRealmChangePacket(String qiRealmId, String bodyRealmId) {
        byte attachmentMask = CULTIVATION_MASK;
        short cultivationFieldMask = 0;

        if (qiRealmId != null) cultivationFieldMask |= CULT_QI_REALM;
        if (bodyRealmId != null) cultivationFieldMask |= CULT_BODY_REALM;

        return new UnifiedDeltaSyncPacket(
            attachmentMask,
            cultivationFieldMask, (short) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0,

            // Realm cultivation data
            qiRealmId != null ? Optional.of(qiRealmId) : Optional.empty(),
            bodyRealmId != null ? Optional.of(bodyRealmId) : Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(),

            // All resource data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // All combat stats data empty
            Optional.empty(),

            // No damage feedback data
            Optional.empty(), Optional.empty(),

            // All race data empty
            Optional.empty(), Optional.empty(),

            // All talent data empty
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),

            // No pill consumption data
            Optional.empty()
        );
    }
}