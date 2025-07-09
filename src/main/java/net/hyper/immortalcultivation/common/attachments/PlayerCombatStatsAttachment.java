package net.hyper.immortalcultivation.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.common.interfaces.ITickable;
import net.hyper.immortalcultivation.core.network.packets.server.PlayerCombatStatsSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.Optional;

public class PlayerCombatStatsAttachment implements ISyncable, ITickable {

    // === INSTANCES === //
    @Nullable
    private transient Player player;
    private transient boolean dirty = false;

    // --- Permanent Bonuses ---
    private double permanentAttackDamageBonus = 0.0;
    private double permanentMovementSpeedBonus = 0.0;
    private double permanentJumpHeightBonus = 0.0;

    // --- Temporary Bonuses ---
    private double attackDamageBonus = 0.0;
    private double movementSpeedBonus = 0.0;
    private double jumpHeightBonus = 0.0;

    // === CONSTRUCTORS === //
    public PlayerCombatStatsAttachment() {}

    public PlayerCombatStatsAttachment(@Nullable Player player) {
        this.player = player;
    }

    private PlayerCombatStatsAttachment(double pAttack, double pSpeed, double pJump, double tAttack, double tSpeed, double tJump) {
        this.permanentAttackDamageBonus = pAttack;
        this.permanentMovementSpeedBonus = pSpeed;
        this.permanentJumpHeightBonus = pJump;
        this.attackDamageBonus = tAttack;
        this.movementSpeedBonus = tSpeed;
        this.jumpHeightBonus = tJump;
    }

    // === GETTERS & SETTERS === //
    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayerSafe() {
        return player;
    }

    public double getPermanentAttackDamageBonus() {
        return permanentAttackDamageBonus;
    }

    public void setPermanentAttackDamageBonus(double permanentAttackDamageBonus) {
        if (this.permanentAttackDamageBonus != permanentAttackDamageBonus) {
            this.permanentAttackDamageBonus = permanentAttackDamageBonus;
            markDirty();
        }
    }

    public double getPermanentMovementSpeedBonus() {
        return permanentMovementSpeedBonus;
    }

    public void setPermanentMovementSpeedBonus(double permanentMovementSpeedBonus) {
        if (this.permanentMovementSpeedBonus != permanentMovementSpeedBonus) {
            this.permanentMovementSpeedBonus = permanentMovementSpeedBonus;
            markDirty();
        }
    }

    public double getPermanentJumpHeightBonus() {
        return permanentJumpHeightBonus;
    }

    public void setPermanentJumpHeightBonus(double permanentJumpHeightBonus) {
        if (this.permanentJumpHeightBonus != permanentJumpHeightBonus) {
            this.permanentJumpHeightBonus = permanentJumpHeightBonus;
            markDirty();
        }
    }

    public double getAttackDamageBonus() {
        return attackDamageBonus;
    }

    public void setAttackDamageBonus(double attackDamageBonus) {
        if (this.attackDamageBonus != attackDamageBonus) {
            this.attackDamageBonus = attackDamageBonus;
            markDirty();
        }
    }

    public double getMovementSpeedBonus() {
        return movementSpeedBonus;
    }

    public void setMovementSpeedBonus(double movementSpeedBonus) {
        if (this.movementSpeedBonus != movementSpeedBonus) {
            this.movementSpeedBonus = movementSpeedBonus;
            markDirty();
        }
    }

    public double getJumpHeightBonus() {
        return jumpHeightBonus;
    }

    public void setJumpHeightBonus(double jumpHeightBonus) {
        if (this.jumpHeightBonus != jumpHeightBonus) {
            this.jumpHeightBonus = jumpHeightBonus;
            markDirty();
        }
    }

    // === CORE LOGIC === //
    public double getEffectiveAttackDamageMultiplier() {
        return 1.0 + permanentAttackDamageBonus + attackDamageBonus;
    }

    public double getEffectiveMovementSpeedMultiplier() {
        return permanentMovementSpeedBonus + movementSpeedBonus;
    }

    public double getEffectiveJumpHeightMultiplier() {
        return permanentJumpHeightBonus + jumpHeightBonus;
    }

    // === IMPLEMENTATIONS === //
    @Override
    public Optional<ISyncPacket> createPacket(boolean isFullSync) {
        if (player == null || player.level().isClientSide() || !isDirty()) {
            return Optional.empty();
        }
        CompoundTag data = new CompoundTag();
        data.putDouble("p_attack", permanentAttackDamageBonus);
        data.putDouble("p_speed", permanentMovementSpeedBonus);
        data.putDouble("p_jump", permanentJumpHeightBonus);
        data.putDouble("t_attack", attackDamageBonus);
        data.putDouble("t_speed", movementSpeedBonus);
        data.putDouble("t_jump", jumpHeightBonus);
        clearDirty();
        return Optional.of(new PlayerCombatStatsSyncPacket(data));
    }

    @Override
    public void tick() {
        // Combat stats are reactive, no per-tick logic needed
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
        this.permanentAttackDamageBonus = data.getDouble("p_attack");
        this.permanentMovementSpeedBonus = data.getDouble("p_speed");
        this.permanentJumpHeightBonus = data.getDouble("p_jump");
        this.attackDamageBonus = data.getDouble("t_attack");
        this.movementSpeedBonus = data.getDouble("t_speed");
        this.jumpHeightBonus = data.getDouble("t_jump");
    }

    // === SERIALIZATION === //
    public static class Serializer implements IAttachmentSerializer<CompoundTag, PlayerCombatStatsAttachment> {
        @Override
        public PlayerCombatStatsAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PlayerCombatStatsAttachment attachment = CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to decode PlayerCombatStatsAttachment: {}", error))
                    .map(com.mojang.datafixers.util.Pair::getFirst)
                    .orElseGet(PlayerCombatStatsAttachment::new);

            if (holder instanceof Player player) {
                attachment.setPlayer(player);
            }
            return attachment;
        }

        @Override
        public @Nullable CompoundTag write(PlayerCombatStatsAttachment attachment, HolderLookup.Provider provider) {
            return (CompoundTag) CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment)
                    .resultOrPartial(error -> ImmortalCultivation.LOGGER.error("Failed to encode PlayerCombatStatsAttachment: {}", error))
                    .orElse(new CompoundTag());
        }
    }

    // === CODEC === //
    public static final Codec<PlayerCombatStatsAttachment> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.optionalFieldOf("p_attack", 0.0).forGetter(PlayerCombatStatsAttachment::getPermanentAttackDamageBonus),
                    Codec.DOUBLE.optionalFieldOf("p_speed", 0.0).forGetter(PlayerCombatStatsAttachment::getPermanentMovementSpeedBonus),
                    Codec.DOUBLE.optionalFieldOf("p_jump", 0.0).forGetter(PlayerCombatStatsAttachment::getPermanentJumpHeightBonus),
                    Codec.DOUBLE.optionalFieldOf("t_attack", 0.0).forGetter(PlayerCombatStatsAttachment::getAttackDamageBonus),
                    Codec.DOUBLE.optionalFieldOf("t_speed", 0.0).forGetter(PlayerCombatStatsAttachment::getMovementSpeedBonus),
                    Codec.DOUBLE.optionalFieldOf("t_jump", 0.0).forGetter(PlayerCombatStatsAttachment::getJumpHeightBonus)
            ).apply(instance, PlayerCombatStatsAttachment::new)
    );
}
