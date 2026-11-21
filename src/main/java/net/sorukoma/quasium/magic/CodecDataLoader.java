package net.sorukoma.quasium.magic;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class CodecDataLoader<T> extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String folder;
    private final Codec<T> codec;
    private final Consumer<Map<ResourceLocation, T>> sink;

    public CodecDataLoader(String folder, Codec<T> codec, Consumer<Map<ResourceLocation, T>> sink) {
        super(new Gson(), folder);
        this.folder = folder;
        this.codec = codec;
        this.sink = sink;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, T> loaded = new TreeMap<>();
        jsons.forEach((id, json) -> codec.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LOGGER.error("Skipping {} {}: {}", folder, id, error))
                .ifPresent(value -> loaded.put(id, value)));
        sink.accept(loaded);
        LOGGER.info("Loaded {} entries from data/*/{}", loaded.size(), folder);
    }
}
