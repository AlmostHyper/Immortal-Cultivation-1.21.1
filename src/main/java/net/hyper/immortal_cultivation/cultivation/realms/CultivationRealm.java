package net.hyper.immortal_cultivation.cultivation.realms;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class CultivationRealm  {

    // Base Values
    public abstract double getBaseHealth();
    public abstract double getBaseAttackDamage();
    public abstract double getBaseAttackSpeed();
    public abstract double getMovementSpeed();
    public double getSprintSpeed() {return this.getMovementSpeed() * 1.3;}
    public abstract double getJumpHeight();

    // Qi
    public abstract double getStartingQi();
    public abstract double getRequiredQi();

    // Realms
    public List<CultivationRealm> getCultivationRealms(Player player){return new ArrayList<>();}
    public List<CultivationRealm> getPreviousCultivationRealm(Player player) {return new ArrayList<>();}
    public List<CultivationRealm> getNextCultivationRealm(Player player) {return new ArrayList<>();}

    @Nullable
    public CultivationRealm getRealmBreakthrough(Player player) {return null;}
    @Nullable
    public CultivationRealm getSpecialRealmBreakthrough(Player player) {return null;}
    @Nullable
    public CultivationRealm getBoundaryRealmBreakthrough(Player player) {return null;}

    // Resists
    public abstract double getExplosionResistance();
    public abstract double getKnockbackResistance();
    public abstract double getEarthResistance();
    public abstract double getFireResistance();
    public abstract double getMetalResistance();
    public abstract double getWaterResistance();
    public abstract double getWoodResistance();



}
