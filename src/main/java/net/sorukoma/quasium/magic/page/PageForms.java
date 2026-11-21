package net.sorukoma.quasium.magic.page;

import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.magic.SpellAction;
import net.sorukoma.quasium.magic.SpellForm;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class PageForms {
    private static final Map<SpellForm, BiConsumer<CastContext, SpellAction>> LAUNCHERS = new EnumMap<>(SpellForm.class);

    static {
        register(SpellForm.BOLT, (ctx, action) -> ctx.level().addFreshEntity(SmokeBoltEntity.launch(ctx)));
        register(SpellForm.SELF, (ctx, action) -> {
            if (action instanceof PageAction page) {
                page.self(ctx);
            } else if (ctx.caster() != null) {
                action.hitEntity(ctx, ctx.caster());
            }
        });
    }

    private PageForms() {
    }

    public static void register(SpellForm form, BiConsumer<CastContext, SpellAction> launcher) {
        LAUNCHERS.put(form, launcher);
    }

    public static boolean launch(CastContext ctx, SpellAction action) {
        BiConsumer<CastContext, SpellAction> launcher = LAUNCHERS.get(ctx.spell().form());
        if (launcher == null) return false;
        launcher.accept(ctx, action);
        return true;
    }
}
