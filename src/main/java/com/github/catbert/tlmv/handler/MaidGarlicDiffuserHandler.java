package com.github.catbert.tlmv.handler;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.*;

/**
 * 猎人女仆移动大蒜扩散器。检测背包中 garlic_diffuser_* + purified_garlic，
 * 以女仆实体为中心注册/注销 Vampirism 大蒜区块。
 * 每 2 分钟消耗 1 个 purified_garlic。
 */
@EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class MaidGarlicDiffuserHandler {

    private static final int FUEL_DURATION = 120; // every 1s tick => 2 minutes per garlic
    private static final Set<UUID> activeMaids = new HashSet<>();
    private static final Map<UUID, int[]> registeredChunks = new HashMap<>();
    private static final Map<UUID, Integer> fuelTimers = new HashMap<>();
    private static final Map<UUID, String> diffuserTypes = new HashMap<>();

    private static final ResourceLocation GARLIC_NORMAL = ResourceLocation.fromNamespaceAndPath("vampirism", "garlic_diffuser_normal");
    private static final ResourceLocation GARLIC_IMPROVED = ResourceLocation.fromNamespaceAndPath("vampirism", "garlic_diffuser_improved");
    private static final ResourceLocation GARLIC_WEAK = ResourceLocation.fromNamespaceAndPath("vampirism", "garlic_diffuser_weak");
    private static final ResourceLocation PURIFIED_GARLIC = ResourceLocation.fromNamespaceAndPath("vampirism", "purified_garlic");

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof EntityMaid maid)) return;
        if (maid.level().isClientSide()) return;

        var cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
        if (!cap.isHunter()) {
            unregisterIfNeeded(maid);
            return;
        }

        if (maid.tickCount % 20 != 0) return; // every 1 second

        var inv = maid.getAvailableInv(true);
        String diffuserType = findDiffuser(inv);
        boolean hasGarlic = hasPurifiedGarlic(inv);

        if (diffuserType == null || !hasGarlic) {
            unregisterIfNeeded(maid);
            return;
        }

        UUID id = maid.getUUID();
        if (!activeMaids.contains(id)) {
            // Register garlic effect
            registerGarlic(maid, id, diffuserType);
            fuelTimers.put(id, FUEL_DURATION);
            diffuserTypes.put(id, diffuserType);
            activeMaids.add(id);
        } else {
            // Tick fuel
            int fuel = fuelTimers.getOrDefault(id, 0);
            fuel--;
            if (fuel <= 0) {
                // Consume one purified_garlic
                if (consumePurifiedGarlic(inv)) {
                    fuel = FUEL_DURATION;
                } else {
                    unregisterIfNeeded(maid);
                    return;
                }
            }
            fuelTimers.put(id, fuel);
            // Update garlic position (maid moved)
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

    private static String findDiffuser(net.neoforged.neoforge.items.IItemHandler inv) {
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

    private static boolean hasPurifiedGarlic(net.neoforged.neoforge.items.IItemHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(inv.getStackInSlot(i).getItem());
            if (PURIFIED_GARLIC.equals(key)) return true;
        }
        return false;
    }

    private static boolean consumePurifiedGarlic(net.neoforged.neoforge.items.IItemHandler inv) {
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
        try {
            Object garlicHandler = Class.forName("de.teamlapen.vampirism.api.VampirismAPI")
                    .getMethod("garlicHandler", net.minecraft.world.level.Level.class)
                    .invoke(null, maid.level());

            int dist = getDist(type);
            int baseX = maid.blockPosition().getX() >> 4;
            int baseZ = maid.blockPosition().getZ() >> 4;
            Object strength = getStrength(type);

            int[] chunkId = new int[1];
            // Use reflection to register
            var registerMethod = garlicHandler.getClass().getMethod("registerGarlicBlock",
                    Class.forName("de.teamlapen.vampirism.api.EnumStrength"),
                    net.minecraft.world.level.ChunkPos[].class);
            net.minecraft.world.level.ChunkPos[] chunks = new net.minecraft.world.level.ChunkPos[(2 * dist + 1) * (2 * dist + 1)];
            int idx = 0;
            for (int x = -dist; x <= dist; x++)
                for (int z = -dist; z <= dist; z++)
                    chunks[idx++] = new net.minecraft.world.level.ChunkPos(x + baseX, z + baseZ);
            chunkId[0] = (int) registerMethod.invoke(garlicHandler, strength, chunks);
            registeredChunks.put(id, chunkId);
        } catch (Exception e) {
            TLMVMain.LOGGER.warn("[MaidGarlicDiffuser] Failed to register garlic: {}", e.getMessage());
        }
    }

    private static void updateGarlicPosition(EntityMaid maid, UUID id, String type) {
        // Re-register at new position
        unregisterGarlic(maid, id);
        registerGarlic(maid, id, type);
    }

    private static void unregisterGarlic(EntityMaid maid, UUID id) {
        int[] chunkId = registeredChunks.remove(id);
        if (chunkId == null) return;
        try {
            Object garlicHandler = Class.forName("de.teamlapen.vampirism.api.VampirismAPI")
                    .getMethod("garlicHandler", net.minecraft.world.level.Level.class)
                    .invoke(null, maid.level());
            garlicHandler.getClass().getMethod("removeGarlicBlock", int.class).invoke(garlicHandler, chunkId[0]);
        } catch (Exception e) {
            TLMVMain.LOGGER.warn("[MaidGarlicDiffuser] Failed to unregister garlic: {}", e.getMessage());
        }
    }

    private static int getDist(String type) {
        if (type.contains("improved")) return 3;
        if (type.contains("weak")) return 1;
        return 2; // normal
    }

    private static Object getStrength(String type) {
        try {
            Class<?> enumClass = Class.forName("de.teamlapen.vampirism.api.EnumStrength");
            if (type.contains("weak")) return Enum.valueOf(enumClass.asSubclass(Enum.class), "WEAK");
            if (type.contains("improved")) return Enum.valueOf(enumClass.asSubclass(Enum.class), "MEDIUM");
            return Enum.valueOf(enumClass.asSubclass(Enum.class), "MEDIUM");
        } catch (Exception e) { return null; }
    }
}
