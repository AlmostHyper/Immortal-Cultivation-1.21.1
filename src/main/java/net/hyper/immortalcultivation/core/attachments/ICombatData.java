package net.hyper.immortalcultivation.core.attachments;

import net.hyper.immortalcultivation.core.cultivation.BonusSource;

public interface ICombatData {

    enum CombatStatType {
        ATTACK_DAMAGE("Attack Damage"),
        SPIRITUAL_DAMAGE("Spiritual Damage"),
        CRITICAL_CHANCE("Critical Chance"),
        CRITICAL_DAMAGE("Critical Damage"),
        ATTACK_SPEED("Attack Speed");

        private final String displayName;
        CombatStatType(String displayName) {this.displayName = displayName;}
        public String getDisplayName() {return displayName;}
    }

    double getBaseCombatStat(CombatStatType type);
    double getBonusCombatStat(CombatStatType type, BonusSource source);
    double getPermanentBonusCombatStat(CombatStatType type, BonusSource source);
    double getMultiplier(CombatStatType type);
    double getFinalCombatStat(CombatStatType type);
}
