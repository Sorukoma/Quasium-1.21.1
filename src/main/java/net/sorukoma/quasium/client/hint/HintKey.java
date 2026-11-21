package net.sorukoma.quasium.client.hint;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.sorukoma.quasium.QuasiumMod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class HintKey {
    public static final KeyMapping KEY = new KeyMapping("key.quasium.hint", KeyConflictContext.GUI, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_W, "key.categories.quasium");
    private static final long FRESH_MS = 100;
    private static final float DECAY = 0.05f;
    private static final int BORDER_START = 0xFF5000FF;
    private static final int BORDER_MIDDLE = 0xFF5555FF;
    private static final int BORDER_END = 0xFFFFFFFF;

    private static ResourceLocation hovered;
    private static ItemStack hoveredStack = ItemStack.EMPTY;
    private static long hoveredAt;
    private static ResourceLocation tracking;
    private static float progress;
    private static float progressBefore;
    private static boolean waitRelease;

    private HintKey() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null || !RenderSystem.isOnRenderThread()) return;
        ResourceLocation scene = Hints.sceneFor(event.getItemStack());
        boolean subject = scene != null && minecraft.screen instanceof HintScreen open && open.id().equals(scene);
        hovered = subject ? null : scene;
        hoveredStack = event.getItemStack();
        hoveredAt = Util.getMillis();
        if (scene == null) return;
        Component line = subject ? Component.translatable("quasium.hint.subject").withStyle(ChatFormatting.GREEN) : holdLine(minecraft.font, scene);
        List<Component> tooltip = event.getToolTip();
        if (tooltip.size() < 2) {
            tooltip.add(line);
        } else {
            tooltip.add(1, line);
        }
    }

    private static Component holdLine(Font font, ResourceLocation scene) {
        Component key = KEY.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GRAY);
        Component text = Component.translatable("quasium.hint.tooltip", key).withStyle(ChatFormatting.DARK_GRAY);
        float shown = shown(scene);
        if (shown <= 0f) return text;
        int bars = font.width(text) / font.width("|");
        int filled = (int) (shown * bars);
        return Component.literal("|".repeat(filled)).withStyle(ChatFormatting.GRAY)
                .append(Component.literal("|".repeat(bars - filled)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static float shown(ResourceLocation scene) {
        if (!scene.equals(tracking)) return 0f;
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        return Math.min(1f, Mth.lerp(partialTick, progressBefore, progress) * 8f / 7f);
    }

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ResourceLocation scene = Hints.sceneFor(event.getItemStack());
        float shown = scene == null ? 0f : shown(scene);
        if (shown <= 0f) return;
        int border = shown < 0.5f ? HintTheme.mix(BORDER_START, BORDER_MIDDLE, shown * 2f)
                : HintTheme.mix(BORDER_MIDDLE, BORDER_END, (shown - 0.5f) * 2f);
        event.setBorderStart(border);
        event.setBorderEnd(border);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation target = minecraft.screen != null && Util.getMillis() - hoveredAt <= FRESH_MS ? hovered : null;
        progressBefore = progress;
        if (target == null || !target.equals(tracking)) {
            tracking = target;
            progress = progressBefore = 0f;
        }
        boolean held = minecraft.screen != null && held(minecraft, minecraft.screen);
        if (!held) waitRelease = false;
        if (target == null) return;
        if (!held || waitRelease) {
            progress = Math.max(0f, progress - DECAY);
            return;
        }
        if (progress < 1f) {
            progress = Math.min(1f, progress + Math.max(0.25f, progress) * 0.25f);
            return;
        }
        progress = progressBefore = 0f;
        waitRelease = true;
        Hints.open(target, minecraft.screen, hoveredStack);
    }

    private static boolean held(Minecraft minecraft, Screen screen) {
        if (typing(screen)) return false;
        if (KEY.isDown()) return true;
        if (KEY.isUnbound() || !KEY.getKeyModifier().isActive(KeyConflictContext.GUI)) return false;
        InputConstants.Key key = KEY.getKey();
        long window = minecraft.getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> false;
        };
    }

    private static boolean typing(Screen screen) {
        return screen.children().stream().anyMatch(child -> child instanceof EditBox box && box.isFocused());
    }
}
