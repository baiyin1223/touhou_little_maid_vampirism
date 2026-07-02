package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.network.OpenMaidTrainingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * 在 Hunter Trainer GUI 右上角注入"训练女仆"按钮。
 * 需要玩家猎人等级 >= 4 且打开了 HunterTrainerScreen。
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = TLMVMain.MOD_ID)
public class HunterTrainerGuiHandler {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof de.teamlapen.vampirism.client.gui.screens.HunterTrainerScreen)) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int hunterLevel = getPlayerHunterLevel(player);
        if (hunterLevel < 4) return;

        int x = screen.width / 2 + 20;
        int y = screen.height / 2 - 75;
        event.addListener(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("gui.touhou_little_maid_vampirism.train_maid"),
                btn -> {
                    var trainerPos = getTrainerFromScreen(screen);
                    if (trainerPos != null) {
                        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new OpenMaidTrainingPacket(trainerPos));
                    }
                }
        ).pos(x, y).size(60, 20).build());
    }

    private static int getPlayerHunterLevel(Player player) {
        try {
            Class<?> fphClass = Class.forName("de.teamlapen.vampirism.entity.factions.FactionPlayerHandler");
            var inst = fphClass.getMethod("get", Player.class).invoke(null, player);
            Class<?> factionClass = Class.forName("de.teamlapen.vampirism.api.entity.factions.IPlayableFaction");
            Class<?> vrefClass = Class.forName("de.teamlapen.vampirism.api.VReference");
            Object hunterFaction = vrefClass.getField("HUNTER_FACTION").get(null);
            Object level = inst.getClass().getMethod("getCurrentLevel", factionClass).invoke(inst, hunterFaction);
            if (level instanceof Integer i) return i;
        } catch (Exception ignored) {}
        return 0;
    }

    private static net.minecraft.core.BlockPos getTrainerFromScreen(Screen screen) {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        var level = Minecraft.getInstance().level;
        if (level == null) return null;

        var box = player.getBoundingBox().inflate(6.0);
        var trainers = level.getEntitiesOfClass(
                de.teamlapen.vampirism.entity.hunter.HunterTrainerEntity.class,
                box, e -> e.isAlive());
        if (!trainers.isEmpty()) {
            return trainers.get(0).blockPosition();
        }
        return null;
    }
}
