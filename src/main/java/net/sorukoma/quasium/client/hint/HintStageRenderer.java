package net.sorukoma.quasium.client.hint;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.client.geode.QuasiumCrustModel;
import net.sorukoma.quasium.hint.HintScene;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class HintStageRenderer {
    static final float START_YAW = 215f;
    private static final float PITCH = 35f;
    private static final float BLOCK_PIXELS = 30f;
    private static final float DEPTH = 400f;
    private static final float DROP_TICKS = 8f;
    private static final float DROP_HEIGHT = 1.4f;
    private static final float DROP_STAGGER = 2f;
    private static final float DROP_SPREAD = 24f;
    private static final int OUTLINE_COLOR = 0xEEEEEE;
    private static final int SHADOW = 0x66000000;
    private static final int FLASH = 0xAAC6FFC9;
    private static final float SHADOW_DEPTH = 4f;
    private static final float NUDGE = 1 / 512f;
    private static final double BOB = 0.08;

    private HintStageRenderer() {
    }

    static float scale(AABB bounds, float width, float height) {
        double across = Math.max(0.5, Math.sqrt(bounds.getXsize() * bounds.getXsize() + bounds.getZsize() * bounds.getZsize()));
        double tall = bounds.getYsize() * Math.cos(Math.toRadians(PITCH)) + across * Math.sin(Math.toRadians(PITCH));
        return (float) Math.min(BLOCK_PIXELS, Math.min(width / across, height / tall));
    }

    static Matrix4f render(GuiGraphics graphics, HintStage stage, HintScene.Step step, float centerX, float centerY, float scale,
                           float yaw, float stepTime, float time, float fade, float flash) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        Vec3 center = stage.bounds().getCenter();
        pose.translate(centerX, centerY, DEPTH);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(PITCH));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-center.x, -center.y, -center.z);
        Matrix4f view = new Matrix4f(pose.last().pose());
        drawBlocks(graphics, stage, stepTime);
        platform(graphics, stage.floor(), flash);
        pose.popPose();
        clearDepth(graphics);
        outline(graphics, view, step.highlight(), fade);
        List<HintScene.Icon> icons = step.icons();
        for (int index = 0; index < icons.size(); index++) icon(graphics, view, icons.get(index), time + index * 7f);
        clearDepth(graphics);
        return view;
    }

    private static void clearDepth(GuiGraphics graphics) {
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    private static void drawBlocks(GuiGraphics graphics, HintStage stage, float stepTime) {
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        PoseStack pose = graphics.pose();
        Lighting.setupFor3DItems();
        for (Map.Entry<BlockPos, BlockState> entry : stage.blocks().entrySet()) {
            BlockPos pos = entry.getKey();
            float drop = drop(stage.dropOrder(pos), stage.dropCount(), stepTime);
            if (drop < 0) continue;
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY() + drop, pos.getZ());
            blocks.renderSingleBlock(entry.getValue(), pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    QuasiumCrustModel.ALWAYS_REVEALED, null);
            pose.popPose();
        }
        graphics.flush();
    }

    private static float drop(int order, int count, float stepTime) {
        if (order < 0) return 0f;
        float progress = (stepTime - order * Math.min(DROP_STAGGER, DROP_SPREAD / count)) / DROP_TICKS;
        if (progress < 0f) return -1f;
        float left = 1f - Math.min(1f, progress);
        return left * left * left * DROP_HEIGHT;
    }

    private static void platform(GuiGraphics graphics, int floor, float flash) {
        if (floor <= 0) return;
        float glow = flash * 0.9f;
        float swell = 2 * glow * glow - 1;
        float rise = 1 - swell * swell;
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer quads = graphics.bufferSource().getBuffer(RenderType.debugQuads());
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (matrix.transformDirection(side.getStepX(), 0, side.getStepZ(), new Vector3f()).z <= 0) continue;
            wall(quads, matrix, side, floor, -1 - SHADOW_DEPTH, -1, 0, SHADOW);
            if (rise > 0) wall(quads, matrix, side, floor, -1, -0.5f + 0.75f * rise, HintTheme.alpha(FLASH, glow), FLASH & 0xFFFFFF);
        }
        graphics.flush();
    }

    private static void wall(VertexConsumer quads, Matrix4f matrix, Direction side, int floor, float bottom, float top, int bottomColor,
                             int topColor) {
        float out = NUDGE * 2 + floor / 2f;
        float centerX = floor / 2f + side.getStepX() * out;
        float centerZ = floor / 2f + side.getStepZ() * out;
        float alongX = side.getStepZ() * floor / 2f;
        float alongZ = side.getStepX() * floor / 2f;
        quads.addVertex(matrix, centerX - alongX, top, centerZ - alongZ).setColor(topColor);
        quads.addVertex(matrix, centerX + alongX, top, centerZ + alongZ).setColor(topColor);
        quads.addVertex(matrix, centerX + alongX, bottom, centerZ + alongZ).setColor(bottomColor);
        quads.addVertex(matrix, centerX - alongX, bottom, centerZ - alongZ).setColor(bottomColor);
    }

    private static void outline(GuiGraphics graphics, Matrix4f view, List<BlockPos> cells, float fade) {
        if (cells.isEmpty() || fade < 1 / 16f) return;
        float width = Math.max(1f, view.getScale(new Vector3f()).x / 16f);
        int color = Mth.floor(fade * 255f) << 24 | OUTLINE_COLOR;
        VertexConsumer quads = graphics.bufferSource().getBuffer(RenderType.debugQuads());
        Matrix4f flat = graphics.pose().last().pose();
        for (Edge edge : edges(cells)) {
            Vector3f from = view.transformPosition(edge.start().getX(), edge.start().getY(), edge.start().getZ(), new Vector3f());
            BlockPos end = edge.start().relative(edge.axis(), 1);
            Vector3f to = view.transformPosition(end.getX(), end.getY(), end.getZ(), new Vector3f());
            line(quads, flat, from, to, width, color);
        }
        graphics.flush();
    }

    private record Edge(BlockPos start, Direction.Axis axis) {
    }

    private static Set<Edge> edges(List<BlockPos> cells) {
        Set<BlockPos> filled = new HashSet<>(cells);
        Set<Edge> seen = new HashSet<>();
        Set<Edge> drawn = new HashSet<>();
        for (BlockPos cell : filled) {
            for (Direction.Axis axis : Direction.Axis.values()) {
                Direction.Axis across = Direction.Axis.values()[(axis.ordinal() + 1) % 3];
                Direction.Axis up = Direction.Axis.values()[(axis.ordinal() + 2) % 3];
                for (int corner = 0; corner < 4; corner++) {
                    BlockPos start = cell.relative(across, corner & 1).relative(up, corner >> 1);
                    Edge edge = new Edge(start, axis);
                    if (!seen.add(edge)) continue;
                    boolean here = filled.contains(start);
                    boolean side = filled.contains(start.relative(across, -1));
                    boolean above = filled.contains(start.relative(up, -1));
                    boolean diagonal = filled.contains(start.relative(across, -1).relative(up, -1));
                    int count = (here ? 1 : 0) + (side ? 1 : 0) + (above ? 1 : 0) + (diagonal ? 1 : 0);
                    if (count % 2 == 1 || count == 2 && here == diagonal) drawn.add(edge);
                }
            }
        }
        return drawn;
    }

    private static void line(VertexConsumer quads, Matrix4f matrix, Vector3f from, Vector3f to, float width, int color) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 0.01f) return;
        float nx = -dy / length * width / 2f;
        float ny = dx / length * width / 2f;
        quads.addVertex(matrix, from.x + nx, from.y + ny, DEPTH).setColor(color);
        quads.addVertex(matrix, from.x - nx, from.y - ny, DEPTH).setColor(color);
        quads.addVertex(matrix, to.x - nx, to.y - ny, DEPTH).setColor(color);
        quads.addVertex(matrix, to.x + nx, to.y + ny, DEPTH).setColor(color);
    }

    private static void icon(GuiGraphics graphics, Matrix4f view, HintScene.Icon icon, float time) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(icon.item()));
        if (stack.isEmpty()) return;
        Vec3 at = icon.pos().add(0, Math.sin(time * 0.12) * BOB, 0);
        Vector3f screen = view.transformPosition((float) at.x, (float) at.y, (float) at.z, new Vector3f());
        graphics.pose().pushPose();
        graphics.pose().translate(screen.x - 8f, screen.y - 8f, DEPTH);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    @Nullable
    static Vector3f anchor(Matrix4f view, List<BlockPos> cells) {
        if (cells.isEmpty()) return null;
        Vec3 sum = Vec3.ZERO;
        for (BlockPos cell : cells) sum = sum.add(Vec3.atCenterOf(cell));
        Vec3 center = sum.scale(1.0 / cells.size());
        return view.transformPosition((float) center.x, (float) center.y, (float) center.z, new Vector3f());
    }

    @Nullable
    static BlockPos pick(HintStage stage, Matrix4f view, double mouseX, double mouseY) {
        Matrix4f back = new Matrix4f(view).invert();
        Vector3f near = back.transformPosition((float) mouseX, (float) mouseY, DEPTH + 2000f, new Vector3f());
        Vector3f far = back.transformPosition((float) mouseX, (float) mouseY, DEPTH - 2000f, new Vector3f());
        Vec3 from = new Vec3(near);
        Vec3 to = new Vec3(far);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Map.Entry<BlockPos, BlockState> entry : stage.blocks().entrySet()) {
            BlockHitResult hit = entry.getValue().getShape(EmptyBlockGetter.INSTANCE, entry.getKey()).clip(from, to, entry.getKey());
            if (hit == null || hit.getLocation().distanceToSqr(from) >= bestDistance) continue;
            bestDistance = hit.getLocation().distanceToSqr(from);
            best = entry.getKey();
        }
        return best;
    }
}
