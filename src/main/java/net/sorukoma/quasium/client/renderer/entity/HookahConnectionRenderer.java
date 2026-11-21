package net.sorukoma.quasium.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import org.joml.Matrix4f;

import java.util.UUID;

public class HookahConnectionRenderer extends EntityRenderer<HookahConnectionEntity> {
    // Для невидимой сущности текстура не нужна, но базовый класс требует ResourceLocation.
    // Можно использовать любую заглушку или текстуру, если она будет использоваться для отладки.
    private static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/hookah_connection.png");

    public HookahConnectionRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public ResourceLocation getTextureLocation(HookahConnectionEntity pEntity) {
        return DUMMY_TEXTURE;
    }

    @Override
    public void render(HookahConnectionEntity pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);

        UUID smoker = pEntity.getAttachedPlayerUUID();
        Player player = smoker == null ? null : pEntity.level().getPlayerByUUID(smoker);
        if (player == null || pEntity.getHookahBlockPos() == null) {
            return;
        }

        int playerLight = this.entityRenderDispatcher.getRenderer(player).getPackedLightCoords(player, pPartialTick);
        int hookahLight = this.getPackedLightCoords(pEntity, pPartialTick);
        renderHose(pPoseStack, pBuffer, player, Vec3.atCenterOf(pEntity.getHookahBlockPos()), pPartialTick, hookahLight, playerLight);
    }

    public static void renderHose(PoseStack pPoseStack, MultiBufferSource pBuffer, Player player, Vec3 hookahCenter,
                                  float pPartialTick, int hookahLight, int playerLight) {
        renderHose(pPoseStack, pBuffer, getPlayerHandPos(player, pPartialTick), hookahCenter, hookahLight, playerLight);
    }

    public static void renderHose(PoseStack pPoseStack, MultiBufferSource pBuffer, Vec3 hand, Vec3 hookahCenter,
                                  int hookahLight, int handLight) {
        pPoseStack.pushPose();

        // Поскольку сущность HookahConnectionEntity находится в центре блока кальяна,
        // нам нужно преобразовать координаты игрока относительно этой сущности.
        Vec3 transformedPlayerHandPos = hand.subtract(hookahCenter);

        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.leash());
        Matrix4f matrix4f = pPoseStack.last().pose();

        // Цвет линии (черный, как у лески)
        int r = 0;
        int g = 0;
        int b = 0;
        int a = 255;

        // Параметры для отрисовки сегментированной линии, аналогично RenderType.leash()
        float diffX = (float) transformedPlayerHandPos.x;
        float diffY = (float) transformedPlayerHandPos.y;
        float diffZ = (float) transformedPlayerHandPos.z;

        float f3 = 0.025F; // Толщина линии
        float f4 = Mth.invSqrt(diffX * diffX + diffZ * diffZ) * f3 / 2.0F;
        float f5 = diffZ * f4; // Смещение по X для толщины
        float f6 = diffX * f4; // Смещение по Z для толщины

        // Отрисовываем 24 сегмента линии
        for (int i1 = 0; i1 <= 24; i1++) {
            float f = (float)i1 / 24.0F;
            int interpolatedLight = (int)Mth.lerp(f, (float)hookahLight, (float)handLight);

            // Альтернативный цвет для сегментов (для имитации текстуры или просто для визуального эффекта)
            float segmentColorFactor = i1 % 2 == 0 ? 0.7F : 1.0F;
            float currentR = r * segmentColorFactor;
            float currentG = g * segmentColorFactor;
            float currentB = b * segmentColorFactor;

            // Расчет текущей точки на линии с учетом небольшой кривизны по Y (как у лески)
            float currentX = diffX * f;
            float currentY = diffY > 0.0F ? diffY * f * f : diffY - diffY * (1.0F - f) * (1.0F - f);
            float currentZ = diffZ * f;

            // Добавляем две вершины для каждого сегмента, чтобы создать толщину линии
            vertexconsumer.addVertex(matrix4f, currentX - f5, currentY + f3, currentZ + f6)
                          .setColor(currentR, currentG, currentB, a)
                          .setLight(interpolatedLight);
            vertexconsumer.addVertex(matrix4f, currentX + f5, currentY + f3 - f3, currentZ - f6)
                          .setColor(currentR, currentG, currentB, a)
                          .setLight(interpolatedLight);
        }

        pPoseStack.popPose();
    }

    // Вспомогательный метод для получения позиции руки игрока
    private static Vec3 getPlayerHandPos(Player player, float partialTicks) {
        // Это приближенная позиция. Для более точной позиции руки может потребоваться
        // доступ к модели игрока или более сложные вычисления.
        // Для удочки используется getLeashOffset, который специфичен для привязанных сущностей.
        // Для игрока, держащего предмет, мы можем использовать позицию глаз и смещение.

        Vec3 eyePos = player.getEyePosition(partialTicks);

        // Смещения, чтобы имитировать позицию руки. Эти значения могут потребовать настройки.
        // Для правши рука обычно находится справа и немного впереди/внизу от глаз.
        float yaw = player.getViewYRot(partialTicks) * ((float)Math.PI / 180F);
        float pitch = player.getViewXRot(partialTicks) * ((float)Math.PI / 180F);

        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        float sinPitch = Mth.sin(pitch);
        float cosPitch = Mth.cos(pitch);

        // Эти смещения взяты из логики рендеринга удочки или других предметов в руке.
        // Они могут быть не идеальными и требуют тестирования.
        double offsetX = -0.3 * sinYaw * cosPitch; // Вправо/влево
        double offsetY = -0.5 + (0.3 * sinPitch); // Вниз
        double offsetZ = 0.3 * cosYaw * cosPitch; // Вперед

        return eyePos.add(offsetX, offsetY, offsetZ);
    }
}