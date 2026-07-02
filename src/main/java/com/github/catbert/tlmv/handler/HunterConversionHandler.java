package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.level.HunterLevelManager;
import com.github.catbert.tlmv.network.SyncVampireMaidPacket;
import com.github.catbert.tlmv.network.TLMVNetwork;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class HunterConversionHandler {

    private static final ResourceLocation GARLIC_INJECTION = new ResourceLocation("vampirism", "injection_garlic");

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof EntityMaid maid)) return;

        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!player.isShiftKeyDown()) return;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        var garlicItem = ForgeRegistries.ITEMS.getValue(GARLIC_INJECTION);
        if (garlicItem == null || !held.is(garlicItem)) return;

        var cap = ModCapabilities.getVampireMaid(maid).orElse(null);
        if (cap == null) return;

        if (cap.isVampire()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.touhou_little_maid_vampirism.cannot_convert_vampire"), true);
            return;
        }
        if (cap.isHunter()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.touhou_little_maid_vampirism.already_hunter"), true);
            return;
        }

        cap.setHunter(true);
        cap.setHunterLevel(1);
        cap.setPoisonousBlood(true);
        // Write to Vampirism ExtendedCreature (actual poison mechanics)
        VampirismAPI.getExtendedCreatureVampirism(maid).ifPresent(ext -> ext.setPoisonousBlood(true));
        HunterLevelManager.applyLevel(maid, 1);

        // Visual effect
        if (maid.level() instanceof ServerLevel serverLevel) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt != null) {
                bolt.moveTo(maid.getX(), maid.getY(), maid.getZ());
                bolt.setVisualOnly(true);
                serverLevel.addFreshEntity(bolt);
            }
        }

        // Sync
        TLMVNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new SyncVampireMaidPacket(maid.getId(), cap.isVampire(), cap.getVampireLevel(), cap.isHunter(), cap.getHunterLevel()));

        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.touhou_little_maid_vampirism.maid_became_hunter"), true);

        // Consume item
        if (!player.isCreative()) {
            held.shrink(1);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
