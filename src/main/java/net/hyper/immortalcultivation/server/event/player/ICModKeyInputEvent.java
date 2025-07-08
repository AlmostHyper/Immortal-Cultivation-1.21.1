package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.registry.ICModKeyBinds;
import net.hyper.immortalcultivation.client.gui.screens.CultivationBreakthroughScreen;
import net.hyper.immortalcultivation.client.gui.screens.CultivationStatsScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UNIFIED CULTIVATION KEY INPUT SYSTEM
 *
 * This consolidated event handler manages all key input functionality for the cultivation system,
 * providing a single point of control for all keyboard interactions and screen navigation.
 *
 * Key Features:
 * - Centralized key input processing with safety checks
 * - Screen state management and navigation
 * - Key combo detection and handling
 * - Input validation and conflict prevention
 * - Performance optimized with input debouncing
 * - Debug mode support for development
 * - Accessibility considerations for key handling
 *
 * Architecture:
 * - Uses NEOFORGE BUS for runtime key input events
 * - CLIENT-SIDE ONLY (Dist.CLIENT) for input handling
 * - Integrates with ICModKeyBinds for registered key mappings
 * - Provides hooks for future key-based functionality
 * - Error isolation prevents cascade failures
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, value = Dist.CLIENT)
public class ICModKeyInputEvent {

    // === INPUT DEBOUNCING SYSTEM ===

    /**
     * Tracks last input times to prevent accidental double-triggering.
     * This is especially important for cultivation interfaces that might take time to load.
     */
    private static final Map<String, Long> lastInputTime = new ConcurrentHashMap<>();
    private static final long INPUT_DEBOUNCE_MS = 200; // 200ms between same key presses

    // === DEBUG AND DEVELOPMENT ===

    /**
     * Debug mode enables additional logging and input validation.
     * Can be toggled for development purposes.
     */
    private static boolean debugMode = false;
    private static final Map<String, Integer> keyPressCount = new ConcurrentHashMap<>();

    // === SCREEN STATE TRACKING ===

    /**
     * Tracks the last opened screen to prevent unnecessary screen transitions.
     * This helps with performance and user experience.
     */
    private static String lastOpenedScreen = null;
    private static long lastScreenOpenTime = 0;

    // === MAIN KEY INPUT HANDLER ===

    /**
     * Unified key input handler that processes all cultivation-related keyboard inputs.
     *
     * This event handler responds to key presses and manages screen navigation, key combinations,
     * and input validation. It consolidates all key input logic to prevent conflicts and
     * provide consistent behavior across the cultivation system.
     *
     * InputEvent.Key is a NEOFORGE BUS event because it represents a runtime action that the
     * player is taking during gameplay, not a mod initialization event.
     *
     * @param event The key input event containing key, action, and modifier information
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();

        // === SAFETY AND VALIDATION CHECKS ===

        // Only process key presses (not releases) and only when in-game
        // This check ensures we don't interfere with menu navigation or create conflicts
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null) {
            return;
        }

        // Ensure player is available and in-world
        // These safety checks prevent crashes when the player isn't fully loaded
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // Check if game is paused or in a state where input should be ignored
        if (minecraft.isPaused() && minecraft.level.isClientSide()) {
            return;
        }

        // === KEY INPUT PROCESSING ===

        try {
            // Handle cultivation stats keybind (primary interface)
            if (ICModKeyBinds.CULTIVATION_STATS_KEY.consumeClick()) {
                handleCultivationStatsKey(minecraft);
            }

            // Handle breakthrough analysis keybind (specialized interface)
            if (ICModKeyBinds.BREAKTHROUGH_SCREEN_KEY.consumeClick()) {
                handleBreakthroughScreenKey(minecraft);
            }

            // Handle potential key combinations
            handleKeyCombinatons(event, minecraft);

            // Update debug statistics if enabled
            if (debugMode) {
                updateDebugStatistics(event);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing key input: {}", e.getMessage(), e);
        }
    }

    // === INDIVIDUAL KEY HANDLERS ===

    /**
     * Handles the cultivation stats screen key input with debouncing and validation.
     *
     * The cultivation stats screen is the primary interface for monitoring cultivation progress,
     * viewing realm information, and checking bonuses. This handler ensures smooth navigation
     * and prevents accidental double-opening.
     *
     * @param minecraft The Minecraft client instance
     */
    private static void handleCultivationStatsKey(Minecraft minecraft) {
        String keyId = "cultivation_stats";

        if (!isInputAllowed(keyId)) {
            if (debugMode) {
                ImmortalCultivation.LOGGER.debug("Cultivation stats key input blocked by debouncing");
            }
            return;
        }

        try {
            ImmortalCultivation.LOGGER.debug("Opening cultivation stats screen via keybind");

            // Track screen opening for state management
            lastOpenedScreen = "cultivation_stats";
            lastScreenOpenTime = System.currentTimeMillis();

            // Open the cultivation stats screen
            minecraft.setScreen(new CultivationStatsScreen());

            // Update input tracking
            updateInputTime(keyId);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to open cultivation stats screen: {}", e.getMessage());
        }
    }

