package com.github.catbert.tlmv.client.gui;

import com.github.catbert.tlmv.network.RevertHunterMaidPacket;
import com.github.catbert.tlmv.network.TLMVNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.entity.Entity;

@OnlyIn(Dist.CLIENT)
public class RevertHunterScreen extends Screen {
    private final int maidId;

    public RevertHunterScreen(int maidId) {
        super(Component.translatable("gui.touhou_little_maid_vampirism.revert_hunter.title"));
        this.maidId = maidId;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int buttonWidth = 60;
        int gap = 6;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.yes"),
                btn -> {
                    TLMVNetwork.INSTANCE.sendToServer(new RevertHunterMaidPacket(maidId));
                    this.onClose();
                }
        ).bounds(centerX - buttonWidth - gap / 2, this.height / 2 + 10, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.no"),
                btn -> this.onClose()
        ).bounds(centerX + gap / 2, this.height / 2 + 10, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.touhou_little_maid_vampirism.revert_hunter.desc"),
                this.width / 2, this.height / 2 - 20, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
