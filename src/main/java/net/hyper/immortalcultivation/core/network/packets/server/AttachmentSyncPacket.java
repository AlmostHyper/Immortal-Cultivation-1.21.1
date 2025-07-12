import net.hyper.immortalcultivation.common.interfaces.ISyncPacket;
import net.hyper.immortalcultivation.common.interfaces.ISyncable;
import net.hyper.immortalcultivation.server.event.world.ICModAttachmentUpdateEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Generic sync packet for all attachment types
 */
public record AttachmentSyncPacket<T extends ISyncable>(ResourceLocation attachmentId, CompoundTag data) implements ISyncPacket {

    public static final StreamCodec<FriendlyByteBuf, AttachmentSyncPacket<?>> STREAM_CODEC =
        StreamCodec.of(AttachmentSyncPacket::encode, AttachmentSyncPacket::decode);

    @Override
    public void handleOnClient(Player player) {
        // Generic handling logic that works for all attachments
        var attachment = getAttachmentByResourceLocation(player, attachmentId);
        if (attachment instanceof ISyncable syncable) {
            syncable.applySync(data);
            NeoForge.EVENT_BUS.post(new ICModAttachmentUpdateEvent(player, attachment));
        }
    }
}