    /**
     * Handles the breakthrough analysis screen key input with sophisticated validation.
     *
     * The breakthrough screen provides detailed analysis of breakthrough chances, failure bonuses,
     * and cultivation progression. This is a more specialized interface that requires additional
     * validation to ensure it's appropriate to open.
     *
     * @param minecraft The Minecraft client instance
     */
    private static void handleBreakthroughScreenKey(Minecraft minecraft) {
        String keyId = "breakthrough_screen";

        if (!isInputAllowed(keyId)) {
            if (debugMode) {
                ImmortalCultivation.LOGGER.debug("Breakthrough screen key input blocked by debouncing");
            }
            return;
        }

        try {
            // Check if player can access breakthrough functionality
            if (!canPlayerAccessBreakthroughScreen(minecraft)) {
                if (debugMode) {
                    ImmortalCultivation.LOGGER.debug("Player cannot access breakthrough screen - requirements not met");
                }
                return;
            }

            ImmortalCultivation.LOGGER.debug("Opening breakthrough analysis screen via keybind");

            // Track screen opening for state management
            lastOpenedScreen = "breakthrough_analysis";
            lastScreenOpenTime = System.currentTimeMillis();

            // Open the breakthrough analysis screen
            minecraft.setScreen(new CultivationBreakthroughScreen());

            // Update input tracking
            updateInputTime(keyId);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to open breakthrough screen: {}", e.getMessage());
        }
    }

    // === KEY COMBINATION HANDLING ===

    /**
     * Handles advanced key combinations for power users and debugging.
     *
     * This method processes multi-key combinations that can trigger special functionality
     * or debug modes. It's designed to be easily extendable for future key combinations.
     *
     * @param event The key input event
     * @param minecraft The Minecraft client instance
     */
    private static void handleKeyCombinatons(InputEvent.Key event, Minecraft minecraft) {
        // Check for debug mode toggle (Ctrl + Shift + D while in cultivation system)
        if (isKeyComboPressed(GLFW.GLFW_KEY_D, true, true, false)) {
            toggleDebugMode();
            return;
        }

        // Check for emergency screen close (Escape during cultivation screen loading)
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && isLastScreenCultivationRelated()) {
            handleEmergencyScreenClose(minecraft);
            return;
        }

