package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.client.gui.RevertHunterScreen;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = TLMVMain.MOD_ID)
public class RevertHunterHandler {

    private static final ResourceLocation SANGUINARE_INJECTION = ResourceLocation.fromNamespaceAndPath("vampirism", "injection_sanguinare");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof EntityMaid maid)) return;

        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        var held = player.getItemInHand(InteractionHand.MAIN_HAND);
        var injectionItem = BuiltInRegistries.ITEM.get(SANGUINARE_INJECTION);
        if (injectionItem == null || !held.is(injectionItem)) return;

        var cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
        if (cap == null) return;

        // Open confirmation screen
        Minecraft.getInstance().setScreen(new RevertHunterScreen(maid.getId()));
        event.setCanceled(true);
    }
}
