package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

        VampireMaidCapability cap = capOpt.get();
        int level = cap.getVampireLevel();
        String displayText = VampireMaidCapability.getVampireDisplayPrefix(level);
        if (displayText.endsWith("-")) {
            displayText = displayText.substring(0, displayText.length() - 1);
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        int leftPos = gui.getGuiLeft();
        int topPos = gui.getGuiTop();

        int textX = leftPos + 28;
        int textY = topPos + 23;

        graphics.drawCenteredString(font, displayText, textX, textY, 0xFFFFFF);
    }
}
