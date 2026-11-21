package net.sorukoma.quasium.magic.page.form;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.magic.page.PageForms;

public final class PageFormRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeVeilEntity>> SMOKE_VEIL =
            ENTITY_TYPES.register("smoke_veil", () -> EntityType.Builder.<SmokeVeilEntity>of(SmokeVeilEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .noSave()
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build("smoke_veil"));

    private PageFormRegistry() {
    }

    public static void register(IEventBus eventBus, ModContainer container) {
        ENTITY_TYPES.register(eventBus);
        container.registerConfig(ModConfig.Type.COMMON, FormConfig.SPEC, "quasium-forms.toml");
        PageForms.register(SpellForm.WALL, (ctx, action) -> {
            if (ctx.caster() != null) ctx.level().addFreshEntity(SmokeVeilEntity.raise(ctx, ctx.caster()));
        });
    }
}
