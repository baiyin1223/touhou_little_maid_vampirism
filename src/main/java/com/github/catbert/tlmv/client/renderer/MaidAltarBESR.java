package com.github.catbert.tlmv.client.renderer;

import com.github.catbert.tlmv.blockentity.MaidAltarBlockEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MaidAltarBESR implements BlockEntityRenderer<MaidAltarBlockEntity> {

    private final ResourceLocation enderDragonCrystalBeamTextures = ResourceLocation.fromNamespaceAndPath("touhou_little_maid_vampirism", "textures/entity/infusion_beam.png");
    private final ResourceLocation beaconBeamTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/beacon_beam.png");

    public MaidAltarBESR(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull MaidAltarBlockEntity be, float partialTicks, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource iRenderTypeBuffer, int combinedLight, int combinedOverlay) {
        MaidAltarBlockEntity.Phase phase = be.getCurrentPhase();
        BlockPos[] tips = be.getTips();

        if (phase == MaidAltarBlockEntity.Phase.BEAM1 && tips != null) {
            float cX = be.getBlockPos().getX() + 0.5f;
            float cY = be.getBlockPos().getY() + 3f;
            float cZ = be.getBlockPos().getZ() + 0.5f;
            matrixStack.pushPose();
            matrixStack.translate(0.5, 3, 0.5);
            for (BlockPos tip : tips) {
                this.renderBeam(matrixStack, iRenderTypeBuffer, -(be.getRunningTick() + partialTicks),
                        tip.getX() + 0.5f - cX, tip.getY() + 0.5f - cY, tip.getZ() + 0.5f - cZ, combinedLight, false);
            }
            matrixStack.popPose();
        }

        if (phase == MaidAltarBlockEntity.Phase.BEAM2 && tips != null) {
            EntityMaid maid = be.getTargetMaid();
            if (maid != null) {
                for (BlockPos tip : tips) {
                    matrixStack.pushPose();
                    matrixStack.translate(tip.getX() - be.getBlockPos().getX() + 0.5, tip.getY() - be.getBlockPos().getY() + 0.5, tip.getZ() - be.getBlockPos().getZ() + 0.5);
                    this.renderBeam(matrixStack, iRenderTypeBuffer, -(be.getRunningTick() + partialTicks),
                            (float) (maid.getX() - (tip.getX() + 0.5)),
                            (float) (maid.getY() + 1.2f - (tip.getY() + 0.5)),
                            (float) (maid.getZ() - (tip.getZ() + 0.5)),
                            combinedLight, true);
                    matrixStack.popPose();
                }
            }
        }
    }

    private void renderBeam(@NotNull PoseStack matrixStack, @NotNull MultiBufferSource renderTypeBuffer, float partialTicks, float dx, float dy, float dz, int packedLight, boolean beacon) {
        float distFlat = Mth.sqrt(dx * dx + dz * dz);
        float dist = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        matrixStack.pushPose();
        matrixStack.mulPose(Axis.YP.rotation(((float) (-Math.atan2(dz, dx)) - ((float) Math.PI / 2F))));
        matrixStack.mulPose(Axis.XP.rotation((float) (-Math.atan2(distFlat, dy)) - ((float) Math.PI / 2F)));
        VertexConsumer ivertexbuilder = renderTypeBuffer.getBuffer(RenderType.entitySmoothCutout(beacon ? beaconBeamTexture : enderDragonCrystalBeamTextures));
        float f2 = partialTicks * 0.05f;
        float f3 = dist / 32.0F + partialTicks * 0.05f;
        float f4 = 0.0F;
        float f5 = 0.2F;
        float f6 = 0.0F;
        PoseStack.Pose matrixstack$entry = matrixStack.last();
        Matrix4f matrix4f = matrixstack$entry.pose();

        for (int j = 1; j <= 8; ++j) {
            float f7 = Mth.sin((float) j * ((float) Math.PI * 2F) / 8.0F) * 0.2F;
            float f8 = Mth.cos((float) j * ((float) Math.PI * 2F) / 8.0F) * 0.2F;
            float f9 = (float) j / 8.0F;
            ivertexbuilder.addVertex(matrix4f, f4, f5, 0.0F).setColor(75, 0, 0, 255).setUv(f6, f2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(matrixstack$entry, 0.0F, -1.0F, 0.0F);
            ivertexbuilder.addVertex(matrix4f, f4 * 0.5f, f5 * 0.5f, dist).setColor(255, 0, 0, 255).setUv(f6, f3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(matrixstack$entry, 0.0F, -1.0F, 0.0F);
            ivertexbuilder.addVertex(matrix4f, f7 * 0.5f, f8 * 0.5f, dist).setColor(255, 0, 0, 255).setUv(f9, f3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(matrixstack$entry, 0.0F, -1.0F, 0.0F);
            ivertexbuilder.addVertex(matrix4f, f7, f8, 0.0F).setColor(75, 0, 0, 255).setUv(f9, f2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(matrixstack$entry, 0.0F, -1.0F, 0.0F);
            f4 = f7;
            f5 = f8;
            f6 = f9;
        }

        matrixStack.popPose();
    }

}
