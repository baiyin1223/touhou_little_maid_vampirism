package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.client.gui.AltarCleansingScreen;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TLMVMain.MOD_ID, value = Dist.CLIENT)
public class AltarCleansingHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide) return;

        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Check if block is vampirism:altar_cleansing
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!"vampirism".equals(blockId.getNamespace()) || !"altar_cleansing".equals(blockId.getPath())) {
            return;
        }

        // Check if player is a vampire
        FactionPlayerHandler fph = FactionPlayerHandler.get(player);
        if (fph.isInFaction(VReference.VAMPIRE_FACTION)) {
            // Cancel original event to prevent Vampirism from opening its screen
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            // Open our custom screen
            Minecraft.getInstance().setScreen(new AltarCleansingScreen(pos));
        }
    }
}
