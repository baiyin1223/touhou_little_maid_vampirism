package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = TLMVMain.MOD_ID)
public class MaidGuiOverlayHandler {

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractMaidContainerGui<?> gui)) {
            return;
        }

        EntityMaid maid = gui.getMaid();
        if (maid == null) {
            return;
        }

        var capOpt = ModCapabilities.getVampireMaid(maid).resolve();
        if (capOpt.isEmpty() || !capOpt.get().isVampire()) {
            return;
        }

        int level = capOpt.get().getVampireLevel();
        Component displayText = Component.translatable("gui.touhou_little_maid_vampirism.vampire_rank." + level);

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        int leftPos = gui.getGuiLeft();
        int topPos = gui.getGuiTop();

        int textX = leftPos + 28;
        int textY = topPos + 23;

        graphics.drawCenteredString(font, displayText, textX, textY, 0xFFFFFF);
    }
}
