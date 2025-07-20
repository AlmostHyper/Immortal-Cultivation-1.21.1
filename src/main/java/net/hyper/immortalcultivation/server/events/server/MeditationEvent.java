package net.hyper.immortalcultivation.server.events.server;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class MeditationEvent extends Event {
    public enum Action {START, STOP, INTERRUPT}

    private final ServerPlayer player;
    private final Action action;
    private boolean interrupted = false;

    public MeditationEvent(ServerPlayer player, Action action) {
        this.player = player;
        this.action = action;
    }

    public ServerPlayer getPlayer() {return player;}
    public Action getAction() {return action;}

    public boolean isInterruptable() {return true;}

    public boolean isInterrupted() {return interrupted;}

    public void setInterrupted(boolean interrupt) {this.interrupted = interrupt;}
}