        // Future key combinations can be added here
        // Examples: Quick breakthrough attempt, cultivation focus switching, etc.
    }

    // === VALIDATION AND HELPER METHODS ===

    /**
     * Checks if input is allowed based on debouncing rules and current game state.
     *
     * This method implements input debouncing to prevent accidental double-triggers and
     * validates that the current game state allows for the requested input.
     *
     * @param keyId The identifier for the key being pressed
     * @return true if input should be processed, false if it should be ignored
     */
    private static boolean isInputAllowed(String keyId) {
        Long lastTime = lastInputTime.get(keyId);
        long currentTime = System.currentTimeMillis();

        if (lastTime != null && (currentTime - lastTime) < INPUT_DEBOUNCE_MS) {
            return false;
        }

        // Additional validation can be added here
        // Examples: check player state, screen cooldowns, etc.

        return true;
    }

    /**
     * Updates the last input time for debouncing purposes.
     *
     * @param keyId The identifier for the key that was pressed
     */
    private static void updateInputTime(String keyId) {
        lastInputTime.put(keyId, System.currentTimeMillis());
    }

    /**
     * Checks if the player can access the breakthrough screen based on cultivation state.
     *
     * This method can be extended to check cultivation requirements, unlock conditions,
     * or other prerequisites for accessing the breakthrough functionality.
     *
     * @param minecraft The Minecraft client instance
     * @return true if the player can access the breakthrough screen
     */
    private static boolean canPlayerAccessBreakthroughScreen(Minecraft minecraft) {
        // Basic validation - player must exist and be in-world
        if (minecraft.player == null) {
            return false;
        }

        // Future validation can be added here:
        // - Check if player has any cultivation progress
        // - Check if player has unlocked breakthrough functionality
        // - Check if player is in appropriate game mode
        // - Check cultivation realm requirements

        return true; // Currently allows all players access
    }

    /**
     * Checks if a specific key combination is currently pressed.
     *
     * @param key The primary key to check
     * @param requireCtrl Whether Ctrl must be pressed
     * @param requireShift Whether Shift must be pressed
     * @param requireAlt Whether Alt must be pressed
     * @return true if the key combination is pressed
     */
    private static boolean isKeyComboPressed(int key, boolean requireCtrl, boolean requireShift, boolean requireAlt) {
        long window = Minecraft.getInstance().getWindow().getWindow();

        // Check primary key
        if (GLFW.glfwGetKey(window, key) != GLFW.GLFW_PRESS) {
            return false;
        }

        // Check modifier keys
        boolean ctrlPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean shiftPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean altPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

        return (ctrlPressed == requireCtrl) && (shiftPressed == requireShift) && (altPressed == requireAlt);
    }

    /**
     * Checks if the last opened screen was cultivation-related.
     *
     * @return true if the last screen was a cultivation interface
     */
    private static boolean isLastScreenCultivationRelated() {
        return lastOpenedScreen != null &&
            (lastOpenedScreen.equals("cultivation_stats") || lastOpenedScreen.equals("breakthrough_analysis"));
    }

    // === DEBUG AND DEVELOPMENT FEATURES ===

    /**
     * Toggles debug mode for key input processing.
     * Debug mode enables additional logging and statistics collection.
     */
    private static void toggleDebugMode() {
        debugMode = !debugMode;
        String status = debugMode ? "enabled" : "disabled";
        ImmortalCultivation.LOGGER.info("Key input debug mode {}", status);

        if (!debugMode) {
            // Clear debug statistics when disabling
            keyPressCount.clear();
        }
    }

    /**
     * Updates debug statistics for key input monitoring.
     *
     * @param event The key input event to record
     */
    private static void updateDebugStatistics(InputEvent.Key event) {
        String keyName = GLFW.glfwGetKeyName(event.getKey(), event.getScanCode());
        if (keyName == null) {
            keyName = "Key_" + event.getKey();
        }

        keyPressCount.merge(keyName, 1, Integer::sum);

        // Log frequent key presses for analysis
        int count = keyPressCount.get(keyName);
        if (count % 10 == 0) { // Every 10th press
            ImmortalCultivation.LOGGER.debug("Key {} pressed {} times", keyName, count);
        }
    }

    /**
     * Handles emergency screen close situations.
     * This can be useful when screens fail to load properly or become unresponsive.
     *
     * @param minecraft The Minecraft client instance
     */
    private static void handleEmergencyScreenClose(Minecraft minecraft) {
        if (lastScreenOpenTime > 0 && (System.currentTimeMillis() - lastScreenOpenTime) < 2000) {
            // Only handle emergency close within 2 seconds of opening a cultivation screen
            ImmortalCultivation.LOGGER.debug("Emergency screen close triggered");
            minecraft.setScreen(null);
            lastOpenedScreen = null;
            lastScreenOpenTime = 0;
        }
    }

    // === UTILITY METHODS FOR EXTERNAL ACCESS ===

    /**
     * Provides external access to debug mode status for other systems.
     *
     * @return true if debug mode is currently enabled
     */
    public static boolean isDebugModeEnabled() {
        return debugMode;
    }

    /**
     * Gets debug statistics for monitoring and analysis.
     *
     * @return A copy of the current key press statistics
     */
    public static Map<String, Integer> getDebugStatistics() {
        return Map.copyOf(keyPressCount);
    }

    /**
     * Clears all input tracking data.
     * This method can be called externally for cleanup or reset purposes.
     */
    public static void clearInputTracking() {
        lastInputTime.clear();
        keyPressCount.clear();
        lastOpenedScreen = null;
        lastScreenOpenTime = 0;

        ImmortalCultivation.LOGGER.debug("Key input tracking data cleared");
    }

    /**
     * Forces a screen to open bypassing normal validation (for debugging).
     * This method should only be used for development and testing purposes.
     *
     * @param screenType The type of screen to force open ("stats" or "breakthrough")
     */
    public static void forceOpenScreen(String screenType) {
        if (!debugMode) {
            ImmortalCultivation.LOGGER.warn("Force open screen called but debug mode is disabled");
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        try {
            switch (screenType.toLowerCase()) {
                case "stats", "cultivation", "cultivation_stats":
                    minecraft.setScreen(new CultivationStatsScreen());
                    ImmortalCultivation.LOGGER.debug("Force opened cultivation stats screen");
                    break;
                case "breakthrough", "breakthrough_analysis":
                    minecraft.setScreen(new CultivationBreakthroughScreen());
                    ImmortalCultivation.LOGGER.debug("Force opened breakthrough analysis screen");
                    break;
                default:
                    ImmortalCultivation.LOGGER.warn("Unknown screen type for force open: {}", screenType);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to force open screen '{}': {}", screenType, e.getMessage());
        }
    }

    /**
     * Cleanup method that can be called periodically to prevent memory leaks.
     * This removes old input tracking data that's no longer needed.
     */
    public static void performPeriodicCleanup() {
        long cleanupThreshold = System.currentTimeMillis() - 300_000; // 5 minutes

        // Remove old input time entries
        lastInputTime.entrySet().removeIf(entry -> entry.getValue() < cleanupThreshold);

        // Reset screen tracking if it's very old
        if (lastScreenOpenTime > 0 && lastScreenOpenTime < cleanupThreshold) {
            lastOpenedScreen = null;
            lastScreenOpenTime = 0;
        }
    }
}