package net.sorukoma.quasium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.sorukoma.quasium.QuasiumMod;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class RecipeGate {
    public static final ResourceLocation DRYING = recipe("drying_shelf_from_planks_and_sticks");
    public static final ResourceLocation FERMENTATION = recipe("fermentation_barrel_from_planks_and_iron");
    public static final ResourceLocation SOAKING = recipe("soaking_bowl_from_brick_and_clay");
    public static final ResourceLocation ANVIL = recipe("quasium_anvil_from_quasium_blocks");
    public static final ResourceLocation ESSENCE = recipe("essence_press");
    public static final ResourceLocation DISTILLATION = recipe("distillation_cube");
    public static final ResourceLocation MIXING = recipe("mixing_table");
    public static final ResourceLocation KILN = recipe("smoke_kiln");
    public static final ResourceLocation SAMOVAR = recipe("samovar");
    private static final int CHECK_EVERY = 10;

    private static Consumer<Set<ResourceLocation>> listener;
    private static Set<ResourceLocation> announced = Set.of();
    private static int ticks;

    private RecipeGate() {
    }

    public static boolean isLocked(ResourceLocation recipeId) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && gating(player) && !player.getRecipeBook().contains(recipeId);
    }

    public static Set<ResourceLocation> locked() {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (player == null || connection == null || !gating(player)) return Set.of();
        Set<ResourceLocation> locked = new HashSet<>();
        for (RecipeHolder<?> holder : connection.getRecipeManager().getRecipes()) {
            if (QuasiumMod.MOD_ID.equals(holder.id().getNamespace()) && !player.getRecipeBook().contains(holder.id())) {
                locked.add(holder.id());
            }
        }
        return locked;
    }

    public static void listen(Consumer<Set<ResourceLocation>> consumer) {
        listener = consumer;
        announced = Set.of();
        ticks = 0;
    }

    public static void tick() {
        if (listener == null || ++ticks % CHECK_EVERY != 0) return;
        Set<ResourceLocation> now = locked();
        if (now.equals(announced)) return;
        announced = now;
        listener.accept(now);
    }

    public static boolean gating(LocalPlayer player) {
        return QuasiumClientConfig.HIDE_LOCKED_RECIPES.get() && !player.isCreative() && !player.isSpectator();
    }

    private static ResourceLocation recipe(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }
}
