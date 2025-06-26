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
 * Body Cultivation Pills provide permanent increases to Body cultivation rate.
 * Each tier corresponds to spirit stone quality and provides different bonuses.
 * Updated to work with the unified PlayerCultivationAttachment and network system.
 */
public class BodyCultivationPillItem extends CultivationPillItem {

    private final PlayerCultivationAttachment.PillTier tier;

    public BodyCultivationPillItem(Item.Properties properties, PlayerCultivationAttachment.PillTier tier) {
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
                PlayerCultivationAttachment.CultivationType.BODY,
                tier.getBodyAdditiveBonus(),
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
            boolean success = cultivationAttachment.consumeBodyCultivationPill(tier);

            if (success) {
                // Send success message
                String bonusDescription = tier.getBodyBonusDescription();
                player.sendSystemMessage(Component.literal("💪 You consumed a " + tier.getDisplayName() +
                        " Body Cultivation Pill! Body cultivation rate increased by " + bonusDescription)
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
        // Light sound effect on client - slightly deeper tone for body cultivation
        level.playLocalSound(
            player.getX(), player.getY(), player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.PLAYERS,
            0.5f, 1.2f, false
        );
    }

    /**
     * Play full server-side visual and audio effects when pill is consumed - themed for body cultivation
     */
    private void playServerSideEffects(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            // Main sound effect - slightly deeper tone for body cultivation
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
            if (tier.ordinal() >= PlayerCultivationAttachment.PillTier.HIGH_QUALITY.ordinal()) {
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

            // Special aura effect for top quality pills - more intense for body cultivation
            if (tier == PlayerCultivationAttachment.PillTier.TOP_QUALITY) {
                serverLevel.sendParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    8,
                    0.3,
                    0.3,
                    0.3,
                    0.15
                );

                // Additional crimson effect for body cultivation
                serverLevel.sendParticles(
                    ParticleTypes.CRIMSON_SPORE,
                    player.getX(),
                    player.getY() + 0.5,
                    player.getZ(),
                    12,
                    0.4,
                    0.6,
                    0.4,
                    0.05
                );
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);

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

        // Add current total bonus if we have a player context
        Level level = context.level();
        if (level != null && level.isClientSide()) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null) {
                PlayerCultivationAttachment cultivationAttachment =
                    minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());

                if (cultivationAttachment != null) {
                    // Show current total bonus if any pills have been consumed
                    double currentBonus = cultivationAttachment.getBodyCultivationRawAdditiveBonus();
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

        // Xianxia flavor text based on tier - themed for body cultivation
        String flavorText = switch (tier) {
            case LOW_QUALITY -> "\"Strengthens the flesh of young cultivators.\"";
            case MEDIUM_QUALITY -> "\"Tempers the bones with earthly power.\"";
            case HIGH_QUALITY -> "\"Forges the body into unyielding steel.\"";
            case TOP_QUALITY -> "\"Transforms mortals into divine vessels.\"";
        };
        tooltipComponents.add(Component.literal(flavorText)
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_RED));
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
        return tier.getDisplayName() + " Body Cultivation Pill";
    }
}