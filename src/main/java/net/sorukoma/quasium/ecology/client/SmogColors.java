package net.sorukoma.quasium.ecology.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.progression.ConfigArgs;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class SmogColors {
    private static final int ASHEN = 0xFF7C7873;
    private static final int DEAD = 0xFF7E6E57;
    private static final int GRAY = 0xFF6F6D6B;
    private static final float[] AMOUNT = {0f, 0f, 0.3f, 0.45f, 0.6f, 0.8f};

    private SmogColors() {
    }

    public static int tint(int color, int smog) {
        int level = Math.min(smog, AMOUNT.length - 1);
        if (level < 2) return color;
        int target = level == 4 ? DEAD : level == 5 ? GRAY : ASHEN;
        float amount = AMOUNT[level] * ConfigArgs.synced(EcologyConfig.SMOG_TINT_PCT) / 100f;
        return FastColor.ARGB32.lerp(amount, 0xFF000000 | color, target) & 0xFFFFFF;
    }

    private static int smoggy(int color, BlockPos pos) {
        return tint(color, ClientSmog.smog(pos));
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level == null || pos == null ? GrassColor.getDefaultColor()
                        : smoggy(BiomeColors.getAverageGrassColor(level,
                                state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos), pos),
                Blocks.LARGE_FERN, Blocks.TALL_GRASS);
        event.register((state, level, pos, tint) -> level == null || pos == null ? GrassColor.getDefaultColor()
                        : smoggy(BiomeColors.getAverageGrassColor(level, pos), pos),
                Blocks.GRASS_BLOCK, Blocks.FERN, Blocks.SHORT_GRASS, Blocks.POTTED_FERN);
        event.register((state, level, pos, tint) -> pos == null ? FoliageColor.getEvergreenColor()
                : smoggy(FoliageColor.getEvergreenColor(), pos), Blocks.SPRUCE_LEAVES);
        event.register((state, level, pos, tint) -> pos == null ? FoliageColor.getBirchColor()
                : smoggy(FoliageColor.getBirchColor(), pos), Blocks.BIRCH_LEAVES);
        event.register((state, level, pos, tint) -> level == null || pos == null ? FoliageColor.getDefaultColor()
                        : smoggy(BiomeColors.getAverageFoliageColor(level, pos), pos),
                Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.VINE, Blocks.MANGROVE_LEAVES);
    }
}
