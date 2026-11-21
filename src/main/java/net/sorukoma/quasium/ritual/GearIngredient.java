package net.sorukoma.quasium.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.function.Predicate;
import java.util.stream.Stream;

public record GearIngredient(Fits fits) implements ICustomIngredient {
    public static final MapCodec<GearIngredient> CODEC = Fits.CODEC.fieldOf("fits").xmap(GearIngredient::new, GearIngredient::fits);
    public static final StreamCodec<RegistryFriendlyByteBuf, GearIngredient> STREAM_CODEC =
            ByteBufCodecs.idMapper(index -> Fits.values()[index], Fits::ordinal).map(GearIngredient::new, GearIngredient::fits).cast();

    public enum Fits implements StringRepresentable {
        BOOTS("boots", GearIngredient::isBoots),
        DAMAGEABLE("damageable", ItemStack::isDamageableItem);

        public static final Codec<Fits> CODEC = StringRepresentable.fromEnum(Fits::values);
        private final String name;
        private final Predicate<ItemStack> test;

        Fits(String name, Predicate<ItemStack> test) {
            this.name = name;
            this.test = test;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static boolean isBoots(ItemStack stack) {
        if (stack.is(ItemTags.FOOT_ARMOR)) return true;
        EquipmentSlot slot = stack.getEquipmentSlot();
        if (slot == null) {
            Equipable equipable = Equipable.get(stack);
            slot = equipable == null ? null : equipable.getEquipmentSlot();
        }
        return slot == EquipmentSlot.FEET;
    }

    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && fits.test.test(stack);
    }

    @Override
    public Stream<ItemStack> getItems() {
        return BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(this::test);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return RitualRegistry.GEAR.get();
    }
}
