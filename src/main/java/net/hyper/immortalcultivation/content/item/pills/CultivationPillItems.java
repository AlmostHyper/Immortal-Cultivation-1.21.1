package net.hyper.immortalcultivation.content.item.pills;

import net.hyper.immortalcultivation.common.capability.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.capability.attachments.PlayerPillConsumptionAttachment;
import net.hyper.immortalcultivation.content.block.entities.PillFurnaceBlockEntity;
import net.hyper.immortalcultivation.content.item.BaseConsumableItem;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Supplier;

public class CultivationPillItems {

    // Base cultivation rate bonuses for each pill tier (per second)
    private static final double LOW_QUALITY_BASE_RATE = 0.005;      // 0.005/sec
    private static final double MEDIUM_QUALITY_BASE_RATE = 0.02;    // 0.02/sec
    private static final double HIGH_QUALITY_BASE_RATE = 0.1;       // 0.1/sec
    private static final double TOP_QUALITY_BASE_RATE = 1.0;        // 1.0/sec

    // === QI CULTIVATION PILLS ===

    // Low Quality Qi Cultivation Pill
    public static Supplier<Item> LOW_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    // Get both attachments - cultivation for effects, pill consumption for tracking
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

                    if (cultivation != null && pillAttachment != null) {
                        double baseRateBonus = LOW_QUALITY_BASE_RATE;
                        double rarityMultipliedBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        // Apply diminishing returns using the new pill attachment
                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                        double effectiveness = pillManager.consumePill(this, rarity);
                        double finalRateBonus = rarityMultipliedBonus * effectiveness;

                        // Apply cultivation effect
                        cultivation.addQiBonusWithDiminishing(finalRateBonus, 0.0, 1.0);

                        // Sync pill consumption data to client for tooltip updates
                        pillAttachment.forceSync();

                        // Enhanced message with diminishing returns info
                        String rarityText = rarity != null ? rarity.getName() + " " : "";
                        int consumptionCount = pillManager.getConsumptionCount(this, rarity);

                        MutableComponent message = Component.literal("Consumed " + rarityText + "Low Quality Qi Pill - Rate increased by " +
                                String.format("%.3f", finalRateBonus) + "/sec")
                            .withStyle(ChatFormatting.AQUA);

                        if (effectiveness < 1.0) {
                            message = message.append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(String.format("%.1f%% effectiveness", effectiveness * 100))
                                    .withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(", consumed ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(String.valueOf(consumptionCount)).withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(" times)").withStyle(ChatFormatting.GRAY));
                        }

                        player.sendSystemMessage(message);
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                addQiPillTooltip(stack, context, tooltip, "Low", ChatFormatting.GRAY,
                    "Qi Condensation", LOW_QUALITY_BASE_RATE, this);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.CELESTIAL ||
                    rarity == PillFurnaceBlockEntity.PillRarity.HEAVENLY;
            }
        };
    }

    // Medium Quality Qi Cultivation Pill
    public static Supplier<Item> MEDIUM_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

                    if (cultivation != null && pillAttachment != null) {
                        double baseRateBonus = MEDIUM_QUALITY_BASE_RATE;
                        double rarityMultipliedBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                        double effectiveness = pillManager.consumePill(this, rarity);
                        double finalRateBonus = rarityMultipliedBonus * effectiveness;

                        cultivation.addQiBonusWithDiminishing(finalRateBonus, 0.0, 1.0);
                        pillAttachment.forceSync();

                        // Enhanced message with diminishing returns info
                        String rarityText = rarity != null ? rarity.getName() + " " : "";
                        int consumptionCount = pillManager.getConsumptionCount(this, rarity);

                        MutableComponent message = Component.literal("Consumed " + rarityText + "Medium Quality Qi Pill - Rate increased by " +
                                String.format("%.3f", finalRateBonus) + "/sec")
                            .withStyle(ChatFormatting.AQUA);

                        if (effectiveness < 1.0) {
                            message = message.append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(String.format("%.1f%% effectiveness", effectiveness * 100))
                                    .withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(", consumed ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(String.valueOf(consumptionCount)).withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(" times)").withStyle(ChatFormatting.GRAY));
                        }

                        player.sendSystemMessage(message);
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                addQiPillTooltip(stack, context, tooltip, "Medium", ChatFormatting.GREEN,
                    "Foundation Establishment", MEDIUM_QUALITY_BASE_RATE, this);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.CELESTIAL ||
                    rarity == PillFurnaceBlockEntity.PillRarity.HEAVENLY;
            }
        };
    }

    // High Quality Qi Cultivation Pill
    public static Supplier<Item> HIGH_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(32)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

                    if (cultivation != null && pillAttachment != null) {
                        double baseRateBonus = HIGH_QUALITY_BASE_RATE;
                        double rarityMultipliedBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                        double effectiveness = pillManager.consumePill(this, rarity);
                        double finalRateBonus = rarityMultipliedBonus * effectiveness;

                        cultivation.addQiBonusWithDiminishing(finalRateBonus, 0.0, 1.0);
                        pillAttachment.forceSync();

                        sendConsumptionMessage(player, "High Quality Qi Pill", rarity, finalRateBonus,
                            effectiveness, pillManager.getConsumptionCount(this, rarity), ChatFormatting.AQUA);
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                addQiPillTooltip(stack, context, tooltip, "High", ChatFormatting.BLUE,
                    "Core Formation", HIGH_QUALITY_BASE_RATE, this);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.CELESTIAL ||
                    rarity == PillFurnaceBlockEntity.PillRarity.HEAVENLY;
            }
        };
    }

    // Top Quality Qi Cultivation Pill
    public static Supplier<Item> TOP_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(16)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

                    if (cultivation != null && pillAttachment != null) {
                        double baseRateBonus = TOP_QUALITY_BASE_RATE;
                        double rarityMultipliedBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                        double effectiveness = pillManager.consumePill(this, rarity);
                        double finalRateBonus = rarityMultipliedBonus * effectiveness;

                        cultivation.addQiBonusWithDiminishing(finalRateBonus, 0.0, 1.0);
                        pillAttachment.forceSync();

                        sendConsumptionMessage(player, "Top Quality Qi Pill", rarity, finalRateBonus,
                            effectiveness, pillManager.getConsumptionCount(this, rarity), ChatFormatting.AQUA);
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                addQiPillTooltip(stack, context, tooltip, "Top", ChatFormatting.LIGHT_PURPLE,
                    "Nascent Soul", TOP_QUALITY_BASE_RATE, this);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                return true; // Always has enchantment glint
            }
        };
    }

    // === BODY CULTIVATION PILLS ===

    // Low Quality Body Cultivation Pill
    public static Supplier<Item> LOW_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

                    if (cultivation != null && pillAttachment != null) {
                        double baseRateBonus = LOW_QUALITY_BASE_RATE;
                        double rarityMultipliedBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                        double effectiveness = pillManager.consumePill(this, rarity);
                        double finalRateBonus = rarityMultipliedBonus * effectiveness;

                        cultivation.addBodyBonusWithDiminishing(finalRateBonus, 0.0, 1.0);
                        pillAttachment.forceSync();

                        sendConsumptionMessage(player, "Low Quality Body Pill", rarity, finalRateBonus,
                            effectiveness, pillManager.getConsumptionCount(this, rarity), ChatFormatting.RED);
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                addBodyPillTooltip(stack, context, tooltip, "Low", ChatFormatting.GRAY,
                    "Mortal Realm", LOW_QUALITY_BASE_RATE, this);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.CELESTIAL ||
                    rarity == PillFurnaceBlockEntity.PillRarity.HEAVENLY;
            }
        };
    }

    // Medium Quality Body Cultivation Pill
    public static Supplier<Item> MEDIUM_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

                    if (cultivation != null && pillAttachment != null) {
                        double baseRateBonus = MEDIUM_QUALITY_BASE_RATE;
                        double rarityMultipliedBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                        double effectiveness = pillManager.consumePill(this, rarity);
                        double finalRateBonus = rarityMultipliedBonus * effectiveness;

                        cultivation.addBodyBonusWithDiminishing(finalRateBonus, 0.0, 1.0);
                        pillAttachment.forceSync();

                        sendConsumptionMessage(player, "Medium Quality Body Pill", rarity, finalRateBonus,
                            effectiveness, pillManager.getConsumptionCount(this, rarity), ChatFormatting.RED);
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                addBodyPillTooltip(stack, context, tooltip, "Medium", ChatFormatting.GREEN,
                    "Foundation Establishment", MEDIUM_QUALITY_BASE_RATE, this);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.CELESTIAL ||
                    rarity == PillFurnaceBlockEntity.PillRarity.HEAVENLY;
            }
        };
    }

    // High Quality Body Cultivation Pill
    public static Supplier<Item> HIGH_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(32)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

                    if (cultivation != null && pillAttachment != null) {
                        double baseRateBonus = HIGH_QUALITY_BASE_RATE;
                        double rarityMultipliedBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                        double effectiveness = pillManager.consumePill(this, rarity);
                        double finalRateBonus = rarityMultipliedBonus * effectiveness;

                        cultivation.addBodyBonusWithDiminishing(finalRateBonus, 0.0, 1.0);
                        pillAttachment.forceSync();

                        sendConsumptionMessage(player, "High Quality Body Pill", rarity, finalRateBonus,
                            effectiveness, pillManager.getConsumptionCount(this, rarity), ChatFormatting.RED);
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                addBodyPillTooltip(stack, context, tooltip, "High", ChatFormatting.BLUE,
                    "Core Formation", HIGH_QUALITY_BASE_RATE, this);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.CELESTIAL ||
                    rarity == PillFurnaceBlockEntity.PillRarity.HEAVENLY;
            }
        };
    }

    // Top Quality Body Cultivation Pill
    public static Supplier<Item> TOP_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(16)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

                    if (cultivation != null && pillAttachment != null) {
                        double baseRateBonus = TOP_QUALITY_BASE_RATE;
                        double rarityMultipliedBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                        double effectiveness = pillManager.consumePill(this, rarity);
                        double finalRateBonus = rarityMultipliedBonus * effectiveness;

                        cultivation.addBodyBonusWithDiminishing(finalRateBonus, 0.0, 1.0);
                        pillAttachment.forceSync();

                        sendConsumptionMessage(player, "Top Quality Body Pill", rarity, finalRateBonus,
                            effectiveness, pillManager.getConsumptionCount(this, rarity), ChatFormatting.RED);
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                addBodyPillTooltip(stack, context, tooltip, "Top", ChatFormatting.LIGHT_PURPLE,
                    "Core Formation+", TOP_QUALITY_BASE_RATE, this);
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                return true; // Always has enchantment glint
            }
        };
    }

    // === UTILITY METHODS ===

    /**
     * Get the rarity of a pill from its NBT data
     */
    private static PillFurnaceBlockEntity.PillRarity getRarity(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        CompoundTag tag = customData.copyTag();
        String rarityName = tag.getString("PillRarity");

        if (rarityName.isEmpty()) return null;

        try {
            return PillFurnaceBlockEntity.PillRarity.valueOf(rarityName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Apply rarity multiplier to base amount
     */
    private static double applyRarityMultiplier(ItemStack stack, double baseAmount) {
        PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
        if (rarity == null) return baseAmount;

        return baseAmount * rarity.getEffectMultiplier();
    }

    /**
     * Helper method to send consumption messages with diminishing returns info
     */
    private static void sendConsumptionMessage(Player player, String pillName, PillFurnaceBlockEntity.PillRarity rarity,
                                               double finalRate, double effectiveness, int consumptionCount, ChatFormatting color) {
        String rarityText = rarity != null ? rarity.getName() + " " : "";

        MutableComponent message = Component.literal("Consumed " + rarityText + pillName + " - Rate increased by " +
                String.format("%.3f", finalRate) + "/sec")
            .withStyle(color);

        if (effectiveness < 1.0) {
            message = message.append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.1f%% effectiveness", effectiveness * 100))
                    .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(", consumed ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(consumptionCount)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" times)").withStyle(ChatFormatting.GRAY));
        }

        player.sendSystemMessage(message);
    }

    /**
     * Helper method to add Qi pill tooltips with comprehensive diminishing returns info
     */
    private static void addQiPillTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                         String quality, ChatFormatting qualityColor, String realm, double baseRate, Item pillItem) {
        PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

        tooltip.add(Component.literal("Quality: " + quality + " (" + realm + ")")
            .withStyle(qualityColor));

        if (rarity != null) {
            tooltip.add(Component.literal("Rarity: ")
                .append(Component.literal(rarity.getName())
                    .withStyle(rarity.getColor())));
        }

        double rarityAdjustedRate = applyRarityMultiplier(stack, baseRate);

        // Show base effect rate
        tooltip.add(Component.literal("Base Effect: +" + String.format("%.3f", rarityAdjustedRate) + " Qi/sec")
            .withStyle(ChatFormatting.DARK_AQUA));

        // Show diminishing returns info in tooltip
        Player player = getPlayerFromContext(context);
        if (player != null) {
            PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());
            if (pillAttachment != null) {
                PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                double effectiveness = pillManager.getNextConsumptionEffectiveness(pillItem, rarity);
                int consumptionCount = pillManager.getConsumptionCount(pillItem, rarity);
                double effectiveRate = rarityAdjustedRate * effectiveness;

                // Current effective rate
                tooltip.add(Component.literal("Current Effect: +" + String.format("%.3f", effectiveRate) + " Qi/sec")
                    .withStyle(effectiveness >= 0.8 ? ChatFormatting.AQUA :
                        effectiveness >= 0.5 ? ChatFormatting.YELLOW : ChatFormatting.RED));

                if (consumptionCount > 0 || effectiveness < 1.0) {
                    tooltip.add(Component.empty());

                    // Effectiveness percentage with color coding
                    ChatFormatting effectivenessColor = effectiveness >= 0.8 ? ChatFormatting.GREEN :
                        effectiveness >= 0.5 ? ChatFormatting.YELLOW :
                            effectiveness >= 0.3 ? ChatFormatting.GOLD : ChatFormatting.RED;

                    tooltip.add(Component.literal("◆ Effectiveness: " + String.format("%.1f%%", effectiveness * 100))
                        .withStyle(effectivenessColor));

                    tooltip.add(Component.literal("◆ Consumed: " + consumptionCount + " times")
                        .withStyle(ChatFormatting.GRAY));

                    // Show progression at key milestones
                    if (consumptionCount < 100) {
                        // Show what effectiveness will be at next major milestone
                        int nextMilestone = getNextMilestone(consumptionCount);
                        double futureEffectiveness = PillConsumptionManager.calculateEffectivenessAtCount(rarity, nextMilestone);
                        tooltip.add(Component.literal("◆ At " + nextMilestone + " consumed: " +
                                String.format("%.1f%%", futureEffectiveness * 100) + " effectiveness")
                            .withStyle(ChatFormatting.DARK_GRAY));
                    }

                    // Show rarity-specific diminishing rate info
                    if (rarity != null) {
                        String decayDescription = getRarityDecayDescription(rarity);
                        tooltip.add(Component.literal("◆ " + decayDescription)
                            .withStyle(ChatFormatting.DARK_PURPLE));
                    }
                }
            }
        } else {
            tooltip.add(Component.literal("Effect: +" + String.format("%.3f", rarityAdjustedRate) + " Qi/sec")
                .withStyle(ChatFormatting.AQUA));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Permanently increases Qi cultivation rate")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Subject to diminishing returns")
            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    /**
     * Helper method to add Body pill tooltips with comprehensive diminishing returns info
     */
    private static void addBodyPillTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                           String quality, ChatFormatting qualityColor, String realm, double baseRate, Item pillItem) {
        PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

        tooltip.add(Component.literal("Quality: " + quality + " (" + realm + ")")
            .withStyle(qualityColor));

        if (rarity != null) {
            tooltip.add(Component.literal("Rarity: ")
                .append(Component.literal(rarity.getName())
                    .withStyle(rarity.getColor())));
        }

        double rarityAdjustedRate = applyRarityMultiplier(stack, baseRate);

        // Show base effect rate
        tooltip.add(Component.literal("Base Effect: +" + String.format("%.3f", rarityAdjustedRate) + " Body/sec")
            .withStyle(ChatFormatting.DARK_RED));

        // Show diminishing returns info in tooltip
        Player player = getPlayerFromContext(context);
        if (player != null) {
            PlayerPillConsumptionAttachment pillAttachment = player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());
            if (pillAttachment != null) {
                PillConsumptionManager pillManager = pillAttachment.getPillConsumptionManager();
                double effectiveness = pillManager.getNextConsumptionEffectiveness(pillItem, rarity);
                int consumptionCount = pillManager.getConsumptionCount(pillItem, rarity);
                double effectiveRate = rarityAdjustedRate * effectiveness;

                // Current effective rate
                tooltip.add(Component.literal("Current Effect: +" + String.format("%.3f", effectiveRate) + " Body/sec")
                    .withStyle(effectiveness >= 0.8 ? ChatFormatting.RED :
                        effectiveness >= 0.5 ? ChatFormatting.GOLD : ChatFormatting.DARK_RED));

                if (consumptionCount > 0 || effectiveness < 1.0) {
                    tooltip.add(Component.empty());

                    // Effectiveness percentage with color coding
                    ChatFormatting effectivenessColor = effectiveness >= 0.8 ? ChatFormatting.GREEN :
                        effectiveness >= 0.5 ? ChatFormatting.YELLOW :
                            effectiveness >= 0.3 ? ChatFormatting.GOLD : ChatFormatting.RED;

                    tooltip.add(Component.literal("◆ Effectiveness: " + String.format("%.1f%%", effectiveness * 100))
                        .withStyle(effectivenessColor));

                    tooltip.add(Component.literal("◆ Consumed: " + consumptionCount + " times")
                        .withStyle(ChatFormatting.GRAY));

                    // Show progression at key milestones
                    if (consumptionCount < 100) {
                        // Show what effectiveness will be at next major milestone
                        int nextMilestone = getNextMilestone(consumptionCount);
                        double futureEffectiveness = PillConsumptionManager.calculateEffectivenessAtCount(rarity, nextMilestone);
                        tooltip.add(Component.literal("◆ At " + nextMilestone + " consumed: " +
                                String.format("%.1f%%", futureEffectiveness * 100) + " effectiveness")
                            .withStyle(ChatFormatting.DARK_GRAY));
                    }

                    // Show rarity-specific diminishing rate info
                    if (rarity != null) {
                        String decayDescription = getRarityDecayDescription(rarity);
                        tooltip.add(Component.literal("◆ " + decayDescription)
                            .withStyle(ChatFormatting.DARK_PURPLE));
                    }
                }
            }
        } else {
            tooltip.add(Component.literal("Effect: +" + String.format("%.3f", rarityAdjustedRate) + " Body/sec")
                .withStyle(ChatFormatting.RED));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Permanently increases body cultivation rate")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Subject to diminishing returns")
            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    /**
     * Get the next consumption milestone for showing progression info
     */
    private static int getNextMilestone(int currentCount) {
        if (currentCount < 10) return 10;
        if (currentCount < 25) return 25;
        if (currentCount < 50) return 50;
        if (currentCount < 75) return 75;
        if (currentCount < 100) return 100;
        return ((currentCount / 25) + 1) * 25; // Every 25 after 100
    }

    /**
     * Get a description of how fast this rarity's effectiveness decays
     */
    private static String getRarityDecayDescription(PillFurnaceBlockEntity.PillRarity rarity) {
        return switch (rarity) {
            case PillFurnaceBlockEntity.PillRarity.MORTAL -> "Fast diminishing returns";
            case PillFurnaceBlockEntity.PillRarity.PROFOUND -> "Moderate diminishing returns";
            case PillFurnaceBlockEntity.PillRarity.DIVINE -> "Slow diminishing returns";
            case PillFurnaceBlockEntity.PillRarity.CELESTIAL -> "Very slow diminishing returns";
            case PillFurnaceBlockEntity.PillRarity.HEAVENLY -> "Minimal diminishing returns";
        };
    }

    /**
     * Helper method to safely get the player from tooltip context
     */
    @OnlyIn(Dist.CLIENT)
    private static Player getPlayerFromContext(Item.TooltipContext context) {
        try {
            // Try to get the level from context
            if (context.level() != null && context.level().isClientSide()) {
                // On client side, get the local player
                Minecraft mc = Minecraft.getInstance();
                return mc.player;
            }
        } catch (Exception e) {
            // If we can't access the player, just return null
            // This will cause tooltips to show base values without diminishing returns info
        }

        return null;
    }
}