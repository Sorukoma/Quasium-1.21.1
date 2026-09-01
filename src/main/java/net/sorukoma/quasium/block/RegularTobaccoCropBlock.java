package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class RegularTobaccoCropBlock extends TobaccoCropBlock {

    private final Supplier<Item> seedsItem;
    private final Supplier<Item> leavesItem;
    private final Supplier<Item> shearSeedDropItem;

    /** Шанс дропа семян махорки при стрижке обычного табака ножницами. Меняйте это значение для тестирования. */
    private static final float MAKHORKA_SHEAR_SEED_DROP_CHANCE = 0.05F;

    public RegularTobaccoCropBlock(Supplier<Item> seedsItem, Supplier<Item> leavesItem, Supplier<Item> shearSeedDropItem, BlockBehaviour.Properties properties) {
        super(properties);
        this.seedsItem = seedsItem;
        this.leavesItem = leavesItem;
        this.shearSeedDropItem = shearSeedDropItem;
    }

    @Override
    protected float getShearSeedDropChance() {
        return MAKHORKA_SHEAR_SEED_DROP_CHANCE;
    }

    @Override
    protected Item getShearSeedDropItem() {
        return shearSeedDropItem.get();
    }

    @Override
    protected Item getSeedsItem() {
        return seedsItem.get();
    }

    @Override
    protected Item getLeavesItem() {
        return leavesItem.get();
    }

    @Override
    public MapCodec<? extends RegularTobaccoCropBlock> codec() {
        return simpleCodec(props -> new RegularTobaccoCropBlock(seedsItem, leavesItem, shearSeedDropItem, props));
    }
}