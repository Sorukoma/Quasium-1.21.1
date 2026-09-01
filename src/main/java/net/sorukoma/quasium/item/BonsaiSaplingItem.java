package net.sorukoma.quasium.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class BonsaiSaplingItem extends Item {
    private final Supplier<Block> bonsaiPot;
    private final String treeType;

    public BonsaiSaplingItem(Supplier<Block> bonsaiPot, String treeType, Properties properties) {
        super(properties);
        this.bonsaiPot = bonsaiPot;
        this.treeType = treeType;
    }

    public String treeType() {
        return treeType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).is(bonsaiPot.get())) {
            return InteractionResult.PASS;
        }
        return InteractionResult.FAIL;
    }
}
