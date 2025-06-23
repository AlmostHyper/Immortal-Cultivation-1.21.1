package net.hyper.immortalcultivation.item.pills;

import net.hyper.immortalcultivation.attachment.PlayerCultivationBonusAttachment;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Qi Cultivation Pills provide permanent increases to Qi cultivation rate.
 * Each tier corresponds to spirit stone quality and provides different bonuses.
 */
public class QiCultivationPillItem extends CultivationPillItem {

    private final PlayerCultivationBonusAttachment.PillTier tier;

    public QiCultivationPillItem(Item.Properties properties, PlayerCultivationBonusAttachment.PillTier tier) {
        super(properties);
        this.tier = tier;
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        // Get the cultivation bonus attachment
        PlayerCultivationBonusAttachment bonusAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());

        if (bonusAttachment == null) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("Cultivation bonus system not initialized!")
                    .withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(itemStack);
        }

        // Only process on server side
        if (!level.isClientSide()) {
            // Attempt to consume the pill
            boolean success = bonusAttachment.consumeQiCultivationPill(tier);

            if (success) {
                // Send success message
                String bonusDescription = tier.getQiBonusDescription();
                player.sendSystemMessage(Component.literal("You consumed a " + tier.getDisplayName() +
                        " Qi Cultivation Pill! Qi cultivation rate increased by " + bonusDescription)
                    .withStyle(ChatFormatting.GOLD));

                // Play effects
                playConsumptionEffects(level, player);

                // Consume the item
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }

                return InteractionResultHolder.consume(itemStack);
            } else {
                player.sendSystemMessage(Component.literal("Failed to consume pill!")
                    .withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(itemStack);
            }
        }

        // Client side - just indicate we're trying to use it
        return InteractionResultHolder.consume(itemStack);
    }

    /**
     * Play visual and audio effects when pill is consumed
     */
    private void playConsumptionEffects(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            // Sound effect
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.7f, 1.2f);

            // Particle effects - different colors based on tier
            var particleType = switch (tier) {
                case LOW_QUALITY -> ParticleTypes.ENCHANT;
                case MEDIUM_QUALITY -> ParticleTypes.END_ROD;
                case HIGH_QUALITY -> ParticleTypes.SOUL_FIRE_FLAME;
                case TOP_QUALITY -> ParticleTypes.SOUL;
            };

            int particleCount = switch (tier) {
                case LOW_QUALITY -> 20;
                case MEDIUM_QUALITY -> 30;
                case HIGH_QUALITY -> 40;
                case TOP_QUALITY -> 60;
            };

            // Create a swirling effect around the player
            serverLevel.sendParticles(
                particleType,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                particleCount,
                0.5,
                1.0,
                0.5,
                0.1
            );

            // Additional sparkle effect for higher tiers
            if (tier.ordinal() >= PlayerCultivationBonusAttachment.PillTier.HIGH_QUALITY.ordinal()) {
                serverLevel.sendParticles(
                    ParticleTypes.FIREWORK,
                    player.getX(),
                    player.getY() + 1.5,
                    player.getZ(),
                    10,
                    0.3,
                    0.3,
                    0.3,
                    0.05
                );
            }
        }
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, TooltipContext.of(level), tooltipComponents, isAdvanced);

        // Basic description
        tooltipComponents.add(Component.literal("A pill that permanently increases Qi cultivation rate.")
            .withStyle(ChatFormatting.GRAY));

        // Bonus amount
        String bonusDescription = tier.getQiBonusDescription();
        tooltipComponents.add(Component.literal("Bonus: " + bonusDescription + " cultivation rate")
            .withStyle(ChatFormatting.AQUA));

        // Tier information
        tooltipComponents.add(Component.literal("Tier: " + tier.getDisplayName())
            .withStyle(ChatFormatting.YELLOW));

        // Add warning about consumption limits if we have a player context
        if (level != null && level.isClientSide()) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null) {
                PlayerCultivationBonusAttachment bonusAttachment =
                    minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());

                if (bonusAttachment != null) {
                    // Show current total bonus if any pills have been consumed
                    double currentBonus = bonusAttachment.getQiCultivationRawAdditiveBonus();
                    if (currentBonus > 0) {
                        String currentBonusDescription;
                        if (currentBonus >= 1.0) {
                            currentBonusDescription = "+" + String.format("%.1f", currentBonus) + "/sec";
                        } else if (currentBonus >= 0.1) {
                            currentBonusDescription = "+" + String.format("%.1f", currentBonus) + "/sec";
                        } else if (currentBonus >= 0.01) {
                            currentBonusDescription = "+" + String.format("%.2f", currentBonus) + "/sec";
                        } else {
                            currentBonusDescription = "+" + String.format("%.3f", currentBonus) + "/sec";
                        }

                        tooltipComponents.add(Component.literal("Current Total Bonus: " + currentBonusDescription)
                            .withStyle(ChatFormatting.GOLD));
                    }
                }
            }
        }

        // Usage hint
        tooltipComponents.add(Component.literal("Right-click to consume")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * Get the tier of this pill
     */
    public PlayerCultivationBonusAttachment.PillTier getTier() {
        return tier;
    }

    /**
     * Get display name with tier
     */
    public String getTieredName() {
        return tier.getDisplayName() + " Qi Cultivation Pill";
    }
}