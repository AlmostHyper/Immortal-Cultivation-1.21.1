
package net.hyper.immortalcultivation.server.event.server;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.attachment.AttachmentType;


public class ICModAttachmentUpdateEvent extends Event {
    private final Player player;
    private final AttachmentType<?> attachmentType;

    public ICModAttachmentUpdateEvent(Player player, AttachmentType<?> attachmentType) {
        this.player = player;
        this.attachmentType = attachmentType;
    }

    public Player getPlayer() {
        return player;
    }

    public AttachmentType<?> getAttachmentType() {
        return attachmentType;
    }

    public boolean isAttachmentType(AttachmentType<?> type) {
        return this.attachmentType.equals(type);
    }
}
