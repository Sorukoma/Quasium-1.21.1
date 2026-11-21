package net.sorukoma.quasium.client.renderer.entity;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

final class GolemDance {
    private static final float TEMPO = 0.45f;
    private static final float BOUNCE = 1.5f;
    private static final float SWAY = 0.25f;
    private static final float WAVE = 0.2f;
    private static final float RIGHT_ARM_UP = 2.670354f;
    private static final float LEFT_ARM_UP = -2.3561945f;

    private GolemDance() {
    }

    static void apply(float ageInTicks, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
        float beat = ageInTicks * TEMPO;
        float bounce = Math.abs(Mth.sin(beat)) * BOUNCE;
        head.y -= bounce;
        body.y -= bounce;
        rightArm.y -= bounce;
        leftArm.y -= bounce;
        head.zRot = Mth.sin(beat) * SWAY;
        rightArm.xRot = 0f;
        leftArm.xRot = 0f;
        rightArm.zRot = RIGHT_ARM_UP + Mth.cos(beat) * WAVE;
        leftArm.zRot = LEFT_ARM_UP - Mth.cos(beat) * WAVE;
    }
}
