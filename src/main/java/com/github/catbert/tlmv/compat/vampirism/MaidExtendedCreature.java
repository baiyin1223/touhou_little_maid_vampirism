package com.github.catbert.tlmv.compat.vampirism;

import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
import de.teamlapen.vampirism.entity.ExtendedCreature;
import net.minecraft.world.entity.PathfinderMob;

/**
 * 自定义 ExtendedCreature，用于女仆实体。
 * 当女仆已经是吸血鬼时，阻止其他吸血鬼对其吸血。
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
                return false;
            }
        }
        return super.canBeBitten(biter);
    }
}
