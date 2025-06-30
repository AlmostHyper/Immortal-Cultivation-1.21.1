package net.hyper.immortalcultivation.item.pills;

import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.block.entities.PillFurnaceBlockEntity;
import net.hyper.immortalcultivation.item.BaseConsumableItem;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

    // Base cultivation values for each pill tier
    private static final double LOW_QUALITY_BASE = 20.0;
    private static final double MEDIUM_QUALITY_BASE = 50.0;
    private static final double HIGH_QUALITY_BASE = 100.0;
    private static final double TOP_QUALITY_BASE = 200.0;

    // Low Quality Qi Cultivation Pill (for Mortal -> Qi Condensation)
    public static Supplier<Item> LOW_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    if (cultivation != null) {
                        double baseAmount = LOW_QUALITY_BASE;
                        double finalAmount = applyRarityMultiplier(consumedStack, baseAmount);

                        cultivation.addQiCultivation(finalAmount);

                        // Apply minor effects based on rarity
                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);

                        player.sendSystemMessage(
                            Component.literal("Absorbed " + String.format("%.0f", finalAmount) + " Qi from the pill")
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

                // Base effect
                double baseAmount = LOW_QUALITY_BASE;
                double finalAmount = applyRarityMultiplier(stack, baseAmount);
                tooltip.add(Component.literal("Effect: +" + String.format("%.0f", finalAmount) + " Qi Cultivation")
                    .withStyle(ChatFormatting.AQUA));

                // Description
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("A basic pill for beginning cultivators")
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
                        double baseAmount = MEDIUM_QUALITY_BASE;
                        double finalAmount = applyRarityMultiplier(consumedStack, baseAmount);

                        cultivation.addQiCultivation(finalAmount);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);

                        player.sendSystemMessage(
                            Component.literal("Absorbed " + String.format("%.0f", finalAmount) + " Qi from the pill")
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

                double baseAmount = MEDIUM_QUALITY_BASE;
                double finalAmount = applyRarityMultiplier(stack, baseAmount);
                tooltip.add(Component.literal("Effect: +" + String.format("%.0f", finalAmount) + " Qi Cultivation")
                    .withStyle(ChatFormatting.AQUA));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Refined for Qi Condensation cultivators")
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
                        double baseAmount = HIGH_QUALITY_BASE;
                        double finalAmount = applyRarityMultiplier(consumedStack, baseAmount);

                        cultivation.addQiCultivation(finalAmount);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);

                        player.sendSystemMessage(
                            Component.literal("Absorbed " + String.format("%.0f", finalAmount) + " Qi from the pill")
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

                double baseAmount = HIGH_QUALITY_BASE;
                double finalAmount = applyRarityMultiplier(stack, baseAmount);
                tooltip.add(Component.literal("Effect: +" + String.format("%.0f", finalAmount) + " Qi Cultivation")
                    .withStyle(ChatFormatting.AQUA));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Potent pills for established cultivators")
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
                        double baseAmount = TOP_QUALITY_BASE;
                        double finalAmount = applyRarityMultiplier(consumedStack, baseAmount);

                        cultivation.addQiCultivation(finalAmount);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);

                        player.sendSystemMessage(
                            Component.literal("Absorbed " + String.format("%.0f", finalAmount) + " Qi from the pill")
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

                double baseAmount = TOP_QUALITY_BASE;
                double finalAmount = applyRarityMultiplier(stack, baseAmount);
                tooltip.add(Component.literal("Effect: +" + String.format("%.0f", finalAmount) + " Qi Cultivation")
                    .withStyle(ChatFormatting.AQUA));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Treasured pills for advanced cultivators")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                return true; // Always has enchantment glint
            }
        };
    }

    // Body Cultivation Pills follow the same pattern
    public static Supplier<Item> LOW_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new BaseConsumableItem(new Item.Properties().stacksTo(64)) {
            @Override
            protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
                if (entity instanceof Player player && !level.isClientSide()) {
                    PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                    if (cultivation != null) {
                        double baseAmount = LOW_QUALITY_BASE;
                        double finalAmount = applyRarityMultiplier(consumedStack, baseAmount);

                        cultivation.addBodyCultivation(finalAmount);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);

                        player.sendSystemMessage(
                            Component.literal("Body strengthened by " + String.format("%.0f", finalAmount) + " points")
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

                double baseAmount = LOW_QUALITY_BASE;
                double finalAmount = applyRarityMultiplier(stack, baseAmount);
                tooltip.add(Component.literal("Effect: +" + String.format("%.0f", finalAmount) + " Body Cultivation")
                    .withStyle(ChatFormatting.RED));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Basic body tempering medicine")
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
                        double baseAmount = MEDIUM_QUALITY_BASE;
                        double finalAmount = applyRarityMultiplier(consumedStack, baseAmount);

                        cultivation.addBodyCultivation(finalAmount);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);

                        player.sendSystemMessage(
                            Component.literal("Absorbed " + String.format("%.0f", finalAmount) + " Body from the pill")
                                .withStyle(ChatFormatting.AQUA)
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

                double baseAmount = MEDIUM_QUALITY_BASE;
                double finalAmount = applyRarityMultiplier(stack, baseAmount);
                tooltip.add(Component.literal("Effect: +" + String.format("%.0f", finalAmount) + " Body Cultivation")
                    .withStyle(ChatFormatting.AQUA));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Refined for Foundation Establishment cultivators")
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
                        double baseAmount = HIGH_QUALITY_BASE;
                        double finalAmount = applyRarityMultiplier(consumedStack, baseAmount);

                        cultivation.addBodyCultivation(finalAmount);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);

                        player.sendSystemMessage(
                            Component.literal("Absorbed " + String.format("%.0f", finalAmount) + " Body from the pill")
                                .withStyle(ChatFormatting.AQUA)
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

                double baseAmount = HIGH_QUALITY_BASE;
                double finalAmount = applyRarityMultiplier(stack, baseAmount);
                tooltip.add(Component.literal("Effect: +" + String.format("%.0f", finalAmount) + " Body Cultivation")
                    .withStyle(ChatFormatting.AQUA));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Potent pills for established cultivators")
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
                        double baseAmount = TOP_QUALITY_BASE;
                        double finalAmount = applyRarityMultiplier(consumedStack, baseAmount);

                        cultivation.addBodyCultivation(finalAmount);

                        PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);

                        player.sendSystemMessage(
                            Component.literal("Absorbed " + String.format("%.0f", finalAmount) + " Body from the pill")
                                .withStyle(ChatFormatting.AQUA)
                        );
                    }
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);

                tooltip.add(Component.literal("Quality: Top (Nascent Soul)")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

                if (rarity != null) {
                    tooltip.add(Component.literal("Rarity: ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor())));
                }

                double baseAmount = TOP_QUALITY_BASE;
                double finalAmount = applyRarityMultiplier(stack, baseAmount);
                tooltip.add(Component.literal("Effect: +" + String.format("%.0f", finalAmount) + " Body Cultivation")
                    .withStyle(ChatFormatting.AQUA));

                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Treasured pills for advanced cultivators")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }

            @Override
            public boolean isFoil(ItemStack stack) {
                return true; // Always has enchantment glint
            }
        };
    }

    // Helper methods for rarity system - FIXED for 1.21.1
    private static PillFurnaceBlockEntity.PillRarity getRarity(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("PillRarity")) {
                String rarityName = tag.getString("PillRarity");
                try {
                    return PillFurnaceBlockEntity.PillRarity.valueOf(rarityName);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static double applyRarityMultiplier(ItemStack stack, double baseAmount) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("EffectMultiplier")) {
                return baseAmount * tag.getDouble("EffectMultiplier");
            }
        }
        return baseAmount;
    }
}