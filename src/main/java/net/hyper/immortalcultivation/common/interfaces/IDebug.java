package net.hyper.immortalcultivation.common.interfaces;

public interface IDebug {
    boolean isDebugEnabled();
    boolean showDebugUI();
    void logDebug(String category, String message, Object args);
    void logPerformance(String operation, long startTime);
    void dumpState(String component, Object state);
}
