package net.hyper.immortalcultivation.content.item.armors;

/*
public class ICModArmorItems extends ArmorItem {
    private static final Map<Holder<ArmorMaterial>, List<MobEffectInstance>> MATERIAL_TO_EFFECT_MAP =
        (new ImmutableMap.Builder<Holder<ArmorMaterial>, List<MobEffectInstance>>())
            .put(ICModArmorMaterials.EARTH_ASPECTED,
                List.of(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 0, false, false)))

            .put(ICModArmorMaterials.FIRE_ASPECTED,
                List.of(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, false, false)))

            .put(ICModArmorMaterials.METAL_ASPECTED,
                List.of(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false)))

            .put(ICModArmorMaterials.WATER_ASPECTED,
                List.of(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false)))

            .put(ICModArmorMaterials.WOOD_ASPECTED,
                List.of(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, false)))

            .put(ICModArmorMaterials.SKY_IRON,
                List.of(new MobEffectInstance(MobEffects.HEALTH_BOOST, 200, 5, false, false),
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, false)))


            .build();

    public ICModArmorItems(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        if(pEntity instanceof Player player) {
            if(!pLevel.isClientSide() && hasFullSuitOfArmorOn(player)) {
                evaluateArmorEffects(player);
            }
        }
        super.inventoryTick(pStack, pLevel, pEntity, pSlotId, pIsSelected);
    }


    private void evaluateArmorEffects(Player player) {
        for(Map.Entry<Holder<ArmorMaterial>, List<MobEffectInstance>> entry : MATERIAL_TO_EFFECT_MAP.entrySet()) {
            Holder<ArmorMaterial> mapArmorMaterial = entry.getKey();
            List<MobEffectInstance> mapEffect = entry.getValue();

            if(hasPlayerCorrectArmorOn(mapArmorMaterial, player)) {
                addEffectToPlayer(player, mapEffect);
            }
        }
    }

    private void addEffectToPlayer(Player player, List<MobEffectInstance> mapEffect) {
        boolean hasPlayerEffect = mapEffect.stream().anyMatch(effect -> player.hasEffect(effect.getEffect()));

        if(!hasPlayerEffect) {
            for (MobEffectInstance effect : mapEffect) {
                player.addEffect(new MobEffectInstance(effect.getEffect(),
                    effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
            }
        }
    }

    private boolean hasPlayerCorrectArmorOn(Holder<ArmorMaterial> mapArmorMaterial, Player player) {
        for(ItemStack armorStack : player.getArmorSlots()) {
            if(!(armorStack.getItem() instanceof ArmorItem)) {
                return false;
            }
        }

        ArmorItem boots = ((ArmorItem) player.getInventory().getArmor(0).getItem());
        ArmorItem leggings = ((ArmorItem) player.getInventory().getArmor(1).getItem());
        ArmorItem chestplate = ((ArmorItem) player.getInventory().getArmor(2).getItem());
        ArmorItem helmet = ((ArmorItem) player.getInventory().getArmor(3).getItem());

        return boots.getMaterial() == mapArmorMaterial && leggings.getMaterial() == mapArmorMaterial
            && chestplate.getMaterial() == mapArmorMaterial && helmet.getMaterial() == mapArmorMaterial;
    }

    private boolean hasFullSuitOfArmorOn(Player player) {
        ItemStack boots = player.getInventory().getArmor(0);
        ItemStack leggings = player.getInventory().getArmor(1);
        ItemStack chestplate = player.getInventory().getArmor(2);
        ItemStack helmet = player.getInventory().getArmor(3);

        return !boots.isEmpty() && !leggings.isEmpty() && !chestplate.isEmpty() && !helmet.isEmpty();
    }
} */
