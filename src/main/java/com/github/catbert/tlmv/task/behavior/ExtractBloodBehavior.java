package com.github.catbert.tlmv.task.behavior;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.config.subconfig.TaskConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class ExtractBloodBehavior extends Behavior<EntityMaid> {

    private static final Random RANDOM = new Random();
    private static final String[] BUBBLE_LANG_KEYS = {
        "chat_bubble.touhou_little_maid_vampirism.collecting_blood.1",
        "chat_bubble.touhou_little_maid_vampirism.collecting_blood.2",
        "chat_bubble.touhou_little_maid_vampirism.collecting_blood.3",
        "chat_bubble.touhou_little_maid_vampirism.collecting_blood.4",
        "chat_bubble.touhou_little_maid_vampirism.collecting_blood.5"
    };

    private long lastActionTime = -1;
    private long soundStartTime = -1;
    private int containerSlot = -1;  // 当前正在填充的背包槽位索引

    public ExtractBloodBehavior() {
        super(Map.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) {
            return false;
        }
        LivingEntity target = targetOpt.get();
        if (!target.isAlive()) {
            return false;
        }
        if (maid.distanceToSqr(target) > 2.5 * 2.5) {
            return false;
        }
        return hasAvailableContainer(maid);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        containerSlot = findContainerSlot(maid);
        lastActionTime = -1;
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) {
            return;
        }
        LivingEntity target = targetOpt.get();

        int cooldown = TaskConfig.COLLECT_BLOOD_COOLDOWN.get();
        if (lastActionTime < 0 || gameTime - lastActionTime >= cooldown) {
            lastActionTime = gameTime;
            performExtraction(level, maid, target);
        }

        // 1秒(20 ticks)后停止吸血音效
        if (soundStartTime > 0 && gameTime - soundStartTime >= 20) {
            ResourceLocation soundId = new ResourceLocation("vampirism", "entity.vampire_feeding");
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundStopSoundPacket(soundId, SoundSource.NEUTRAL));
            }
            soundStartTime = -1;
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) {
            return false;
        }
        LivingEntity target = targetOpt.get();
        if (!target.isAlive()) {
            return false;
        }
        if (maid.distanceToSqr(target) > 3.0 * 3.0) {
            return false;
        }
        if (!(target instanceof net.minecraft.world.entity.PathfinderMob pathfinderTarget)) {
            return false;
        }
        var ext = VampirismAPI.getExtendedCreatureVampirism(pathfinderTarget).orElse(null);
        if (ext == null || ext.getBlood() <= 1) {
            return false;
        }
        return hasAvailableContainer(maid);
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        try {
            containerSlot = -1;

            // 停止吸血音效
            if (soundStartTime > 0) {
                ResourceLocation soundId = new ResourceLocation("vampirism", "entity.vampire_feeding");
                if (level.getServer() != null) {
                    for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                        player.connection.send(new ClientboundStopSoundPacket(soundId, SoundSource.NEUTRAL));
                    }
                }
                soundStartTime = -1;
            }

        } catch (Exception e) {
            TLMVMain.LOGGER.error("[ExtractBloodBehavior] Error during stop", e);
        }
    }

    private void performExtraction(ServerLevel level, EntityMaid maid, LivingEntity target) {
        if (!(target instanceof net.minecraft.world.entity.PathfinderMob pathfinderTarget)) {
            return;
        }
        var ext = VampirismAPI.getExtendedCreatureVampirism(pathfinderTarget).orElse(null);
        if (ext == null) {
            return;
        }
        int targetBlood = ext.getBlood();
        if (targetBlood <= 1) {
            return;
        }

        var inv = maid.getAvailableInv(true);

        // 查找或验证当前容器槽位
        if (containerSlot < 0 || containerSlot >= inv.getSlots()
                || !FindBloodTargetBehavior.isAvailableContainer(inv.getStackInSlot(containerSlot))) {
            containerSlot = findContainerSlot(maid);
        }
        if (containerSlot < 0) {
            return;
        }

        ItemStack container = inv.getStackInSlot(containerSlot);
        int maxExtract = TaskConfig.COLLECT_BLOOD_EXTRACT.get();
        int extractAmount = Math.min(maxExtract, targetBlood - 1);
        if (extractAmount <= 0) {
            return;
        }

        int actualExtracted;

        if (container.is(Items.GLASS_BOTTLE)) {
            // 玻璃瓶 -> 血瓶：从堆叠中取出1个玻璃瓶，转换为血瓶
            Item bloodBottleItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vampirism", "blood_bottle"));
            if (bloodBottleItem == null) {
                return;
            }

            // 安全检查：如果玻璃瓶堆叠数 > 1，需要确保有额外空槽位存放新血瓶
            if (container.getCount() > 1) {
                boolean hasEmptySlot = false;
                for (int j = 0; j < inv.getSlots(); j++) {
                    if (inv.getStackInSlot(j).isEmpty()) {
                        hasEmptySlot = true;
                        break;
                    }
                }
                if (!hasEmptySlot) {
                    // 背包满载，无法存放新血瓶，暂停提取
                    containerSlot = -1;
                    return;
                }
            }
            // count == 1 时，提取后槽位变空，新血瓶可以放入，安全

            actualExtracted = Math.min(extractAmount, 9);

            // 从槽位中取出1个玻璃瓶（处理堆叠情况）
            inv.extractItem(containerSlot, 1, false);

            // 创建血瓶并放入背包
            ItemStack bloodBottle = new ItemStack(bloodBottleItem);
            bloodBottle.setDamageValue(actualExtracted);
            ItemStack remainder = bloodBottle;
            for (int j = 0; j < inv.getSlots(); j++) {
                remainder = inv.insertItem(j, remainder, false);
                if (remainder.isEmpty()) break;
            }
            if (!remainder.isEmpty()) {
                maid.spawnAtLocation(remainder);
            }

            // 重置槽位，下次需要重新查找（可能找到刚创建的血瓶继续填充）
            containerSlot = -1;
        } else if (isBloodBottle(container)) {
            // 血瓶 -> 填充更多血（直接在背包中修改）
            int currentDamage = container.getDamageValue();
            int newDamage = Math.min(currentDamage + extractAmount, 9);
            actualExtracted = newDamage - currentDamage;
            if (actualExtracted <= 0) {
                return;
            }
            // 直接修改背包中的物品 damage 值
            container.setDamageValue(newDamage);

            if (newDamage >= 9) {
                // 血瓶已满，下次需要查找新容器
                containerSlot = -1;
            }
        } else {
            return;
        }

        // 更新目标血量
        ext.setBlood(targetBlood - actualExtracted);

        // sync 反射调用
        try {
            ext.getClass().getMethod("sync").invoke(ext);
        } catch (Exception e) {
            TLMVMain.LOGGER.warn("Failed to sync blood value", e);
        }

        // 挥臂动画
        maid.swing(InteractionHand.MAIN_HAND);

        // 红色粒子效果
        spawnBloodParticles(level, maid, target);

        // 吸血音效
        SoundEvent feedingSound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("vampirism", "entity.vampire_feeding"));
        if (feedingSound != null) {
            maid.level().playSound(null, maid.blockPosition(), feedingSound, SoundSource.NEUTRAL, 1.0F, 1.0F);
            soundStartTime = level.getGameTime();
        } else {
            maid.level().playSound(null, maid.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }

        // 30% 概率显示聊天气泡，持续3秒
        if (RANDOM.nextFloat() < 0.3f) {
            String langKey = BUBBLE_LANG_KEYS[RANDOM.nextInt(BUBBLE_LANG_KEYS.length)];
            TextChatBubbleData bubble = TextChatBubbleData.create(
                60,
                Component.translatable(langKey),
                IChatBubbleData.TYPE_2,
                0
            );
            maid.getChatBubbleManager().addChatBubble(bubble);
        }

        TLMVMain.LOGGER.debug("[ExtractBloodBehavior] Extracted {} blood from {} (remaining: {})",
                actualExtracted, target.getName().getString(), targetBlood - actualExtracted);
    }

    private void spawnBloodParticles(ServerLevel level, EntityMaid maid, LivingEntity target) {
        DustParticleOptions redDust = new DustParticleOptions(new Vector3f(0.8f, 0.0f, 0.0f), 1.0f);
        for (int p = 0; p < 5; p++) {
            double t = (p + 1.0) / 6.0;
            double px = target.getX() + (maid.getX() - target.getX()) * t;
            double py = target.getY() + 0.5 + (maid.getY() + 0.5 - target.getY() - 0.5) * t;
            double pz = target.getZ() + (maid.getZ() - target.getZ()) * t;
            level.sendParticles(redDust, px, py, pz, 1, 0.1, 0.1, 0.1, 0);
        }
    }

    /**
     * 在背包中查找可用容器的槽位。
     * 优先查找未满血瓶，其次查找玻璃瓶。
     * @return 背包槽位索引，-1 表示没找到
     */
    private int findContainerSlot(EntityMaid maid) {
        var inv = maid.getAvailableInv(true);
        // 优先找未满血瓶
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && isBloodBottle(stack) && stack.getDamageValue() < 9) {
                return i;
            }
        }
        // 其次找玻璃瓶
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.is(Items.GLASS_BOTTLE)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isBloodBottle(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "vampirism:blood_bottle".equals(key.toString());
    }

    private boolean hasAvailableContainer(EntityMaid maid) {
        var inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            if (FindBloodTargetBehavior.isAvailableContainer(inv.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

}
