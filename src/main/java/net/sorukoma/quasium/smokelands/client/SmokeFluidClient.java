package net.sorukoma.quasium.smokelands.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.sorukoma.quasium.QuasiumMod;
import org.joml.Vector3f;

final class SmokeFluidClient implements IClientFluidTypeExtensions {
    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "block/smoke_still");
    private static final ResourceLocation FLOWING = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "block/smoke_flowing");
    private static final Vector3f FOG = new Vector3f(0.36f, 0.33f, 0.38f);
    private static final float FOG_END = 5f;

    @Override
    public ResourceLocation getStillTexture() {
        return STILL;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOWING;
    }

    @Override
    public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                                   Vector3f fluidFogColor) {
        return new Vector3f(FOG);
    }

    @Override
    public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance,
                                float farDistance, FogShape shape) {
        RenderSystem.setShaderFogStart(-1f);
        RenderSystem.setShaderFogEnd(FOG_END);
    }
}
