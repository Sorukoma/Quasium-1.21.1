package net.sorukoma.quasium.smokelands;

import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.data.PlayerKaifData;

import java.util.function.IntSupplier;

public final class SmokelandsKaif {
    private static IntSupplier client = () -> 0;

    private SmokelandsKaif() {
    }

    public static void readClientFrom(IntSupplier supplier) {
        client = supplier;
    }

    public static int of(Player player) {
        return player.level().isClientSide ? client.getAsInt() : PlayerKaifData.getKaifValue(player);
    }
}
