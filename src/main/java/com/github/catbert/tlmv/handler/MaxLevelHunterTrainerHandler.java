package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Field;

/**
 * 允许满级（14级）猎人玩家与HunterTrainerEntity交互并打开训练GUI。
 * Vampirism默认阻止满级玩家打开GUI（因为没有下一级训练需求）。
 */
@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class MaxLevelHunterTrainerHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof de.teamlapen.vampirism.entity.hunter.HunterTrainerEntity trainer)) return;

        Player player = event.getEntity();
        if (player.isShiftKeyDown()) return;

        // Check player hunter level via reflection
        int lvl = getHunterLevel(player);
        if (lvl < 14) return; // Only handle max level, let Vampirism handle others

        if (trainer.level().isClientSide()) return;

        // Check if trainer already has a trainee
        if (getTrainee(trainer) != null) {
            player.sendSystemMessage(Component.translatable("text.vampirism.i_am_busy_right_now"));
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        // Open HunterTrainerMenu (same as Vampirism does for lower levels)
        try {
            Class<?> menuClass = Class.forName("de.teamlapen.vampirism.inventory.HunterTrainerMenu");
            player.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> {
                    try {
                        return (net.minecraft.world.inventory.AbstractContainerMenu) menuClass
                            .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, trainer.getClass())
                            .newInstance(containerId, inv, trainer);
                    } catch (Exception e) {
                        TLMVMain.LOGGER.error("[MaxLevelHunterTrainer] Failed to create HunterTrainerMenu", e);
                        return null;
                    }
                },
                trainer.getDisplayName()
            ));
            setTrainee(trainer, player);
            trainer.getNavigation().stop();
        } catch (Exception e) {
            TLMVMain.LOGGER.error("[MaxLevelHunterTrainer] Failed to open trainer GUI", e);
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static int getHunterLevel(Player player) {
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

    private static Player getTrainee(Object trainer) {
        try {
            Field f = trainer.getClass().getDeclaredField("trainee");
            f.setAccessible(true);
            return (Player) f.get(trainer);
        } catch (Exception e) { return null; }
    }

    private static void setTrainee(Object trainer, Player player) {
        try {
            Field f = trainer.getClass().getDeclaredField("trainee");
            f.setAccessible(true);
            f.set(trainer, player);
        } catch (Exception ignored) {}
    }
}
