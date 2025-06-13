package net.hyper.immortalcultivation.skilltree;

import java.util.HashMap;
import java.util.Map;

public class DaoProgress {
    private final Map<String, Float> daoXP = new HashMap<>();

    public void addXP(String daoType, float amount) {
        daoXP.merge(daoType, amount, Float::sum);
    }

    public float getXP(String daoType) {
        return daoXP.getOrDefault(daoType, 0f);
    }

    public void setXP(String daoType, float amount) {
        daoXP.put(daoType, amount);
    }
}
