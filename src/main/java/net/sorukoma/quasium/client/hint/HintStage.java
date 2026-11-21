package net.sorukoma.quasium.client.hint;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.hint.HintScene;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class HintStage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockState FLOOR_LIGHT = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState FLOOR_DARK = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();

    private final HintScene scene;
    private final Map<String, Optional<BlockState>> parsed = new HashMap<>();
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final Map<BlockPos, Integer> fresh = new HashMap<>();
    private final AABB bounds;

    HintStage(HintScene scene) {
        this.scene = scene;
        this.bounds = measure(scene);
    }

    void show(int step) {
        blocks.clear();
        fresh.clear();
        for (int x = 0; x < scene.floor(); x++) {
            for (int z = 0; z < scene.floor(); z++) blocks.put(new BlockPos(x, -1, z), (x + z) % 2 == 0 ? FLOOR_LIGHT : FLOOR_DARK);
        }
        for (int index = 0; index <= step; index++) {
            HintScene.Step current = scene.steps().get(index);
            current.remove().forEach(blocks::remove);
            for (HintScene.Placement placement : current.place()) {
                Optional<BlockState> state = parsed.computeIfAbsent(placement.state(), HintStage::read);
                if (state.isEmpty()) continue;
                blocks.put(placement.pos(), state.get());
                if (index == step) fresh.put(placement.pos(), fresh.size());
            }
        }
    }

    Map<BlockPos, BlockState> blocks() {
        return blocks;
    }

    int dropOrder(BlockPos pos) {
        return fresh.getOrDefault(pos, -1);
    }

    int dropCount() {
        return fresh.size();
    }

    AABB bounds() {
        return bounds;
    }

    int floor() {
        return scene.floor();
    }

    private static AABB measure(HintScene scene) {
        AABB box = scene.floor() > 0 ? new AABB(0, -1, 0, scene.floor(), 0, scene.floor()) : null;
        for (HintScene.Step step : scene.steps()) {
            for (HintScene.Placement placement : step.place()) box = grow(box, new AABB(placement.pos()));
        }
        return box == null ? new AABB(Vec3.ZERO, new Vec3(1, 1, 1)) : box;
    }

    private static AABB grow(AABB box, AABB part) {
        return box == null ? part : box.minmax(part);
    }

    private static Optional<BlockState> read(String text) {
        try {
            BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), text, false).blockState();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getBlockRenderer().getBlockModel(state) == minecraft.getModelManager().getMissingModel()) {
                LOGGER.warn("Hint scene block {} has no model, skipped", text);
                return Optional.empty();
            }
            return Optional.of(state);
        } catch (CommandSyntaxException exception) {
            LOGGER.warn("Hint scene block {} does not parse, skipped: {}", text, exception.getMessage());
            return Optional.empty();
        }
    }
}
