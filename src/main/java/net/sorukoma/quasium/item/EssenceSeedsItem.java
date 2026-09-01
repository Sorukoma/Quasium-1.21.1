package net.sorukoma.quasium.item;

import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class EssenceSeedsItem extends TobaccoSeedsItem {
    public EssenceSeedsItem(Supplier<Block> cropBlock, Properties properties) {
        super(cropBlock, properties);
    }
}
