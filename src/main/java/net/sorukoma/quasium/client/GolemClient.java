package net.sorukoma.quasium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.golem.GolemFeedPacket;
import net.sorukoma.quasium.golem.GolemMood;
import net.sorukoma.quasium.golem.MarkBlocks;
import net.sorukoma.quasium.golem.MarkType;
import net.sorukoma.quasium.golem.SmokeMarkBlockEntity;
import net.sorukoma.quasium.golem.WorkerGolem;
import net.sorukoma.quasium.golem.ZoneBox;
import net.sorukoma.quasium.golem.program.MarkedPlace;
import net.sorukoma.quasium.golem.program.ProgramBlocks;
import net.sorukoma.quasium.screen.GolemFeederScreen;
import net.sorukoma.quasium.screen.GolemScreen;
import net.sorukoma.quasium.screen.ModMenuTypes;
import net.sorukoma.quasium.screen.ProgramTableScreen;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class GolemClient {
    private static final int BINDING_TICKS = 4;
    private static final int BINDING_RANGE = 24;
    private static final double DOT_STEP = 0.5;
    private static final float DOT_SIZE = 0.5f;
    private static final double EDGE_STEP = 0.2;
    private static final DustParticleOptions PLACE_EDGE = new DustParticleOptions(Vec3.fromRGB24(MarkType.PROGRAM.color()).toVector3f(), 0.4f);
    private static final DustParticleOptions PLACE_GLOW = new DustParticleOptions(Vec3.fromRGB24(MarkType.PROGRAM.color()).toVector3f(), 1.0f);

    private GolemClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.GOLEM_MENU.get(), GolemScreen::new);
        event.register(ModMenuTypes.GOLEM_FEEDER_MENU.get(), GolemFeederScreen::new);
        event.register(ModMenuTypes.PROGRAM_TABLE_MENU.get(), ProgramTableScreen::new);
    }

    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || minecraft.player == null) return;
        if (!Screen.hasControlDown() || !minecraft.player.isShiftKeyDown()) return;
        if (!(minecraft.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof WorkerGolem golem)) return;
        if (GolemMood.Food.of(minecraft.player.getMainHandItem()) == null) return;
        PacketDistributor.sendToServer(new GolemFeedPacket(golem.getId()));
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.isPaused() || player.tickCount % BINDING_TICKS != 0) return;
        for (InteractionHand hand : InteractionHand.values()) showPlace(level, player.getItemInHand(hand).get(ProgramBlocks.MARKED_PLACE.get()));
        if (!player.isHolding(MarkBlocks.GOLEM_STAFF.get())) return;
        Map<UUID, WorkerGolem> golems = new HashMap<>();
        for (WorkerGolem golem : level.getEntitiesOfClass(WorkerGolem.class, player.getBoundingBox().inflate(BINDING_RANGE * 2))) {
            golems.put(golem.getUUID(), golem);
        }
        ChunkPos from = new ChunkPos(player.blockPosition().offset(-BINDING_RANGE, 0, -BINDING_RANGE));
        ChunkPos to = new ChunkPos(player.blockPosition().offset(BINDING_RANGE, 0, BINDING_RANGE));
        for (int x = from.x; x <= to.x; x++) {
            for (int z = from.z; z <= to.z; z++) {
                for (BlockEntity entity : level.getChunk(x, z).getBlockEntities().values()) {
                    if (entity instanceof SmokeMarkBlockEntity mark) drawBindings(level, mark, golems);
                }
            }
        }
    }

    private static void showPlace(ClientLevel level, @Nullable MarkedPlace place) {
        if (place == null || place.where().dimension() != level.dimension()) return;
        BlockPos pos = place.where().pos();
        outline(level, PLACE_EDGE, new AABB(pos).inflate(0.02), EDGE_STEP);
        RandomSource random = level.random;
        Vec3 glow = Vec3.atCenterOf(pos).relative(place.face(), 0.75)
                .add(random.nextDouble() * 0.4 - 0.2, random.nextDouble() * 0.4 - 0.2, random.nextDouble() * 0.4 - 0.2);
        level.addParticle(PLACE_GLOW, glow.x, glow.y, glow.z, 0.0, 0.0, 0.0);
    }

    private static void drawBindings(ClientLevel level, SmokeMarkBlockEntity mark, Map<UUID, WorkerGolem> golems) {
        DustParticleOptions dust = new DustParticleOptions(Vec3.fromRGB24(mark.type().color()).toVector3f(), DOT_SIZE);
        Vec3 spot = Vec3.atBottomCenterOf(mark.getBlockPos()).add(0.0, 0.15, 0.0);
        for (UUID id : mark.golems()) {
            WorkerGolem golem = golems.get(id);
            if (golem != null) dots(level, dust, golem.position().add(0.0, golem.getBbHeight() * 0.5, 0.0), spot);
        }
        for (BlockPos store : mark.links()) dots(level, dust, spot, Vec3.atCenterOf(store));
        if (mark.type() == MarkType.ZONE) outline(level, dust, ZoneBox.area(level, mark.getBlockPos()), DOT_STEP);
    }

    private static void outline(ClientLevel level, DustParticleOptions dust, AABB box, double step) {
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};
        for (double y : ys) {
            for (double z : zs) dots(level, dust, new Vec3(box.minX, y, z), new Vec3(box.maxX, y, z), step);
            for (double x : xs) dots(level, dust, new Vec3(x, y, box.minZ), new Vec3(x, y, box.maxZ), step);
        }
        for (double x : xs) {
            for (double z : zs) dots(level, dust, new Vec3(x, box.minY, z), new Vec3(x, box.maxY, z), step);
        }
    }

    private static void dots(ClientLevel level, DustParticleOptions dust, Vec3 from, Vec3 to) {
        dots(level, dust, from, to, DOT_STEP);
    }

    private static void dots(ClientLevel level, DustParticleOptions dust, Vec3 from, Vec3 to, double step) {
        Vec3 path = to.subtract(from);
        int count = (int) (path.length() / step);
        for (int i = 1; i < count; i++) {
            Vec3 dot = from.add(path.scale((double) i / count));
            level.addParticle(dust, dot.x, dot.y, dot.z, 0.0, 0.0, 0.0);
        }
    }
}
