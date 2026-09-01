package net.sorukoma.quasium.item;

import net.minecraft.world.item.Item;
import net.sorukoma.quasium.hookah.HookahShaftVariant;

/**
 * Шахта модульного кальяна. Закрепляется на установленной колбе через shift+ПКМ.
 * Вариант (цвет) определяет, какая шахта рендерится поверх колбы.
 */
public class HookahShaftItem extends Item {
    private final HookahShaftVariant variant;

    public HookahShaftItem(HookahShaftVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public HookahShaftVariant getVariant() {
        return variant;
    }
}
