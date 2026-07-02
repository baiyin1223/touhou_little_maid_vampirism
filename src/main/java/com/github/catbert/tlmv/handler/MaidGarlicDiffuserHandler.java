package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.EnumStrength;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class MaidGarlicDiffuserHandler {

    private static final int FUEL_DURATION = 120;
    private static final Set<UUID> activeMaids = new HashSet<>();
    private static final Map<UUID, int[]> registeredChunks = new HashMap<>();
    private static final Map<UUID, Integer> fuelTimers = new HashMap<>();
    private static final Map<UUID, String> diffuserTypes = new HashMap<>();

    private static final ResourceLocation GARLIC_NORMAL = new ResourceLocation("vampirism", "garlic_diffuser_normal");
    private static final ResourceLocation GARLIC_IMPROVED = new ResourceLocation("vampirism", "garlic_diffuser_improved");
    private static final ResourceLocation GARLIC_WEAK = new ResourceLocation("vampirism", "garlic_diffuser_weak");
    private static final ResourceLocation PURIFIED_GARLIC = new ResourceLocation("vampirism", "purified_garlic");

    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid)) return;
        if (maid.level().isClientSide()) return;

        var cap = ModCapabilities.getVampireMaid(maid).orElse(null);
        if (cap == null || !cap.isHunter()) {
            unregisterIfNeeded(maid);
            return;
        }

        if (maid.tickCount % 20 != 0) return;

        var inv = maid.getAvailableInv(true);
        String diffuserType = findDiffuser(inv);
        boolean hasGarlic = hasPurifiedGarlic(inv);

        if (diffuserType == null || !hasGarlic) {
            unregisterIfNeeded(maid);
            return;
        }

        UUID id = maid.getUUID();
        if (!activeMaids.contains(id)) {
            registerGarlic(maid, id, diffuserType);
            fuelTimers.put(id, FUEL_DURATION);
            diffuserTypes.put(id, diffuserType);
            activeMaids.add(id);
        } else {
            int fuel = fuelTimers.getOrDefault(id, 0);
            fuel--;
            if (fuel <= 0) {
                if (consumePurifiedGarlic(inv)) {
                    fuel = FUEL_DURATION;
                } else {
                    unregisterIfNeeded(maid);
                    return;
                }
            }
            fuelTimers.put(id, fuel);
            updateGarlicPosition(maid, id, diffuserType);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof EntityMaid maid) {
            unregisterIfNeeded(maid);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof EntityMaid maid) {
            unregisterIfNeeded(maid);
        }
    }

    private static void unregisterIfNeeded(EntityMaid maid) {
        UUID id = maid.getUUID();
        if (activeMaids.remove(id)) {
            unregisterGarlic(maid, id);
            fuelTimers.remove(id);
            diffuserTypes.remove(id);
            registeredChunks.remove(id);
        }
    }

    private static String findDiffuser(IItemHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            var stack = inv.getStackInSlot(i);
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key == null) continue;
            if (key.equals(GARLIC_NORMAL) || key.equals(GARLIC_IMPROVED) || key.equals(GARLIC_WEAK)) {
                return key.toString();
            }
        }
        return null;
    }

    private static boolean hasPurifiedGarlic(IItemHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(inv.getStackInSlot(i).getItem());
            if (PURIFIED_GARLIC.equals(key)) return true;
        }
        return false;
    }

    private static boolean consumePurifiedGarlic(IItemHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            var stack = inv.getStackInSlot(i);
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (PURIFIED_GARLIC.equals(key)) {
                inv.extractItem(i, 1, false);
                return true;
            }
        }
        return false;
    }

    private static void registerGarlic(EntityMaid maid, UUID id, String type) {
        VampirismAPI.getVampirismWorld(maid.level()).ifPresent(garlicHandler -> {
            int dist = getDist(type);
            int baseX = maid.blockPosition().getX() >> 4;
            int baseZ = maid.blockPosition().getZ() >> 4;
            EnumStrength strength = getStrength(type);

            ChunkPos[] chunks = new ChunkPos[(2 * dist + 1) * (2 * dist + 1)];
            int idx = 0;
            for (int x = -dist; x <= dist; x++)
                for (int z = -dist; z <= dist; z++)
                    chunks[idx++] = new ChunkPos(x + baseX, z + baseZ);
            int chunkId = garlicHandler.registerGarlicBlock(strength, chunks);
            registeredChunks.put(id, new int[]{chunkId});
        });
    }

    private static void updateGarlicPosition(EntityMaid maid, UUID id, String type) {
        unregisterGarlic(maid, id);
        registerGarlic(maid, id, type);
    }

    private static void unregisterGarlic(EntityMaid maid, UUID id) {
        int[] chunkId = registeredChunks.remove(id);
        if (chunkId == null) return;
        VampirismAPI.getVampirismWorld(maid.level()).ifPresent(garlicHandler -> {
            garlicHandler.removeGarlicBlock(chunkId[0]);
        });
    }

    private static int getDist(String type) {
        if (type.contains("improved")) return 3;
        if (type.contains("weak")) return 1;
        return 2;
    }

    private static EnumStrength getStrength(String type) {
        if (type.contains("weak")) return EnumStrength.WEAK;
        if (type.contains("improved")) return EnumStrength.MEDIUM;
        return EnumStrength.MEDIUM;
    }
}
