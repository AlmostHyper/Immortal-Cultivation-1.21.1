package net.hyper.immortalcultivation.core.attachments;

import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public interface ICultivationData {

    enum CultivationType {
        QI("Qi"),
        BODY("Body");

        private final String displayName;
        CultivationType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // === Core === //

    long getCurrentCultivation(CultivationType type);
    void setCurrentCultivation(CultivationType type, long value);
    long getMaxCultivation(CultivationType type);
    void setMaxCultivation(CultivationType type, long value);
    void addCultivation(CultivationType type, long amount, boolean respectMaximum);
    void removeCultivation(CultivationType type, long amount, boolean respectMinimum);
    double getProgress(CultivationType type);
    CultivationRealm getPreviousRealm (CultivationType type);
    CultivationRealm getCurrentRealm (CultivationType type);
    void setCurrentRealm(CultivationType type, CultivationRealm realm, @Nullable Player player, boolean silent);

    // === Rates === //

    double getBaseCultivationRate(CultivationType type);

    // === Fractional Accumulation === //

    double getRegenFraction(CultivationType type);
    void setRegenFraction(CultivationType type, double fraction);
    void addRegenFraction(CultivationType type, double amount);
}