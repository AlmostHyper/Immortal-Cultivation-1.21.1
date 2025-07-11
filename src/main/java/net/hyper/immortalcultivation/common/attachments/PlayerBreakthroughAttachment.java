package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.interfaces.ITickable;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerBreakthroughSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlayerBreakthroughAttachment implements ISyncable, ITickable {

    // === CONSTANTS === //
    private static final long BREAKTHROUGH_COOLDOWN_TICKS = 600L; // 30 seconds at 20 TPS
    private static final double MAX_SUCCESS_RATE = 0.95; // 95% max
    private static final double MIN_SUCCESS_RATE = 0.05; // 5% min
    private static final double FAILURE_BONUS_PER_ATTEMPT = 0.05; // 5% per failed attempt

    // === INSTANCES === //
    @Nullable
    private transient Player player;
    private transient boolean dirty = false;

    // --- Breakthrough Data ---
    private long qiBreakthroughAttempts = 0L;
    private long bodyBreakthroughAttempts = 0L;
    private boolean qiBreakthroughCooldown = false;
    private boolean bodyBreakthroughCooldown = false;
    private long lastQiBreakthroughAttempt = 0L;
    private long lastBodyBreakthroughAttempt = 0L;

    // === CONSTRUCTORS === //
    public PlayerBreakthroughAttachment() {}

    public PlayerBreakthroughAttachment(@Nullable Player player) {
        this.player = player;
    }

    private PlayerBreakthroughAttachment(long qiAttempts, long bodyAttempts, boolean qiCooldown, boolean bodyCooldown, long lastQiAttempt, long lastBodyAttempt) {
        this.qiBreakthroughAttempts = qiAttempts;
        this.bodyBreakthroughAttempts = bodyAttempts;
        this.qiBreakthroughCooldown = qiCooldown;
        this.bodyBreakthroughCooldown = bodyCooldown;
        this.lastQiBreakthroughAttempt = lastQiAttempt;
        this.lastBodyBreakthroughAttempt = lastBodyAttempt;
    }

    // === GETTERS & SETTERS === //
    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayerSafe() {
        return player;
    }

    public long getQiBreakthroughAttempts() {
        return qiBreakthroughAttempts;
    }

    public void setQiBreakthroughAttempts(long qiBreakthroughAttempts) {
        if (this.qiBreakthroughAttempts != qiBreakthroughAttempts) {
            this.qiBreakthroughAttempts = qiBreakthroughAttempts;
            markDirty();
        }
    }

    public long getBodyBreakthroughAttempts() {
        return bodyBreakthroughAttempts;
    }

    public void setBodyBreakthroughAttempts(long bodyBreakthroughAttempts) {
        if (this.bodyBreakthroughAttempts != bodyBreakthroughAttempts) {
            this.bodyBreakthroughAttempts = bodyBreakthroughAttempts;
            markDirty();
        }
    }

    public boolean isQiBreakthroughCooldown() {
        return qiBreakthroughCooldown;
    }

    public void setQiBreakthroughCooldown(boolean qiBreakthroughCooldown) {
        if (this.qiBreakthroughCooldown != qiBreakthroughCooldown) {
            this.qiBreakthroughCooldown = qiBreakthroughCooldown;
            markDirty();
        }
    }

    public boolean isBodyBreakthroughCooldown() {
        return bodyBreakthroughCooldown;
    }

    public void setBodyBreakthroughCooldown(boolean bodyBreakthroughCooldown) {
        if (this.bodyBreakthroughCooldown != bodyBreakthroughCooldown) {
            this.bodyBreakthroughCooldown = bodyBreakthroughCooldown;
            markDirty();
        }
    }

    public long getLastQiBreakthroughAttempt() {
        return lastQiBreakthroughAttempt;
    }

    public void setLastQiBreakthroughAttempt(long lastQiBreakthroughAttempt) {
        if (this.lastQiBreakthroughAttempt != lastQiBreakthroughAttempt) {
            this.lastQiBreakthroughAttempt = lastQiBreakthroughAttempt;
            markDirty();
        }
    }

    public long getLastBodyBreakthroughAttempt() {
        return lastBodyBreakthroughAttempt;
    }

    public void setLastBodyBreakthroughAttempt(long lastBodyBreakthroughAttempt) {
        if (this.lastBodyBreakthroughAttempt != lastBodyBreakthroughAttempt) {
            this.lastBodyBreakthroughAttempt = lastBodyBreakthroughAttempt;
            markDirty();
        }
    }

    // === CORE LOGIC === //
    public BreakthroughResult attemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return new BreakthroughResult(false, "No player reference", null, breakthroughType, 0.0, 0L);
        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) return new BreakthroughResult(false, "Cultivation system unavailable", null, breakthroughType, 0.0, 0L);

        if (!canAttemptTypedBreakthrough(cultivationType, breakthroughType)) {
            String reason = getUnavailabilityReason(cultivationType, breakthroughType);
            return new BreakthroughResult(false, reason, null, breakthroughType, 0.0, getBreakthroughAttempts(cultivationType));
        }

        CultivationRealm currentRealm = cultivation.getRealm(cultivationType);
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();
        if (nextRealms.isEmpty()) return new BreakthroughResult(false, "No higher realm available", null, breakthroughType, 0.0, getBreakthroughAttempts(cultivationType));

        CultivationRealm targetRealm = nextRealms.get(0);
        double successChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);
        long currentAttempts;

        if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
            setLastQiBreakthroughAttempt(player.level().getGameTime());
            currentAttempts = this.qiBreakthroughAttempts;
        } else {
            setLastBodyBreakthroughAttempt(player.level().getGameTime());
            currentAttempts = this.bodyBreakthroughAttempts;
        }

        boolean success = player.getRandom().nextDouble() < successChance;
        if (success) {
            cultivation.setRealm(cultivationType, targetRealm);
            if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) setQiBreakthroughAttempts(0L);
            else setBodyBreakthroughAttempts(0L);
            String message = String.format("%s %s breakthrough successful! (%d attempts, %.0f%% chance)", cultivationType.getDisplayName(), breakthroughType.getDisplayName(), currentAttempts, successChance * 100);
            ImmortalCultivation.LOGGER.info("{} {} breakthrough SUCCESS for {} after {} attempts ({}% chance)", cultivationType.getDisplayName(), breakthroughType.getDisplayName(), player.getName().getString(), currentAttempts, (int) (successChance * 100));
            return new BreakthroughResult(true, message, targetRealm, breakthroughType, successChance, currentAttempts);
        } else {
            if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
                setQiBreakthroughAttempts(this.qiBreakthroughAttempts + 1);
                setQiBreakthroughCooldown(true);
            } else {
                setBodyBreakthroughAttempts(this.bodyBreakthroughAttempts + 1);
                setBodyBreakthroughCooldown(true);
            }
            double nextChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);
            String message = String.format("%s %s breakthrough failed (attempt %d, %.0f%% chance). Next: %.0f%%", cultivationType.getDisplayName(), breakthroughType.getDisplayName(), currentAttempts + 1, successChance * 100, nextChance * 100);
            ImmortalCultivation.LOGGER.info("{} {} breakthrough FAILED for {} (attempt {}, {}% chance)", cultivationType.getDisplayName(), breakthroughType.getDisplayName(), player.getName().getString(), currentAttempts + 1, (int) (successChance * 100));
            return new BreakthroughResult(false, message, null, breakthroughType, successChance, currentAttempts + 1);
        }
    }

    public double calculateBreakthroughSuccessChance(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        double baseChance = breakthroughType.getBaseSuccessChance();
        long attempts = getBreakthroughAttempts(cultivationType);
        double failureBonus = attempts * FAILURE_BONUS_PER_ATTEMPT;
        double talentBonus = calculateTalentBonus(cultivationType);
        double focusBonus = calculateFocusBonus(cultivationType);
        return Math.min(MAX_SUCCESS_RATE, Math.max(MIN_SUCCESS_RATE, baseChance + failureBonus + talentBonus + focusBonus));
    }

    public boolean canAttemptTypedBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return false;
        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null || !cultivation.canBreakthrough(cultivationType) || isBreakthroughOnCooldown(cultivationType)) return false;
        return !cultivation.getRealm(cultivationType).getNextCultivationRealms().isEmpty();
    }

    // === IMPLEMENTATIONS === //
    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        if (player == null) {
            return Optional.empty();
        }

        CompoundTag data = new CompoundTag();
        data.putLong("qi_attempts", this.qiBreakthroughAttempts);
        data.putLong("body_attempts", this.bodyBreakthroughAttempts);
        data.putBoolean("qi_cooldown", this.qiBreakthroughCooldown);
        data.putBoolean("body_cooldown", this.bodyBreakthroughCooldown);
        data.putLong("last_qi_attempt", this.lastQiBreakthroughAttempt);
        data.putLong("last_body_attempt", this.lastBodyBreakthroughAttempt);

        return Optional.of(new PlayerBreakthroughSyncPacket(data));
    }

    @Override
    public void tick() {
        updateCooldowns();
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    @Override
    public void clearDirty() {
        this.dirty = false;
    }

    // === HELPER METHODS === //
    public void applySync(CompoundTag data) {
        this.qiBreakthroughAttempts = data.getLong("qi_attempts");
        this.bodyBreakthroughAttempts = data.getLong("body_attempts");
        this.qiBreakthroughCooldown = data.getBoolean("qi_cooldown");
        this.bodyBreakthroughCooldown = data.getBoolean("body_cooldown");
        this.lastQiBreakthroughAttempt = data.getLong("last_qi_attempt");
        this.lastBodyBreakthroughAttempt = data.getLong("last_body_attempt");
    }

    private PlayerCultivationAttachment getCultivationAttachment() {
        if (player == null) return null;
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivation != null && cultivation.getPlayerSafe() == null) {
            cultivation.setPlayer(player);
        }
        return cultivation;
    }

    private PlayerTalentsAttachment getTalentAttachment() {
        if (player == null) return null;
        return player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
    }

    private double calculateTalentBonus(PlayerCultivationAttachment.CultivationType cultivationType) {
        return 0.0;
    }

    private double calculateFocusBonus(PlayerCultivationAttachment.CultivationType cultivationType) {
        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) return 0.0;
        return (cultivation.getCurrentFocus() == cultivationType) ? 0.05 : 0.0;
    }

    private String getUnavailabilityReason(PlayerCultivationAttachment.CultivationType cultivationType, BreakthroughType breakthroughType) {
        if (player == null) return "No player reference";
        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) return "Cultivation system unavailable";
        if (!cultivation.canBreakthrough(cultivationType)) return cultivationType.getDisplayName() + " cultivation not at maximum";
        if (isBreakthroughOnCooldown(cultivationType)) return cultivationType.getDisplayName() + " breakthrough on cooldown";
        if (cultivation.getRealm(cultivationType).getNextCultivationRealms().isEmpty()) return "No higher realm available";
        return "Requirements not met";
    }

    public BreakthroughOptions getAllAvailableBreakthroughOptions() {
        List<BreakthroughOption> qiOptions = new ArrayList<>();
        List<BreakthroughOption> bodyOptions = new ArrayList<>();

        PlayerCultivationAttachment cultivation = getCultivationAttachment();
        if (cultivation == null) {
            ImmortalCultivation.LOGGER.warn("Cannot get breakthrough options: cultivation attachment unavailable");
            return new BreakthroughOptions(qiOptions, bodyOptions);
        }

        qiOptions.addAll(generateBreakthroughOptionsForType(PlayerCultivationAttachment.CultivationType.QI, cultivation));
        bodyOptions.addAll(generateBreakthroughOptionsForType(PlayerCultivationAttachment.CultivationType.BODY, cultivation));

        return new BreakthroughOptions(qiOptions, bodyOptions);
    }

    private List<BreakthroughOption> generateBreakthroughOptionsForType(
            PlayerCultivationAttachment.CultivationType cultivationType,
            PlayerCultivationAttachment cultivation) {
        List<BreakthroughOption> options = new ArrayList<>();
        for (BreakthroughType breakthroughType : BreakthroughType.values()) {
            BreakthroughOption option = createBreakthroughOption(cultivationType, breakthroughType, cultivation);
            if (option != null) {
                options.add(option);
            }
        }
        return options;
    }

    private BreakthroughOption createBreakthroughOption(
            PlayerCultivationAttachment.CultivationType cultivationType,
            BreakthroughType breakthroughType,
            PlayerCultivationAttachment cultivation) {
        CultivationRealm currentRealm = cultivation.getRealm(
                cultivationType == PlayerCultivationAttachment.CultivationType.QI ?
                        ICultivationData.CultivationType.QI : ICultivationData.CultivationType.BODY
        );

        if (currentRealm == null) {
            ImmortalCultivation.LOGGER.warn("Current realm is null for cultivation type: {}", cultivationType);
            return null;
        }

        CultivationRealm targetRealm = determineTargetRealm(currentRealm, breakthroughType, cultivation, cultivationType);
        if (targetRealm == null) {
            return null;
        }

        double successChance = calculateBreakthroughSuccessChance(cultivationType, breakthroughType);
        boolean available = canAttemptTypedBreakthrough(cultivationType, breakthroughType);
        String unavailabilityReason = available ? "" : getUnavailabilityReason(cultivationType, breakthroughType);

        return new BreakthroughOption(
                breakthroughType,
                targetRealm,
                successChance,
                cultivationType,
                available,
                unavailabilityReason
        );
    }

    private CultivationRealm determineTargetRealm(
            CultivationRealm currentRealm,
            BreakthroughType breakthroughType,
            PlayerCultivationAttachment cultivation,
            PlayerCultivationAttachment.CultivationType cultivationType) {
        ICultivationData.CultivationType dataType = cultivationType == PlayerCultivationAttachment.CultivationType.QI ?
                ICultivationData.CultivationType.QI : ICultivationData.CultivationType.BODY;
        return switch (breakthroughType) {
            case MINOR -> currentRealm.getMinorBreakthrough(player, cultivation, dataType);
            case MAJOR -> currentRealm.getMajorBreakthrough(player, cultivation, dataType);
            case SPECIAL -> currentRealm.getSpecialBreakthrough(player, cultivation, dataType);
            case BOUNDARY -> currentRealm.getBoundaryBreakthrough(player, cultivation, dataType);
        };
    }

    private void updateCooldowns() {
        if (player == null || player.level().isClientSide()) return;
        long currentTime = player.level().getGameTime();
        if (isQiBreakthroughCooldown() && (currentTime - getLastQiBreakthroughAttempt()) >= BREAKTHROUGH_COOLDOWN_TICKS) {
            setQiBreakthroughCooldown(false);
        }
        if (isBodyBreakthroughCooldown() && (currentTime - getLastBodyBreakthroughAttempt()) >= BREAKTHROUGH_COOLDOWN_TICKS) {
            setBodyBreakthroughCooldown(false);
        }
    }

    public long getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType type) {
        return (type == PlayerCultivationAttachment.CultivationType.QI) ? qiBreakthroughAttempts : bodyBreakthroughAttempts;
    }

    public boolean isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType type) {
        return (type == PlayerCultivationAttachment.CultivationType.QI) ? qiBreakthroughCooldown : bodyBreakthroughCooldown;
    }

    // === SERIALIZATION === //
    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerBreakthroughAttachment> {
        @Override
        public PlayerBreakthroughAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerBreakthroughAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerBreakthroughAttachment: {}", error))
                    .map(com.mojang.datafixers.util.Pair::getFirst)
                    .orElseGet(PlayerBreakthroughAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }
            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerBreakthroughAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerBreakthroughAttachment: {}", error))
                    .orElse(new CompoundTag());
        }
    }

    // === CODEC === //
    public static final Codec<PlayerBreakthroughAttachment> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.optionalFieldOf("qi_attempts", 0L).forGetter(PlayerBreakthroughAttachment::getQiBreakthroughAttempts),
                    Codec.LONG.optionalFieldOf("body_attempts", 0L).forGetter(PlayerBreakthroughAttachment::getBodyBreakthroughAttempts),
                    Codec.BOOL.optionalFieldOf("qi_cooldown", false).forGetter(PlayerBreakthroughAttachment::isQiBreakthroughCooldown),
                    Codec.BOOL.optionalFieldOf("body_cooldown", false).forGetter(PlayerBreakthroughAttachment::isBodyBreakthroughCooldown),
                    Codec.LONG.optionalFieldOf("last_qi_attempt", 0L).forGetter(PlayerBreakthroughAttachment::getLastQiBreakthroughAttempt),
                    Codec.LONG.optionalFieldOf("last_body_attempt", 0L).forGetter(PlayerBreakthroughAttachment::getLastBodyBreakthroughAttempt)
            ).apply(instance, PlayerBreakthroughAttachment::new)
    );

    // Enum and Record definitions for Breakthrough
    public enum BreakthroughType {
        MINOR(0.50, "Minor Breakthrough"),
        MAJOR(0.30, "Major Breakthrough"),
        SPECIAL(0.20, "Special Breakthrough"),
        BOUNDARY(0.10, "Boundary Breakthrough");

        private final double baseSuccessChance;
        private final String displayName;

        BreakthroughType(double baseSuccessChance, String displayName) {
            this.baseSuccessChance = baseSuccessChance;
            this.displayName = displayName;
        }

        public double getBaseSuccessChance() { return baseSuccessChance; }
        public String getDisplayName() { return displayName; }
    }

    public record BreakthroughResult(boolean success, String message, @Nullable CultivationRealm newRealm, BreakthroughType type, double successChance, long attemptNumber) {}
    public record BreakthroughOption(BreakthroughType type, CultivationRealm targetRealm, double successChance, PlayerCultivationAttachment.CultivationType cultivationType, boolean available, String unavailabilityReason) {}
    public record BreakthroughOptions(List<BreakthroughOption> qiOptions, List<BreakthroughOption> bodyOptions) {}
    public record BreakthroughChanceBreakdown(double baseChance, double failureBonus, double talentBonus, double focusBonus, double totalChance, long attempts) {}
}
