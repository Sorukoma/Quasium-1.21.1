package net.sorukoma.quasium.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import net.sorukoma.quasium.block.TobaccoPoleBlock;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.selection.genetics.Genetics;
import net.sorukoma.quasium.selection.genetics.Lineage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TobaccoPoleBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int progress;
    private int totalTime;
    private ItemStack result = ItemStack.EMPTY;
    private boolean crossing;
    private boolean matured;
    private boolean cloned;

    public TobaccoPoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOBACCO_POLE_BE.get(), pos, state);
    }

    public ItemStack getResult() { return result; }
    public int getProgress() { return progress; }
    public int getTotalTime() { return totalTime; }
    public boolean isCrossing() { return crossing; }
    public boolean isMatured() { return matured; }
    public boolean holdsClone() { return cloned; }

    private void tryStartCrossing() {
        if (level == null || crossing || matured) return;
        BlockPos[] parents = findParents();
        if (parents == null) return;
        TobaccoPoleBlock pole = (TobaccoPoleBlock) getBlockState().getBlock();
        ItemStack offspring = level.random.nextFloat() > pole.getSuccessChance() ? ItemStack.EMPTY : offspring(parents, pole);
        for (BlockPos parent : parents) {
            level.setBlock(parent, level.getBlockState(parent).setValue(TobaccoCropBlock.AGE, 3), 3);
        }
        progress = 0;
        totalTime = 1200 + level.random.nextInt(1201);
        crossing = true;
        result = offspring;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private ItemStack offspring(BlockPos[] parents, TobaccoPoleBlock pole) {
        TobaccoCropBlockEntity first = crop(parents[0]);
        TobaccoCropBlockEntity second = crop(parents[1]);
        double averageQuality = (first.getQualityStars() + second.getQualityStars()) / 2.0;
        double chance = Genetics.hybridChance(pole.getMutationChance(), averageQuality, SelectionConfig.MUTATION_QUALITY_BONUS_PCT.get());
        LOGGER.debug("Tobacco pole at {}: hybrid chance {} for {} x {}", worldPosition, chance,
                Lineage.key(first.lineage()), Lineage.key(second.lineage()));
        cloned = level.random.nextDouble() >= chance;
        if (cloned) {
            BlockPos parent = parents[level.random.nextInt(2)];
            BlockState state = level.getBlockState(parent);
            return ((TobaccoCropBlock) state.getBlock()).seedsWithGenes(state, crop(parent), 1);
        }
        return hybridSeed(first, second, level.getBlockState(parents[0]), level.getBlockState(parents[1]), pole.getMutationChance());
    }

    private ItemStack hybridSeed(TobaccoCropBlockEntity first, TobaccoCropBlockEntity second, BlockState firstState, BlockState secondState,
                                 float mutationChance) {
        boolean firstHybrid = Lineage.isHybrid(first.lineage());
        boolean secondHybrid = Lineage.isHybrid(second.lineage());
        int strength = cross(first.getStrengthStars(), firstHybrid, second.getStrengthStars(), secondHybrid);
        int yield = cross(first.getYieldStars(), firstHybrid, second.getYieldStars(), secondHybrid);
        int quality = cross(first.getQualityStars(), firstHybrid, second.getQualityStars(), secondHybrid);
        String mutation = inheritMutation(mutationOf(firstState), mutationOf(secondState), mutationChance);
        if ("double_yield".equals(mutation)) yield = Math.min(5, yield + 1);
        if ("concentrated".equals(mutation)) strength = Math.min(5, strength + 1);
        if ("premium_quality".equals(mutation)) quality = Math.min(5, quality + 1);
        ItemStack seed = ModItems.HYBRID_TOBACCO_SEEDS.get().getDefaultInstance();
        seed.set(ModDataComponents.TOBACCO_VARIETY.get(), TobaccoVarieties.HYBRID);
        seed.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), strength);
        seed.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), yield);
        seed.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), quality);
        if (!mutation.isEmpty()) seed.set(ModDataComponents.TOBACCO_MUTATION.get(), mutation);
        Lineage.apply(seed, Lineage.merge(first.lineage(), second.lineage()));
        return seed;
    }

    private int cross(int first, boolean firstHybrid, int second, boolean secondHybrid) {
        int roll = Genetics.roll(level.random);
        if (firstHybrid == secondHybrid) return Genetics.inherit(first, second, roll);
        return firstHybrid ? Genetics.recombine(first, second, roll) : Genetics.recombine(second, first, roll);
    }

    private String inheritMutation(String firstMutation, String secondMutation, float mutationChance) {
        String mutation = "";
        boolean parentToxic = "toxic".equals(firstMutation) || "toxic".equals(secondMutation);
        if (parentToxic && level.random.nextFloat() < 0.5f) {
            mutation = "toxic";
        } else if (!parentToxic && level.random.nextFloat() < 0.03f) {
            mutation = "toxic";
        }
        if (mutation.isEmpty() && !firstMutation.isEmpty() && !secondMutation.isEmpty()) {
            if (level.random.nextFloat() < 0.75f) {
                mutation = level.random.nextBoolean() ? firstMutation : secondMutation;
            }
        } else if (mutation.isEmpty() && !firstMutation.isEmpty() && !"toxic".equals(firstMutation) && level.random.nextFloat() < 0.35f) {
            mutation = firstMutation;
        } else if (mutation.isEmpty() && !secondMutation.isEmpty() && !"toxic".equals(secondMutation) && level.random.nextFloat() < 0.35f) {
            mutation = secondMutation;
        }
        if (mutation.isEmpty() && level.random.nextFloat() < mutationChance) mutation = switch (level.random.nextInt(5)) {
            case 0 -> "double_yield";
            case 1 -> "fast_growth";
            case 2 -> "hardy";
            case 3 -> "concentrated";
            default -> "premium_quality";
        };
        return mutation;
    }

    private static String mutationOf(BlockState state) {
        return TobaccoCropBlock.mutationName(state.getValue(TobaccoCropBlock.MUTATION));
    }

    public boolean sameParentsOnly() {
        List<BlockPos[]> pairs = maturePairs();
        return !pairs.isEmpty() && pairs.stream().noneMatch(this::differ);
    }

    @Nullable
    private BlockPos[] findParents() {
        return maturePairs().stream().filter(this::differ).findFirst().orElse(null);
    }

    private boolean differ(BlockPos[] pair) {
        return !Lineage.key(crop(pair[0]).lineage()).equals(Lineage.key(crop(pair[1]).lineage()));
    }

    private List<BlockPos[]> maturePairs() {
        List<BlockPos[]> pairs = new ArrayList<>();
        if (level == null) return pairs;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos firstPos = worldPosition.relative(direction);
            if (!isMatureCrop(firstPos)) continue;
            for (int distance = 1; distance <= 2; distance++) {
                BlockPos secondPos = worldPosition.relative(direction.getOpposite(), distance);
                if (isMatureCrop(secondPos)) {
                    pairs.add(new BlockPos[]{firstPos, secondPos});
                    break;
                }
            }
        }
        return pairs;
    }

    private TobaccoCropBlockEntity crop(BlockPos pos) {
        return (TobaccoCropBlockEntity) level.getBlockEntity(pos);
    }

    private boolean isMatureCrop(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof TobaccoCropBlock crop && crop.isMaxAge(state)
                && level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TobaccoPoleBlockEntity entity) {
        if (level.isClientSide) return;
        // Готовое семя ждёт сбора игроком.
        if (entity.matured) return;
        // Нет активного скрещивания — пробуем начать новое (если рядом созрела пара родителей).
        if (!entity.crossing) {
            if (level.getGameTime() % 20 == 0) entity.tryStartCrossing();
            return;
        }
        // Идёт скрещивание — крутим таймер.
        if (entity.progress < entity.totalTime) {
            entity.progress++;
            if (level instanceof ServerLevel server && entity.progress % 60 == 0) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, 2, .2, .1, .2, .01);
            }
            if (entity.progress % 100 == 0) {
                level.playSound(null, pos, SoundEvents.BEE_LOOP, SoundSource.BLOCKS, .18f, 1.4f);
            }
            // Периодически синхронизируем прогресс, чтобы рендерер плавно увеличивал масштаб семечка.
            if (entity.progress % 20 == 0) entity.sync();
            else entity.setChanged();
            return;
        }
        // Таймер завершён.
        entity.crossing = false;
        if (entity.result.isEmpty()) {
            // Неудачное скрещивание — сбрасываем состояние, цикл начнётся заново.
            entity.progress = 0;
            entity.totalTime = 0;
        } else {
            entity.matured = true;
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + .5, pos.getY() + 1.0, pos.getZ() + .5, 8, .25, .1, .25, .02);
            }
        }
        entity.sync();
    }

    public ItemStack takeResult() {
        if (!matured || result.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = result;
        // Сбрасываем состояние, чтобы жёрдочка могла начать новое скрещивание.
        result = ItemStack.EMPTY;
        matured = false;
        cloned = false;
        crossing = false;
        progress = 0;
        totalTime = 0;
        sync();
        return taken;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("progress", progress); tag.putInt("totalTime", totalTime);
        tag.putBoolean("crossing", crossing); tag.putBoolean("matured", matured); tag.putBoolean("cloned", cloned);
        if (!result.isEmpty()) tag.put("result", result.saveOptional(provider));
        return tag;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) { tag.merge(getUpdateTag(provider)); super.saveAdditional(tag, provider); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider); progress = tag.getInt("progress"); totalTime = tag.getInt("totalTime");
        crossing = tag.getBoolean("crossing"); matured = tag.getBoolean("matured"); cloned = tag.getBoolean("cloned");
        result = ItemStack.parseOptional(provider, tag.getCompound("result"));
    }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
