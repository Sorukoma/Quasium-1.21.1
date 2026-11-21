package net.sorukoma.quasium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.inventory.InventoryMenu;
import net.sorukoma.quasium.QuasiumMod;

public final class GuiBars {
    public static final int THICKNESS = 5;
    public static final int FLAME = 14;
    public static final int ARROW_W = 24;
    public static final int ARROW_H = 16;
    private static final int SPRITE_LENGTH = 182;
    private static final int TILE = 16;
    private static final int WATER_COLOR = 0x3F76E4;
    private static final ResourceLocation FURNACE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation LIT = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final ResourceLocation WATER = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation SMOKE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/block/note_jar_smoke.png");

    private GuiBars() {
    }

    public static void bar(GuiGraphics graphics, BossEvent.BossBarColor color, int x, int y, int length, float fill) {
        track(graphics, color, x, y, length);
        fill(graphics, color, x, y, 0, Math.round(length * Mth.clamp(fill, 0f, 1f)));
    }

    public static void track(GuiGraphics graphics, BossEvent.BossBarColor color, int x, int y, int length) {
        span(graphics, sprite(color, "background"), x, y, 0, length);
    }

    public static void fill(GuiGraphics graphics, BossEvent.BossBarColor color, int x, int y, int from, int to) {
        span(graphics, sprite(color, "progress"), x, y, from, to);
    }

    public static void flame(GuiGraphics graphics, int x, int y, float fill) {
        graphics.blit(FURNACE, x, y, 56, 36, FLAME, FLAME);
        if (fill <= 0f) return;
        int lit = Mth.ceil(Math.min(fill, 1f) * (FLAME - 1)) + 1;
        graphics.blitSprite(LIT, FLAME, FLAME, 0, FLAME - lit, x, y + FLAME - lit, FLAME, lit);
    }

    public static void arrow(GuiGraphics graphics, int x, int y, float fill) {
        graphics.blit(FURNACE, x, y, 79, 34, ARROW_W, ARROW_H);
        int width = Mth.ceil(Mth.clamp(fill, 0f, 1f) * ARROW_W);
        if (width > 0) graphics.blitSprite(BURN, ARROW_W, ARROW_H, 0, 0, x, y, width, ARROW_H);
    }

    public static void water(GuiGraphics graphics, int x, int y, int width, int height, float fill) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER);
        int atlasWidth = Math.round(sprite.contents().width() / (sprite.getU1() - sprite.getU0()));
        int atlasHeight = Math.round(sprite.contents().height() / (sprite.getV1() - sprite.getV0()));
        tint(graphics, WATER_COLOR);
        tiles(graphics, sprite.atlasLocation(), Math.round(sprite.getU0() * atlasWidth), Math.round(sprite.getV0() * atlasHeight),
                atlasWidth, atlasHeight, x, y, width, height, fill);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    public static void smoke(GuiGraphics graphics, int x, int y, int width, int height, float fill, int color) {
        tint(graphics, color);
        tiles(graphics, SMOKE, 0, 0, TILE, TILE, x, y, width, height, fill);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    public static int percent(float fraction) {
        return Math.round(Mth.clamp(fraction, 0f, 1f) * 100);
    }

    public static void tint(GuiGraphics graphics, int color) {
        graphics.setColor((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f, 1f);
    }

    private static ResourceLocation sprite(BossEvent.BossBarColor color, String part) {
        return ResourceLocation.withDefaultNamespace("boss_bar/" + color.getName() + "_" + part);
    }

    private static void span(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int from, int to) {
        if (to - from < 2) return;
        graphics.blitSprite(sprite, SPRITE_LENGTH, THICKNESS, 0, 0, x + from, y, 1, THICKNESS);
        graphics.blitSprite(sprite, SPRITE_LENGTH, THICKNESS, from + 1, 0, x + from + 1, y, to - from - 2, THICKNESS);
        graphics.blitSprite(sprite, SPRITE_LENGTH, THICKNESS, SPRITE_LENGTH - 1, 0, x + to - 1, y, 1, THICKNESS);
    }

    private static void tiles(GuiGraphics graphics, ResourceLocation texture, int u, int v, int textureWidth, int textureHeight,
                              int x, int y, int width, int height, float fill) {
        int level = Math.round(height * Mth.clamp(fill, 0f, 1f));
        for (int row = 0; row < level; row += TILE) {
            int tall = Math.min(TILE, level - row);
            for (int column = 0; column < width; column += TILE) {
                int wide = Math.min(TILE, width - column);
                graphics.blit(texture, x + column, y + height - row - tall, u, v + TILE - tall, wide, tall, textureWidth, textureHeight);
            }
        }
    }
}
