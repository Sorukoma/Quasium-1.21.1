package net.sorukoma.quasium.smokelands.block;

import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MudBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.smokelands.SmokelandsRegistry;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SmokelandsBlocks {
    public static final DeferredBlock<Block> ASH_SAND = block("ash_sand",
            () -> new ColoredFallingBlock(new ColorRGBA(0x7A746E), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5f).sound(SoundType.SAND)));
    public static final DeferredBlock<Block> ASH_STONE = block("ash_stone", () -> new Block(stone(MapColor.COLOR_GRAY, 1.5f)));
    public static final DeferredBlock<Block> ASH_BRICKS = block("ash_bricks", () -> new Block(stone(MapColor.COLOR_GRAY, 2.0f)));
    public static final DeferredBlock<Block> CRACKED_ASH_BRICKS = block("cracked_ash_bricks", () -> new Block(stone(MapColor.COLOR_GRAY, 2.0f)));
    public static final DeferredBlock<Block> PETRIFIED_SMOKE = block("petrified_smoke",
            () -> new PetrifiedSmokeBlock(stone(MapColor.COLOR_PURPLE, 3.0f).sound(SoundType.CALCITE)));
    public static final Map<NoteType, DeferredBlock<Block>> MOSAICS = mosaics();
    public static final DeferredBlock<Block> ASH_CACTUS = block("ash_cactus",
            () -> new AshCactusBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).randomTicks().strength(0.4f)
                    .sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> SMOLDER = block("smolder",
            () -> new SmolderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.6f).sound(SoundType.GRAVEL)
                    .lightLevel(state -> 6).emissiveRendering((state, level, pos) -> true)));
    public static final DeferredBlock<Block> SOOT_MUD = block("soot_mud",
            () -> new MudBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MUD).mapColor(MapColor.COLOR_BLACK)));
    public static final DeferredBlock<Block> NICOTINE_MOSS = block("nicotine_moss",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.3f).sound(SoundType.MOSS)));
    public static final DeferredBlock<Block> HOOKAH_LOG = block("hookah_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG).mapColor(MapColor.COLOR_BROWN)));
    public static final DeferredBlock<Block> NICOTINE_LEAVES = block("nicotine_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LEAVES).mapColor(MapColor.COLOR_LIGHT_GREEN)));
    public static final DeferredBlock<Block> HOSE_VINE = block("hose_vine",
            () -> new VineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.VINE).mapColor(MapColor.COLOR_BROWN)));
    public static final DeferredBlock<PrimordialTobaccoBlock> PRIMORDIAL_TOBACCO = SmokelandsRegistry.BLOCKS.register("primordial_tobacco",
            () -> new PrimordialTobaccoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission()
                    .instabreak().sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY)));
    public static final Map<NoteType, DeferredBlock<NoteClotBlock>> CLOTS = clots();
    public static final DeferredBlock<Block> BUDDING_DREAM_QUARTZ = block("budding_dream_quartz",
            () -> new BuddingDreamQuartzBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).randomTicks()
                    .strength(1.5f).sound(SoundType.AMETHYST).requiresCorrectToolForDrops().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> DREAM_QUARTZ_BUD = block("dream_quartz_bud",
            () -> new AmethystClusterBlock(4, 4, crystal(SoundType.SMALL_AMETHYST_BUD, 2)));
    public static final DeferredBlock<Block> DREAM_QUARTZ_CLUSTER = block("dream_quartz_cluster",
            () -> new AmethystClusterBlock(7, 3, crystal(SoundType.AMETHYST_CLUSTER, 5)));
    public static final DeferredBlock<Block> ANCIENT_FLASK = block("ancient_flask",
            () -> new AncientFlaskBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.3f).sound(SoundType.GLASS)
                    .lightLevel(state -> 9).noOcclusion()));

    private SmokelandsBlocks() {
    }

    public static void load() {
    }

    static BlockBehaviour.Properties stone(MapColor color, float strength) {
        return BlockBehaviour.Properties.of().mapColor(color).requiresCorrectToolForDrops().strength(strength, 6.0f).sound(SoundType.STONE);
    }

    static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> factory) {
        DeferredBlock<T> block = SmokelandsRegistry.BLOCKS.register(name, factory);
        SmokelandsRegistry.ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    private static BlockBehaviour.Properties crystal(SoundType sound, int light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).forceSolidOn().noOcclusion().sound(sound)
                .strength(1.5f).lightLevel(state -> light).pushReaction(PushReaction.DESTROY);
    }

    private static Map<NoteType, DeferredBlock<NoteClotBlock>> clots() {
        Map<NoteType, DeferredBlock<NoteClotBlock>> clots = new EnumMap<>(NoteType.class);
        for (NoteType note : NoteType.values()) {
            clots.put(note, block(note.id() + "_clot", () -> new NoteClotBlock(note, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0f).sound(SoundType.AMETHYST).lightLevel(state -> 3 + state.getValue(NoteClotBlock.CHARGES)).noOcclusion())));
        }
        return clots;
    }

    private static Map<NoteType, DeferredBlock<Block>> mosaics() {
        Map<NoteType, DeferredBlock<Block>> mosaics = new EnumMap<>(NoteType.class);
        for (NoteType note : NoteType.values()) {
            mosaics.put(note, block(note.id() + "_mosaic", () -> new Block(stone(MapColor.COLOR_GRAY, 1.5f))));
        }
        return mosaics;
    }
}
