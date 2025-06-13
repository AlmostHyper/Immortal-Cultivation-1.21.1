package net.hyper.immortal_cultivation.component;

public record PlayerQiData(int current, int max, int regenRate) {
    public PlayerQiData clamp() {
        return new PlayerQiData(Math.min(current, max), max, regenRate);
    }
}
