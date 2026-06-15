package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class MakhorkaCropBlock extends TobaccoCropBlock {

    private final Supplier<Item> seedsItem;
    private final Supplier<Item> leavesItem;

    public MakhorkaCropBlock(Supplier<Item> seedsItem, Supplier<Item> leavesItem, BlockBehaviour.Properties properties) {
        super(properties);
        this.seedsItem = seedsItem;
        this.leavesItem = leavesItem;
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
    public MapCodec<? extends MakhorkaCropBlock> codec() {
        return simpleCodec(props -> new MakhorkaCropBlock(seedsItem, leavesItem, props));
    }
}