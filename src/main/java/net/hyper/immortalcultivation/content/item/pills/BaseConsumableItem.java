package net.hyper.immortalcultivation.content.item.pills;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Base class for consumable items like pills, elixirs, and potions
 * Provides standard consumption behavior with customizable effects
 */
public abstract class BaseConsumableItem extends Item {

    private final int useDuration;
    private final UseAnim useAnimation;
    private final SoundEvent consumeSound;

    public BaseConsumableItem(Properties properties) {
        this(properties, 32, UseAnim.EAT, SoundEvents.GENERIC_EAT);
    }

    public BaseConsumableItem(Properties properties, int useDuration, UseAnim useAnimation, SoundEvent consumeSound) {
        super(properties);
        this.useDuration = useDuration;
        this.useAnimation = useAnimation;
        this.consumeSound = consumeSound;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }

        if (!level.isClientSide) {
            // FIXED: Pass the actual ItemStack being consumed so rarity data can be accessed
            applyEffects(level, entity, stack);
        }

        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, getCooldownDuration());

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }

    /**
     * Apply the effects of consuming this item
     * @param level The world
     * @param entity The entity consuming the item
     * @param consumedStack The actual ItemStack being consumed (contains rarity data for pills)
     */
    protected abstract void applyEffects(Level level, LivingEntity entity, ItemStack consumedStack);

    /**
     * Get the cooldown duration after consuming
     * @return Cooldown in ticks (20 ticks = 1 second)
     */
    protected int getCooldownDuration() {
        return 0; // No cooldown by default
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return useDuration;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return useAnimation;
    }

    @Override
    public SoundEvent getEatingSound() {
        return consumeSound;
    }

    public boolean isEdible() {
        return true;
    }
}