package net.sorukoma.quasium.golem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class GolemNap {
    public static final String SLEEPER = "Ashulbek887";
    static final Codec<GolemNap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("puffs").forGetter(nap -> nap.puffs),
            Codec.BOOL.fieldOf("wants_bed").forGetter(nap -> nap.wantsBed),
            Codec.BOOL.fieldOf("asleep").forGetter(nap -> nap.asleep)).apply(instance, GolemNap::new));
    private static final int PUFFS_TO_SLEEP = 10;
    private static final double HEARING = 16.0;

    int puffs;
    boolean wantsBed;
    boolean asleep;

    GolemNap() {
    }

    private GolemNap(int puffs, boolean wantsBed, boolean asleep) {
        this.puffs = puffs;
        this.wantsBed = wantsBed;
        this.asleep = asleep;
    }

    public int puffs() {
        return puffs;
    }

    public boolean asleep() {
        return asleep;
    }

    boolean worthSaving() {
        return puffs > 0 || wantsBed || asleep;
    }

    static boolean sleeper(WorkerGolem golem) {
        Component name = golem.getCustomName();
        return name != null && SLEEPER.equals(name.getString());
    }

    static boolean holdsSleep(WorkerGolem golem) {
        return golem.getExistingData(SmokeMarks.NAP).map(GolemNap::asleep).orElse(false);
    }

    static void puffed(ClayGolemEntity golem) {
        if (!sleeper(golem)) return;
        GolemNap nap = golem.getData(SmokeMarks.NAP);
        if (nap.wantsBed || nap.asleep || ++nap.puffs < PUFFS_TO_SLEEP) return;
        nap.puffs = 0;
        nap.wantsBed = true;
        say(golem, "quasium.golem.ashulbek.home");
    }

    static boolean heard(WorkerGolem golem) {
        return golem.level().getNearestPlayer(golem, HEARING) != null;
    }

    static void say(WorkerGolem golem, String key) {
        Component line = Component.translatable("chat.type.text", golem.getDisplayName(), Component.translatable(key));
        for (Player player : golem.level().players()) {
            if (player.distanceToSqr(golem) <= HEARING * HEARING) player.displayClientMessage(line, false);
        }
    }

    static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ClayGolemEntity golem) || !(event.getSource().getDirectEntity() instanceof Player)) return;
        golem.getExistingData(SmokeMarks.NAP).filter(GolemNap::asleep).ifPresent(nap -> {
            nap.asleep = false;
            golem.note("nap_kicked");
        });
    }
}
