package net.sorukoma.quasium.smokelands.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.smokelands.DraftCycle;

final class SmokelandsSky extends DimensionSpecialEffects {
    private static final float PULSE = 0.35f;

    SmokelandsSky() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return fogColor;
        float pulse = DraftCycle.pulse(minecraft.level, minecraft.getTimer().getGameTimeDeltaPartialTick(false));
        return fogColor.scale(1.0 - PULSE + PULSE * 2 * pulse);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return true;
    }
}
