package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.client.gui.AltarCleansingScreen;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID, value = Dist.CLIENT)
public class AltarCleansingHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide) return;

        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Check if block is vampirism:altar_cleansing
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null || !"vampirism".equals(blockId.getNamespace()) || !"altar_cleansing".equals(blockId.getPath())) {
            return;
        }

        // Check if player is a vampire
        FactionPlayerHandler.getOpt(player).ifPresent(handler -> {
            if (handler.isInFaction(VReference.VAMPIRE_FACTION)) {
                // Cancel original event to prevent Vampirism from opening its screen
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                // Open our custom screen
                Minecraft.getInstance().setScreen(new AltarCleansingScreen(pos));
            }
        });
    }
}
