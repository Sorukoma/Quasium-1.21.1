package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.sorukoma.quasium.component.ModDataComponents;

import java.util.function.Supplier;

public class VarietyTobaccoCropBlock extends TobaccoCropBlock {
    private final Supplier<Item> seedsItem;
    private final Supplier<Item> leavesItem;
    private final String variety;

    public VarietyTobaccoCropBlock(Supplier<Item> seedsItem, Supplier<Item> leavesItem, String variety,
                                   BlockBehaviour.Properties properties) {
        super(properties);
        this.seedsItem = seedsItem;
        this.leavesItem = leavesItem;
        this.variety = variety;
        registerDefaultState(defaultBlockState()
                .setValue(AGE, 0).setValue(WATERED, false)
                .setValue(MUTATION, 0));
    }

    @Override
    public String getVariety() {
        return variety;
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
    protected void applyLeafData(net.minecraft.world.item.ItemStack leaves, net.minecraft.world.level.block.state.BlockState state,
                                 net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        super.applyLeafData(leaves, state, level, pos);
        leaves.set(ModDataComponents.TOBACCO_VARIETY.get(), variety);
    }

    @Override
    public MapCodec<? extends VarietyTobaccoCropBlock> codec() {
        return simpleCodec(props -> new VarietyTobaccoCropBlock(seedsItem, leavesItem, variety, props));
    }
}
