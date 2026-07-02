package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class MaxLevelHunterTrainerHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof de.teamlapen.vampirism.entity.hunter.HunterTrainerEntity trainer)) return;

        Player player = event.getEntity();
        if (player.isShiftKeyDown()) return;

        int lvl = getHunterLevel(player);
        if (lvl < 14) return;

        if (trainer.level().isClientSide()) return;

        if (getTrainee(trainer) != null) {
            player.sendSystemMessage(Component.translatable("text.vampirism.i_am_busy_right_now"));
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        try {
            Class<?> menuClass = Class.forName("de.teamlapen.vampirism.inventory.HunterTrainerMenu");
            player.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> {
                    try {
                        return (net.minecraft.world.inventory.AbstractContainerMenu) menuClass
                            .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, trainer.getClass())
                            .newInstance(containerId, inv, trainer);
                    } catch (Exception e) { return null; }
                },
                trainer.getDisplayName()
            ));
            setTrainee(trainer, player);
            trainer.getNavigation().stop();
        } catch (Exception e) { return; }

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
