package net.sorukoma.quasium.client.geode;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.sorukoma.quasium.client.ClientKaifData;
import net.sorukoma.quasium.selection.geode.GeodeReveal;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class QuasiumCrustModel extends BakedModelWrapper<BakedModel> {
    public static final ModelProperty<Boolean> REVEALED = new ModelProperty<>();
    public static final ModelData ALWAYS_REVEALED = ModelData.builder().with(REVEALED, true).build();

    private final BakedModel disguise;

    public QuasiumCrustModel(BakedModel revealed, BakedModel disguise) {
        super(revealed);
        this.disguise = disguise;
    }

    private BakedModel shown() {
        return GeodeReveal.revealed(ClientKaifData.getKaifValue()) ? originalModel : disguise;
    }

    private BakedModel shown(ModelData data) {
        return Boolean.TRUE.equals(data.get(REVEALED)) ? originalModel : shown();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return shown().getQuads(state, side, rand);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData,
                                    @Nullable RenderType renderType) {
        return shown(extraData).getQuads(state, side, rand, extraData, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return shown().getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return shown(data).getParticleIcon(data);
    }
}
