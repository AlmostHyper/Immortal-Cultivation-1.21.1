package net.hyper.immortalcultivation.content.item.pills;

import net.hyper.immortalcultivation.common.attachments.PlayerPillConsumptionAttachment;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.content.block.entities.PillFurnaceBlockEntity;
import net.hyper.immortalcultivation.common.attachments.AttachmentRegistry;
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
    private static final double LOW_QUALITY_BASE_RATE = 0.005;
    private static final double MEDIUM_QUALITY_BASE_RATE = 0.02;
    private static final double HIGH_QUALITY_BASE_RATE = 0.1;
    private static final double TOP_QUALITY_BASE_RATE = 1.0;

    // === QI CULTIVATION PILLS ===
    public static Supplier<Item> LOW_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new CultivationPillItem(new Item.Properties().stacksTo(64), ICultivationData.CultivationType.QI, LOW_QUALITY_BASE_RATE, "Low Quality Qi Pill", "Low", "Qi Condensation", ChatFormatting.GRAY, false);
    }

    public static Supplier<Item> MEDIUM_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new CultivationPillItem(new Item.Properties().stacksTo(64), ICultivationData.CultivationType.QI, MEDIUM_QUALITY_BASE_RATE, "Medium Quality Qi Pill", "Medium", "Foundation Establishment", ChatFormatting.GREEN, false);
    }

    public static Supplier<Item> HIGH_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new CultivationPillItem(new Item.Properties().stacksTo(32), ICultivationData.CultivationType.QI, HIGH_QUALITY_BASE_RATE, "High Quality Qi Pill", "High", "Core Formation", ChatFormatting.BLUE, false);
    }

    public static Supplier<Item> TOP_QUALITY_QI_CULTIVATION_PILL() {
        return () -> new CultivationPillItem(new Item.Properties().stacksTo(16), ICultivationData.CultivationType.QI, TOP_QUALITY_BASE_RATE, "Top Quality Qi Pill", "Top", "Nascent Soul", ChatFormatting.LIGHT_PURPLE, true);
    }

    // === BODY CULTIVATION PILLS ===
    public static Supplier<Item> LOW_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new CultivationPillItem(new Item.Properties().stacksTo(64), ICultivationData.CultivationType.BODY, LOW_QUALITY_BASE_RATE, "Low Quality Body Pill", "Low", "Mortal Realm", ChatFormatting.GRAY, false);
    }

    public static Supplier<Item> MEDIUM_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new CultivationPillItem(new Item.Properties().stacksTo(64), ICultivationData.CultivationType.BODY, MEDIUM_QUALITY_BASE_RATE, "Medium Quality Body Pill", "Medium", "Foundation Establishment", ChatFormatting.GREEN, false);
    }

    public static Supplier<Item> HIGH_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new CultivationPillItem(new Item.Properties().stacksTo(32), ICultivationData.CultivationType.BODY, HIGH_QUALITY_BASE_RATE, "High Quality Body Pill", "High", "Core Formation", ChatFormatting.BLUE, false);
    }

    public static Supplier<Item> TOP_QUALITY_BODY_CULTIVATION_PILL() {
        return () -> new CultivationPillItem(new Item.Properties().stacksTo(16), ICultivationData.CultivationType.BODY, TOP_QUALITY_BASE_RATE, "Top Quality Body Pill", "Top", "Core Formation+", ChatFormatting.LIGHT_PURPLE, true);
    }


    // === UTILITY METHODS ===

    public static PillFurnaceBlockEntity.PillRarity getRarity(ItemStack stack) {
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

    public static double applyRarityMultiplier(ItemStack stack, double baseAmount) {
        PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
        return rarity != null ? baseAmount * rarity.getEffectMultiplier() : baseAmount;
    }

    public static void sendConsumptionMessage(Player player, String pillName, PillFurnaceBlockEntity.PillRarity rarity,
                                              double finalRate, double effectiveness, int consumptionCount, ChatFormatting color) {
        String rarityText = rarity != null ? rarity.getName() + " " : "";
        MutableComponent message = Component.literal("Consumed " + rarityText + pillName + " - Rate increased by " +
                        String.format("%.3f", finalRate) + "/sec")
                .withStyle(color);

        if (effectiveness < 1.0) {
            message.append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format("%.1f%% effectiveness", effectiveness * 100))
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(", consumed ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(consumptionCount)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" times)").withStyle(ChatFormatting.GRAY));
        }
        player.sendSystemMessage(message);
    }

    public static int getNextMilestone(int currentCount) {
        if (currentCount < 10) return 10;
        if (currentCount < 25) return 25;
        if (currentCount < 50) return 50;
        if (currentCount < 100) return 100;
        return ((currentCount / 50) + 1) * 50;
    }

    public static String getRarityDecayDescription(PillFurnaceBlockEntity.PillRarity rarity) {
        return switch (rarity) {
            case MORTAL -> "Fast diminishing returns";
            case PROFOUND -> "Moderate diminishing returns";
            case DIVINE -> "Slow diminishing returns";
            case CELESTIAL -> "Very slow diminishing returns";
            case HEAVENLY -> "Minimal diminishing returns";
        };
    }

    @OnlyIn(Dist.CLIENT)
    public static Player getPlayerFromContext(Item.TooltipContext context) {
        return Minecraft.getInstance().player;
    }


    /**
     * A generalized pill item that handles effects and tooltips based on its configured properties.
     */
    private static class CultivationPillItem extends BaseConsumableItem {

        private final ICultivationData.CultivationType cultivationType;
        private final double baseRate;
        private final String pillName;
        private final String quality;
        private final String realm;
        private final ChatFormatting qualityColor;
        private final boolean alwaysFoil;

        public CultivationPillItem(Properties properties, ICultivationData.CultivationType cultivationType, double baseRate, String pillName, String quality, String realm, ChatFormatting qualityColor, boolean alwaysFoil) {
            super(properties);
            this.cultivationType = cultivationType;
            this.baseRate = baseRate;
            this.pillName = pillName;
            this.quality = quality;
            this.realm = realm;
            this.qualityColor = qualityColor;
            this.alwaysFoil = alwaysFoil;
        }

        @Override
        protected void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack) {
            if (!(entity instanceof Player player) || level.isClientSide()) return;

            player.getExistingData(AttachmentRegistry.PLAYER_CULTIVATION).ifPresent(cultivation ->
                player.getExistingData(AttachmentRegistry.PLAYER_PILL_CONSUMPTION).ifPresent(pillAttachment -> {
                    PillFurnaceBlockEntity.PillRarity rarity = getRarity(consumedStack);
                    double rarityMultipliedBonus = applyRarityMultiplier(consumedStack, this.baseRate);

                    // Call consumePill directly on the attachment - no more PillConsumptionManager
                    double effectiveness = pillAttachment.consumePill(this, rarity); // Attachment handles dirty marking internally
                    double finalRateBonus = rarityMultipliedBonus * effectiveness;

                    cultivation.addBonusWithDiminishing(this.cultivationType, finalRateBonus, 0.0, 1.0);

                    sendConsumptionMessage(player, this.pillName, rarity, finalRateBonus,
                        effectiveness, pillAttachment.getConsumptionCount(this, rarity),
                        this.cultivationType == ICultivationData.CultivationType.QI ? ChatFormatting.AQUA : ChatFormatting.RED);
                })
            );
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
            ChatFormatting primaryColor = this.cultivationType == ICultivationData.CultivationType.QI ? ChatFormatting.AQUA : ChatFormatting.RED;
            ChatFormatting darkPrimaryColor = this.cultivationType == ICultivationData.CultivationType.QI ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_RED;
            String typeName = this.cultivationType.name().charAt(0) + this.cultivationType.name().substring(1).toLowerCase();

            tooltip.add(Component.literal("Quality: " + this.quality + " (" + this.realm + ")").withStyle(this.qualityColor));
            if (rarity != null) {
                tooltip.add(Component.literal("Rarity: ").append(Component.literal(rarity.getName()).withStyle(rarity.getColor())));
            }

            double rarityAdjustedRate = applyRarityMultiplier(stack, this.baseRate);
            tooltip.add(Component.literal("Base Effect: +" + String.format("%.3f", rarityAdjustedRate) + " " + typeName + "/sec").withStyle(darkPrimaryColor));

            Player player = getPlayerFromContext(context);
            if (player != null) {
                player.getExistingData(AttachmentRegistry.PLAYER_PILL_CONSUMPTION).ifPresent(pillAttachment -> {
                    // Call methods directly on the attachment
                    double effectiveness = pillAttachment.getNextConsumptionEffectiveness(this, rarity);
                    int count = pillAttachment.getConsumptionCount(this, rarity);
                    double effectiveRate = rarityAdjustedRate * effectiveness;

                    tooltip.add(Component.literal("Current Effect: +" + String.format("%.3f", effectiveRate) + " " + typeName + "/sec")
                        .withStyle(effectiveness >= 0.8 ? primaryColor : effectiveness >= 0.5 ? ChatFormatting.YELLOW : ChatFormatting.RED));

                    if (count > 0 || effectiveness < 1.0) {
                        tooltip.add(Component.empty());
                        ChatFormatting effColor = effectiveness >= 0.8 ? ChatFormatting.GREEN : effectiveness >= 0.5 ? ChatFormatting.YELLOW : ChatFormatting.RED;
                        tooltip.add(Component.literal("◆ Effectiveness: " + String.format("%.1f%%", effectiveness * 100)).withStyle(effColor));
                        tooltip.add(Component.literal("◆ Consumed: " + count + " times").withStyle(ChatFormatting.GRAY));

                        if (count < 100) {
                            int milestone = getNextMilestone(count);
                            // Use static method for future effectiveness calculation
                            double futureEff = PlayerPillConsumptionAttachment.calculateEffectivenessAtCount(rarity, milestone);
                            tooltip.add(Component.literal("◆ At " + milestone + ": " + String.format("%.1f%%", futureEff * 100) + " eff.").withStyle(ChatFormatting.DARK_GRAY));
                        }
                        if (rarity != null) {
                            tooltip.add(Component.literal("◆ " + getRarityDecayDescription(rarity)).withStyle(ChatFormatting.DARK_PURPLE));
                        }
                    }
                });
            }

            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Permanently increases " + typeName + " cultivation rate.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.literal("Subject to diminishing returns.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            if (this.alwaysFoil) return true;
            PillFurnaceBlockEntity.PillRarity rarity = getRarity(stack);
            return rarity == PillFurnaceBlockEntity.PillRarity.CELESTIAL || rarity == PillFurnaceBlockEntity.PillRarity.HEAVENLY;
        }
    }
}
