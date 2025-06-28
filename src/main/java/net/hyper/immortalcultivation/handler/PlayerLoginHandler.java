package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.cultivation.PhysiqueType;
import net.hyper.immortalcultivation.network.server.OpenRaceSelectionSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles player login initialization for the unified dual cultivation system.
 * This ensures that all unified cultivation attachments are properly initialized when
 * a player joins the world, including both Qi and Body cultivation paths.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Player {} logged in, initializing unified dual cultivation system attachments",
                player.getName().getString());

            // Initialize all unified cultivation attachments in the proper order
            // Order matters here because some attachments depend on others
            initializeRaceAttachment(player);       // First: race (affects cultivation multipliers)
            initializeTalentAttachment(player);     // Second: talents (spiritual roots & physique)
            initializeCultivationAttachment(player); // Third: cultivation (realms, progress, focus, bonuses)
            initializeResourceAttachment(player);   // Fourth: resources (health, qi, body energy)

            // IMPORTANT: Force sync all data to ensure client has latest values
            forceSyncAllUnifiedData(player);

            // Check for divine dao destiny achievement (both heavenly talents)
            checkForDivineDaoDestiny(player);

            ImmortalCultivation.LOGGER.info("Unified dual cultivation system fully initialized for player: {}",
                player.getName().getString());
        }
    }

    /**
     * Initialize unified race attachment.
     * This determines the player's cultivation path bonuses and penalties.
     * Race selection must happen before talents are assigned as it affects base multipliers.
     */
    private static void initializeRaceAttachment(ServerPlayer player) {
        try {
            PlayerRaceAttachment raceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RACE.get());

            if (raceAttachment != null) {
                // IMPORTANT: Set player reference for proper functionality
                raceAttachment.setPlayer(player);

                // Check if race has been assigned
                if (!raceAttachment.hasRaceAssigned()) {
                    // Send packet to client to open race selection screen
                    ICModNetwork.sendToPlayer(new OpenRaceSelectionSyncPacket(), player);

                    ImmortalCultivation.LOGGER.info("Player {} needs to select a cultivation race",
                        player.getName().getString());

                    // Send welcome message
                    Component welcomeMessage = Component.literal("⚡ Welcome, young cultivator! ⚡\n")
                        .append(Component.literal("Before you begin your journey to immortality, you must choose your race.\n"))
                        .append(Component.literal("This choice is permanent and will shape your cultivation path!"))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                    player.sendSystemMessage(welcomeMessage);

                } else {
                    // Race already assigned - log current state
                    ImmortalCultivation.LOGGER.info("Player {} cultivation race: {} (Qi: {}x, Body: {}x)",
                        player.getName().getString(),
                        raceAttachment.getRaceDisplayName(),
                        raceAttachment.getQiMultiplier(),
                        raceAttachment.getBodyMultiplier());

                    // Send reminder message about their race
                    Component raceInfo = Component.literal("Your cultivation race: ")
                        .append(Component.literal(raceAttachment.getRaceDisplayName())
                            .withStyle(Style.EMPTY.withColor(raceAttachment.getRaceColor()).withBold(true)))
                        .append(Component.literal(" - " + raceAttachment.getRaceBonusDescription())
                            .withStyle(ChatFormatting.GRAY));
                    player.sendSystemMessage(raceInfo);
                }

            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null race attachment! This should never happen!",
                    player.getName().getString());

                // Attempt recovery by creating default race
                player.sendSystemMessage(Component.literal("Error: Race data not found. Please contact an administrator.")
                    .withStyle(ChatFormatting.RED));
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Critical error initializing race for player {}",
                player.getName().getString(), e);

            // Send error message to player
            player.sendSystemMessage(Component.literal("A critical error occurred during race initialization. Please rejoin.")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
    }

    /**
     * Initialize unified talent attachment (spiritual roots + physique).
     * This determines the player's natural affinity for both Qi and Body cultivation
     * and provides permanent multipliers to all cultivation and regeneration.
     */
    private static void initializeTalentAttachment(ServerPlayer player) {
        try {
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            if (talentAttachment != null) {
                // IMPORTANT: Set player reference for proper functionality
                talentAttachment.setPlayer(player);

                // Initialize spiritual roots if not assigned
                if (!talentAttachment.hasSpiritualRootAssigned()) {
                    SpiritualRootType generatedType = SpiritualRootType.generateRandom(player.getRandom());
                    talentAttachment.assignSpiritualRoot(generatedType);

                    sendSpiritualRootAwakeningMessage(player, generatedType);
                    playSpiritualRootAwakeningEffects(player, generatedType);

                    ImmortalCultivation.LOGGER.info("Player {} assigned spiritual roots: {} ({}x multiplier)",
                        player.getName().getString(), generatedType.getDisplayName(), generatedType.getCultivationMultiplier());
                }

                // Initialize physique if not assigned
                if (!talentAttachment.hasPhysiqueAssigned()) {
                    PhysiqueType generatedType = PhysiqueType.generateRandom(player.getRandom());
                    talentAttachment.assignPhysique(generatedType);

                    sendPhysiqueAwakeningMessage(player, generatedType);
                    playPhysiqueAwakeningEffects(player, generatedType);

                    ImmortalCultivation.LOGGER.info("Player {} assigned physique: {} ({}x multiplier)",
                        player.getName().getString(), generatedType.getDisplayName(), generatedType.getCultivationMultiplier());
                }

                // Log current unified talent state for debugging
                ImmortalCultivation.LOGGER.info("Player {} unified talents - Spiritual Root: {} ({}x), Physique: {} ({}x), Overall: {}x",
                    player.getName().getString(),
                    talentAttachment.getSpiritualRootDisplayName(),
                    talentAttachment.getSpiritualRootMultiplier(),
                    talentAttachment.getPhysiqueDisplayName(),
                    talentAttachment.getPhysiqueMultiplier(),
                    talentAttachment.getOverallCultivationMultiplier());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null talent attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing talents for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize unified cultivation attachment (realms + cultivation progress + focus + bonuses).
     * This sets up the dual realm system and ensures both cultivation paths
     * start at the proper mortal realm with correct focus and bonus states.
     */
    private static void initializeCultivationAttachment(ServerPlayer player) {
        try {
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment != null) {
                // IMPORTANT: Set player reference for realm calculations and combat stats
                cultivationAttachment.setPlayer(player);

                // Initialize Qi realm if null
                if (cultivationAttachment.getCurrentQiRealm() == null) {
                    ImmortalCultivation.LOGGER.warn("Player {} has null Qi realm, setting to default mortal realm",
                        player.getName().getString());
                    cultivationAttachment.setCurrentQiRealm(ICModCultivationRealms.MORTAL);
                }

                // Initialize Body realm if null
                if (cultivationAttachment.getCurrentBodyRealm() == null) {
                    ImmortalCultivation.LOGGER.warn("Player {} has null Body realm, setting to default mortal realm",
                        player.getName().getString());
                    cultivationAttachment.setCurrentBodyRealm(ICModCultivationRealms.MORTAL);
                }

                ImmortalCultivation.LOGGER.info("Player {} unified cultivation initialized - Qi: {} ({}/{}), Body: {} ({}/{}), Focus: {}, Bonuses: Qi +{}/sec, Body +{}/sec",
                    player.getName().getString(),
                    cultivationAttachment.getCurrentQiRealm().getDisplayName(),
                    cultivationAttachment.getCurrentQiCultivation(),
                    cultivationAttachment.getMaxQiCultivation(),
                    cultivationAttachment.getCurrentBodyRealm().getDisplayName(),
                    cultivationAttachment.getCurrentBodyCultivation(),
                    cultivationAttachment.getMaxBodyCultivation(),
                    cultivationAttachment.getCurrentFocus().getDisplayName(),
                    String.format("%.3f", cultivationAttachment.getQiCultivationRawAdditiveBonus()),
                    String.format("%.3f", cultivationAttachment.getBodyCultivationRawAdditiveBonus()));
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null cultivation attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing cultivation for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize unified resource attachment (health + qi + body energy).
     * This manages the consumable resources used for techniques and abilities,
     * separate from the cultivation progress values.
     */
    private static void initializeResourceAttachment(ServerPlayer player) {
        try {
            PlayerResourceAttachment resourceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resourceAttachment != null) {
                // IMPORTANT: Set player reference for resource calculations
                resourceAttachment.setPlayer(player);

                // Update max values based on current cultivation state
                updateResourceMaxValues(player, resourceAttachment);

                ImmortalCultivation.LOGGER.info("Player {} unified resources initialized - Health: {}/{}, Qi: {}/{}, Body: {}/{}, Meditating: {}",
                    player.getName().getString(),
                    resourceAttachment.getCurrentHealth(),
                    resourceAttachment.getMaxHealth(),
                    resourceAttachment.getCurrentQi(),
                    resourceAttachment.getMaxQi(),
                    resourceAttachment.getCurrentBody(),
                    resourceAttachment.getMaxBody(),
                    resourceAttachment.isPlayerMeditating());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null resource attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing resources for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Update resource max values based on current cultivation state
     */
    private static void updateResourceMaxValues(ServerPlayer player, PlayerResourceAttachment resourceAttachment) {
        try {
            // Calculate and set max health
            long maxHealth = resourceAttachment.getBaseMaxHealth();
            resourceAttachment.setMaxHealth(maxHealth);

            // Calculate and set max qi
            long maxQi = resourceAttachment.getBaseMaxQi();
            resourceAttachment.setMaxQi(maxQi);

            // Calculate and set max body
            long maxBody = resourceAttachment.getBaseMaxBody();
            resourceAttachment.setMaxBody(maxBody);

            ImmortalCultivation.LOGGER.debug("Updated max values for player {} - Health: {}, Qi: {}, Body: {}",
                player.getName().getString(), maxHealth, maxQi, maxBody);
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error updating resource max values for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Check if player has achieved divine dao destiny (both heavenly talents)
     * This is the rarest possible achievement: 0.000001% chance (1 in 1,000,000)
     */
    private static void checkForDivineDaoDestiny(ServerPlayer player) {
        try {
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            if (talentAttachment != null && talentAttachment.hasDivineDaoDestiny()) {
                // DIVINE DAO DESTINY ACHIEVED!
                broadcastDivineDaoDestiny(player,
                    talentAttachment.getSpiritualRootDisplayName(),
                    talentAttachment.getPhysiqueDisplayName());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error checking divine dao destiny for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Broadcast legendary achievement to all players on the server
     * Called for 1.4x multiplier talents (1.9% chance)
     */
    private static void broadcastLegendaryAchievement(ServerPlayer player, String type, String talentName, String multiplier) {
        try {
            Component playerName = Component.literal(player.getName().getString())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            Component talent = Component.literal(talentName)
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);

            Component broadcast = Component.literal("Heaven shines down on ")
                .append(playerName)
                .append(Component.literal(", who has been born with "))
                .append(talent)
                .append(Component.literal("!\n"))
                .append(Component.literal("A prodigy emerges!"))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);

            // Send to all players
            player.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
                if (!serverPlayer.equals(player)) { // Don't send to the awakening player
                    serverPlayer.sendSystemMessage(broadcast);

                    // Play sound effect for other players
                    serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3f, 1.5f);
                }
            });

            ImmortalCultivation.LOGGER.info("Broadcasted legendary {} awakening for player {} ({})",
                type, player.getName().getString(), talentName);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error broadcasting legendary achievement for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Broadcast heavenly achievement to all players on the server
     * Called for 1.5x multiplier talents (0.1% chance)
     */
    private static void broadcastHeavenlyAchievement(ServerPlayer player, String type, String talentName, String multiplier) {
        try {
            Component playerName = Component.literal(player.getName().getString())
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            Component talent = Component.literal(talentName)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            Component broadcast = Component.literal("THE HEAVENS TREMBLE IN ANTICIPATION!\n")
                .append(playerName)
                .append(Component.literal(" has DEFIED FATE!\n"))
                .append(Component.literal("The legendary "))
                .append(talent)
                .append(Component.literal(" has chosen them!\n"))
                .append(Component.literal("DESTINED FOR IMMORTALITY!"))
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);

            // Send to all players
            player.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
                if (!serverPlayer.equals(player)) { // Don't send to the awakening player
                    serverPlayer.sendSystemMessage(broadcast);

                    // Play epic sound effects for other players
                    serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 2.0f);
                    serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                        SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.2f, 1.8f);
                }
            });

            ImmortalCultivation.LOGGER.info("Broadcasted HEAVENLY {} awakening for player {} ({})",
                type, player.getName().getString(), talentName);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error broadcasting heavenly achievement for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Broadcast divine dao destiny achievement - the rarest possible: both heavenly talents
     * Called when player has both Heavenly Spiritual Root AND Heavenly Physique (0.000001% chance)
     */
    private static void broadcastDivineDaoDestiny(ServerPlayer player, String rootName, String physiqueName) {
        try {
            Component playerName = Component.literal(player.getName().getString())
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            Component root = Component.literal(rootName)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            Component physique = Component.literal(physiqueName)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            Component broadcast = Component.literal("⚡ ════════ DIVINE DAO DESTINY ════════ ⚡\n")
                .append(Component.literal("ALL OF CREATION TREMBLES! THE DAO ITSELF BOWS IN RESPECT!\n"))
                .append(playerName)
                .append(Component.literal(" has achieved DIVINE DAO DESTINY!\n"))
                .append(Component.literal("Blessed with BOTH the "))
                .append(root)
                .append(Component.literal(" AND "))
                .append(physique)
                .append(Component.literal("!\n"))
                .append(Component.literal("Trample the Heavens and shatter the Earth, fated Immortal Emperor!"))
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);

            // Send to ALL players including the achiever (this is so rare it deserves to be seen by everyone)
            player.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
                serverPlayer.sendSystemMessage(broadcast);

                // Play ultimate sound effects for ALL players
                serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 2.0f);
                serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.8f, 2.0f);
                serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.4f, 2.0f);

                // Ultimate particle effects for all players
                if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        serverPlayer.getX(),
                        serverPlayer.getY() + 2.0,
                        serverPlayer.getZ(),
                        100,
                        2.0,
                        2.0,
                        2.0,
                        0.2
                    );
                    serverLevel.sendParticles(
                        ParticleTypes.DRAGON_BREATH,
                        serverPlayer.getX(),
                        serverPlayer.getY() + 1.0,
                        serverPlayer.getZ(),
                        50,
                        1.5,
                        1.5,
                        1.5,
                        0.1
                    );
                    serverLevel.sendParticles(
                        ParticleTypes.TOTEM_OF_UNDYING,
                        serverPlayer.getX(),
                        serverPlayer.getY() + 1.0,
                        serverPlayer.getZ(),
                        30,
                        1.0,
                        1.0,
                        1.0,
                        0.05
                    );
                }
            });

            ImmortalCultivation.LOGGER.info("🌟💫 ULTIMATE DESTINY ACHIEVED! Player {} has both {} and {} - 1 in 10,000 chance! 💫🌟",
                player.getName().getString(), rootName, physiqueName);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error broadcasting divine dao destiny for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Send rarity-appropriate spiritual root awakening message
     */
    private static void sendSpiritualRootAwakeningMessage(ServerPlayer player, SpiritualRootType rootType) {
        float multiplier = rootType.getCultivationMultiplier();
        Component rootName = Component.literal(rootType.getDisplayName())
            .withStyle(Style.EMPTY.withColor(rootType.getColor()));

        Component message;

        if (multiplier == 1.0f) {
            // Mortal Root - 50% chance
            message = Component.literal("✨ You are born with a ")
                .append(rootName)
                .append(". The path of cultivation awaits...")
                .withStyle(ChatFormatting.GRAY);
        } else if (multiplier == 1.1f) {
            // Common Roots - 30% total
            message = Component.literal("✨ The dao smiles upon your birth! You possess the ")
                .append(rootName)
                .append("!")
                .withStyle(ChatFormatting.GREEN);
        } else if (multiplier == 1.2f) {
            // Uncommon Roots - 15% total
            message = Component.literal("✨ Blessed by fate! You have awakened with the ")
                .append(rootName)
                .append("! Your cultivation potential exceeds the common!")
                .withStyle(ChatFormatting.BLUE);
        } else if (multiplier == 1.3f) {
            // Rare Roots - 3% total
            message = Component.literal("⚡ EXTRAORDINARY FORTUNE! The heavens have blessed your birth with the ")
                .append(rootName)
                .append("! Your talent is one in a thousand!")
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        } else if (multiplier == 1.4f) {
            // Legendary Roots - 1.9% total
            message = Component.literal("🌟 DIVINE PROVIDENCE! You have been chosen by destiny itself! ")
                .append("The ")
                .append(rootName)
                .append(" flows through your very soul! One in ten thousand!")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            // BROADCAST TO SERVER
            broadcastLegendaryAchievement(player, "spiritual root", rootType.getDisplayName(), "1.4x");
        } else if (multiplier == 1.5f) {
            // Heavenly Root - 0.1% chance
            message = Component.literal("💫 HEAVEN-SHAKING MIRACLE! You have DEFIED FATE ITSELF! ")
                .append("The legendary ")
                .append(rootName)
                .append(" has chosen you! Destined for IMMORTALITY!")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);

            // BROADCAST TO SERVER
            broadcastHeavenlyAchievement(player, "spiritual root", rootType.getDisplayName(), "1.5x");
        } else {
            // Fallback
            message = Component.literal("✨ You have awakened the ")
                .append(rootName)
                .append("!");
        }

        player.sendSystemMessage(message);

        // Add multiplier info for non-mortal roots
        if (multiplier > 1.0f) {
            Component multiplierInfo = Component.literal("» Cultivation multiplier: ")
                .append(Component.literal(String.format("%.1fx", multiplier))
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(multiplierInfo);
        }
    }

    /**
     * Send rarity-appropriate physique awakening message
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
        } else if (multiplier == 1.1f) {
            // Common Physiques - 30% total
            message = Component.literal("🔥 The heavens have blessed your physical form! You possess the ")
                .append(physiqueName)
                .append("!")
                .withStyle(ChatFormatting.GREEN);
        } else if (multiplier == 1.2f) {
            // Uncommon Physiques - 15% total
            message = Component.literal("🔥 Fortune smiles upon you! Your body houses the ")
                .append(physiqueName)
                .append("! Superior to common mortals!")
                .withStyle(ChatFormatting.BLUE);
        } else if (multiplier == 1.3f) {
            // Rare Physiques - 3% total
            message = Component.literal("⚡ INCREDIBLE LUCK! Your mortal shell conceals the ")
                .append(physiqueName)
                .append("! A prodigy among thousands!")
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        } else if (multiplier == 1.4f) {
            // Legendary Physiques - 1.9% total
            message = Component.literal("🌟 BY THE WILL OF HEAVEN! Your body is a divine vessel! ")
                .append("The ")
                .append(physiqueName)
                .append(" courses through your very bones! One in ten thousand!")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            // BROADCAST TO SERVER
            broadcastLegendaryAchievement(player, "physique", physiqueType.getDisplayName(), "1.4x");
        } else if (multiplier == 1.5f) {
            // Heavenly Physique - 0.1% chance
            message = Component.literal("💫 HEAVEN-DEFYING DESTINY! Your physical form TRANSCENDS MORTALITY! ")
                .append("The almighty ")
                .append(physiqueName)
                .append(" has awakened! Destined to shatter the heavens!")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);

            // BROADCAST TO SERVER
            broadcastHeavenlyAchievement(player, "physique", physiqueType.getDisplayName(), "1.5x");
        } else {
            // Fallback
            message = Component.literal("🔥 You have awakened the ")
                .append(physiqueName)
                .append("!");
        }

        player.sendSystemMessage(message);

        // Add multiplier info for non-mortal physiques
        if (multiplier > 1.0f) {
            Component multiplierInfo = Component.literal("» Cultivation multiplier: ")
                .append(Component.literal(String.format("%.1fx", multiplier))
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(multiplierInfo);
        }
    }

    /**
     * Play rarity-appropriate effects for spiritual root awakening
     */
    private static void playSpiritualRootAwakeningEffects(ServerPlayer player, SpiritualRootType rootType) {
        float multiplier = rootType.getCultivationMultiplier();

        if (multiplier == 1.0f) {
            // Mortal - Simple effect
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else if (multiplier == 1.1f) {
            // Common - Gentle chime
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
            spawnParticles(player, ParticleTypes.ENCHANT, 15);
        } else if (multiplier == 1.2f) {
            // Uncommon - Pleasant sound with more particles
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            spawnParticles(player, ParticleTypes.ENCHANT, 25);
        } else if (multiplier == 1.3f) {
            // Rare - Bell sound with special particles
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.3f);
            spawnParticles(player, ParticleTypes.END_ROD, 30);
            spawnParticles(player, ParticleTypes.ENCHANT, 20);
        } else if (multiplier == 1.4f) {
            // Legendary - Thunder with dramatic particles
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.5f);
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
            spawnParticles(player, ParticleTypes.END_ROD, 50);
            spawnParticles(player, ParticleTypes.DRAGON_BREATH, 25);
        } else if (multiplier == 1.5f) {
            // Heavenly - Epic combination of effects
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 2.0f);
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.3f, 1.8f);
            spawnParticles(player, ParticleTypes.END_ROD, 80);
            spawnParticles(player, ParticleTypes.DRAGON_BREATH, 40);
            spawnParticles(player, ParticleTypes.TOTEM_OF_UNDYING, 30);
        }
    }

    /**
     * Play rarity-appropriate effects for physique awakening
     */
    private static void playPhysiqueAwakeningEffects(ServerPlayer player, PhysiqueType physiqueType) {
        float multiplier = physiqueType.getCultivationMultiplier();

        if (multiplier == 1.0f) {
            // Mortal - Simple effect
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.8f);
        } else if (multiplier == 1.1f) {
            // Common - Gentle effect
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.9f);
            spawnParticles(player, ParticleTypes.FLAME, 12);
        } else if (multiplier == 1.2f) {
            // Uncommon - Stronger effect
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.8f);
            spawnParticles(player, ParticleTypes.FLAME, 20);
            spawnParticles(player, ParticleTypes.SMOKE, 15);
        } else if (multiplier == 1.3f) {
            // Rare - Fire effects
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            spawnParticles(player, ParticleTypes.SOUL_FIRE_FLAME, 25);
            spawnParticles(player, ParticleTypes.FLAME, 20);
        } else if (multiplier == 1.4f) {
            // Legendary - Intense fire and energy
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.2f);
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.3f);
            spawnParticles(player, ParticleTypes.SOUL_FIRE_FLAME, 40);
            spawnParticles(player, ParticleTypes.LAVA, 20);
        } else if (multiplier == 1.5f) {
            // Heavenly - Overwhelming power
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 1.8f);
            player.level().playSound(null, player.blockPosition(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.5f);
            spawnParticles(player, ParticleTypes.SOUL_FIRE_FLAME, 60);
            spawnParticles(player, ParticleTypes.DRAGON_BREATH, 30);
            spawnParticles(player, ParticleTypes.EXPLOSION, 15);
        }
    }

    /**
     * Spawn particles around the player
     */
    private static void spawnParticles(ServerPlayer player, net.minecraft.core.particles.ParticleOptions particle, int count) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                particle,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                count,
                0.5,
                1.0,
                0.5,
                0.1
            );
        }
    }

    /**
     * Force sync all unified cultivation data to ensure client has the latest values.
     * This is critical for proper display in GUIs and HUD elements.
     */
    private static void forceSyncAllUnifiedData(ServerPlayer player) {
        try {
            ImmortalCultivation.LOGGER.info("Force syncing all unified cultivation data for player {}",
                player.getName().getString());

            // Sync talent attachment (spiritual roots + physique)
            PlayerTalentAttachment talentAttachment = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            if (talentAttachment != null) {
                talentAttachment.setPlayer(player); // IMPORTANT: Set player reference
                talentAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Synced unified talents for player {}", player.getName().getString());
            }

            // Sync cultivation attachment (realms + cultivation progress + focus + bonuses)
            PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            if (cultivationAttachment != null) {
                cultivationAttachment.setPlayer(player); // IMPORTANT: Set player reference
                cultivationAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Synced unified cultivation for player {}", player.getName().getString());
            }

            // Sync resource attachment (health + qi + body energy)
            PlayerResourceAttachment resourceAttachment = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            if (resourceAttachment != null) {
                resourceAttachment.setPlayer(player); // IMPORTANT: Set player reference
                resourceAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Synced unified resources for player {}", player.getName().getString());
            }

            // OPTIONAL: Sync combat stats if still using separate attachment
            try {
                PlayerCombatStatsAttachment combatStatsAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                if (combatStatsAttachment != null) {
                    combatStatsAttachment.setPlayer(player);
                    combatStatsAttachment.forceSync();
                    ImmortalCultivation.LOGGER.debug("Synced combat stats for player {}", player.getName().getString());
                }
            } catch (Exception e) {
                // Combat stats might not exist in unified system
                ImmortalCultivation.LOGGER.debug("Combat stats attachment not available (might be unified): {}",
                    e.getMessage());
            }

            ImmortalCultivation.LOGGER.info("Successfully synced all unified cultivation data for player {}",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing unified data for player {}",
                player.getName().getString(), e);
        }
    }
}