package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

public class TobaccoPoleBlockEntity extends BlockEntity {
    private int progress;
    private int totalTime;
    private ItemStack result = ItemStack.EMPTY;
    private boolean crossing;
    private boolean matured;

    public TobaccoPoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOBACCO_POLE_BE.get(), pos, state);
    }

    public ItemStack getResult() { return result; }
    public int getProgress() { return progress; }
    public int getTotalTime() { return totalTime; }
    public boolean isCrossing() { return crossing; }
    public boolean isMatured() { return matured; }

    /** Пытается начать новое скрещивание, если рядом есть пара зрелых растений. */
    private void tryStartCrossing() {
        if (level == null || crossing || matured) return;
        BlockPos[] parents = findParents();
        if (parents == null) return;
        TobaccoCropBlock first = (TobaccoCropBlock) level.getBlockState(parents[0]).getBlock();
        TobaccoCropBlock second = (TobaccoCropBlock) level.getBlockState(parents[1]).getBlock();
        BlockState firstState = level.getBlockState(parents[0]);
        BlockState secondState = level.getBlockState(parents[1]);
        String firstVariety = first.getVariety();
        String secondVariety = second.getVariety();
        TobaccoPoleBlock pole = (TobaccoPoleBlock) getBlockState().getBlock();
        // Скрещивание "потребляет" зрелость родителей вне зависимости от исхода.
        level.setBlock(parents[0], firstState.setValue(TobaccoCropBlock.AGE, 3), 3);
        level.setBlock(parents[1], secondState.setValue(TobaccoCropBlock.AGE, 3), 3);
        progress = 0;
        totalTime = 1200 + level.random.nextInt(1201);
        crossing = true;
        if (level.random.nextFloat() > pole.getSuccessChance()) {
            result = ItemStack.EMPTY;
        } else {
            result = createOffspring(firstVariety, secondVariety, firstState, secondState, pole.getMutationChance());
        }
        sync();
    }

    /** Отмечает BE изменённым и рассылает обновление клиенту, чтобы рендерер видел семя/прогресс. */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private ItemStack createOffspring(String first, String second, BlockState firstState, BlockState secondState, float mutationChance) {
        ItemStack seed = ModItems.HYBRID_TOBACCO_SEEDS.get().getDefaultInstance();
        int strength = average(TobaccoVarieties.strength(first), TobaccoVarieties.strength(second));
        int yield = average(TobaccoVarieties.yield(first), TobaccoVarieties.yield(second));
        int quality = average(TobaccoVarieties.quality(first), TobaccoVarieties.quality(second));
        if (level.random.nextFloat() < 0.2f) strength = Math.min(5, strength + 1);
        if (level.random.nextFloat() < 0.1f) quality = Math.max(1, quality - 1);
        String firstMutation = mutationOf(firstState);
        String secondMutation = mutationOf(secondState);
        String mutation = "";
        // Малый шанс "токсичной" мутации: гибрид даёт лист/смесь, наносящий урон при курении.
        // Наследуется от родителя-токсика с повышенным шансом, либо изредка появляется спонтанно.
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
        if ("double_yield".equals(mutation)) yield = Math.min(5, yield + 1);
        if ("concentrated".equals(mutation)) strength = Math.min(5, strength + 1);
        if ("premium_quality".equals(mutation)) quality = Math.min(5, quality + 1);
        seed.set(ModDataComponents.TOBACCO_VARIETY.get(), "hybrid");
        seed.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), strength);
        seed.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), yield);
        seed.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), quality);
        if (!mutation.isEmpty()) seed.set(ModDataComponents.TOBACCO_MUTATION.get(), mutation);
        return seed;
    }

    private static String mutationOf(BlockState state) {
        return TobaccoCropBlock.mutationName(state.getValue(TobaccoCropBlock.MUTATION));
    }


    private ItemStack seedFor(String variety) {
        return switch (variety) {
            case TobaccoVarieties.PESCHANIK -> ModItems.PESCHANIK_SEEDS.get().getDefaultInstance();
            case TobaccoVarieties.BOLOTNIK -> ModItems.BOLOTNIK_SEEDS.get().getDefaultInstance();
            case TobaccoVarieties.LEDYANOY -> ModItems.LEDYANOY_SEEDS.get().getDefaultInstance();
            default -> ModItems.DIKAR_SEEDS.get().getDefaultInstance();
        };
    }

    private static int average(int first, int second) {
        return Math.max(1, Math.min(5, Math.round((first + second) / 2.0f)));
    }

    @Nullable
    private BlockPos[] findParents() {
        if (level == null) return null;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos firstPos = worldPosition.relative(direction);
            BlockPos secondPos = worldPosition.relative(direction.getOpposite());
            if (isMatureCrop(firstPos) && isMatureCrop(secondPos)) return new BlockPos[]{firstPos, secondPos};
            secondPos = worldPosition.relative(direction.getOpposite(), 2);
            if (isMatureCrop(firstPos) && isMatureCrop(secondPos)) return new BlockPos[]{firstPos, secondPos};
        }
        return null;
    }

    private boolean isMatureCrop(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof TobaccoCropBlock crop && state.getValue(TobaccoCropBlock.AGE) == crop.getMaxAge();
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
        crossing = false;
        progress = 0;
        totalTime = 0;
        sync();
        return taken;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("progress", progress); tag.putInt("totalTime", totalTime);
        tag.putBoolean("crossing", crossing); tag.putBoolean("matured", matured);
        if (!result.isEmpty()) tag.put("result", result.saveOptional(provider));
        return tag;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) { tag.merge(getUpdateTag(provider)); super.saveAdditional(tag, provider); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider); progress = tag.getInt("progress"); totalTime = tag.getInt("totalTime");
        crossing = tag.getBoolean("crossing"); matured = tag.getBoolean("matured");
        result = tag.contains("result") ? ItemStack.parse(provider, tag.getCompound("result")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
