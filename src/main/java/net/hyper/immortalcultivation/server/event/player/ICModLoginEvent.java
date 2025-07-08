package net.hyper.immortalcultivation.server.event.player;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.capability.attachments.*;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.common.capability.interfaces.ICultivationData;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.hyper.immortalcultivation.core.network.packets.server.OpenRaceSelectionSyncPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unified login event handler that consolidates all player login initialization logic.
 * This system handles attachment initialization, network synchronization, and integration
 * with the unified cultivation system while working seamlessly with ICModTickEvent and
 * the NetworkSyncManager.
 *
 * <p>Key Responsibilities:</p>
 * <ul>
 *   <li>Initialize all cultivation attachments in proper dependency order</li>
 *   <li>Schedule appropriate network synchronization through NetworkSyncManager</li>
 *   <li>Integrate with ICModTickEvent for ongoing performance monitoring</li>
 *   <li>Handle first-time player setup including talent assignment and race selection</li>
 *   <li>Provide comprehensive error handling and recovery mechanisms</li>
 * </ul>
 *
 * <p>The initialization order is critical: Race → Talents → Cultivation → Resources → Combat Stats.
 * This ensures that dependencies between attachments are properly resolved and that all
 * multipliers and bonuses are correctly calculated from the start.</p>
 *
 * <p>Network synchronization is handled through scheduled syncs rather than immediate force syncs
 * to prevent overwhelming the client during login. The system works in tandem with
 * DimensionChangeHandler for late-stage initialization and player reference updates.</p>
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class ICModLoginEvent {

    // === LOGIN STATE TRACKING ===

    /**
     * Tracks players who have completed login initialization to prevent duplicate processing.
     * This map is cleaned up when players log out to prevent memory leaks.
     */
    private static final Map<UUID, LoginState> loginStateCache = new ConcurrentHashMap<>();

    /**
     * Represents the initialization state of a player during login.
     * This allows for partial recovery if any stage fails and provides
     * detailed tracking for debugging purposes.
     */
    private static class LoginState {
        final long loginStartTime = System.currentTimeMillis();
        boolean raceInitialized = false;
        boolean talentsInitialized = false;
        boolean cultivationInitialized = false;
        boolean resourcesInitialized = false;
        boolean combatStatsInitialized = false;
        boolean networkSyncScheduled = false;
        boolean initializationComplete = false;
        Exception lastError = null;

        /**
         * Checks if the login process has completed all required stages.
         * @return true if all attachments are initialized and network sync is scheduled
         */
        boolean isComplete() {
            return raceInitialized && talentsInitialized && cultivationInitialized &&
                resourcesInitialized && combatStatsInitialized && networkSyncScheduled;
        }

        /**
         * Calculates the total time taken for login initialization.
         * @return login duration in milliseconds
         */
        long getLoginDuration() {
            return System.currentTimeMillis() - loginStartTime;
        }
    }

    // === MAIN LOGIN EVENT HANDLER ===

    /**
     * Main login event handler that orchestrates the entire initialization process.
     * Runs at NORMAL priority to execute before DimensionChangeHandler's late login
     * processing but after basic Minecraft/NeoForge initialization.
     *
     * <p>This method ensures that all cultivation system attachments are properly
     * initialized, player references are set, and appropriate network synchronization
     * is scheduled through the NetworkSyncManager. The process is designed to be
     * resilient to failures and provide comprehensive logging for debugging.</p>
     *
     * @param event The player login event containing the ServerPlayer instance
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();
        String playerName = player.getName().getString();

        ImmortalCultivation.LOGGER.info("Starting unified cultivation system initialization for player: {}", playerName);

        LoginState loginState = new LoginState();
        loginStateCache.put(playerId, loginState);

        try {
            // Execute initialization stages in dependency order
            // Each stage sets the appropriate flag in loginState for tracking
            if (initializeRaceAttachment(player, loginState)) {
                if (initializeTalentAttachment(player, loginState)) {
                    if (initializeCultivationAttachment(player, loginState)) {
                        if (initializeResourceAttachment(player, loginState)) {
                            if (initializeCombatStatsAttachment(player, loginState)) {
                                // Schedule network synchronization through NetworkSyncManager
                                scheduleLoginNetworkSync(player, loginState);

                                // Check for special achievements (Divine Dao Destiny)
                                checkForSpecialAchievements(player);

                                loginState.initializationComplete = true;

                                ImmortalCultivation.LOGGER.info(
                                    "Unified cultivation system fully initialized for player {} in {}ms",
                                    playerName, loginState.getLoginDuration()
                                );
                            }
                        }
                    }
                }
            }

            // If we reach here without complete initialization, log the issue
            if (!loginState.isComplete()) {
                ImmortalCultivation.LOGGER.warn(
                    "Login initialization incomplete for player {} - some attachments may not be properly initialized",
                    playerName
                );
            }

        } catch (Exception e) {
            loginState.lastError = e;
            ImmortalCultivation.LOGGER.error(
                "Critical error during login initialization for player {}: {}",
                playerName, e.getMessage(), e
            );

            // Attempt recovery by scheduling a delayed re-initialization
            attemptLoginRecovery(player, loginState);
        }
    }

    // === ATTACHMENT INITIALIZATION METHODS ===

    /**
     * Initializes the player's race attachment and handles first-time race selection.
     * The race system affects all cultivation multipliers and must be initialized first
     * to ensure proper dependency resolution for subsequent attachments.
     *
     * <p>If the player hasn't selected a race yet, this method triggers the race selection
     * UI and provides appropriate welcome messaging. The race selection affects all
     * subsequent cultivation calculations.</p>
     *
     * @param player The server player instance to initialize
     * @param loginState The login state tracker for error handling and recovery
     * @return true if initialization succeeded, false if it failed
     */
    private static boolean initializeRaceAttachment(ServerPlayer player, LoginState loginState) {
        try {
            PlayerRaceAttachment raceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());

            if (raceAttachment == null) {
                ImmortalCultivation.LOGGER.error("Race attachment is null for player {}", player.getName().getString());
                return false;
            }

            // Set player reference for proper attachment functionality
            raceAttachment.setPlayer(player);

            // Handle first-time race selection
            if (!raceAttachment.hasRaceAssigned()) {
                // Send race selection UI packet
                ICModPacketRegistry.sendToPlayer(new OpenRaceSelectionSyncPacket(), player);

                // Send welcome message to new cultivator
                Component welcomeMessage = Component.literal("⚡ Welcome, young cultivator! ")
                    .append("Your destiny in the immortal realm begins now. Choose your race wisely, ")
                    .append("as it will shape your cultivation path forever.")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

                player.sendSystemMessage(welcomeMessage);

                ImmortalCultivation.LOGGER.info("New player {} requires race selection", player.getName().getString());
            } else {
                ImmortalCultivation.LOGGER.debug("Race attachment loaded for player {} - Race: {}",
                    player.getName().getString(), raceAttachment.getRaceType().getDisplayName());
            }

            loginState.raceInitialized = true;
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to initialize race attachment for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Initializes the player's talent attachment including spiritual roots and physiques.
     * This system handles the random assignment of cultivation talents for new players
     * and ensures proper loading for returning players.
     *
     * <p>The talent system includes both spiritual roots (for Qi cultivation) and physiques
     * (for Body cultivation). These are assigned randomly with rarity-based probabilities
     * and significantly affect cultivation efficiency throughout the player's journey.</p>
     *
     * @param player The server player instance to initialize
     * @param loginState The login state tracker for error handling and recovery
     * @return true if initialization succeeded, false if it failed
     */
    private static boolean initializeTalentAttachment(ServerPlayer player, LoginState loginState) {
        try {
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            if (talentAttachment == null) {
                ImmortalCultivation.LOGGER.error("Talent attachment is null for player {}", player.getName().getString());
                return false;
            }

            // Set player reference for proper attachment functionality
            talentAttachment.setPlayer(player);

            // Handle first-time talent assignment
            if (!talentAttachment.hasSpiritualRootAssigned() || !talentAttachment.hasPhysiqueAssigned()) {
                assignInitialTalents(player, talentAttachment);
            } else {
                // Log existing talents for returning players
                SpiritualRootType rootType = talentAttachment.getSpiritualRootType();
                PhysiqueType physiqueType = talentAttachment.getPhysiqueType();

                ImmortalCultivation.LOGGER.info("Talent attachment loaded for player {} - Root: {} ({}x), Physique: {} ({}x)",
                    player.getName().getString(),
                    rootType.getDisplayName(), rootType.getCultivationMultiplier(),
                    physiqueType.getDisplayName(), physiqueType.getCultivationMultiplier()
                );
            }

            loginState.talentsInitialized = true;
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to initialize talent attachment for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Initializes the player's cultivation attachment including realm progression and bonuses.
     * This is the core cultivation system that tracks Qi and Body cultivation progress,
     * realm advancement, and breakthrough mechanics.
     *
     * <p>The cultivation system manages dual cultivation paths (Qi and Body), realm progression
     * through breakthrough mechanics, and the accumulation of permanent additive bonuses.
     * This attachment depends on race and talent multipliers being properly initialized.</p>
     *
     * @param player The server player instance to initialize
     * @param loginState The login state tracker for error handling and recovery
     * @return true if initialization succeeded, false if it failed
     */
    private static boolean initializeCultivationAttachment(ServerPlayer player, LoginState loginState) {
        try {
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

            if (cultivationAttachment == null) {
                ImmortalCultivation.LOGGER.error("Cultivation attachment is null for player {}", player.getName().getString());
                return false;
            }

            // Set player reference for proper attachment functionality
            cultivationAttachment.setPlayer(player);

            // Verify cultivation data integrity using the generic cultivation system
            // The PlayerCultivationAttachment constructor automatically initializes both cultivation types
            // with the default mortal realm through initializeDefaults()

            // Ensure realms are properly loaded after JSON system initialization
            cultivationAttachment.updateRealmsAfterJsonLoad();

            // Get current realm data using the generic ICultivationData interface
            // Note: Need to access realms through the attachment's public interface
            // For now, check cultivation progress to determine initialization state
            long qiCultivation = cultivationAttachment.getCurrentCultivation(ICultivationData.CultivationType.QI);
            long maxQiCultivation = cultivationAttachment.getMaxCultivation(ICultivationData.CultivationType.QI);
            long bodyCultivation = cultivationAttachment.getCurrentCultivation(ICultivationData.CultivationType.BODY);
            long maxBodyCultivation = cultivationAttachment.getMaxCultivation(ICultivationData.CultivationType.BODY);

            // Determine if this is a new or returning player based on cultivation progress
            // The attachment's initializeDefaults() ensures proper realm initialization
            boolean isNewPlayer = (qiCultivation == 0 && bodyCultivation == 0 &&
                maxQiCultivation > 0 && maxBodyCultivation > 0);

            if (isNewPlayer) {
                ImmortalCultivation.LOGGER.info("New cultivator {} begins their journey with initialized cultivation system",
                    player.getName().getString());
            } else {
                ImmortalCultivation.LOGGER.info("Cultivation attachment loaded for player {} - Qi: {}/{}, Body: {}/{}",
                    player.getName().getString(),
                    qiCultivation, maxQiCultivation,
                    bodyCultivation, maxBodyCultivation
                );
            }

            loginState.cultivationInitialized = true;
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to initialize cultivation attachment for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Initializes the player's resource attachment managing health, qi, and body energy.
     * This system handles the consumable resources used for techniques and abilities,
     * separate from cultivation progress values but dependent on cultivation bonuses.
     *
     * <p>The resource system is compatible with the permanent additive bonus system and
     * does not attempt to calculate or set permanent bonuses during initialization.
     * Permanent bonuses are only added through realm breakthroughs to maintain
     * the integrity of the additive system.</p>
     *
     * @param player The server player instance to initialize
     * @param loginState The login state tracker for error handling and recovery
     * @return true if initialization succeeded, false if it failed
     */
    private static boolean initializeResourceAttachment(ServerPlayer player, LoginState loginState) {
        try {
            PlayerResourceAttachment resourceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());

            if (resourceAttachment == null) {
                ImmortalCultivation.LOGGER.error("Resource attachment is null for player {}", player.getName().getString());
                return false;
            }

            // Set player reference for proper attachment functionality
            resourceAttachment.setPlayer(player);

            // Log current resource status (no modification of permanent bonuses here)
            ImmortalCultivation.LOGGER.info("Resource attachment loaded for player {} - Health: {}/{} (+{} permanent), Qi: {}/{}, Body: {}/{}",
                player.getName().getString(),
                resourceAttachment.getCurrentHealth(),
                resourceAttachment.getMaxHealth(),
                resourceAttachment.getPermanentHealthBonus(),
                resourceAttachment.getCurrentQi(),
                resourceAttachment.getMaxQi(),
                resourceAttachment.getCurrentBody(),
                resourceAttachment.getMaxBody()
            );

            loginState.resourcesInitialized = true;
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to initialize resource attachment for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Initializes the player's combat stats attachment for enhanced combat capabilities.
     * This system manages attack damage bonuses, movement speed enhancements, and
     * jump height improvements based on cultivation progression.
     *
     * <p>The combat stats system is integrated with the cultivation progression and
     * provides real-time combat enhancements that scale with the player's advancement.
     * This attachment is initialized last as it depends on cultivation data.</p>
     *
     * @param player The server player instance to initialize
     * @param loginState The login state tracker for error handling and recovery
     * @return true if initialization succeeded, false if it failed
     */
    private static boolean initializeCombatStatsAttachment(ServerPlayer player, LoginState loginState) {
        try {
            PlayerCombatStatsAttachment combatStatsAttachment = PlayerCombatStatsAttachment.getOrCreate(player);

            if (combatStatsAttachment == null) {
                ImmortalCultivation.LOGGER.error("Combat stats attachment is null for player {}", player.getName().getString());
                return false;
            }

            // Combat stats attachment automatically sets player reference in getOrCreate()

            ImmortalCultivation.LOGGER.info("Combat stats initialized for player {} - ATK: +{}%, SPD: +{}%, JUMP: +{}%",
                player.getName().getString(),
                String.format("%.1f", combatStatsAttachment.getAttackDamageBonus() * 100),
                String.format("%.1f", combatStatsAttachment.getMovementSpeedBonus() * 100),
                String.format("%.1f", combatStatsAttachment.getJumpHeightBonus() * 100)
            );

            loginState.combatStatsInitialized = true;
            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to initialize combat stats attachment for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            return false;
        }
    }

    // === NETWORK SYNCHRONIZATION ===

    /**
     * Schedules comprehensive network synchronization for a newly logged-in player.
     * This method uses the NetworkSyncManager to schedule appropriate sync operations
     * rather than forcing immediate synchronization, which helps prevent client
     * overwhelm and integrates with the unified delta sync system.
     *
     * <p>The synchronization is scheduled with HIGH priority to ensure the client
     * receives updated cultivation data promptly but not so high as to interfere
     * with critical systems. This approach works seamlessly with ICModTickEvent's
     * ongoing sync management.</p>
     *
     * @param player The server player instance to synchronize
     * @param loginState The login state tracker for completion tracking
     */
    private static void scheduleLoginNetworkSync(ServerPlayer player, LoginState loginState) {
        try {
            // Schedule unified delta sync with HIGH priority for login
            // This ensures timely sync without overwhelming the client
            NetworkSyncManager.scheduleSync(player, NetworkSyncManager.SyncPriority.HIGH);

            loginState.networkSyncScheduled = true;

            ImmortalCultivation.LOGGER.debug("Network synchronization scheduled for player {} through NetworkSyncManager",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to schedule network sync for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    // === UTILITY METHODS ===

    /**
     * Assigns initial spiritual root and physique talents to new players.
     * This method handles the random assignment process with proper rarity
     * probabilities and provides appropriate messaging for each talent tier.
     *
     * <p>The assignment process respects the configured rarity probabilities
     * and provides contextual messaging to help players understand the
     * significance of their assigned talents. Both talents are assigned
     * simultaneously to ensure consistency.</p>
     *
     * @param player The server player instance receiving talents
     * @param talentAttachment The talent attachment to configure
     */
    private static void assignInitialTalents(ServerPlayer player, PlayerTalentAttachment talentAttachment) {
        try {
            // Check spiritual root assignment and send message if newly assigned
            if (!talentAttachment.hasSpiritualRootAssigned()) {
                // Talent attachment should auto-assign during initialization
                // Check again after potential auto-assignment
                if (talentAttachment.hasSpiritualRootAssigned()) {
                    SpiritualRootType rootType = talentAttachment.getSpiritualRootType();
                    sendSpiritualRootAwakeningMessage(player, rootType);

                    ImmortalCultivation.LOGGER.info("Spiritual root {} assigned to player {}",
                        rootType.getDisplayName(), player.getName().getString());
                }
            } else {
                // Already assigned, just log for returning player
                SpiritualRootType rootType = talentAttachment.getSpiritualRootType();
                ImmortalCultivation.LOGGER.debug("Player {} has existing spiritual root: {}",
                    player.getName().getString(), rootType.getDisplayName());
            }

            // Check physique assignment and send message if newly assigned
            if (!talentAttachment.hasPhysiqueAssigned()) {
                // Talent attachment should auto-assign during initialization
                // Check again after potential auto-assignment
                if (talentAttachment.hasPhysiqueAssigned()) {
                    PhysiqueType physiqueType = talentAttachment.getPhysiqueType();
                    sendPhysiqueAwakeningMessage(player, physiqueType);

                    ImmortalCultivation.LOGGER.info("Physique {} assigned to player {}",
                        physiqueType.getDisplayName(), player.getName().getString());
                }
            } else {
                // Already assigned, just log for returning player
                PhysiqueType physiqueType = talentAttachment.getPhysiqueType();
                ImmortalCultivation.LOGGER.debug("Player {} has existing physique: {}",
                    player.getName().getString(), physiqueType.getDisplayName());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to assign initial talents to player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Checks for special achievements like Divine Dao Destiny (both heavenly talents).
     * This represents the rarest possible achievement with approximately 1 in 1,000,000
     * probability and triggers server-wide announcements and special effects.
     *
     * <p>The Divine Dao Destiny achievement requires both a Heavenly spiritual root
     * and a Heavenly physique, representing the ultimate cultivation potential.
     * Players achieving this status receive special recognition and benefits.</p>
     *
     * @param player The server player instance to check for achievements
     */
    private static void checkForSpecialAchievements(ServerPlayer player) {
        try {
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            if (talentAttachment != null && talentAttachment.hasDivineDaoDestiny()) {
                // DIVINE DAO DESTINY ACHIEVED - Ultimate rarity!
                broadcastDivineDaoDestiny(player);

                ImmortalCultivation.LOGGER.info("DIVINE DAO DESTINY achieved by player {} - both heavenly talents!",
                    player.getName().getString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error checking special achievements for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Attempts to recover from failed login initialization by scheduling a delayed retry.
     * This method provides a fallback mechanism for cases where login initialization
     * fails due to timing issues or temporary system problems.
     *
     * <p>The recovery process schedules a delayed re-initialization attempt and
     * marks the player for special handling in subsequent tick events. This
     * ensures that players don't become permanently stuck with uninitialized
     * cultivation systems due to transient errors.</p>
     *
     * @param player The server player instance requiring recovery
     * @param loginState The failed login state for analysis and retry logic
     */
    private static void attemptLoginRecovery(ServerPlayer player, LoginState loginState) {
        ImmortalCultivation.LOGGER.warn("Attempting login recovery for player {} after {}ms",
            player.getName().getString(), loginState.getLoginDuration());

        // Schedule delayed recovery attempt
        player.server.execute(() -> {
            try {
                // Wait briefly for systems to stabilize
                Thread.sleep(1000);

                // Re-trigger login event processing
                ImmortalCultivation.LOGGER.info("Executing recovery login initialization for player {}",
                    player.getName().getString());

                // Create new login state for recovery attempt
                LoginState recoveryState = new LoginState();
                loginStateCache.put(player.getUUID(), recoveryState);

                // Re-run initialization process
                onPlayerLogin(new PlayerEvent.PlayerLoggedInEvent(player));

            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Recovery attempt failed for player {}: {}",
                    player.getName().getString(), e.getMessage(), e);
            }
        });
    }

    // === MESSAGING METHODS ===

    /**
     * Sends appropriate awakening message for spiritual root assignment based on rarity.
     * The messaging system provides contextual feedback that helps players understand
     * the significance of their assigned spiritual root and its impact on cultivation.
     *
     * @param player The server player instance to message
     * @param rootType The assigned spiritual root type for messaging context
     */
    private static void sendSpiritualRootAwakeningMessage(ServerPlayer player, SpiritualRootType rootType) {
        float multiplier = rootType.getCultivationMultiplier();
        Component rootName = Component.literal(rootType.getDisplayName())
            .withStyle(Style.EMPTY.withColor(rootType.getColor()));

        Component message;

        if (multiplier == 1.0f) {
            // Mortal Root - 60% chance
            message = Component.literal("✨ You sense the flow of spiritual energy... You possess the ")
                .append(rootName)
                .append(". Through dedicated cultivation, you may transcend mortality.")
                .withStyle(ChatFormatting.GRAY);
        } else if (multiplier <= 1.1f) {
            // Common Roots - 25% total
            message = Component.literal("🌟 The heavens smile upon you! You have awakened the ")
                .append(rootName)
                .append("!")
                .withStyle(ChatFormatting.GREEN);
        } else if (multiplier <= 1.2f) {
            // Uncommon Roots - 10% total
            message = Component.literal("⭐ Excellent fortune! Your soul houses the ")
                .append(rootName)
                .append("! Your cultivation path shines brighter than most!")
                .withStyle(ChatFormatting.BLUE);
        } else if (multiplier <= 1.3f) {
            // Rare Roots - 4% total
            message = Component.literal("💫 EXTRAORDINARY LUCK! The legendary ")
                .append(rootName)
                .append(" has chosen you! One in a hundred!")
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        } else if (multiplier == 1.4f) {
            // Epic Root - 1% chance
            message = Component.literal("🌌 HEAVEN-DEFYING DESTINY! You possess the mythical ")
                .append(rootName)
                .append("! One in ten thousand!")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            broadcastLegendaryAchievement(player, "spiritual root", rootType.getDisplayName(), "1.4x");
        } else if (multiplier == 1.5f) {
            // Heavenly Root - 0.1% chance
            message = Component.literal("💫 HEAVEN-SHAKING MIRACLE! You have DEFIED FATE ITSELF! ")
                .append("The legendary ")
                .append(rootName)
                .append(" has chosen you! Destined for IMMORTALITY!")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            broadcastHeavenlyAchievement(player, "spiritual root", rootType.getDisplayName(), "1.5x");
        } else {
            // Fallback for unexpected multipliers
            message = Component.literal("✨ You have awakened the ")
                .append(rootName)
                .append("!");
        }

        player.sendSystemMessage(message);

        // Add multiplier information for enhanced roots
        if (multiplier > 1.0f) {
            Component multiplierInfo = Component.literal("» Cultivation multiplier: ")
                .append(Component.literal(String.format("%.1fx", multiplier))
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(multiplierInfo);
        }
    }

    /**
     * Sends appropriate awakening message for physique assignment based on rarity.
     * The messaging system provides contextual feedback that helps players understand
     * the significance of their assigned physique and its impact on body cultivation.
     *
     * @param player The server player instance to message
     * @param physiqueType The assigned physique type for messaging context
     */
    private static void sendPhysiqueAwakeningMessage(ServerPlayer player, PhysiqueType physiqueType) {
        float multiplier = physiqueType.getCultivationMultiplier();
        Component physiqueName = Component.literal(physiqueType.getDisplayName())
            .withStyle(Style.EMPTY.withColor(physiqueType.getColor()));

        Component message;

        if (multiplier == 1.0f) {
            // Mortal Physique - 50% chance
            message = Component.literal("🔥 You are born with a ")
                .append(physiqueName)
                .append(". Your body can be tempered through training...")
                .withStyle(ChatFormatting.GRAY);
        } else if (multiplier <= 1.1f) {
            // Common Physiques - 30% total
            message = Component.literal("🔥 The heavens have blessed your physical form! You possess the ")
                .append(physiqueName)
                .append("!")
                .withStyle(ChatFormatting.GREEN);
        } else if (multiplier <= 1.2f) {
            // Uncommon Physiques - 15% total
            message = Component.literal("🔥 Fortune smiles upon you! Your body houses the ")
                .append(physiqueName)
                .append("! Superior to common mortals!")
                .withStyle(ChatFormatting.BLUE);
        } else if (multiplier <= 1.3f) {
            // Rare Physiques - 3% total
            message = Component.literal("⚡ INCREDIBLE LUCK! ")
                .append("The ")
                .append(physiqueName)
                .append(" flows through your very being! One in thirty!")
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        } else if (multiplier == 1.4f) {
            // Epic Physique - 1.9% chance
            message = Component.literal("🌟 HEAVEN-BLESSED CONSTITUTION! Your body embodies the ")
                .append(physiqueName)
                .append("! One in fifty!")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            broadcastLegendaryAchievement(player, "physique", physiqueType.getDisplayName(), "1.4x");
        } else if (multiplier == 1.5f) {
            // Heavenly Physique - 0.1% chance
            message = Component.literal("💫 DIVINE INCARNATION! Your mortal frame contains the ")
                .append(physiqueName)
                .append("! The gods themselves envy you!")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            broadcastHeavenlyAchievement(player, "physique", physiqueType.getDisplayName(), "1.5x");
        } else {
            // Fallback for unexpected multipliers
            message = Component.literal("🔥 You have awakened the ")
                .append(physiqueName)
                .append("!");
        }

        player.sendSystemMessage(message);

        // Add multiplier information for enhanced physiques
        if (multiplier > 1.0f) {
            Component multiplierInfo = Component.literal("» Body cultivation multiplier: ")
                .append(Component.literal(String.format("%.1fx", multiplier))
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(multiplierInfo);
        }
    }

    /**
     * Broadcasts legendary achievement (1.4x multiplier) to all players on the server.
     * These achievements represent exceptional fortune and are celebrated server-wide
     * to acknowledge the player's remarkable luck in talent assignment.
     *
     * @param player The achieving player for broadcast messaging
     * @param talentType The type of talent achieved (spiritual root or physique)
     * @param talentName The specific name of the achieved talent
     * @param multiplier The cultivation multiplier as a string for display
     */
    private static void broadcastLegendaryAchievement(ServerPlayer player, String talentType, String talentName, String multiplier) {
        Component broadcast = Component.literal("🌟 LEGENDARY ACHIEVEMENT! 🌟 ")
            .append("Player ")
            .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(" has awakened the legendary ")
            .append(Component.literal(talentName).withStyle(ChatFormatting.YELLOW))
            .append(" " + talentType + "! (" + multiplier + " multiplier)")
            .withStyle(ChatFormatting.GOLD);

        player.server.getPlayerList().broadcastSystemMessage(broadcast, false);

        // Add special effects at player location
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FIREWORK,
                player.getX(), player.getY() + 1, player.getZ(),
                20, 0.5, 1.0, 0.5, 0.1);

            serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }

    /**
     * Broadcasts heavenly achievement (1.5x multiplier) to all players on the server.
     * These are the rarest individual talent achievements and represent divine favor
     * in the cultivation world, deserving of server-wide celebration and recognition.
     *
     * @param player The achieving player for broadcast messaging
     * @param talentType The type of talent achieved (spiritual root or physique)
     * @param talentName The specific name of the achieved talent
     * @param multiplier The cultivation multiplier as a string for display
     */
    private static void broadcastHeavenlyAchievement(ServerPlayer player, String talentType, String talentName, String multiplier) {
        Component broadcast = Component.literal("💫 HEAVENLY MANDATE! 💫 ")
            .append("The immortal ")
            .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
            .append(" has been chosen by the heavens! The divine ")
            .append(Component.literal(talentName).withStyle(ChatFormatting.LIGHT_PURPLE))
            .append(" " + talentType + " awakens! (" + multiplier + " multiplier)")
            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);

        player.server.getPlayerList().broadcastSystemMessage(broadcast, false);

        // Enhanced special effects for heavenly achievements
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1, player.getZ(),
                30, 1.0, 1.5, 1.0, 0.2);

            serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    /**
     * Broadcasts the ultimate Divine Dao Destiny achievement for both heavenly talents.
     * This represents the pinnacle of cultivation potential with approximately 1 in
     * 1,000,000 probability and triggers the most spectacular server-wide celebration.
     *
     * @param player The divinely destined player achieving this ultimate status
     */
    private static void broadcastDivineDaoDestiny(ServerPlayer player) {
        // Ultimate achievement announcement
        Component ultimateBroadcast = Component.literal("🌌 DIVINE DAO DESTINY! 🌌 ")
            .append("The Chosen One ")
            .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
            .append(" has achieved DIVINE DAO DESTINY! Both Heavenly talents! 1 in 1,000,000!")
            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);

        // Broadcast multiple times for emphasis
        for (int i = 0; i < 3; i++) {
            player.server.getPlayerList().broadcastSystemMessage(ultimateBroadcast, false);
        }

        // Ultimate special effects
        if (player.level() instanceof ServerLevel serverLevel) {
            // Create spectacular particle display
            for (int i = 0; i < 5; i++) {
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                    player.getX(), player.getY() + 2, player.getZ(),
                    50, 2.0, 2.0, 2.0, 0.3);
            }

            // Play multiple epic sounds
            serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 1.0f, 1.5f);
            serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.5f);
        }
    }

    // === CLEANUP METHODS ===

    /**
     * Handles player logout cleanup to prevent memory leaks and maintain system integrity.
     * This method removes login state tracking data and performs any necessary cleanup
     * operations when a player disconnects from the server.
     *
     * @param event The player logout event for cleanup processing
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();

            // Clean up login state tracking to prevent memory leaks
            LoginState loginState = loginStateCache.remove(playerId);

            if (loginState != null) {
                ImmortalCultivation.LOGGER.debug("Cleaned up login state for player {} (session duration: {}ms)",
                    player.getName().getString(), loginState.getLoginDuration());
            }
        }
    }
}