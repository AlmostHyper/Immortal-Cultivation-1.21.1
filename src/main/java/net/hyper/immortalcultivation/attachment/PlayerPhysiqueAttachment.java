package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.cultivation.PhysiqueType;
import net.minecraft.nbt.CompoundTag;

public class PlayerPhysiqueAttachment {
    private PhysiqueType rootType;
    private boolean hasBeenAssigned;

    public PlayerPhysiqueAttachment() {
        this.rootType = PhysiqueType.MORTAL_PHYSIQUE;
        this.hasBeenAssigned = false;
    }

    public PlayerPhysiqueAttachment(PhysiqueType rootType) {
        this.rootType = rootType;
        this.hasBeenAssigned = true;
    }

    // Getters
    public PhysiqueType getRootType() { return rootType; }
    public boolean hasBeenAssigned() { return hasBeenAssigned; }

    // Modifier methods
    public float getCultivationMultiplier() {
        return rootType.getCultivationMultiplier();
    }

    public boolean canCultivate() {
        return true; // All players can cultivate with any physique
    }

    // Assignment
    public void assignRootType(PhysiqueType type) {
        this.rootType = type;
        this.hasBeenAssigned = true;
    }

    // Serialization
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("rootType", rootType.name());
        tag.putBoolean("hasBeenAssigned", hasBeenAssigned);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("rootType")) {
            try {
                this.rootType = PhysiqueType.valueOf(tag.getString("rootType"));
            } catch (IllegalArgumentException e) {
                this.rootType = PhysiqueType.MORTAL_PHYSIQUE;
            }
        }
        this.hasBeenAssigned = tag.getBoolean("hasBeenAssigned");
    }

    public static PlayerPhysiqueAttachment fromTag(CompoundTag tag) {
        PlayerPhysiqueAttachment attachment = new PlayerPhysiqueAttachment();
        attachment.load(tag);
        return attachment;
    }

    // === Codec === //

    public static final Codec<PlayerPhysiqueAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("rootType", "MORTAL_PHYSIQUE").forGetter(comp -> comp.rootType.name()),
        Codec.BOOL.optionalFieldOf("hasBeenAssigned", false).forGetter(PlayerPhysiqueAttachment::hasBeenAssigned)
    ).apply(instance, (rootTypeString, assigned) -> {
        PlayerPhysiqueAttachment comp = new PlayerPhysiqueAttachment();
        try {
            comp.rootType = PhysiqueType.valueOf(rootTypeString);
        } catch (IllegalArgumentException e) {
            comp.rootType = PhysiqueType.MORTAL_PHYSIQUE;
        }
        comp.hasBeenAssigned = assigned;
        return comp;
    }));
}
