package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.util.VampirismHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class HunterTargetHandler {

    private static Class<?> hunterBaseClass;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!VampirismHelper.isVampirismLoaded()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!isHunterEntity(mob)) {
            return;
        }

        mob.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(
            mob,
            EntityMaid.class,
            5,
            true,
            false,
            living -> {
                if (living instanceof EntityMaid maid) {
                    return ModCapabilities.getVampireMaid(maid)
                        .map(cap -> cap.isVampire())
                        .orElse(false);
                }
                return false;
            }
        ));
    }

    private static boolean isHunterEntity(Mob mob) {
        try {
            if (hunterBaseClass == null) {
                hunterBaseClass = Class.forName("de.teamlapen.vampirism.entity.hunter.HunterBaseEntity");
            }
            return hunterBaseClass.isInstance(mob);
        } catch (ClassNotFoundException e) {
            TLMVMain.LOGGER.warn("[TLMV] HunterBaseEntity class not found, falling back to class name check");
            String className = mob.getClass().getName();
            return className.startsWith("de.teamlapen.vampirism") && className.contains("hunter");
        }
    }
}
