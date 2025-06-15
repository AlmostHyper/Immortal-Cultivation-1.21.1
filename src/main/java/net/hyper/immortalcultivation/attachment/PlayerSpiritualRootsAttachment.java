package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.minecraft.nbt.CompoundTag;

public class PlayerSpiritualRootsAttachment {
    private SpiritualRootType rootType;
    private boolean hasBeenAssigned;

    public PlayerSpiritualRootsAttachment() {
        this.rootType = SpiritualRootType.MORTAL_ROOT;
        this.hasBeenAssigned = false;
    }

    public PlayerSpiritualRootsAttachment(SpiritualRootType rootType) {
        this.rootType = rootType;
        this.hasBeenAssigned = true;
    }

    // Getters
    public SpiritualRootType getRootType() { return rootType; }
    public boolean hasBeenAssigned() { return hasBeenAssigned; }

    // Modifier methods
    public float getCultivationMultiplier() {
        return rootType.getCultivationMultiplier();
    }

    public boolean canCultivate() {
        return true; // All players can cultivate with any spiritual root
    }

    // Assignment
    public void assignRootType(SpiritualRootType type) {
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
                this.rootType = SpiritualRootType.valueOf(tag.getString("rootType"));
            } catch (IllegalArgumentException e) {
                this.rootType = SpiritualRootType.MORTAL_ROOT;
            }
        }
        this.hasBeenAssigned = tag.getBoolean("hasBeenAssigned");
    }

    public static PlayerSpiritualRootsAttachment fromTag(CompoundTag tag) {
        PlayerSpiritualRootsAttachment attachment = new PlayerSpiritualRootsAttachment();
        attachment.load(tag);
        return attachment;
    }

    // === Codec === //

    public static final Codec<PlayerSpiritualRootsAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("rootType", "MORTAL_ROOT").forGetter(comp -> comp.rootType.name()),
        Codec.BOOL.optionalFieldOf("hasBeenAssigned", false).forGetter(PlayerSpiritualRootsAttachment::hasBeenAssigned)
    ).apply(instance, (rootTypeString, assigned) -> {
        PlayerSpiritualRootsAttachment comp = new PlayerSpiritualRootsAttachment();
        try {
            comp.rootType = SpiritualRootType.valueOf(rootTypeString);
        } catch (IllegalArgumentException e) {
            comp.rootType = SpiritualRootType.MORTAL_ROOT;
        }
        comp.hasBeenAssigned = assigned;
        return comp;
    }));
}
