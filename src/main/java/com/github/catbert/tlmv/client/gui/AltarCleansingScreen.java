package com.github.catbert.tlmv.client.gui;

import com.github.catbert.tlmv.network.ClearMaidVampirePacket;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.network.ServerboundSimpleInputEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class AltarCleansingScreen extends Screen {
    private final BlockPos altarPos;

    public AltarCleansingScreen(BlockPos pos) {
        super(Component.translatable("gui.touhou_little_maid_vampirism.altar_cleansing.title"));
        this.altarPos = pos;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int buttonWidth = 150;
        int gap = 4;

        // Row 1: Revert player level + Cancel (side by side)
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.touhou_little_maid_vampirism.altar_cleansing.revert_player"),
                btn -> {
                    VampirismMod.proxy.sendToServer(new ServerboundSimpleInputEvent(
                            ServerboundSimpleInputEvent.Event.REVERT_BACK));
                    this.onClose();
                }
        ).bounds(centerX - buttonWidth - gap / 2, this.height / 2 + 10, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                btn -> this.onClose()
        ).bounds(centerX + gap / 2, this.height / 2 + 10, buttonWidth, 20).build());

        // Row 2: Clear maid level (centered, below the first row)
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.touhou_little_maid_vampirism.clear_maid_level"),
                btn -> {
                    PacketDistributor.sendToServer(new ClearMaidVampirePacket(altarPos));
                    this.onClose();
                }
        ).bounds(centerX - buttonWidth / 2, this.height / 2 + 36, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.touhou_little_maid_vampirism.altar_cleansing.desc"),
                this.width / 2, this.height / 2 - 20, 0xAAAAAA);
    }
}
