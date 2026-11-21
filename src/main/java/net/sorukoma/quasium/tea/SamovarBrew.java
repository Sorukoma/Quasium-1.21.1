package net.sorukoma.quasium.tea;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.progression.ConfigArgs;

public record SamovarBrew(ItemStack water, ItemStack leaves, ItemStack fuel, ItemStack emptyTeapot, ItemStack fullTeapot) {
    public static SamovarBrew current() {
        ItemStack full = new ItemStack(TeaBlocks.TEAPOT.get());
        TeapotItem.setCups(full, ConfigArgs.synced(EcologyConfig.TEAPOT_CUPS));
        ItemStack leaves = new ItemStack(TeaBlocks.DRIED_TEA.get(), ConfigArgs.synced(EcologyConfig.SAMOVAR_LEAVES_PER_POT));
        return new SamovarBrew(new ItemStack(Items.WATER_BUCKET), leaves, new ItemStack(Items.CHARCOAL), new ItemStack(TeaBlocks.TEAPOT.get()), full);
    }

    public Component summary() {
        return Component.translatable("quasium.jei.samovar.summary", ConfigArgs.synced(EcologyConfig.SAMOVAR_WATER_ML),
                ConfigArgs.synced(EcologyConfig.SAMOVAR_BREW_SECONDS), ConfigArgs.synced(EcologyConfig.TEAPOT_CUPS));
    }
}
