package net.sorukoma.quasium.ritual.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.BossEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientHookahInputHandler;
import net.sorukoma.quasium.client.GuiBars;
import net.sorukoma.quasium.item.HookahPipeItem;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.artifact.QuasiumHarness;
import net.sorukoma.quasium.ritual.brazier.RitualBrazierBlockEntity;
import net.sorukoma.quasium.ritual.brazier.RitualPressPacket;
import net.sorukoma.quasium.ritual.brazier.RitualPulse;
import net.sorukoma.quasium.ritual.brazier.RitualSite;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class PulseIndicator {
    private static final int BAR_WIDTH = 100;
    private static final int ROW = 10;
    private static final int MARKER = 0xFFFFFFFF;
    private static final int MARKER_ON_BEAT = 0xFFFFD27A;

    private PulseIndicator() {
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().options.keyUse.matchesMouse(event.getButton())) pressed();
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().options.keyUse.matches(event.getKey(), event.getScanCode())) pressed();
    }

    private static void pressed() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) return;
        RitualBrazierBlockEntity brazier = target(minecraft);
        if (brazier != null) PacketDistributor.sendToServer(new RitualPressPacket(brazier.getBlockPos(), now(minecraft)));
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) return;
        RitualBrazierBlockEntity brazier = target(minecraft);
        RitualPulse pulse = brazier == null ? null : brazier.pulse();
        if (pulse == null) return;
        double now = minecraft.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
        Gui gui = minecraft.gui;
        int x = graphics.guiWidth() / 2 - BAR_WIDTH / 2;
        int y = graphics.guiHeight() - Math.max(gui.leftHeight, gui.rightHeight) + 2;
        int windowHalf = (int) Math.round(pulse.windowMs() / (double) pulse.periodMs() * BAR_WIDTH);
        int center = BAR_WIDTH / 2;
        GuiBars.track(graphics, BossEvent.BossBarColor.WHITE, x, y, BAR_WIDTH);
        GuiBars.fill(graphics, BossEvent.BossBarColor.GREEN, x, y, center - windowHalf, center + windowHalf);
        double sweep = pulse.phase(now) + 0.5;
        int marker = x + (int) Math.round((sweep - Math.floor(sweep)) * (BAR_WIDTH - 1));
        graphics.fill(marker, y - 2, marker + 1, y + GuiBars.THICKNESS + 2, pulse.onBeat(now) ? MARKER_ON_BEAT : MARKER);
        gui.leftHeight += ROW;
        gui.rightHeight += ROW;
    }

    @Nullable
    private static RitualBrazierBlockEntity target(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !HookahPipeItem.holdsPipe(player)) return null;
        BlockPos hooked = ClientHookahInputHandler.getConnectedHookahPos();
        if (hooked != null) {
            return minecraft.level.getBlockEntity(hooked) instanceof RitualBrazierBlockEntity brazier && brazier.pulse() != null ? brazier : null;
        }
        if (!ClientHookahInputHandler.isConnectedToBackpack() || !QuasiumHarness.worn(player)) return null;
        int reach = ConfigArgs.synced(RitualConfig.HARNESS_LINK_RADIUS);
        return RitualSite.braziers(minecraft.level, player.blockPosition(), reach).stream()
                .filter(brazier -> brazier.pulse() != null && brazier.getBlockPos().getCenter().distanceToSqr(player.position()) <= reach * reach)
                .min(Comparator.comparingDouble(brazier -> brazier.getBlockPos().getCenter().distanceToSqr(player.position()))).orElse(null);
    }

    private static double now(Minecraft minecraft) {
        return minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }
}
