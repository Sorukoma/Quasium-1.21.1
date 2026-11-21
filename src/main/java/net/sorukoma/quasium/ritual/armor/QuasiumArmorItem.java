package net.sorukoma.quasium.ritual.armor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModArmorMaterials;
import net.sorukoma.quasium.ritual.RitualRegistry;
import org.jetbrains.annotations.Nullable;

public class QuasiumArmorItem extends ArmorItem {
    public static final ResourceLocation CARAPACE_OUTER =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/models/armor/smoke_carapace_layer_1.png");
    public static final ResourceLocation CARAPACE_INNER =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/models/armor/smoke_carapace_layer_2.png");

    public QuasiumArmorItem(Type type, Properties properties) {
        super(ModArmorMaterials.QUASIUM, type, properties);
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        boolean smoked = stack.has(RitualRegistry.SMOKE_CARAPACE.get()) || entity instanceof LivingEntity wearer && SmokeArmor.wearsCarapace(wearer);
        if (!smoked) return null;
        return innerModel ? CARAPACE_INNER : CARAPACE_OUTER;
    }
}
