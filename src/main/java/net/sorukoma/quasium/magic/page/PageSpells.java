package net.sorukoma.quasium.magic.page;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PageSpells {
    public static final String PREFIX = "page/";
    private static final int MAX_ENTRIES = 4096;
    private static final Map<ResourceLocation, CompiledPage> COMPILED = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ResourceLocation, CompiledPage> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private PageSpells() {
    }

    public static boolean isPage(@Nullable ResourceLocation id) {
        return id != null && QuasiumMod.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith(PREFIX);
    }

    @Nullable
    public static synchronized CompiledPage get(@Nullable ResourceLocation id) {
        return isPage(id) ? COMPILED.get(id) : null;
    }

    static synchronized void put(CompiledPage page) {
        COMPILED.put(page.id(), page);
    }

    static synchronized void clear() {
        COMPILED.clear();
    }
}
