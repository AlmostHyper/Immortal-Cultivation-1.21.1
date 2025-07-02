package net.hyper.immortalcultivation.item.pills;

import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.block.entities.PillFurnaceBlockEntity;
import net.hyper.immortalcultivation.item.BaseConsumableItem;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public class CultivationPillItems {

    // Base cultivation rate bonuses for each pill tier (per second)
    private static final double LOW_QUALITY_BASE_RATE = 0.005;      // 0.005/sec
    private static final double MEDIUM_QUALITY_BASE_RATE = 0.02;    // 0.02/sec
    private static final double HIGH_QUALITY_BASE_RATE = 0.1;       // 0.1/sec
    private static final double TOP_QUALITY_BASE_RATE = 1.0;        // 1.0/sec

    // Low Quality Qi Cultivation Pill (for Mortal -> Qi Condensation)
    public static Supplier<Item> LOW_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    if (cultivation != null) {
                        double baseRateBonus = LOW_QUALITY_BASE_RATE;
                        double finalRateBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        // Add to cultivation rate instead of flat amount
                        cultivation.addQiBonus(finalRateBonus, 0.0);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        String rarityText = rarity != null ? rarity.getName() + " " : "";

                        player.sendSystemMessage(
                            Component.literal("Consumed " + rarityText + "Low Quality Qi Pill - Rate increased by " + String.format("%.3f", finalRateBonus) + "/sec")
                                .withStyle(ChatFormatting.AQUA)
                        );
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

                // Quality tier
                tooltip.add(Component.literal("Quality: Low (Mortal Realm)")
                    .withStyle(ChatFormatting.GRAY));

                // Rarity (if present)
                if (rarity != null) {
                    tooltip.add(Component.literal("Rarity: ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor())));
                }

                // Rate effect (not flat amount)
                double baseRate = LOW_QUALITY_BASE_RATE;
                double finalRate = applyRarityMultiplier(stack, baseRate);
                tooltip.add(Component.literal("Effect: +" + String.format("%.3f", finalRate) + " Qi/sec")
                    .withStyle(ChatFormatting.AQUA));

                // Description
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Permanently increases Qi cultivation rate")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.EPIC ||
                    rarity == PillFurnaceBlockEntity.PillRarity.LEGENDARY;
            }
        };
    }

    // Medium Quality Qi Cultivation Pill (for Qi Condensation progression)
    public static Supplier<Item> MEDIUM_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    if (cultivation != null) {
                        double baseRateBonus = MEDIUM_QUALITY_BASE_RATE;
                        double finalRateBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        cultivation.addQiBonus(finalRateBonus, 0.0);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        String rarityText = rarity != null ? rarity.getName() + " " : "";

                        player.sendSystemMessage(
                            Component.literal("Consumed " + rarityText + "Medium Quality Qi Pill - Rate increased by " + String.format("%.3f", finalRateBonus) + "/sec")
                                .withStyle(ChatFormatting.AQUA)
                        );
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

                tooltip.add(Component.literal("Quality: Medium (Qi Condensation)")
                    .withStyle(ChatFormatting.GREEN));

                if (rarity != null) {
                    tooltip.add(Component.literal("Rarity: ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor())));
                }

                double baseRate = MEDIUM_QUALITY_BASE_RATE;
                double finalRate = applyRarityMultiplier(stack, baseRate);
                tooltip.add(Component.literal("Effect: +" + String.format("%.3f", finalRate) + " Qi/sec")
                    .withStyle(ChatFormatting.AQUA));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Permanently increases Qi cultivation rate")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.EPIC ||
                    rarity == PillFurnaceBlockEntity.PillRarity.LEGENDARY;
            }
        };
    }

    // High Quality Qi Cultivation Pill (for Foundation Establishment)
    public static Supplier<Item> HIGH_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(32)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    if (cultivation != null) {
                        double baseRateBonus = HIGH_QUALITY_BASE_RATE;
                        double finalRateBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        cultivation.addQiBonus(finalRateBonus, 0.0);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        String rarityText = rarity != null ? rarity.getName() + " " : "";

                        player.sendSystemMessage(
                            Component.literal("Consumed " + rarityText + "High Quality Qi Pill - Rate increased by " + String.format("%.3f", finalRateBonus) + "/sec")
                                .withStyle(ChatFormatting.AQUA)
                        );
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

                tooltip.add(Component.literal("Quality: High (Foundation Establishment)")
                    .withStyle(ChatFormatting.BLUE));

                if (rarity != null) {
                    tooltip.add(Component.literal("Rarity: ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor())));
                }

                double baseRate = HIGH_QUALITY_BASE_RATE;
                double finalRate = applyRarityMultiplier(stack, baseRate);
                tooltip.add(Component.literal("Effect: +" + String.format("%.3f", finalRate) + " Qi/sec")
                    .withStyle(ChatFormatting.AQUA));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Permanently increases Qi cultivation rate")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.EPIC ||
                    rarity == PillFurnaceBlockEntity.PillRarity.LEGENDARY;
            }
        };
    }

    // Top Quality Qi Cultivation Pill (for Core Formation and beyond)
    public static Supplier<Item> TOP_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(16)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    if (cultivation != null) {
                        double baseRateBonus = TOP_QUALITY_BASE_RATE;
                        double finalRateBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        cultivation.addQiBonus(finalRateBonus, 0.0);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        String rarityText = rarity != null ? rarity.getName() + " " : "";

                        player.sendSystemMessage(
                            Component.literal("Consumed " + rarityText + "Top Quality Qi Pill - Rate increased by " + String.format("%.3f", finalRateBonus) + "/sec")
                                .withStyle(ChatFormatting.AQUA)
                        );
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

                tooltip.add(Component.literal("Quality: Top (Core Formation+)")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

                if (rarity != null) {
                    tooltip.add(Component.literal("Rarity: ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor())));
                }

                double baseRate = TOP_QUALITY_BASE_RATE;
                double finalRate = applyRarityMultiplier(stack, baseRate);
                tooltip.add(Component.literal("Effect: +" + String.format("%.3f", finalRate) + " Qi/sec")
                    .withStyle(ChatFormatting.AQUA));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Permanently increases Qi cultivation rate")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
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
                    if (cultivation != null) {
                        double baseRateBonus = LOW_QUALITY_BASE_RATE;
                        double finalRateBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        cultivation.addBodyBonus(finalRateBonus, 0.0);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        String rarityText = rarity != null ? rarity.getName() + " " : "";

                        player.sendSystemMessage(
                            Component.literal("Consumed " + rarityText + "Low Quality Body Pill - Rate increased by " + String.format("%.3f", finalRateBonus) + "/sec")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

                tooltip.add(Component.literal("Quality: Low (Mortal Realm)")
                    .withStyle(ChatFormatting.GRAY));

                if (rarity != null) {
                    tooltip.add(Component.literal("Rarity: ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor())));
                }

                double baseRate = LOW_QUALITY_BASE_RATE;
                double finalRate = applyRarityMultiplier(stack, baseRate);
                tooltip.add(Component.literal("Effect: +" + String.format("%.3f", finalRate) + " Body/sec")
                    .withStyle(ChatFormatting.RED));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Permanently increases body cultivation rate")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.EPIC ||
                    rarity == PillFurnaceBlockEntity.PillRarity.LEGENDARY;
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
                    if (cultivation != null) {
                        double baseRateBonus = MEDIUM_QUALITY_BASE_RATE;
                        double finalRateBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        cultivation.addBodyBonus(finalRateBonus, 0.0);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        String rarityText = rarity != null ? rarity.getName() + " " : "";

                        player.sendSystemMessage(
                            Component.literal("Consumed " + rarityText + "Medium Quality Body Pill - Rate increased by " + String.format("%.3f", finalRateBonus) + "/sec")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

                tooltip.add(Component.literal("Quality: Medium (Foundation Establishment)")
                    .withStyle(ChatFormatting.GREEN));

                if (rarity != null) {
                    tooltip.add(Component.literal("Rarity: ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor())));
                }

                double baseRate = MEDIUM_QUALITY_BASE_RATE;
                double finalRate = applyRarityMultiplier(stack, baseRate);
                tooltip.add(Component.literal("Effect: +" + String.format("%.3f", finalRate) + " Body/sec")
                    .withStyle(ChatFormatting.RED));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Permanently increases body cultivation rate")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.EPIC ||
                    rarity == PillFurnaceBlockEntity.PillRarity.LEGENDARY;
            }
        };
    }

    // High Quality Body Cultivation Pill
    public static Supplier<Item> HIGH_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    if (cultivation != null) {
                        double baseRateBonus = HIGH_QUALITY_BASE_RATE;
                        double finalRateBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        cultivation.addBodyBonus(finalRateBonus, 0.0);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        String rarityText = rarity != null ? rarity.getName() + " " : "";

                        player.sendSystemMessage(
                            Component.literal("Consumed " + rarityText + "High Quality Body Pill - Rate increased by " + String.format("%.3f", finalRateBonus) + "/sec")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

                tooltip.add(Component.literal("Quality: High (Core Formation)")
                    .withStyle(ChatFormatting.BLUE));

                if (rarity != null) {
                    tooltip.add(Component.literal("Rarity: ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor())));
                }

                double baseRate = HIGH_QUALITY_BASE_RATE;
                double finalRate = applyRarityMultiplier(stack, baseRate);
                tooltip.add(Component.literal("Effect: +" + String.format("%.3f", finalRate) + " Body/sec")
                    .withStyle(ChatFormatting.RED));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Permanently increases body cultivation rate")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
                return rarity == PillFurnaceBlockEntity.PillRarity.EPIC ||
                    rarity == PillFurnaceBlockEntity.PillRarity.LEGENDARY;
            }
        };
    }

    // Top Quality Body Cultivation Pill
    public static Supplier<Item> TOP_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    if (cultivation != null) {
                        double baseRateBonus = TOP_QUALITY_BASE_RATE;
                        double finalRateBonus = applyRarityMultiplier(consumedStack, baseRateBonus);

                        cultivation.addBodyBonus(finalRateBonus, 0.0);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                        String rarityText = rarity != null ? rarity.getName() + " " : "";

                        player.sendSystemMessage(
                            Component.literal("Consumed " + rarityText + "Top Quality Body Pill - Rate increased by " + String.format("%.3f", finalRateBonus) + "/sec")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

                tooltip.add(Component.literal("Quality: Top (Core Formation+)")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

                if (rarity != null) {
                    tooltip.add(Component.literal("Rarity: ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor())));
                }

                double baseRate = TOP_QUALITY_BASE_RATE;
                double finalRate = applyRarityMultiplier(stack, baseRate);
                tooltip.add(Component.literal("Effect: +" + String.format("%.3f", finalRate) + " Body/sec")
                    .withStyle(ChatFormatting.RED));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Permanently increases body cultivation rate")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
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
}