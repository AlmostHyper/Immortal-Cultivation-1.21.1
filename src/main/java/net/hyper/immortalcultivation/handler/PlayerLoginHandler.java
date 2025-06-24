package net.hyper.immortalcultivation.handler;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.cultivation.SpiritualRootType;
import net.hyper.immortalcultivation.cultivation.PhysiqueType;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
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
 * Handles player login initialization for the dual cultivation system.
 * This ensures that all cultivation attachments are properly initialized when
 * a player joins the world, including both Qi and Body cultivation paths.
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImmortalCultivation.LOGGER.info("Player {} logged in, initializing dual cultivation system attachments",
                player.getName().getString());

            // Initialize all cultivation attachments in the proper order
            // Order matters here because some attachments depend on others
            initializeSpiritualRoots(player);
            initializePhysique(player);
            initializeRealms(player);
            initializeCultivationFocus(player);
            initializeCultivationBonus(player);
            initializeCombatStats(player);
            initializeQiCultivation(player);
            initializeBodyCultivation(player);
            initializeResources(player);

            // IMPORTANT: Force sync all data to ensure client has latest values
            forceSyncAllData(player);

            // Check for divine dao destiny achievement (both heavenly talents)
            checkForDivineDaoDestiny(player);

            ImmortalCultivation.LOGGER.info("Dual cultivation system fully initialized for player: {}",
                player.getName().getString());
        }
    }

    /**
     * Initialize spiritual roots for Qi cultivation.
     * This determines the player's natural affinity for Qi cultivation and provides
     * a permanent multiplier to all Qi-related cultivation and regeneration.
     */
    private static void initializeSpiritualRoots(ServerPlayer player) {
        try {
            PlayerSpiritualRootAttachment roots = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());

            if (roots != null && !roots.hasBeenAssigned()) {
                // Generate spiritual roots for new players
                SpiritualRootType generatedType = SpiritualRootType.generateRandom(player.getRandom());
                roots.assignRootType(generatedType);

                // Send rarity-appropriate awakening message
                sendSpiritualRootAwakeningMessage(player, generatedType);

                // Play rarity-appropriate effects
                playSpiritualRootAwakeningEffects(player, generatedType);

                ImmortalCultivation.LOGGER.info("Player {} assigned spiritual roots: {} ({}x multiplier)",
                    player.getName().getString(), generatedType.getDisplayName(), generatedType.getCultivationMultiplier());
            }

            // Log current state for debugging
            if (roots != null) {
                ImmortalCultivation.LOGGER.info("Player {} spiritual roots state - Type: {}, Assigned: {}, Multiplier: {}",
                    player.getName().getString(),
                    roots.getRootType().getDisplayName(),
                    roots.hasBeenAssigned(),
                    roots.getCultivationMultiplier());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing spiritual roots for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Check if player has achieved divine dao destiny (both heavenly talents)
     * This is the rarest possible achievement: 0.01% chance (1 in 10,000)
     */
    private static void checkForDivineDaoDestiny(ServerPlayer player) {
        try {
            PlayerSpiritualRootAttachment rootAttachment = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            PlayerPhysiqueAttachment physiqueAttachment = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());

            if (rootAttachment != null && physiqueAttachment != null) {
                boolean hasHeavenlyRoot = rootAttachment.getRootType().getCultivationMultiplier() == 1.5f;
                boolean hasHeavenlyPhysique = physiqueAttachment.getRootType().getCultivationMultiplier() == 1.5f;

                if (hasHeavenlyRoot && hasHeavenlyPhysique) {
                    // DIVINE DAO DESTINY ACHIEVED!
                    broadcastDivineDaoDestiny(player,
                        rootAttachment.getRootType().getDisplayName(),
                        physiqueAttachment.getRootType().getDisplayName());
                }
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
     * Called when player has both Heavenly Spiritual Root AND Heavenly Physique (0.01% chance)
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
                .append(Component.literal(" has achieved DIVINE DAO DIVINITY!\n"))
                .append(Component.literal("Blessed with BOTH the "))
                .append(root)
                .append(Component.literal(" AND "))
                .append(physique)
                .append(Component.literal("!\n"))
                .append(Component.literal("Trample the Heavens and shatter the Earth, Child of Heaven!"))
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
     * Initialize physique for Body cultivation.
     * Similar to spiritual roots but for the Body cultivation path.
     * This provides a multiplier for Body cultivation and physical enhancement.
     */
    private static void initializePhysique(ServerPlayer player) {
        try {
            PlayerPhysiqueAttachment physique = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());

            if (physique != null && !physique.hasBeenAssigned()) {
                // Generate physique for new players
                PhysiqueType generatedType = PhysiqueType.generateRandom(player.getRandom());
                physique.assignRootType(generatedType);

                // Send rarity-appropriate awakening message
                sendPhysiqueAwakeningMessage(player, generatedType);

                // Play rarity-appropriate effects
                playPhysiqueAwakeningEffects(player, generatedType);

                ImmortalCultivation.LOGGER.info("Player {} assigned physique: {} ({}x multiplier)",
                    player.getName().getString(), generatedType.getDisplayName(), generatedType.getCultivationMultiplier());
            }

            // Log current state for debugging
            if (physique != null) {
                ImmortalCultivation.LOGGER.info("Player {} physique state - Type: {}, Assigned: {}, Multiplier: {}",
                    player.getName().getString(),
                    physique.getRootType().getDisplayName(),
                    physique.hasBeenAssigned(),
                    physique.getCultivationMultiplier());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing physique for player {}",
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
     * Initialize realm attachments for both Qi and Body cultivation.
     * This sets up the dual realm system and ensures both cultivation paths
     * start at the proper mortal realm.
     */
    private static void initializeRealms(ServerPlayer player) {
        try {
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment != null) {
                // Ensure player reference is set for realm calculations
                realmAttachment.setPlayer(player);

                // Initialize Qi realm if null
                if (realmAttachment.getCurrentQiRealm() == null) {
                    ImmortalCultivation.LOGGER.warn("Player {} has null Qi realm, setting to default mortal realm",
                        player.getName().getString());
                    realmAttachment.setCurrentRealm(PlayerRealmAttachment.CultivationType.QI,
                        ICModCultivationRealms.MORTAL, false);
                }

                // Initialize Body realm if null
                if (realmAttachment.getCurrentBodyRealm() == null) {
                    ImmortalCultivation.LOGGER.warn("Player {} has null Body realm, setting to default mortal realm",
                        player.getName().getString());
                    realmAttachment.setCurrentRealm(PlayerRealmAttachment.CultivationType.BODY,
                        ICModCultivationRealms.MORTAL, false);
                }

                ImmortalCultivation.LOGGER.info("Player {} realms initialized - Qi: {}, Body: {}",
                    player.getName().getString(),
                    realmAttachment.getCurrentQiRealm().getDisplayName(),
                    realmAttachment.getCurrentBodyRealm().getDisplayName());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null realm attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing realms for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize cultivation focus system.
     * This allows players to focus their cultivation efforts on either Qi or Body
     * cultivation for enhanced rates and bonuses.
     */
    private static void initializeCultivationFocus(ServerPlayer player) {
        try {
            PlayerCultivationFocusAttachment focusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focusAttachment != null) {
                // Default focus is Qi cultivation
                // The attachment initializes with Qi focus by default, so we just log it
                ImmortalCultivation.LOGGER.info("Player {} cultivation focus initialized: {} cultivation (2x rate bonus)",
                    player.getName().getString(),
                    focusAttachment.getCurrentFocus().getDisplayName());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null cultivation focus attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing cultivation focus for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize cultivation bonus attachment.
     * This manages permanent cultivation rate bonuses from consumed pills and other sources.
     * Bonuses persist through death and logout/login cycles.
     */
    private static void initializeCultivationBonus(ServerPlayer player) {
        try {
            PlayerCultivationBonusAttachment bonusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());
            if (bonusAttachment != null) {
                // Log current bonus state for debugging
                ImmortalCultivation.LOGGER.info("Player {} cultivation bonuses initialized - Qi additive: +{}/sec, Body additive: +{}/sec",
                    player.getName().getString(),
                    String.format("%.3f", bonusAttachment.getQiCultivationRawAdditiveBonus()),
                    String.format("%.3f", bonusAttachment.getBodyCultivationRawAdditiveBonus()));

                // Note: We don't reset bonuses here since they should persist through logout/login
                // The attachment preserves all consumed pill bonuses automatically
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null cultivation bonus attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing cultivation bonus for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize combat stats attachment for tracking attack damage, movement speed, and jump height bonuses.
     * This manages permanent bonuses from cultivation that persist through death and logout.
     */
    private static void initializeCombatStats(ServerPlayer player) {
        try {
            PlayerCombatStatsAttachment combatStatsAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStatsAttachment != null) {
                // IMPORTANT: Set player reference for calculations (CODEC doesn't handle this)
                combatStatsAttachment.setPlayer(player);

                ImmortalCultivation.LOGGER.info("Player {} combat stats initialized - ATK: +{}%, SPD: +{}%, JUMP: +{}%",
                    player.getName().getString(),
                    String.format("%.1f", combatStatsAttachment.getAttackDamageBonus() * 100),
                    String.format("%.1f", combatStatsAttachment.getMovementSpeedBonus() * 100),
                    String.format("%.1f", combatStatsAttachment.getJumpHeightBonus() * 100));
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null combat stats attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing combat stats for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize Qi cultivation attachment.
     * This handles the permanent Qi cultivation progress that determines
     * breakthrough eligibility and provides bonuses.
     */
    private static void initializeQiCultivation(ServerPlayer player) {
        try {
            PlayerQiCultivationAttachment qiCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiCultivationAttachment != null) {
                ImmortalCultivation.LOGGER.info("Player {} Qi cultivation initialized: {}/{}",
                    player.getName().getString(),
                    qiCultivationAttachment.getCurrentQiCultivation(),
                    qiCultivationAttachment.getMaxQiCultivation());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null Qi cultivation attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing Qi cultivation for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize Body cultivation attachment.
     * This handles the permanent Body cultivation progress, similar to Qi cultivation
     * but for the physical enhancement path.
     */
    private static void initializeBodyCultivation(ServerPlayer player) {
        try {
            PlayerBodyCultivationAttachment bodyCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivationAttachment != null) {
                ImmortalCultivation.LOGGER.info("Player {} Body cultivation initialized: {}/{}",
                    player.getName().getString(),
                    bodyCultivationAttachment.getCurrentBodyCultivation(),
                    bodyCultivationAttachment.getMaxBodyCultivation());
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null Body cultivation attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing Body cultivation for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Initialize resource attachments (Qi and Body energy).
     * These are the consumable resources used for techniques and abilities,
     * separate from the cultivation progress values.
     */
    private static void initializeResources(ServerPlayer player) {
        try {
            // Initialize Qi resource attachment
            PlayerQiAttachment qiAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            if (qiAttachment != null) {
                ImmortalCultivation.LOGGER.info("Player {} Qi resource initialized: {}/{} (regen: {}/sec)",
                    player.getName().getString(),
                    qiAttachment.getCurrentQi(),
                    qiAttachment.getMaxQi(),
                    String.format("%.2f", qiAttachment.getEffectiveQiRegenerationRate()));
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null Qi resource attachment!",
                    player.getName().getString());
            }

            // Initialize Body resource attachment
            PlayerBodyAttachment bodyAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY.get());
            if (bodyAttachment != null) {
                ImmortalCultivation.LOGGER.info("Player {} Body resource initialized: {}/{} (regen: {}/sec)",
                    player.getName().getString(),
                    bodyAttachment.getCurrentBody(),
                    bodyAttachment.getMaxBody(),
                    String.format("%.2f", bodyAttachment.getEffectiveBodyRegenerationRate()));
            } else {
                ImmortalCultivation.LOGGER.error("Player {} has null Body resource attachment!",
                    player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing resources for player {}",
                player.getName().getString(), e);
        }
    }

    /**
     * Force sync all cultivation data to ensure client has the latest values.
     * This is critical for proper display in GUIs and HUD elements.
     */
    private static void forceSyncAllData(ServerPlayer player) {
        try {
            ImmortalCultivation.LOGGER.info("Force syncing all cultivation data for player {}",
                player.getName().getString());

            // FIXED: Sync spiritual roots (this was missing!)
            PlayerSpiritualRootAttachment rootAttachment = player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            if (rootAttachment != null) {
                rootAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Synced spiritual roots for player {}", player.getName().getString());
            }

            // FIXED: Sync physique (this was missing!)
            PlayerPhysiqueAttachment physiqueAttachment = player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());
            if (physiqueAttachment != null) {
                physiqueAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Synced physique for player {}", player.getName().getString());
            }

            // Sync realm data
            PlayerRealmAttachment realmAttachment = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            if (realmAttachment != null) {
                realmAttachment.forceSync();
            }

            // Sync cultivation focus
            PlayerCultivationFocusAttachment focusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            if (focusAttachment != null) {
                focusAttachment.forceSync();
            }

            // ADDED: Sync cultivation bonus attachment
            PlayerCultivationBonusAttachment bonusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());
            if (bonusAttachment != null) {
                bonusAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Synced cultivation bonuses for player {}", player.getName().getString());
            }

            // Sync combat stats
            PlayerCombatStatsAttachment combatStatsAttachment = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
            if (combatStatsAttachment != null) {
                combatStatsAttachment.setPlayer(player); // IMPORTANT: Set player reference after CODEC loading
                combatStatsAttachment.forceSync();
                ImmortalCultivation.LOGGER.debug("Synced combat stats for player {}", player.getName().getString());
            }

            // Sync Qi cultivation
            PlayerQiCultivationAttachment qiCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            if (qiCultivationAttachment != null) {
                qiCultivationAttachment.forceSync();
            }

            // Sync Body cultivation
            PlayerBodyCultivationAttachment bodyCultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            if (bodyCultivationAttachment != null) {
                bodyCultivationAttachment.forceSync();
            }

            // Sync resources
            PlayerQiAttachment qiAttachment = player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            if (qiAttachment != null) {
                qiAttachment.forceSync();
            }

            PlayerBodyAttachment bodyAttachment = player.getData(ICModAttachmentTypes.PLAYER_BODY.get());
            if (bodyAttachment != null) {
                bodyAttachment.forceSync();
            }

            PlayerHealthAttachment healthAttachment = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            if (healthAttachment != null) {
                healthAttachment.forceSync();
            }

            ImmortalCultivation.LOGGER.info("Successfully synced all cultivation data for player {}",
                player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error force syncing data for player {}",
                player.getName().getString(), e);
        }
    }
}