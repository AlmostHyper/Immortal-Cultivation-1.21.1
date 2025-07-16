package net.hyper.immortalcultivation.server.events;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.server.events.server.CombatEventHandler;
import net.hyper.immortalcultivation.server.events.server.EquipmentEventHandler;
import net.hyper.immortalcultivation.server.events.server.PlayerLifecycleEventHandler;
import net.hyper.immortalcultivation.server.events.client.GUIEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Unified Event Manager for the Immortal Cultivation Mod
 *
 * Manages all event handlers and ensures proper registration with the NeoForge event bus.
 * This class follows the Manager pattern - it coordinates and manages handler classes,
 * while the actual business logic is contained within util classes and handlers.
 *
 * Key Responsibilities:
 * - Register all event handlers with appropriate event buses
 * - Manage handler lifecycle and dependencies
 * - Provide centralized debugging and monitoring for events
 * - Handle event handler cleanup and shutdown
 *
 * Architecture:
 * - Manager Pattern: Coordinates handler classes
 * - Singleton Pattern: Single point of event management
 * - Registry Pattern: Maintains handler registration state
 */
public class EventManager {

    // === SINGLETON INSTANCE === //
    private static EventManager instance;

    // === HANDLER MANAGEMENT === //
    private final Map<String, Object> registeredHandlers = new ConcurrentHashMap<>();
    private final List<String> handlerRegistrationOrder = new ArrayList<>();

    // === CONFIGURATION === //
    private static final boolean DEBUG_EVENTS = true;
    private static final boolean PERFORMANCE_MONITORING = true;

    // === REGISTRATION TRACKING === //
    private boolean initialized = false;
    private boolean handlersRegistered = false;

