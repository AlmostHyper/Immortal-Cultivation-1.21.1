package net.hyper.immortalcultivation.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.core.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.hyper.immortalcultivation.core.attachments.IStatsData;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.cultivation.CultivationRealmRegistry;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Unified Command Manager for the Immortal Cultivation Mod
 *
 * This class serves as the central hub for all command registration and execution
 * logic, following the same architectural principles as ICModNetworkManager with
 * singleton pattern, performance monitoring, and comprehensive debug capabilities.
 *
 * Key Features:
 * - Centralized command registration and management
 * - Performance monitoring for command execution
 * - Comprehensive debug and logging capabilities
 * - Modular command handler system
 * - Permission and validation management
 * - Consistent error handling and user feedback
 *
 * Architecture:
 * - Singleton pattern for global access
 * - Event-driven registration through RegisterCommandsEvent
 * - Modular design for easy command addition
 * - Performance optimization through caching and monitoring
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommandManager {

    // === SINGLETON INSTANCE === //
    public static final CommandManager INSTANCE = new CommandManager();

    // === CONFIGURATION CONSTANTS === //
    private static final String COMMAND_PREFIX = "ic";
    private static final String ADMIN_PERMISSION_LEVEL = "admin";
    private static final String PLAYER_PERMISSION_LEVEL = "player";
    private static final int MAX_COMMAND_EXECUTION_TIME_MS = 100; // Warn if command takes longer than 100ms
    private static final int DEBUG_COMMAND_HISTORY_SIZE = 50;

    // === DEBUG AND MONITORING === //
    private static boolean DEBUG_COMMANDS = true;
    private static boolean PERFORMANCE_MONITORING = true;

    // === COMMAND TRACKING === //
    private final Map<String, CommandHandler> registeredCommands = new ConcurrentHashMap<>();
    private final Map<String, Long> commandExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> commandUsageCount = new ConcurrentHashMap<>();
    private final List<String> recentCommandHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, Long> playerCommandCooldowns = new ConcurrentHashMap<>();

    // === SUGGESTION PROVIDERS === //

    /**
     * Suggestion provider for cultivation types (qi, body)
     */
    private static final SuggestionProvider<CommandSourceStack> CULTIVATION_TYPE_SUGGESTIONS =
        (context, builder) -> SharedSuggestionProvider.suggest(
            new String[]{"qi", "body"}, builder);

    /**
     * Suggestion provider for stat types (health, qi, body, qi_cultivation, body_cultivation)
     */
    private static final SuggestionProvider<CommandSourceStack> STAT_TYPE_SUGGESTIONS =
        (context, builder) -> SharedSuggestionProvider.suggest(
            new String[]{"health", "qi", "body", "qi_cultivation", "body_cultivation"}, builder);

    /**
     * Suggestion provider for cultivation realm IDs
     */
    private static final SuggestionProvider<CommandSourceStack> REALM_ID_SUGGESTIONS =
        (context, builder) -> {
            try {
                return SharedSuggestionProvider.suggest(
                    Arrays.stream(CultivationRealmRegistry.getRealmIds()).sequential()
                        .map(id -> "\"" + id + "\""), // Quote realm IDs that might contain spaces
                    builder);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.warn("Failed to provide realm suggestions: {}", e.getMessage());
                return builder.buildFuture();
            }
        };

    // === PERFORMANCE TRACKING === //
    private final Map<String, Long> performanceTimers = new ConcurrentHashMap<>();

    /**
     * Private constructor for singleton pattern
     */
    private CommandManager() {
        if (DEBUG_COMMANDS) {
            ImmortalCultivation.LOGGER.info("ICModCommandManager instance created");
        }
    }

    // === COMMAND REGISTRATION EVENT === //

    /**
     * Handles the command registration event and registers all mod commands
     *
     * @param event The RegisterCommandsEvent from NeoForge
     */
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        ImmortalCultivation.LOGGER.info("Registering Immortal Cultivation commands...");

        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        INSTANCE.registerAllCommands(dispatcher);

        ImmortalCultivation.LOGGER.info("Successfully registered {} Immortal Cultivation commands",
            INSTANCE.registeredCommands.size());
    }

    // === COMMAND REGISTRATION METHODS === //

    /**
     * Registers all mod commands with the dispatcher
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    private void registerAllCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        startPerformanceTimer("command_registration");

        try {
            // Create the main command root
            LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal(COMMAND_PREFIX)
                .executes(this::handleRootCommand);

            // Register sub-commands here (will be added in following segments)
            registerCultivationCommands(rootCommand);
            registerDebugCommands(rootCommand);
            registerUtilityCommands(rootCommand);

            // Register the complete command tree
            dispatcher.register(rootCommand);

            if (DEBUG_COMMANDS) {
                ImmortalCultivation.LOGGER.debug("Command tree structure: {}",
                    getCommandTreeString(rootCommand));
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error during command registration: {}", e.getMessage(), e);
        } finally {
            endPerformanceTimer("command_registration", 50); // 50ms warning threshold
        }
    }

    /**
     * Registers cultivation-related commands (cultivation realm, stats, etc.)
     *
     * @param rootCommand The root command builder to add sub-commands to
     */
    private void registerCultivationCommands(LiteralArgumentBuilder<CommandSourceStack> rootCommand) {
        // /ic set realm <qi|body> <realm_id>
        rootCommand.then(Commands.literal("set")
            .then(Commands.literal("realm")
                .then(Commands.argument("cultivation_type", StringArgumentType.string())
                    .suggests(CULTIVATION_TYPE_SUGGESTIONS)
                    .then(Commands.argument("realm_id", StringArgumentType.string())
                        .suggests(REALM_ID_SUGGESTIONS)
                        .executes(this::handleSetRealmCommand)
                    )
                )
            )
            // /ic set stat <health|qi|body> <value>
            .then(Commands.literal("stat")
                .then(Commands.argument("stat_type", StringArgumentType.string())
                    .suggests(STAT_TYPE_SUGGESTIONS)
                    .then(Commands.argument("value", LongArgumentType.longArg(0))
                        .executes(this::handleSetStatCommand)
                    )
                )
            )
        );

        // /ic get realm [qi|body]
        rootCommand.then(Commands.literal("get")
            .then(Commands.literal("realm")
                .executes(this::handleGetRealmCommand)
                .then(Commands.argument("cultivation_type", StringArgumentType.string())
                    .suggests(CULTIVATION_TYPE_SUGGESTIONS)
                    .executes(this::handleGetRealmCommand)
                )
            )
            // /ic get stat [health|qi|body]
            .then(Commands.literal("stat")
                .executes(this::handleGetStatCommand)
                .then(Commands.argument("stat_type", StringArgumentType.string())
                    .suggests(STAT_TYPE_SUGGESTIONS)
                    .executes(this::handleGetStatCommand)
                )
            )
        );

        if (DEBUG_COMMANDS) {
            ImmortalCultivation.LOGGER.debug("Cultivation commands registered successfully");
        }
    }

    /**
     * Registers debug-related commands
     *
     * @param rootCommand The root command builder to add sub-commands to
     */
    private void registerDebugCommands(LiteralArgumentBuilder<CommandSourceStack> rootCommand) {
        // Add debug command support
        rootCommand.then(Commands.literal("debug")
            .then(Commands.literal("commands")
                .executes(this::handleDebugCommandsInfo)
            )
        );
    }

    /**
     * Registers utility commands (help, status, etc.)
     *
     * @param rootCommand The root command builder to add sub-commands to
     */
    private void registerUtilityCommands(LiteralArgumentBuilder<CommandSourceStack> rootCommand) {
        // Add help command
        rootCommand.then(Commands.literal("help")
            .executes(this::handleHelpCommand)
        );
    }

    // === COMMAND EXECUTION METHODS === //

    /**
     * Handles the root command execution (shows basic info)
     *
     * @param context The command context
     * @return Command execution result
     */
    private int handleRootCommand(CommandContext<CommandSourceStack> context) {
        return executeCommand("root", context, (ctx, player) -> {
            Component message = Component.literal("Immortal Cultivation Mod v" + getModVersion())
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("\nUse ")
                    .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/" + COMMAND_PREFIX + " help")
                    .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" for available commands")
                    .withStyle(ChatFormatting.GRAY));

            ctx.getSource().sendSuccess(() -> message, false);
            return 1;
        });
    }

    /**
     * Handles the help command execution
     *
     * @param context The command context
     * @return Command execution result
     */
    private int handleHelpCommand(CommandContext<CommandSourceStack> context) {
        return executeCommand("help", context, (ctx, player) -> {
            Component helpMessage = Component.literal("=== Immortal Cultivation Commands ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\n/" + COMMAND_PREFIX + " help")
                    .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - Show this help message")
                    .withStyle(ChatFormatting.GRAY))

                .append(Component.literal("\n\n--- Cultivation Management ---")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal("\n/" + COMMAND_PREFIX + " set realm <qi|body> <realm_id>")
                    .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - Set cultivation realm")
                    .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n/" + COMMAND_PREFIX + " get realm [qi|body]")
                    .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - View current cultivation realms")
                    .withStyle(ChatFormatting.GRAY))

                .append(Component.literal("\n\n--- Stats Management ---")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal("\n/" + COMMAND_PREFIX + " set stat <health|qi|body|qi_cultivation|body_cultivation> <value>")
                    .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - Set current stat or cultivation value")
                    .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n/" + COMMAND_PREFIX + " get stat [health|qi|body]")
                    .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - View current stats")
                    .withStyle(ChatFormatting.GRAY))

                .append(Component.literal("\n\n--- Debug ---")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("\n/" + COMMAND_PREFIX + " debug commands")
                    .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - Show command debug information")
                    .withStyle(ChatFormatting.GRAY));

            ctx.getSource().sendSuccess(() -> helpMessage, false);
            return 1;
        });
    }

    /**
     * Handles debug commands info
     *
     * @param context The command context
     * @return Command execution result
     */
    private int handleDebugCommandsInfo(CommandContext<CommandSourceStack> context) {
        return executeCommand("debug_commands", context, (ctx, player) -> {
            Component debugInfo = Component.literal("=== Command Debug Information ===")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal("\nRegistered Commands: " + registeredCommands.size())
                    .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\nTotal Command Executions: " +
                        commandUsageCount.values().stream().mapToInt(Integer::intValue).sum())
                    .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\nDebug Mode: " + (DEBUG_COMMANDS ? "Enabled" : "Disabled"))
                    .withStyle(DEBUG_COMMANDS ? ChatFormatting.GREEN : ChatFormatting.RED));

            ctx.getSource().sendSuccess(() -> debugInfo, false);
            return 1;
        });
    }

    // === CULTIVATION COMMAND HANDLERS === //

    /**
     * Handles the set realm command: /ic set realm <qi|body> <realm_id>
     *
     * @param context The command context
     * @return Command execution result
     */
    private int handleSetRealmCommand(CommandContext<CommandSourceStack> context) {
        return executeCommand("set_realm", context, (ctx, player) -> {
            if (player == null) {
                ctx.getSource().sendFailure(Component.literal("This command can only be used by players")
                    .withStyle(ChatFormatting.RED));
                return 0;
            }

            try {
                String cultivationTypeStr = StringArgumentType.getString(ctx, "cultivation_type").toLowerCase();
                String realmId = StringArgumentType.getString(ctx, "realm_id");

                // Parse cultivation type
                ICultivationData.CultivationType cultivationType = parseCultivationType(cultivationTypeStr);
                if (cultivationType == null) {
                    ctx.getSource().sendFailure(Component.literal("Invalid cultivation type: " + cultivationTypeStr)
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(" (Valid: qi, body)")
                            .withStyle(ChatFormatting.GRAY)));
                    return 0;
                }

                // Get the realm
                CultivationRealm realm = CultivationRealmRegistry.get(realmId);
                if (realm == null) {
                    ctx.getSource().sendFailure(Component.literal("Unknown realm: " + realmId)
                        .withStyle(ChatFormatting.RED));
                    return 0;
                }

                // Get cultivation attachment
                PlayerCultivationAttachment cultivation = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
                if (cultivation == null) {
                    ctx.getSource().sendFailure(Component.literal("Player cultivation data not found")
                        .withStyle(ChatFormatting.RED));
                    return 0;
                }

                // Get stats attachment for bonus calculation
                PlayerStatsAttachment stats = player.getData(AttachmentRegistry.PLAYER_STATS.get());
                if (stats == null) {
                    ctx.getSource().sendFailure(Component.literal("Player stats data not found")
                        .withStyle(ChatFormatting.RED));
                    return 0;
                }

                // Set the realm
                CultivationRealm oldRealm = cultivation.getRealm(cultivationType);
                cultivation.setRealm(cultivationType, realm, false);

                // Apply CUMULATIVE realm modifiers - calculate all bonuses from mortal to target realm
                applyCumulativeRealmBonuses(player, cultivationType, realm, cultivation);

                // Refresh stats to apply changes

                // Send success message
                Component successMessage = Component.literal("Successfully set ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(cultivationType.getDisplayName())
                        .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" cultivation realm from ")
                        .withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(oldRealm.getDisplayName())
                        .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" to ")
                        .withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(realm.getDisplayName())
                        .withStyle(ChatFormatting.GOLD));

                ctx.getSource().sendSuccess(() -> successMessage, false);

                if (DEBUG_COMMANDS) {
                    ImmortalCultivation.LOGGER.info("Player {} realm changed: {} {} -> {}",
                        player.getName().getString(), cultivationType, oldRealm.getId(), realm.getId());
                }

                return 1;

            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error in set realm command: {}", e.getMessage(), e);
                ctx.getSource().sendFailure(Component.literal("An error occurred while setting the realm")
                    .withStyle(ChatFormatting.RED));
                return 0;
            }
        });
    }

    /**
     * Handles the set stat command: /ic set stat <health|qi|body> <value>
     *
     * @param context The command context
     * @return Command execution result
     */
    private int handleSetStatCommand(CommandContext<CommandSourceStack> context) {
        return executeCommand("set_stat", context, (ctx, player) -> {
            if (player == null) {
                ctx.getSource().sendFailure(Component.literal("This command can only be used by players")
                    .withStyle(ChatFormatting.RED));
                return 0;
            }

            try {
                String statTypeStr = StringArgumentType.getString(ctx, "stat_type").toLowerCase();
                long value = LongArgumentType.getLong(ctx, "value");

                // Parse stat type or cultivation type
                Object[] parsed = parseStatTypeOrCultivation(statTypeStr);
                if (parsed == null) {
                    ctx.getSource().sendFailure(Component.literal("Invalid stat type: " + statTypeStr)
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(" (Valid: health, qi, body, qi_cultivation, body_cultivation)")
                            .withStyle(ChatFormatting.GRAY)));
                    return 0;
                }

                boolean isCultivationStat = (boolean) parsed[1];

                if (isCultivationStat) {
                    // Handle cultivation stat
                    ICultivationData.CultivationType cultivationType = (ICultivationData.CultivationType) parsed[0];

                    PlayerCultivationAttachment cultivation = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
                    if (cultivation == null) {
                        ctx.getSource().sendFailure(Component.literal("Player cultivation data not found")
                            .withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    // Validate value against maximum
                    long maxValue = cultivation.getMaxCultivation(cultivationType);
                    if (value > maxValue) {
                        ctx.getSource().sendFailure(Component.literal("Value " + value + " exceeds maximum " + maxValue)
                            .withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    // Store old value for milestone calculation
                    long oldValue = cultivation.getCurrentCultivation(cultivationType);

                    // Set the new cultivation value (this will trigger milestone processing)
                    cultivation.setCurrentCultivation(cultivationType, value);

                    // FIXED: Ensure milestones are properly recalculated after set command
                    // This prevents double-application and ensures consistency
                    cultivation.recalculateMilestoneBonuses(cultivationType);

                    ctx.getSource().sendSuccess(() -> Component.literal("Set " + cultivationType.getDisplayName() +
                            " cultivation to " + value + " (was " + oldValue + ")")
                        .withStyle(ChatFormatting.GREEN), false);

                    if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                        ImmortalCultivation.LOGGER.debug("Set {} cultivation for {}: {} -> {} (max: {})",
                            cultivationType, player.getName().getString(), oldValue, value, maxValue);
                    }

                    return 1;
                } else {
                    // Handle regular stat (existing code)
                    IStatsData.StatType statType = (IStatsData.StatType) parsed[0];

                    PlayerStatsAttachment stats = player.getData(AttachmentRegistry.PLAYER_STATS.get());
                    if (stats == null) {
                        ctx.getSource().sendFailure(Component.literal("Player stats data not found")
                            .withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    // Validate value against maximum
                    long maxValue = stats.getMaxStat(statType);
                    if (value > maxValue) {
                        ctx.getSource().sendFailure(Component.literal("Value " + value + " exceeds maximum " + statType.getDisplayName() + " (" + maxValue + ")")
                            .withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    // Set the stat
                    long oldValue = stats.getCurrentStat(statType);
                    stats.setCurrentStat(statType, value);

                    // Send success message
                    Component successMessage = Component.literal("Successfully set ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(statType.getDisplayName())
                            .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" from ")
                            .withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(String.valueOf(oldValue))
                            .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" to ")
                            .withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(String.valueOf(value))
                            .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("/" + maxValue)
                            .withStyle(ChatFormatting.GRAY));

                    ctx.getSource().sendSuccess(() -> successMessage, false);

                    if (DEBUG_COMMANDS) {
                        ImmortalCultivation.LOGGER.info("Player {} stat changed: {} {} -> {}",
                            player.getName().getString(), statType, oldValue, value);
                    }
                }

                return 1;

            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error in set stat command: {}", e.getMessage(), e);
                ctx.getSource().sendFailure(Component.literal("An error occurred while setting the stat")
                    .withStyle(ChatFormatting.RED));
                return 0;
            }
        });
    }

    /**
     * Handles the get realm command: /ic get realm [qi|body]
     *
     * @param context The command context
     * @return Command execution result
     */
    private int handleGetRealmCommand(CommandContext<CommandSourceStack> context) {
        return executeCommand("get_realm", context, (ctx, player) -> {
            if (player == null) {
                ctx.getSource().sendFailure(Component.literal("This command can only be used by players")
                    .withStyle(ChatFormatting.RED));
                return 0;
            }

            try {
                PlayerCultivationAttachment cultivation = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
                if (cultivation == null) {
                    ctx.getSource().sendFailure(Component.literal("Player cultivation data not found")
                        .withStyle(ChatFormatting.RED));
                    return 0;
                }

                // Check if specific cultivation type was provided
                String cultivationTypeStr = null;
                try {
                    cultivationTypeStr = StringArgumentType.getString(ctx, "cultivation_type").toLowerCase();
                } catch (IllegalArgumentException ignored) {
                    // No cultivation type specified, show both
                }

                Component message;
                if (cultivationTypeStr != null) {
                    // Show specific cultivation type
                    ICultivationData.CultivationType cultivationType = parseCultivationType(cultivationTypeStr);
                    if (cultivationType == null) {
                        ctx.getSource().sendFailure(Component.literal("Invalid cultivation type: " + cultivationTypeStr)
                            .withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    CultivationRealm realm = cultivation.getRealm(cultivationType);
                    message = Component.literal(cultivationType.getDisplayName() + " Realm: ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(realm.getDisplayName())
                            .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" (Level " + realm.getRealmLevel() + ")")
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    // Show both cultivation types
                    CultivationRealm qiRealm = cultivation.getRealm(ICultivationData.CultivationType.QI);
                    CultivationRealm bodyRealm = cultivation.getRealm(ICultivationData.CultivationType.BODY);

                    message = Component.literal("=== Current Cultivation Realms ===")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        .append(Component.literal("\nQi Realm: ")
                            .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(qiRealm.getDisplayName())
                            .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" (Level " + qiRealm.getRealmLevel() + ")")
                            .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("\nBody Realm: ")
                            .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(bodyRealm.getDisplayName())
                            .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" (Level " + bodyRealm.getRealmLevel() + ")")
                            .withStyle(ChatFormatting.GRAY));
                }

                ctx.getSource().sendSuccess(() -> message, false);
                return 1;

            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error in get realm command: {}", e.getMessage(), e);
                ctx.getSource().sendFailure(Component.literal("An error occurred while getting realm information")
                    .withStyle(ChatFormatting.RED));
                return 0;
            }
        });
    }

    /**
     * Handles the get stat command: /ic get stat [health|qi|body]
     *
     * @param context The command context
     * @return Command execution result
     */
    private int handleGetStatCommand(CommandContext<CommandSourceStack> context) {
        return executeCommand("get_stat", context, (ctx, player) -> {
            if (player == null) {
                ctx.getSource().sendFailure(Component.literal("This command can only be used by players")
                    .withStyle(ChatFormatting.RED));
                return 0;
            }

            try {
                PlayerStatsAttachment stats = player.getData(AttachmentRegistry.PLAYER_STATS.get());
                if (stats == null) {
                    ctx.getSource().sendFailure(Component.literal("Player stats data not found")
                        .withStyle(ChatFormatting.RED));
                    return 0;
                }

                // Check if specific stat type was provided
                String statTypeStr = null;
                try {
                    statTypeStr = StringArgumentType.getString(ctx, "stat_type").toLowerCase();
                } catch (IllegalArgumentException ignored) {
                    // No stat type specified, show all
                }

                Component message;
                if (statTypeStr != null) {
                    // Show specific stat type
                    Object[] parsed = parseStatTypeOrCultivation(statTypeStr);
                    if (parsed == null) {
                        ctx.getSource().sendFailure(Component.literal("Invalid stat type: " + statTypeStr)
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal(" (Valid: health, qi, body, qi_cultivation, body_cultivation)")
                                .withStyle(ChatFormatting.GRAY)));
                        return 0;
                    }

                    boolean isCultivationStat = (boolean) parsed[1];

                    if (isCultivationStat) {
                        // Handle cultivation stat
                        ICultivationData.CultivationType cultivationType = (ICultivationData.CultivationType) parsed[0];

                        PlayerCultivationAttachment cultivation = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
                        if (cultivation == null) {
                            ctx.getSource().sendFailure(Component.literal("Player cultivation data not found")
                                .withStyle(ChatFormatting.RED));
                            return 0;
                        }

                        long current = cultivation.getCurrentCultivation(cultivationType);
                        long max = cultivation.getMaxCultivation(cultivationType);
                        double percentage = max > 0 ? (double) current / max * 100 : 0;

                        message = Component.literal(cultivationType.getDisplayName() + " Cultivation: ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(current + "/" + max)
                                .withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(" (" + String.format("%.1f", percentage) + "%)")
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        // Handle regular stat
                        IStatsData.StatType statType = (IStatsData.StatType) parsed[0];

                        long current = stats.getCurrentStat(statType);
                        long max = stats.getMaxStat(statType);
                        double percentage = max > 0 ? (double) current / max * 100 : 0;

                        message = Component.literal(statType.getDisplayName() + ": ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(current + "/" + max)
                                .withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(" (" + String.format("%.1f", percentage) + "%)")
                                .withStyle(ChatFormatting.GRAY));
                    }
                } else {
                    // Show all stats (both regular stats and cultivation stats)
                    var builder = Component.literal("=== Current Stats ===")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

                    // Show regular stats
                    for (IStatsData.StatType statType : IStatsData.StatType.values()) {
                        long current = stats.getCurrentStat(statType);
                        long max = stats.getMaxStat(statType);
                        double percentage = max > 0 ? (double) current / max * 100 : 0;

                        builder.append(Component.literal("\n" + statType.getDisplayName() + ": ")
                                .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(current + "/" + max)
                                .withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(" (" + String.format("%.1f", percentage) + "%)")
                                .withStyle(ChatFormatting.GRAY));
                    }

                    // Show cultivation stats
                    PlayerCultivationAttachment cultivation = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
                    if (cultivation != null) {
                        for (ICultivationData.CultivationType cultivationType : ICultivationData.CultivationType.values()) {
                            long current = cultivation.getCurrentCultivation(cultivationType);
                            long max = cultivation.getMaxCultivation(cultivationType);
                            double percentage = max > 0 ? (double) current / max * 100 : 0;

                            builder.append(Component.literal("\n" + cultivationType.getDisplayName() + " Cultivation: ")
                                    .withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(current + "/" + max)
                                    .withStyle(ChatFormatting.GOLD))
                                .append(Component.literal(" (" + String.format("%.1f", percentage) + "%)")
                                    .withStyle(ChatFormatting.GRAY));
                        }
                    }

                    message = builder;
                }

                ctx.getSource().sendSuccess(() -> message, false);
                return 1;

            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error in get stat command: {}", e.getMessage(), e);
                ctx.getSource().sendFailure(Component.literal("An error occurred while getting stat information")
                    .withStyle(ChatFormatting.RED));
                return 0;
            }
        });
    }

    // === UTILITY METHODS === //

    // === PARSING HELPER METHODS === //

    /**
     * Parses a string into a CultivationType
     *
     * @param typeStr The string to parse (qi, body)
     * @return The corresponding CultivationType, or null if invalid
     */
    private ICultivationData.CultivationType parseCultivationType(String typeStr) {
        return switch (typeStr.toLowerCase()) {
            case "qi" -> ICultivationData.CultivationType.QI;
            case "body" -> ICultivationData.CultivationType.BODY;
            default -> null;
        };
    }

    /**
     * Parses a string into a StatType or CultivationType for cultivation stats
     *
     * @param typeStr The string to parse (health, qi, body, qi_cultivation, body_cultivation)
     * @return Object[] with [StatType/CultivationType, boolean isCultivationStat]
     */
    private Object[] parseStatTypeOrCultivation(String typeStr) {
        return switch (typeStr.toLowerCase()) {
            case "health" -> new Object[]{IStatsData.StatType.HEALTH, false};
            case "qi" -> new Object[]{IStatsData.StatType.QI, false};
            case "body" -> new Object[]{IStatsData.StatType.BODY, false};
            case "qi_cultivation" -> new Object[]{ICultivationData.CultivationType.QI, true};
            case "body_cultivation" -> new Object[]{ICultivationData.CultivationType.BODY, true};
            default -> null;
        };
    }

    /**
     * Generic command execution wrapper with performance monitoring and error handling
     *
     * @param commandName The name of the command for tracking
     * @param context The command context
     * @param executor The command execution function
     * @return Command execution result
     */
    private int executeCommand(String commandName, CommandContext<CommandSourceStack> context,
                               BiFunction<CommandContext<CommandSourceStack>, ServerPlayer, Integer> executor) {
        long startTime = System.currentTimeMillis();

        try {
            ServerPlayer player = context.getSource().getPlayer();

            // Track command usage
            commandUsageCount.merge(commandName, 1, Integer::sum);
            addToCommandHistory(commandName, player);

            if (DEBUG_COMMANDS) {
                ImmortalCultivation.LOGGER.debug("Executing command '{}' for player: {}",
                    commandName, player != null ? player.getName().getString() : "Console");
            }

            // Execute the command
            int result = executor.apply(context, player);

            return result;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error executing command '{}': {}", commandName, e.getMessage(), e);

            Component errorMessage = Component.literal("An error occurred while executing the command. Please check the logs.")
                .withStyle(ChatFormatting.RED);
            context.getSource().sendFailure(errorMessage);

            return 0;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            commandExecutionTimes.put(commandName, executionTime);

            if (PERFORMANCE_MONITORING && executionTime > MAX_COMMAND_EXECUTION_TIME_MS) {
                ImmortalCultivation.LOGGER.warn("Command '{}' took {}ms to execute (threshold: {}ms)",
                    commandName, executionTime, MAX_COMMAND_EXECUTION_TIME_MS);
            }
        }
    }

    // === PERFORMANCE MONITORING === //

    /**
     * Starts a performance timer for the given operation
     *
     * @param operation The operation name
     */
    private void startPerformanceTimer(String operation) {
        if (PERFORMANCE_MONITORING) {
            performanceTimers.put(operation, System.currentTimeMillis());
        }
    }

    /**
     * Ends a performance timer and logs if it exceeds the threshold
     *
     * @param operation The operation name
     * @param thresholdMs The warning threshold in milliseconds
     */
    private void endPerformanceTimer(String operation, long thresholdMs) {
        if (PERFORMANCE_MONITORING) {
            Long startTime = performanceTimers.remove(operation);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                if (duration > thresholdMs) {
                    ImmortalCultivation.LOGGER.warn("Operation '{}' took {}ms (threshold: {}ms)",
                        operation, duration, thresholdMs);
                }
            }
        }
    }

    // === HELPER METHODS === //

    /**
     * Adds a command to the execution history
     *
     * @param commandName The command name
     * @param player The player who executed it (can be null for console)
     */
    private void addToCommandHistory(String commandName, ServerPlayer player) {
        String historyEntry = String.format("[%d] %s by %s",
            System.currentTimeMillis(),
            commandName,
            player != null ? player.getName().getString() : "Console");

        recentCommandHistory.add(historyEntry);

        // Keep history size limited
        while (recentCommandHistory.size() > DEBUG_COMMAND_HISTORY_SIZE) {
            recentCommandHistory.remove(0);
        }
    }

    /**
     * Gets a string representation of the command tree structure
     *
     * @param rootCommand The root command builder
     * @return String representation of the tree
     */
    private String getCommandTreeString(LiteralArgumentBuilder<CommandSourceStack> rootCommand) {
        // Simple implementation - could be expanded for more detailed tree view
        return COMMAND_PREFIX + " (with " + registeredCommands.size() + " sub-commands)";
    }

    /**
     * Gets the current mod version
     *
     * @return The mod version string
     */
    private String getModVersion() {
        // This could be read from mod metadata, for now return a placeholder
        return "1.0.0";
    }

    // === CONFIGURATION METHODS === //

    /**
     * Enables or disables command debugging
     *
     * @param enabled Whether to enable debug mode
     */
    public static void setDebugCommands(boolean enabled) {
        DEBUG_COMMANDS = enabled;
        ImmortalCultivation.LOGGER.info("Command debugging {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Enables or disables performance monitoring
     *
     * @param enabled Whether to enable performance monitoring
     */
    public static void setPerformanceMonitoring(boolean enabled) {
        PERFORMANCE_MONITORING = enabled;
        ImmortalCultivation.LOGGER.info("Command performance monitoring {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Applies cumulative realm bonuses by simulating natural progression.
     * This method applies each realm's bonuses in sequence from mortal to target realm.
     *
     * @param player The player
     * @param cultivationType The cultivation type being set
     * @param targetRealm The target realm to set
     * @param cultivation The player's cultivation attachment
     */
    private void applyCumulativeRealmBonuses(ServerPlayer player, ICultivationData.CultivationType cultivationType,
                                             CultivationRealm targetRealm, PlayerCultivationAttachment cultivation) {
        try {
            // Get the progression path from mortal to target realm
            List<CultivationRealm> progressionPath = getRealmProgressionPath(targetRealm);

            if (progressionPath.isEmpty()) {
                ImmortalCultivation.LOGGER.warn("No progression path found for realm: {}", targetRealm.getId());
                return;
            }

            // Apply each realm's bonuses in sequence (this simulates natural progression)
            for (CultivationRealm realm : progressionPath) {
                // Set realm temporarily and apply its modifiers
                cultivation.setRealm(cultivationType, realm, false);
                realm.applyRealmModifiers(player, cultivation, cultivationType, false);

                if (DEBUG_COMMANDS) {
                    ImmortalCultivation.LOGGER.debug("Applied bonuses for realm: {} (Level {})",
                        realm.getDisplayName(), realm.getRealmLevel());
                }
            }

            if (DEBUG_COMMANDS) {
                ImmortalCultivation.LOGGER.info("Applied {} realms in progression to reach {}",
                    progressionPath.size(), targetRealm.getDisplayName());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying cumulative realm bonuses: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets the complete progression path from mortal realm to the target realm.
     * This uses the realm level system to include ALL realms that should be traversed,
     * including different stages and special realms nested between regular realms.
     *
     * @param targetRealm The target realm
     * @return List of realms in progression order (including target)
     */
    private List<CultivationRealm> getRealmProgressionPath(CultivationRealm targetRealm) {
        List<CultivationRealm> path = new ArrayList<>();

        try {
            int targetLevel = targetRealm.getRealmLevel();

            if (DEBUG_COMMANDS) {
                ImmortalCultivation.LOGGER.debug("Building progression path to realm: {} (Level {})",
                    targetRealm.getId(), targetLevel);
            }

            // Get all available realms as a collection
            Collection<CultivationRealm> allRealms = CultivationRealmRegistry.getAllRealms();

            // Create a list of all realms with levels from 1 to target level
            List<CultivationRealm> candidateRealms = new ArrayList<>();
            for (CultivationRealm realm : allRealms) {
                int realmLevel = realm.getRealmLevel();
                if (realmLevel > 0 && realmLevel <= targetLevel) {
                    candidateRealms.add(realm);
                    if (DEBUG_COMMANDS) {
                        ImmortalCultivation.LOGGER.debug("Added candidate realm: {} (Level {})",
                            realm.getId(), realmLevel);
                    }
                }
            }

            // Sort by realm level to ensure proper progression order
            candidateRealms.sort((a, b) -> Integer.compare(a.getRealmLevel(), b.getRealmLevel()));

            // Add all realms in order
            path.addAll(candidateRealms);

            if (DEBUG_COMMANDS) {
                ImmortalCultivation.LOGGER.info("Generated complete progression path with {} realms for target: {} (Level {})",
                    path.size(), targetRealm.getId(), targetLevel);

                // Log the complete path for debugging
                StringBuilder pathStr = new StringBuilder("Progression path: ");
                for (int i = 0; i < path.size(); i++) {
                    CultivationRealm realm = path.get(i);
                    pathStr.append(realm.getId()).append("(L").append(realm.getRealmLevel()).append(")");
                    if (i < path.size() - 1) pathStr.append(" -> ");
                }
                ImmortalCultivation.LOGGER.debug(pathStr.toString());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error building progression path for realm {}: {}",
                targetRealm.getId(), e.getMessage(), e);
            // Fallback: just add the target realm
            path.add(targetRealm);
        }

        return path;
    }

    /**
     * Clears existing bonuses that would come from cultivation realms.
     * This prevents double-applying bonuses when setting realms via command.
     *
     * @param stats The player stats attachment
     * @param cultivationType The cultivation type being reset
     */
    private void clearCultivationTypeBonuses(PlayerStatsAttachment stats, ICultivationData.CultivationType cultivationType) {
        // Note: This is a simplified approach. In a more sophisticated system,
        // you might track which bonuses came from which sources.
        // For now, we'll reset to mortal base values and re-apply everything.

        // Reset to mortal base values (this assumes mortal realm provides no bonuses)
        // You might need to adjust this based on your specific mortal realm implementation
        stats.setPermanentStatBonus(IStatsData.StatType.HEALTH, 0);
        stats.setPermanentStatBonus(IStatsData.StatType.QI, 0);
        stats.setPermanentStatBonus(IStatsData.StatType.BODY, 0);
        stats.setPermanentStatRegenBonus(IStatsData.StatType.HEALTH, 0.0);
        stats.setPermanentStatRegenBonus(IStatsData.StatType.QI, 0.0);
        stats.setPermanentStatRegenBonus(IStatsData.StatType.BODY, 0.0);
        stats.setPermanentAttackDamageBonus(0.0);
        stats.setPermanentMovementSpeedBonus(0.0);
        stats.setPermanentJumpHeightBonus(0.0);

        if (DEBUG_COMMANDS) {
            ImmortalCultivation.LOGGER.debug("Cleared existing bonuses for cultivation type: {}", cultivationType);
        }
    }

    // === INTERNAL COMMAND HANDLER INTERFACE === //

    /**
     * Interface for command handlers to standardize command execution
     */
    @FunctionalInterface
    private interface CommandHandler {
        int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }
}