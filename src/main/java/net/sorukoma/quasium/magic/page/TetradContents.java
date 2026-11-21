package net.sorukoma.quasium.magic.page;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record TetradContents(List<SpellPage> pages, int active) {
    public static final int MAX_PAGES = 16;

    public static final Codec<TetradContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpellPage.CODEC.listOf(0, MAX_PAGES).fieldOf("pages").forGetter(TetradContents::pages),
            Codec.INT.optionalFieldOf("active", 0).forGetter(TetradContents::active)
    ).apply(instance, TetradContents::new));

    public static final StreamCodec<ByteBuf, TetradContents> STREAM_CODEC = StreamCodec.composite(
            SpellPage.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PAGES)), TetradContents::pages,
            ByteBufCodecs.VAR_INT, TetradContents::active,
            TetradContents::new);

    public TetradContents {
        pages = List.copyOf(pages);
        active = pages.isEmpty() ? 0 : Mth.clamp(active, 0, pages.size() - 1);
    }

    public static TetradContents empty(int size) {
        return new TetradContents(Collections.nCopies(size, SpellPage.EMPTY), 0);
    }

    public int size() {
        return pages.size();
    }

    public SpellPage page(int index) {
        return index < 0 || index >= pages.size() ? SpellPage.EMPTY : pages.get(index);
    }

    public SpellPage activePage() {
        return page(active);
    }

    public TetradContents with(int index, SpellPage page) {
        if (index < 0 || index >= pages.size()) return this;
        List<SpellPage> changed = new ArrayList<>(pages);
        changed.set(index, page);
        return new TetradContents(changed, active);
    }

    public TetradContents withActive(int index) {
        return new TetradContents(pages, index);
    }

    public TetradContents resized(int size) {
        List<SpellPage> changed = new ArrayList<>(pages.subList(0, Math.min(size, pages.size())));
        while (changed.size() < size) changed.add(SpellPage.EMPTY);
        return new TetradContents(changed, active);
    }

    public int nextFilled(int from, int step) {
        for (int i = 1; i <= pages.size(); i++) {
            int index = Math.floorMod(from + step * i, pages.size());
            if (!pages.get(index).isEmpty()) return index;
        }
        return from;
    }
}
