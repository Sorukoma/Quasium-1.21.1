package net.sorukoma.quasium.client.hint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.hint.HintScene;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class Hints {
    private static Map<ResourceLocation, HintScene> scenes = Map.of();
    private static Map<Item, ResourceLocation> byItem = Map.of();

    private Hints() {
    }

    public static boolean has(ResourceLocation scene) {
        return scenes.containsKey(scene);
    }

    public static void open(ResourceLocation scene, @Nullable Screen parent) {
        open(scene, parent, ItemStack.EMPTY);
    }

    static void open(ResourceLocation scene, @Nullable Screen parent, ItemStack subject) {
        HintScene found = scenes.get(scene);
        if (found == null) return;
        ItemStack shown = subject.isEmpty() ? subjectOf(found) : subject.copyWithCount(1);
        Minecraft.getInstance().setScreen(new HintScreen(scene, found, parent, shown));
    }

    private static ItemStack subjectOf(HintScene scene) {
        for (ResourceLocation item : scene.items()) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(item));
            if (!stack.isEmpty()) return stack;
        }
        return scene.steps().stream().flatMap(step -> step.place().stream())
                .map(placement -> ResourceLocation.tryParse(placement.state().split("\\[", 2)[0])).filter(Objects::nonNull)
                .map(block -> new ItemStack(BuiltInRegistries.BLOCK.get(block))).filter(stack -> !stack.isEmpty())
                .findFirst().orElse(ItemStack.EMPTY);
    }

    @Nullable
    public static ResourceLocation sceneFor(ItemStack stack) {
        return stack.isEmpty() ? null : byItem.get(stack.getItem());
    }

    public static void set(Map<ResourceLocation, HintScene> loaded) {
        Map<ResourceLocation, HintScene> shown = new TreeMap<>();
        Map<Item, ResourceLocation> openers = new HashMap<>();
        loaded.forEach((id, scene) -> {
            if (!scene.steps().isEmpty()) shown.put(id, scene);
        });
        shown.forEach((id, scene) -> scene.items().forEach(item ->
                BuiltInRegistries.ITEM.getOptional(item).ifPresent(found -> openers.putIfAbsent(found, id))));
        scenes = shown;
        byItem = openers;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        scenes = Map.of();
        byItem = Map.of();
    }
}
