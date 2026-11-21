package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class HerderGoal extends ClayChoreGoal {
    private static final int FEED_TAKE = 16;

    private String idle = "no_animals";

    HerderGoal(ClayGolemEntity golem) {
        super(golem, GolemJob.HERDER);
    }

    static void collectDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ClayGolemEntity golem) || golem.job() != GolemJob.HERDER
                || !(event.getEntity() instanceof Animal)) return;
        golem.takeIn(event.getDrops().stream().map(ItemEntity::getItem).toList(), event.getEntity().blockPosition());
        event.setCanceled(true);
    }

    @Nullable
    @Override
    protected Chore next(List<BlockPos> zones) {
        idle = "no_animals";
        for (BlockPos zone : zones) {
            Map<EntityType<?>, List<Animal>> herds = golem.level()
                    .getEntitiesOfClass(Animal.class, ZoneBox.area(golem.level(), zone), HerderGoal::livestock)
                    .stream().collect(Collectors.groupingBy(Entity::getType));
            if (!herds.isEmpty() && idle.equals("no_animals")) idle = "herd_ok";
            for (List<Animal> herd : herds.values()) {
                Chore chore = herd.size() > golem.herdLimit() ? cull(zone, herd) : breed(zone, herd);
                if (chore != null) return chore;
            }
        }
        golem.waitFor(idle);
        return null;
    }

    static Predicate<ItemStack> kept(ClayGolemEntity golem) {
        List<Animal> herd = golem.marks(MarkType.ZONE).stream()
                .flatMap(zone -> golem.level().getEntitiesOfClass(Animal.class, ZoneBox.area(golem.level(), zone), HerderGoal::livestock).stream())
                .toList();
        return stack -> herd.stream().anyMatch(animal -> animal.isFood(stack));
    }

    private static boolean livestock(Animal animal) {
        return animal.isAlive() && !(animal instanceof TamableAnimal) && !(animal instanceof AbstractHorse);
    }

    @Nullable
    private Chore cull(BlockPos zone, List<Animal> herd) {
        if (!golem.hasRoom()) return null;
        Animal victim = nearest(herd.stream().filter(this::cullable).toList());
        if (victim == null) return null;
        return Chore.on(zone, victim, () -> cullable(victim) && golem.hasRoom(), () -> hit(victim));
    }

    private boolean cullable(Animal animal) {
        return !animal.isBaby() && !animal.hasCustomName() && !animal.isLeashed() && !golem.isUnreachable(animal.blockPosition());
    }

    private boolean hit(Animal victim) {
        if (golem.doHurtTarget(victim)) GolemTools.use(golem);
        if (victim.isAlive()) return false;
        golem.note("culled", victim.getType().getDescriptionId());
        return true;
    }

    @Nullable
    private Chore breed(BlockPos zone, List<Animal> herd) {
        long inLove = herd.stream().filter(Animal::isInLove).count();
        int wanted = inLove % 2 == 1 ? 1 : herd.size() + inLove / 2 < golem.herdLimit() ? 2 : 0;
        List<Animal> ready = herd.stream().filter(this::readyToBreed).toList();
        if (wanted == 0 || ready.size() < wanted) return null;
        Animal first = nearest(ready);
        Predicate<ItemStack> feed = first::isFood;
        if (ClayStores.count(golem.cargo(), feed) >= wanted) {
            return Chore.on(zone, first, () -> readyToBreed(first), () -> feed(first, feed));
        }
        return fetch(zone, feed);
    }

    private boolean readyToBreed(Animal animal) {
        return animal.getAge() == 0 && animal.canFallInLove() && !golem.isUnreachable(animal.blockPosition());
    }

    private boolean feed(Animal animal, Predicate<ItemStack> feed) {
        int slot = ClayStores.slotOf(golem.cargo(), feed);
        if (slot < 0) return true;
        golem.cargo().extractItem(slot, 1, false);
        animal.setInLove(null);
        golem.note("fed_animal", animal.getType().getDescriptionId());
        return true;
    }

    @Nullable
    private Chore fetch(BlockPos zone, Predicate<ItemStack> feed) {
        if (!golem.hasRoom()) return null;
        BlockPos store = ClayStores.holding(golem, feed);
        if (store == null) {
            idle = "no_feed";
            return null;
        }
        return Chore.at(zone, store, () -> ClayStores.holds(golem, store, feed), () -> {
            ItemStack taken = ClayStores.take(golem, store, feed, FEED_TAKE);
            if (!taken.isEmpty()) golem.note("took_feed", taken.getCount(), taken.getDescriptionId());
            return true;
        });
    }
}
