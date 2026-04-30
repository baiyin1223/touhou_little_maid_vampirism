package com.github.catbert.tlmv.compat.vampirism;

import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
import de.teamlapen.vampirism.entity.ExtendedCreature;
import net.minecraft.world.entity.PathfinderMob;

/**
 * 自定义 ExtendedCreature，用于女仆实体。
 * 当女仆已经是吸血鬼时，阻止 NPC 吸血鬼对其吸血，但允许玩家吸血鬼吸血。
 */
public class MaidExtendedCreature extends ExtendedCreature {

    public MaidExtendedCreature(PathfinderMob entity) {
        super(entity);
    }

    @Override
    public boolean canBeBitten(IVampire biter) {
        if (getEntity() instanceof EntityMaid maid) {
            boolean isVampire = ModCapabilities.getVampireMaid(maid)
                    .map(cap -> cap.isVampire())
                    .orElse(false);
            if (isVampire) {
                // 允许玩家吸血鬼对吸血鬼女仆吸血（受 blood > 0 约束）
                // 阻止 NPC 吸血鬼对吸血鬼女仆吸血
                if (biter instanceof IVampirePlayer) {
                    return super.canBeBitten(biter); // 默认检查 getBlood() > 0
                }
                return false;
            }
        }
        return super.canBeBitten(biter);
    }
}
