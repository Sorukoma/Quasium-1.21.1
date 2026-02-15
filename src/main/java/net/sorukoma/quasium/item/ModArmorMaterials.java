package net.sorukoma.quasium.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, QuasiumMod.MOD_ID);

    public static final Holder<ArmorMaterial> QUASIUM = ARMOR_MATERIALS.register("quasium", () -> {
        ArmorMaterial material = new ArmorMaterial(
                Map.of(
                        ArmorItem.Type.HELMET, 4,
                        ArmorItem.Type.CHESTPLATE, 9,
                        ArmorItem.Type.LEGGINGS, 7,
                        ArmorItem.Type.BOOTS, 4
                ),
                15, // enchantmentValue (возвращено к исходному значению)
                SoundEvents.ARMOR_EQUIP_DIAMOND,
                () -> Ingredient.of(ModItems.QUASIUMINGOT.get()),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "quasium"))),
                2.0f, // toughness
                0.1f // knockbackResistance
        );
        return material;
    });

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }
}
