package net.sorukoma.quasium.hint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

public record HintScene(List<ResourceLocation> items, int floor, List<Step> steps) {
    public static final int DEFAULT_DURATION = 120;

    public record Placement(String state, BlockPos pos) {
        static final Codec<Placement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("state").forGetter(Placement::state),
                BlockPos.CODEC.fieldOf("pos").forGetter(Placement::pos)
        ).apply(instance, Placement::new));
    }

    public record Icon(ResourceLocation item, Vec3 pos) {
        static final Codec<Icon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Icon::item),
                Vec3.CODEC.fieldOf("pos").forGetter(Icon::pos)
        ).apply(instance, Icon::new));
    }

    public record Step(int duration, List<Placement> place, List<BlockPos> remove, List<BlockPos> highlight, List<Icon> icons,
                       List<String> args) {
        static final Codec<Step> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("duration", DEFAULT_DURATION).forGetter(Step::duration),
                Placement.CODEC.listOf().optionalFieldOf("place", List.of()).forGetter(Step::place),
                BlockPos.CODEC.listOf().optionalFieldOf("remove", List.of()).forGetter(Step::remove),
                BlockPos.CODEC.listOf().optionalFieldOf("highlight", List.of()).forGetter(Step::highlight),
                Icon.CODEC.listOf().optionalFieldOf("icons", List.of()).forGetter(Step::icons),
                Codec.STRING.listOf().optionalFieldOf("args", List.of()).forGetter(Step::args)
        ).apply(instance, Step::new));
    }

    public static final Codec<HintScene> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(HintScene::items),
            Codec.intRange(0, 16).optionalFieldOf("floor", 0).forGetter(HintScene::floor),
            Step.CODEC.listOf().fieldOf("steps").forGetter(HintScene::steps)
    ).apply(instance, HintScene::new));

    public static final Codec<Map<ResourceLocation, HintScene>> MAP_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, CODEC);

    public static String titleKey(ResourceLocation scene) {
        return "quasium.hint." + scene.getPath() + ".title";
    }

    public static String captionKey(ResourceLocation scene, int step) {
        return "quasium.hint." + scene.getPath() + "." + (step + 1);
    }
}
