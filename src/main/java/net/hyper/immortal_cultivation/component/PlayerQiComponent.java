package net.hyper.immortal_cultivation.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

public class PlayerQiComponent {
    private final Player player;

    private long currentQi = 0;
    private long maxQi = 100;

    public PlayerQiComponent(Player holder) {
        this.player = getPlayer();
    }

    public Player getPlayer() {
        return player;
    }

    public long getCurrentQi() {
        return currentQi;
    }

    public void setCurrentQi(long qi) {
        this.currentQi = Math.max(0, qi); // prevents negative Qi
    }

    public long getMaxQi() {
        return maxQi;
    }

    public void setMaxQi(long value) {
        this.maxQi = value;
    }

    public void addQi(long amount) {
        setCurrentQi(currentQi + amount);
    }

    public void consumeQi(long amount) {
        setCurrentQi(currentQi - amount);
    }

    public static final Codec<PlayerQiComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("qi").forGetter(PlayerQiComponent::getCurrentQi)
    ).apply(instance, qi -> {
        PlayerQiComponent comp = new PlayerQiComponent(null);
        comp.currentQi = qi;
        return comp;
    }));
}
