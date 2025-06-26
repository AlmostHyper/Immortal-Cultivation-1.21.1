package net.hyper.immortalcultivation.item.pills;

import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.network.client.ConsumePillRequestPacket;
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
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Qi Cultivation Pills provide permanent increases to Qi cultivation rate.
 * Each tier corresponds to spirit stone quality and provides different bonuses.
 * Updated to work with the unified PlayerCultivationAttachment and network system.
 */
public class QiCultivationPillItem extends CultivationPillItem {

    private final PlayerCultivationAttachment.PillTier tier;

    public QiCultivationPillItem(Item.Properties properties, PlayerCultivationAttachment.PillTier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        // Get the unified cultivation attachment
        PlayerCultivationAttachment cultivationAttachment = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

        if (cultivationAttachment == null) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("Cultivation system not initialized!")
                    .withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(itemStack);
        }

        if (level.isClientSide()) {
            // On client side, send a packet to the server to consume the pill
            PacketDistributor.sendToServer(new ConsumePillRequestPacket(
                PlayerCultivationAttachment.CultivationType.QI,
                tier.getQiAdditiveBonus(),
                0.0, // No multiplicative bonus for now
                getTieredName()
            ));

            // Play client-side effects immediately for responsiveness
            playClientSideEffects(level, player);

            // Consume the item
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            return InteractionResultHolder.consume(itemStack);
        } else {
            // Server side - attempt to consume the pill directly
            boolean success = cultivationAttachment.consumeQiCultivationPill(tier);

            if (success) {
                // Send success message
                String bonusDescription = tier.getQiBonusDescription();
                player.sendSystemMessage(Component.literal("✨ You consumed a " + tier.getDisplayName() +
                        " Qi Cultivation Pill! Qi cultivation rate increased by " + bonusDescription)
                    .withStyle(ChatFormatting.GOLD));

                // Play server-side effects
                playServerSideEffects(level, player);

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
    }

    /**
     * Play immediate client-side effects for responsiveness
     */
    private void playClientSideEffects(Level level, Player player) {
        // Light sound effect on client
        level.playLocalSound(
            player.getX(), player.getY(), player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.PLAYERS,
            0.5f, 1.5f, false
        );
    }

    /**
     * Play full server-side visual and audio effects when pill is consumed
     */
    private void playServerSideEffects(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            // Main sound effect
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
            if (tier.ordinal() >= PlayerCultivationAttachment.PillTier.HIGH_QUALITY.ordinal()) {
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

            // Special aura effect for top quality pills
            if (tier == PlayerCultivationAttachment.PillTier.TOP_QUALITY) {
                serverLevel.sendParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(),
                    player.getY() + 1.2,
                    player.getZ(),
                    5,
                    0.2,
                    0.2,
                    0.2,
                    0.1
                );
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);

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

        // Add current total bonus if we have a player context
        Level level = context.level();
        if (level != null && level.isClientSide()) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null) {
                PlayerCultivationAttachment cultivationAttachment =
                    minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

                if (cultivationAttachment != null) {
                    // Show current total bonus if any pills have been consumed
                    double currentBonus = cultivationAttachment.getQiCultivationRawAdditiveBonus();
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

        // Xianxia flavor text based on tier
        String flavorText = switch (tier) {
            case LOW_QUALITY -> "\"A basic pill for novice cultivators.\"";
            case MEDIUM_QUALITY -> "\"Refined with care by skilled alchemists.\"";
            case HIGH_QUALITY -> "\"Forged with rare spiritual herbs.\"";
            case TOP_QUALITY -> "\"Legendary pill sought by immortals.\"";
        };
        tooltipComponents.add(Component.literal(flavorText)
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_PURPLE));
    }

    /**
     * Get the tier of this pill
     */
    public PlayerCultivationAttachment.PillTier getTier() {
        return tier;
    }

    /**
     * Get display name with tier
     */
    public String getTieredName() {
        return tier.getDisplayName() + " Qi Cultivation Pill";
    }
}