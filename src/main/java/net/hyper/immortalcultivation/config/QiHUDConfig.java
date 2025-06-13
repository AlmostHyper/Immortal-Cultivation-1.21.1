package net.hyper.immortalcultivation.config;

public class QiHUDConfig {
    public static boolean SHOW_QI_HUD = true;
    public static boolean USE_CIRCULAR_HUD = false;
    public static HudPosition HUD_POSITION = HudPosition.BOTTOM_LEFT;
    public static boolean AUTO_HIDE = true;
    public static float HUD_SCALE = 1.0f;

    public enum HudPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }
}