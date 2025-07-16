package net.hyper.immortalcultivation.server.events.server;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.server.events.data.CombatEventData;
import net.hyper.immortalcultivation.server.events.utils.CombatCalculationUtils;
import net.hyper.immortalcultivation.server.events.utils.CombatEventUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Combat Event Handler for the Immortal Cultivation Mod
 *
 * Handles combat-related events following SOLID principles and DRY methodology.
 * Delegates business logic to utility classes while maintaining clean event handling.
 * Now uses CombatEventData for centralized data management.
 *
 * Key Features:
 * - Incoming damage processing with custom cultivation damage system
 * - Damage immunity tracking for sophisticated multi-entity combat
 * - Integration with PlayerStatsAttachment for health management
 * - Performance optimized through centralized data management
 * - Comprehensive debug logging for combat system monitoring
 *
 * Architecture:
 * - Handler manages event subscriptions and validation
 * - CombatEventData provides centralized data storage and management
 * - CombatEventUtils handles business logic and calculations
 * - CombatCalculationUtils provides mathematical operations
 * - Maintains thread safety through data manager
 */
public class CombatEventHandler {

    // === CONSTANTS === //
    private static final String HANDLER_NAME = "CombatEventHandler";

    // === DATA MANAGEMENT === //
    private final CombatEventData combatData;

    // === CONSTRUCTOR === //

    /**
     * Constructor for CombatEventHandler
     * Initializes with CombatEventData instance for centralized data management
     */
    public CombatEventHandler() {
        this.combatData = CombatEventData.getInstance();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Handler initialized with data manager", HANDLER_NAME);
        }
    }

    // === EVENT HANDLERS === //

    /**
     * Handles incoming damage events for players in the cultivation system.
     * Intercepts vanilla damage, converts to custom cultivation damage, and applies
     * immunity tracking for multi-entity combat scenarios.
     *
     * @param event The incoming damage event to process
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing damage event for entity: {}",
                HANDLER_NAME, event.getEntity().getClass().getSimpleName());
        }

        // Validate event and extract player
        Player player = validateAndExtractPlayer(event);
        if (player == null) {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        UUID playerId = player.getUUID();
        DamageSource damageSource = event.getSource();
        float vanillaDamage = event.getAmount();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Processing {} damage for player: {}",
                HANDLER_NAME, vanillaDamage, player.getName().getString());
        }

        try {
            // Check immunity using data manager and utility class
            String sourceKey = CombatCalculationUtils.generateDamageSourceKey(damageSource);
            if (combatData.isPlayerImmune(playerId, sourceKey)) {
                event.setCanceled(true);
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] Player {} is immune to damage from source: {}",
                        HANDLER_NAME, player.getName().getString(), sourceKey);
                }
                return;
            }

            // Get player stats attachment
            PlayerStatsAttachment statsAttachment = getPlayerStatsAttachment(serverPlayer);
            if (statsAttachment == null) {
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("[{}] No stats attachment found for player: {}",
                        HANDLER_NAME, player.getName().getString());
                }
                return;
            }

            // Cancel vanilla damage and process through cultivation system
            event.setCanceled(true);

            // Process damage through utility with data manager
            CombatEventUtils.processDamage(
                serverPlayer,
                statsAttachment,
                vanillaDamage,
                damageSource,
                sourceKey,
                combatData
            );

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error processing damage for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);

            // Don't cancel the event if processing fails to prevent player invincibility
            // Let vanilla damage processing continue as fallback
        }
    }

    // === VALIDATION METHODS === //

    /**
     * Validates the damage event and extracts the player if valid
     *
     * @param event The damage event to validate
     * @return The player if valid, null otherwise
     */
    @Nullable
    private Player validateAndExtractPlayer(LivingIncomingDamageEvent event) {
        // Check if entity is a player
        if (!(event.getEntity() instanceof Player player)) {
            return null;
        }

        // Check if this is a server-side player
        if (!(player instanceof ServerPlayer)) {
            return null;
        }

        // Check for valid damage amount
        if (event.getAmount() <= 0.0f) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Ignoring zero or negative damage for player: {}",
                    HANDLER_NAME, player.getName().getString());
            }
            return null;
        }

        return player;
    }

    /**
     * Gets the PlayerStatsAttachment for a player with error handling
     *
     * @param player The server player
     * @return The PlayerStatsAttachment or null if not found
     */
    @Nullable
    private PlayerStatsAttachment getPlayerStatsAttachment(ServerPlayer player) {
        try {
            PlayerStatsAttachment attachment = player.getData(AttachmentRegistry.PLAYER_STATS);
            if (attachment == null) {
                ImmortalCultivation.LOGGER.warn("[{}] Player {} has no stats attachment",
                    HANDLER_NAME, player.getName().getString());
                return null;
            }

            return attachment;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error getting stats attachment for player {}: {}",
                HANDLER_NAME, player.getName().getString(), e.getMessage(), e);
            return null;
        }
    }

    // === UTILITY METHODS === //

    /**
     * Gets the combat data manager instance for external access if needed
     *
     * @return The CombatEventData instance
     */
    public CombatEventData getCombatData() {
        return combatData;
    }

    /**
     * Performs data cleanup if needed
     * Can be called externally for maintenance operations
     */
    public void performMaintenanceCleanup() {
        try {
            combatData.performCleanupIfNeeded();
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error during maintenance cleanup: {}",
                HANDLER_NAME, e.getMessage(), e);
        }
    }
}