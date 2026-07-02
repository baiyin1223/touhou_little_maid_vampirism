package com.github.catbert.tlmv.client.gui;

import com.github.catbert.tlmv.inventory.MaidTrainingMenu;
import com.github.catbert.tlmv.network.PromoteMaidPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class MaidTrainingScreen extends AbstractContainerScreen<MaidTrainingMenu> {

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("touhou_little_maid_vampirism", "textures/gui/maid_training.png");

    // Ghost items for each slot
    private static final ItemStack GHOST_IRON = new ItemStack(Items.IRON_INGOT);
    private static final ItemStack GHOST_GOLD = new ItemStack(Items.GOLD_INGOT);
    private static final ItemStack GHOST_BOOK;
    private static final ItemStack GHOST_SOUL;
    private static final ItemStack GHOST_BLOOD;

    static {
        var book = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("vampirism", "vampire_book"));
        GHOST_BOOK = book != null ? new ItemStack(book) : ItemStack.EMPTY;
        var soul = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("vampirism", "soul_orb_vampire"));
        GHOST_SOUL = soul != null ? new ItemStack(soul) : ItemStack.EMPTY;
        var blood = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("vampirism", "pure_blood_1"));
        GHOST_BLOOD = blood != null ? new ItemStack(blood) : ItemStack.EMPTY;
    }

    // Slot positions (matching MaidTrainingMenu)
    private static final int[][] SLOT_XY = {{27,26},{57,26},{86,26},{42,50},{71,50}};
    private static final ItemStack[] GHOST_STACKS = {GHOST_IRON, GHOST_GOLD, GHOST_BOOK, GHOST_SOUL, GHOST_BLOOD};

    private final BlockPos trainerPos;

    public MaidTrainingScreen(MaidTrainingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
        // Extract trainerPos from menu (stored in BlockPos field)
        this.trainerPos = extractTrainerPos(menu);
    }

    private static BlockPos extractTrainerPos(MaidTrainingMenu menu) {
        try {
            var field = MaidTrainingMenu.class.getDeclaredField("trainerPos");
            field.setAccessible(true);
            return (BlockPos) field.get(menu);
        } catch (Exception e) { return BlockPos.ZERO; }
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.touhou_little_maid_vampirism.promote_maid"),
                btn -> {
                    PacketDistributor.sendToServer(new PromoteMaidPacket(trainerPos));
                    this.onClose();
                }
        ).pos(x + 104, y + 55).size(60, 20).build());
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Render ghost items in empty slots
        for (int i = 0; i < 5; i++) {
            if (!GHOST_STACKS[i].isEmpty() && !this.menu.slots.get(i).hasItem()) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.25F);
                gui.renderItem(GHOST_STACKS[i], leftPos + SLOT_XY[i][0], topPos + SLOT_XY[i][1]);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}

