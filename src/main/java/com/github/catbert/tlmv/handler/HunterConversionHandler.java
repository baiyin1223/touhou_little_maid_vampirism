package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.util.VampirismHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 猎人女仆转化处理器。
 * 玩家 shift+右键使用 vampirism:injection_garlic 对女仆进行猎人转化。
 */
@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class HunterConversionHandler {

    private static final String INJECTION_GARLIC_ID = "vampirism:injection_garlic";

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;

        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());

        // Check for garlic injection item
        if (!isGarlicInjection(stack)) return;
        // Must be sneaking
        if (!player.isShiftKeyDown()) return;

        if (!(event.getTarget() instanceof LivingEntity target)) return;

        // Check if target is a maid
        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (entityKey == null || !"touhou_little_maid".equals(entityKey.getNamespace())) return;

        VampireMaidCapability cap = target.getData(ModAttachments.VAMPIRE_MAID.get());

        // Cannot convert if already vampire or hunter
        if (cap.isVampire()) {
            player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.cannot_convert_vampire"));
            return;
        }
        if (cap.isHunter()) {
            player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.already_hunter"));
            return;
        }

        // Perform conversion
        cap.setHunter(true);
        cap.setHunterLevel(1);

        // Spawn visual lightning (no damage, no fire)
        if (target.level() instanceof ServerLevel serverLevel) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true);
                serverLevel.addFreshEntity(bolt);
            }
        }

        // Set poisonous blood via Vampirism
        VampirismHelper.setPoisonousBlood(target, true);

        // Consume item
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Notify
        player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.maid_became_hunter"));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean isGarlicInjection(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return INJECTION_GARLIC_ID.equals(key != null ? key.toString() : null);
    }
}
