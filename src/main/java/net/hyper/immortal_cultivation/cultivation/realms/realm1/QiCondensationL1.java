package net.hyper.immortal_cultivation.cultivation.realms.realm1;

import net.hyper.immortal_cultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortal_cultivation.registry.ICModCultivationRealms;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.List;

public class QiCondensationL1 extends CultivationRealm {

    public double getBaseHealth() {return 25.0F;}

    public double getBaseAttackDamage() {return 2.0F;}

    public double getBaseAttackSpeed() {return 4.1;}

    public double getMovementSpeed() {return 0.1;}

    public double getSprintSpeed() {return super.getSprintSpeed();}

    public double getJumpHeight() {return 0.1;}

    public double getStartingQi() {return 0;}

    public double getRequiredQi() {return 100;}

    public List<CultivationRealm> getNextCultivationRealm(Player player) {return Collections.singletonList((CultivationRealm) ICModCultivationRealms.QI_CONDENSATION_L2.get());}


    public double getExplosionResistance() {return 0;}
    public double getKnockbackResistance() {return 0;}
    public double getEarthResistance() {return 0;}
    public double getFireResistance() {return 0;}
    public double getMetalResistance() {return 0;}
    public double getWaterResistance() {return 0;}
    public double getWoodResistance() {return 0;}
}
