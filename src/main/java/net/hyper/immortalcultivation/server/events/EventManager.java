package net.hyper.immortalcultivation.server.events;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.*;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.cultivation.realms.JsonCultivationRealm;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Unified Event Manager for the Immortal Cultivation Mod
 */
@EventBusSubscriber(modid = ImmortalCultivation.MOD_ID)
public class EventManager {

    // === MANAGED EVENTS === //


    /**
     * Sets player reference on attachment if it supports it
     * This ensures attachments have proper player context for packet creation
     *
     * @param attachment The attachment object
     * @param player The player to set as reference
     */
    private static void setPlayerReferenceIfSupported(Object attachment, ServerPlayer player) {
        // Check for known attachment types with setPlayer methods
        if (attachment instanceof PlayerTalentsAttachment talents) {
            talents.setPlayer(player);
        } else if (attachment instanceof PlayerStatsAttachment stats) {
            stats.setPlayer(player);
        } else if (attachment instanceof PlayerCultivationAttachment cultivation) {
            cultivation.setPlayer(player);
        } else if (attachment instanceof PlayerBreakthroughAttachment breakthrough) {
            breakthrough.setPlayer(player);
        } else if (attachment instanceof PlayerRaceAttachment race) {
            race.setPlayer(player);
        } else if (attachment instanceof PlayerPillConsumptionAttachment pills) {
            pills.setPlayer(player);
        }
        // Add other attachment types as needed when you create new attachments
    }
}