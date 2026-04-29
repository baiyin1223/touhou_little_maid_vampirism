package com.github.catbert.tlmv.client.gui;

import com.github.catbert.tlmv.inventory.MaidAltarMenu;
import com.github.catbert.tlmv.network.StartRitualPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MaidAltarScreen extends AbstractContainerScreen<MaidAltarMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("vampirism", "textures/gui/container/altar_of_infusion.png");
    private static final ResourceLocation EMPTY_PURE_BLOOD = ResourceLocation.fromNamespaceAndPath("vampirism", "item/empty_pure_blood");
    private static final ResourceLocation EMPTY_HUMAN_HEART = ResourceLocation.fromNamespaceAndPath("vampirism", "item/empty_human_heart");
    private static final ResourceLocation EMPTY_VAMPIRE_BOOK = ResourceLocation.fromNamespaceAndPath("vampirism", "item/empty_vampire_book");

    private final CyclingSlotBackground pureBloodIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground humanHeartIcon = new CyclingSlotBackground(1);
    private final CyclingSlotBackground vampireBookIcon = new CyclingSlotBackground(2);

    public MaidAltarScreen(@NotNull MaidAltarMenu menu, @NotNull Inventory playerInventory, @NotNull Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.touhou_little_maid_vampirism.start_ritual"),
                this::onStartRitual
        ).bounds(this.leftPos + 113, this.topPos + 59, 57, 15).build());
    }

    private void onStartRitual(Button button) {
        if (this.menu.getBlockPos() != null) {
            PacketDistributor.sendToServer(new StartRitualPacket(this.menu.getBlockPos()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.pureBloodIcon.tick(List.of(EMPTY_PURE_BLOOD));
        this.humanHeartIcon.tick(List.of(EMPTY_HUMAN_HEART));
        this.vampireBookIcon.tick(List.of(EMPTY_VAMPIRE_BOOK));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
        this.pureBloodIcon.render(this.menu, graphics, partialTick, this.leftPos, this.topPos);
        this.humanHeartIcon.render(this.menu, graphics, partialTick, this.leftPos, this.topPos);
        this.vampireBookIcon.render(this.menu, graphics, partialTick, this.leftPos, this.topPos);
    }
}
