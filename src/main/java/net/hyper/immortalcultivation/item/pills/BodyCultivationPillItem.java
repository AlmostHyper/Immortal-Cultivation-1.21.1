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
 * Body Cultivation Pills provide permanent increases to Body cultivation rate.
 * Each tier corresponds to spirit stone quality and provides different bonuses.
 */
public class BodyCultivationPillItem extends CultivationPillItem {

    private final PlayerCultivationBonusAttachment.PillTier tier;

    public BodyCultivationPillItem(Item.Properties properties, PlayerCultivationBonusAttachment.PillTier tier) {
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
            boolean success = bonusAttachment.consumeBodyCultivationPill(tier);

            if (success) {
                // Send success message
                String bonusDescription = tier.getBodyBonusDescription();
                player.sendSystemMessage(Component.literal("You consumed a " + tier.getDisplayName() +
                        " Body Cultivation Pill! Body cultivation rate increased by " + bonusDescription)
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
     * Play visual and audio effects when pill is consumed - themed for body cultivation
     */
    private void playConsumptionEffects(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            // Sound effect - slightly deeper tone for body cultivation
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.7f, 0.9f);

            // Particle effects - more physical/earthy effects for body cultivation
            var particleType = switch (tier) {
                case LOW_QUALITY -> ParticleTypes.FLAME;
                case MEDIUM_QUALITY -> ParticleTypes.SOUL_FIRE_FLAME;
                case HIGH_QUALITY -> ParticleTypes.LAVA;
                case TOP_QUALITY -> ParticleTypes.DRIPPING_LAVA;
            };

            int particleCount = switch (tier) {
                case LOW_QUALITY -> 25;
                case MEDIUM_QUALITY -> 35;
                case HIGH_QUALITY -> 45;
                case TOP_QUALITY -> 65;
            };

            // Create a more grounded effect around the player's feet and body
            serverLevel.sendParticles(
                particleType,
                player.getX(),
                player.getY() + 0.5,
                player.getZ(),
                particleCount,
                0.5,
                0.8,
                0.5,
                0.08
            );

            // Additional smoke effect to represent body tempering
            serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                player.getX(),
                player.getY() + 0.3,
                player.getZ(),
                15,
                0.3,
                0.5,
                0.3,
                0.02
            );

            // Special effect for higher tiers - represents intense body tempering
            if (tier.ordinal() >= PlayerCultivationBonusAttachment.PillTier.HIGH_QUALITY.ordinal()) {
                serverLevel.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    8,
                    0.4,
                    0.4,
                    0.4,
                    0.1
                );
            }
        }
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, TooltipContext.of(level), tooltipComponents, isAdvanced);

        // Basic description
        tooltipComponents.add(Component.literal("A pill that permanently increases Body cultivation rate.")
            .withStyle(ChatFormatting.GRAY));

        // Bonus amount
        String bonusDescription = tier.getBodyBonusDescription();
        tooltipComponents.add(Component.literal("Bonus: " + bonusDescription + " cultivation rate")
            .withStyle(ChatFormatting.RED));

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
                    double currentBonus = bonusAttachment.getBodyCultivationRawAdditiveBonus();
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
        return tier.getDisplayName() + " Body Cultivation Pill";
    }
}