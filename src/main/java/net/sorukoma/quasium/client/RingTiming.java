package net.sorukoma.quasium.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.network.ExhalePacket;
import net.sorukoma.quasium.progression.ConfigArgs;

@OnlyIn(Dist.CLIENT)
public final class RingTiming {
    private static final int BAR_WIDTH = 100;
    private static final int ROW = 10;
    private static long pullStartedAt = -1;
    private static long breathAt;
    private static boolean heldBack;

    private RingTiming() {
    }

    public static void pullStarted() {
        pullStartedAt = System.currentTimeMillis();
    }

    public static void pullReleased(Player player) {
        if (pullStartedAt >= 0 && player.isShiftKeyDown()) {
            PacketDistributor.sendToServer(new ExhalePacket((int) MagicMath.ringOffsetMs(System.currentTimeMillis() - pullStartedAt)));
        }
        pullStartedAt = -1;
        catchBreath(player);
    }

    public static void stoppedByServer() {
        PuffSoundPlayer.stop();
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || !player.getUseItem().is(ModItems.HOOKAH_PIPE.get())) return;
        player.stopUsingItem();
        pullStartedAt = -1;
        heldBack = true;
        catchBreath(player);
    }

    public static void tick(Minecraft minecraft) {
        if (heldBack && !minecraft.options.keyUse.isDown()) heldBack = false;
    }

    public static boolean breathReady(Level level) {
        return !heldBack && level.getGameTime() >= breathAt;
    }

    public static void forget() {
        pullStartedAt = -1;
        breathAt = 0;
        heldBack = false;
    }

    private static void catchBreath(Player player) {
        breathAt = player.level().getGameTime() + ConfigArgs.synced(Config.BREATH_RECOVERY_MS) * MagicMath.TICKS_PER_SECOND / 1000;
    }

    static long currentOffsetMs() {
        return pullStartedAt < 0 ? Long.MAX_VALUE : MagicMath.ringOffsetMs(System.currentTimeMillis() - pullStartedAt);
    }

    public static double markerPosition(long elapsedMs) {
        long lap = elapsedMs % (MagicMath.RING_SWEEP_MS * 2);
        return lap < MagicMath.RING_SWEEP_MS ? lap / (double) MagicMath.RING_SWEEP_MS : 2 - lap / (double) MagicMath.RING_SWEEP_MS;
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (pullStartedAt < 0 || player == null || minecraft.options.hideGui || !player.isShiftKeyDown() || !player.isUsingItem()
                || !player.getUseItem().is(ModItems.HOOKAH_PIPE.get())) return;
        Gui gui = minecraft.gui;
        int x = graphics.guiWidth() / 2 - BAR_WIDTH / 2;
        int y = graphics.guiHeight() - Math.max(gui.leftHeight, gui.rightHeight) + 2;
        int windowHalf = (int) Math.round(ConfigArgs.synced(Config.RING_WINDOW_MS) / 2.0 / MagicMath.RING_SWEEP_MS * BAR_WIDTH);
        int perfectHalf = (int) Math.round(ConfigArgs.synced(Config.RING_PERFECT_MS) / (double) MagicMath.RING_SWEEP_MS * BAR_WIDTH);
        int center = BAR_WIDTH / 2;
        GuiBars.track(graphics, BossBarColor.WHITE, x, y, BAR_WIDTH);
        GuiBars.fill(graphics, BossBarColor.GREEN, x, y, center - windowHalf, center + windowHalf);
        GuiBars.fill(graphics, BossBarColor.YELLOW, x, y, center - perfectHalf, center + perfectHalf);
        int marker = x + (int) Math.round(markerPosition(System.currentTimeMillis() - pullStartedAt) * (BAR_WIDTH - 1));
        graphics.fill(marker, y - 2, marker + 1, y + GuiBars.THICKNESS + 2, 0xFFFFFFFF);
        gui.leftHeight += ROW;
        gui.rightHeight += ROW;
    }
}
