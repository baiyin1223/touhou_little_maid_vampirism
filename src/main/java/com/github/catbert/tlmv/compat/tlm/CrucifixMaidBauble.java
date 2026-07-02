package com.github.catbert.tlmv.compat.tlm;

import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CrucifixMaidBauble implements IMaidBauble {

    private static final int CYCLE_TOTAL = 35;
    private static final int ACTIVE_DURATION = 15;
    private static final Map<UUID, Integer> cycleTimers = new HashMap<>();

    private final int range;
    private final int minHunterLevel;

    public CrucifixMaidBauble(int range, int minHunterLevel) {
        this.range = range;
        this.minHunterLevel = minHunterLevel;
    }

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        if (maid.level().isClientSide()) return;
        if (maid.tickCount % 4 != 0) return;

        var cap = ModCapabilities.getVampireMaid(maid).orElse(null);
        if (cap == null || !cap.isHunter() || cap.getHunterLevel() < minHunterLevel) return;

        UUID id = maid.getUUID();
        int timer = cycleTimers.getOrDefault(id, 0);
        timer = (timer + 1) % CYCLE_TOTAL;
        cycleTimers.put(id, timer);
        if (timer >= ACTIVE_DURATION) return;

        var box = maid.getBoundingBox().inflate(range);
        var entities = maid.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !(e instanceof Player) && affectsEntity(e) && e != maid);

        for (LivingEntity target : entities) {
            Vec3 away = target.position().subtract(maid.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) continue;
            double pushStrength = getPushStrength(determineEntityTier(target));
            Vec3 pushVec = away.normalize().scale(pushStrength);
            target.setPos(target.getX() + pushVec.x, target.getY(), target.getZ() + pushVec.z);
            target.setDeltaMovement(target.getDeltaMovement().add(pushVec.x, 0, pushVec.z));
        }
    }

    private double getPushStrength(int entityTier) {
        return switch (minHunterLevel) {
            case 1 -> entityTier > 1 ? 0.05 : 0.15;
            case 3 -> entityTier > 2 ? 0.05 : 0.20;
            default -> entityTier > 3 ? 0.10 : 0.25;
        };
    }

    private boolean affectsEntity(LivingEntity e) {
        return e.getType().is(net.minecraft.tags.EntityTypeTags.SKELETONS) || isVampire(e);
    }

    private boolean isVampire(LivingEntity e) {
        try {
            Class<?> helperClass = Class.forName("de.teamlapen.vampirism.util.Helper");
            return (boolean) helperClass.getMethod("isVampire", net.minecraft.world.entity.Entity.class).invoke(null, e);
        } catch (Exception ignored) { return false; }
    }

    private static int determineEntityTier(LivingEntity e) {
        try {
            if (e instanceof Player) {
                int level = getVampireLevelOf((Player) e);
                int tier = 1;
                if (level == 14) tier = 3;
                else if (level >= 8) tier = 2;
                if (hasRefinement((Player) e, "CRUCIFIX_RESISTANT")) tier++;
                return tier;
            }
            if (e.getClass().getName().contains("VampireBaronEntity")) return 3;
            if (e.getClass().getName().contains("AdvancedVampireEntity")) return 2;
        } catch (Exception ignored) {}
        return 1;
    }

    private static int getVampireLevelOf(Player p) {
        try {
            Class<?> attrClass = Class.forName("de.teamlapen.vampirism.entity.player.VampirismPlayerAttributes");
            var att = attrClass.getMethod("get", Player.class).invoke(null, p);
            return (int) att.getClass().getField("vampireLevel").get(att);
        } catch (Exception e) { return 0; }
    }

    private static boolean hasRefinement(Player p, String refName) {
        try {
            Class<?> vpClass = Class.forName("de.teamlapen.vampirism.entity.player.vampire.VampirePlayer");
            var vp = vpClass.getMethod("get", Player.class).invoke(null, p);
            var handler = vpClass.getMethod("getSkillHandler").invoke(vp);
            Class<?> modRefClass = Class.forName("de.teamlapen.vampirism.core.ModRefinements");
            var ref = modRefClass.getField(refName).get(null);
            return (boolean) handler.getClass().getMethod("isRefinementEquipped", ref.getClass().getInterfaces()[0])
                    .invoke(handler, ref);
        } catch (Exception e) { return false; }
    }
}
