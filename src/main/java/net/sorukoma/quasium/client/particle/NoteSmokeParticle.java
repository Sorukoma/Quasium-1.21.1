package net.sorukoma.quasium.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ColorParticleOption;

public class NoteSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startSize;

    private NoteSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                              ColorParticleOption color, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.rCol = color.getRed();
        this.gCol = color.getGreen();
        this.bCol = color.getBlue();
        this.alpha = 0.85f;
        this.friction = 0.9f;
        this.gravity = -0.012f;
        this.hasPhysics = false;
        this.lifetime = 14 + random.nextInt(10);
        this.startSize = 0.1f + random.nextFloat() * 0.08f;
        this.quadSize = startSize;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        setSpriteFromAge(sprites);
        float life = age / (float) lifetime;
        quadSize = startSize * (1f + life * 1.6f);
        double fromCamera = Math.sqrt(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z));
        alpha = 0.85f * (1f - life * life) * (float) Math.min(1.0, Math.max(0.0, (fromCamera - 0.6) / 1.4));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption color, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new NoteSmokeParticle(level, x, y, z, dx, dy, dz, color, sprites);
        }
    }
}
