package com.arcane.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Рисует костюм Саира и красную кожу Эйро поверх модели игрока.
 * Это не предмет брони, поэтому снять его нельзя - он существует только пока включена форма.
 */
public class FormFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier SAIR_TEX = new Identifier("arcane", "textures/entity/sair_costume.png");
    private static final Identifier EIRO_TEX = new Identifier("arcane", "textures/entity/eiro_overlay.png");

    private final PlayerEntityModel<AbstractClientPlayerEntity> sairModel;
    private final PlayerEntityModel<AbstractClientPlayerEntity> eiroModel;

    public FormFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context,
                               EntityModelLoader loader) {
        super(context);
        ModelPart sairRoot = loader.getModelPart(ArcaneClient.SAIR_LAYER);
        ModelPart eiroRoot = loader.getModelPart(ArcaneClient.EIRO_LAYER);
        this.sairModel = new PlayerEntityModel<>(sairRoot, false);
        this.eiroModel = new PlayerEntityModel<>(eiroRoot, false);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        int form = ClientForms.get(player.getUuid());
        if (form == 0 || player.isInvisible()) return;

        boolean sair = form == 1;
        PlayerEntityModel<AbstractClientPlayerEntity> m = sair ? sairModel : eiroModel;
        PlayerEntityModel<AbstractClientPlayerEntity> base = this.getContextModel();

        // повторяем позу основной модели игрока
        base.copyStateTo(m);
        m.sneaking = base.sneaking;
        m.leftArmPose = base.leftArmPose;
        m.rightArmPose = base.rightArmPose;
        m.animateModel(player, limbAngle, limbDistance, tickDelta);
        m.setAngles(player, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        // какие части показывать
        m.setVisible(false);
        m.body.visible = true;
        m.leftArm.visible = true;
        m.rightArm.visible = true;
        m.leftLeg.visible = true;
        m.rightLeg.visible = true;
        if (sair) m.hat.visible = true;   // у Саира голову закрывает плетёная шляпа
        else m.head.visible = true;       // у Эйро красится лицо

        RenderLayer layer = sair ? RenderLayer.getEntityCutoutNoCull(SAIR_TEX)
                                 : RenderLayer.getEntityTranslucent(EIRO_TEX);
        VertexConsumer vc = vertexConsumers.getBuffer(layer);
        m.render(matrices, vc, light, LivingEntityRenderer.getOverlay(player, 0.0f), 1.0f, 1.0f, 1.0f, 1.0f);
    }
}