    /**
     * Private constructor for singleton pattern
     */
    private EventManager() {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[EventManager] EventManager instance created");
        }
    }

    /**
     * Gets the singleton instance of EventManager
     *
     * @return The EventManager instance
     */
    public static EventManager getInstance() {
        if (instance == null) {
            synchronized (EventManager.class) {
                if (instance == null) {
                    instance = new EventManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the EventManager and registers all event handlers
     * This method should be called during mod initialization
     */
    public static void initialize() {
        EventManager manager = getInstance();

        if (manager.initialized) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[EventManager] Already initialized, skipping...");
            }
            return;
        }

        ImmortalCultivation.LOGGER.info("[EventManager] Initializing Event Management System...");

        try {
            manager.registerAllHandlers();
            manager.initialized = true;

            ImmortalCultivation.LOGGER.info("[EventManager] Event Management System initialized successfully!");

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                manager.logRegistrationSummary();
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[EventManager] Failed to initialize Event Management System", e);
            throw new RuntimeException("Event system initialization failed", e);
        }
    }

    /**
     * Registers all event handlers with their appropriate event buses
     * Handles both client-side and server-side event registration
     */
    private void registerAllHandlers() {
        if (handlersRegistered) {
            ImmortalCultivation.LOGGER.warn("[EventManager] Handlers already registered, skipping registration");
            return;
        }

        ImmortalCultivation.LOGGER.info("[EventManager] Registering event handlers...");

        // Register server-side handlers with NeoForge event bus
        registerServerHandlers();

        // Note: Client-side handlers are registered via @EventBusSubscriber annotation
        // GUIEvent is already properly annotated and will auto-register
        trackClientHandlers();

        handlersRegistered = true;
        ImmortalCultivation.LOGGER.info("[EventManager] All event handlers registered successfully!");
    }

    /**
     * Registers server-side event handlers with the NeoForge event bus
     */
    private void registerServerHandlers() {
        try {
            // Register Combat Event Handler
            registerHandler("CombatEventHandler", new CombatEventHandler());

            // Register Player Lifecycle Handler
            registerHandler("PlayerLifecycleEventHandler", new PlayerLifecycleEventHandler());

            // Register Equipment Event Handler
            registerHandler("EquipmentEventHandler", new EquipmentEventHandler());

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[EventManager] Server-side handlers registered successfully");
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[EventManager] Failed to register server handlers", e);
            throw new RuntimeException("Server handler registration failed", e);
        }
    }

    /**
     * Tracks client-side handlers that self-register via annotations
     */
    private void trackClientHandlers() {
        try {
            // Track existing client-side handlers (auto-registered via @EventBusSubscriber)
            trackHandler("GUIEvent", "Client-side (auto-registered via @EventBusSubscriber)");
            trackHandler("TooltipEventHandler", "Client-side (auto-registered via @EventBusSubscriber)");

            // Track command handler (auto-registers for command events)
            trackHandler("CommandManager", "Auto-registered (via @EventBusSubscriber for commands)");

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[EventManager] Client-side handlers tracked successfully");
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[EventManager] Failed to track client handlers", e);
        }
    }

    /**
     * Registers a handler instance with the NeoForge event bus
     *
     * @param handlerName The name of the handler for tracking
     * @param handler The handler instance to register
     */
    private void registerHandler(String handlerName, Object handler) {
        try {
            NeoForge.EVENT_BUS.register(handler);
            registeredHandlers.put(handlerName, handler);
            handlerRegistrationOrder.add(handlerName);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[EventManager] Registered handler: {}", handlerName);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[EventManager] Failed to register handler: {}", handlerName, e);
            throw new RuntimeException("Handler registration failed: " + handlerName, e);
        }
    }

    /**
     * Tracks a handler for monitoring without registering it
     * Used for handlers that auto-register via annotations
     *
     * @param handlerName The name of the handler
     * @param registrationMethod The method used for registration
     */
    private void trackHandler(String handlerName, String registrationMethod) {
        registeredHandlers.put(handlerName, registrationMethod);
        handlerRegistrationOrder.add(handlerName);

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[EventManager] Tracked handler: {} ({})", handlerName, registrationMethod);
        }
    }

    /**
     * Logs a summary of all registered handlers for debugging purposes
     */
    private void logRegistrationSummary() {
        ImmortalCultivation.LOGGER.debug("[EventManager] Registration Summary:");
        ImmortalCultivation.LOGGER.debug("[EventManager] Total handlers managed: {}", registeredHandlers.size());

        for (String handlerName : handlerRegistrationOrder) {
            Object handler = registeredHandlers.get(handlerName);
            String handlerType = handler instanceof String ? (String) handler : handler.getClass().getSimpleName();
            ImmortalCultivation.LOGGER.debug("[EventManager]   - {}: {}", handlerName, handlerType);
        }
    }

    /**
     * Gets the registration status of the event system
     *
     * @return true if the event system is initialized and handlers are registered
     */
    public boolean isInitialized() {
        return initialized && handlersRegistered;
    }

    /**
     * Gets the number of registered handlers
     *
     * @return The count of registered handlers
     */
    public int getHandlerCount() {
        return registeredHandlers.size();
    }

    /**
     * Gets a list of all registered handler names
     *
     * @return List of handler names in registration order
     */
    public List<String> getRegisteredHandlerNames() {
        return new ArrayList<>(handlerRegistrationOrder);
    }

    /**
     * Checks if a specific handler is registered
     *
     * @param handlerName The name of the handler to check
     * @return true if the handler is registered
     */
    public boolean isHandlerRegistered(String handlerName) {
        return registeredHandlers.containsKey(handlerName);
    }

    /**
     * Shuts down the event manager and unregisters handlers if needed
     * This method should be called during mod shutdown
     */
    public static void shutdown() {
        EventManager manager = getInstance();

        if (!manager.initialized) {
            return;
        }

        ImmortalCultivation.LOGGER.info("[EventManager] Shutting down Event Management System...");

        try {
            // Clear tracking maps
            manager.registeredHandlers.clear();
            manager.handlerRegistrationOrder.clear();
            manager.initialized = false;
            manager.handlersRegistered = false;

            ImmortalCultivation.LOGGER.info("[EventManager] Event Management System shutdown complete");

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[EventManager] Error during shutdown", e);
        }
    }
}