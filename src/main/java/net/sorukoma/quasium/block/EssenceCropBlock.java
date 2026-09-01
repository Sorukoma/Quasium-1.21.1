package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.function.Supplier;

public class EssenceCropBlock extends CropBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 4);
    private final Supplier<Item> seedsItem;
    private final Supplier<Item> outputItem;

    public EssenceCropBlock(Supplier<Item> seedsItem, Supplier<Item> outputItem, BlockBehaviour.Properties properties) {
        super(properties);
        this.seedsItem = seedsItem;
        this.outputItem = outputItem;
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 4;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected Item getBaseSeedId() {
        return seedsItem.get();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (state.getValue(AGE) < getMaxAge()) {
            return List.of(new ItemStack(seedsItem.get()));
        }
        return List.of(new ItemStack(outputItem.get(), 1), new ItemStack(seedsItem.get(), 1));
    }

    @Override
    public MapCodec<? extends EssenceCropBlock> codec() {
        return simpleCodec(props -> new EssenceCropBlock(seedsItem, outputItem, props));
    }
}
