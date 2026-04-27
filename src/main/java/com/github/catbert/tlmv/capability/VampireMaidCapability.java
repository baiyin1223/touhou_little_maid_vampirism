package com.github.catbert.tlmv.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

public class VampireMaidCapability implements INBTSerializable<CompoundTag> {
    private boolean isVampire = false;
    private boolean hadSanguinare = false;
    private int vampireLevel = 0;

    // Blood decay tracking fields
    private int lastKnownBlood = -1;  // -1 表示未初始化
    private int bloodDecayTimer = 0;   // 高血量衰减计时器
    private int slowDecayTimer = 0;    // 低血量慢速衰减计时器
    private int starvationTimer = 0;   // blood=0 HP伤害计时器

    // Auto-feed state tracking fields
    private int autoFeedTimer = 0;           // 觅血间隔计时器
    private java.util.UUID autoFeedTargetUUID = null;  // 当前觅血目标 UUID
    private int autoFeedState = 0;           // 0=idle, 1=moving, 2=extracting
    private int autoFeedMoveTimer = 0;       // 移动超时计时器

    public boolean isVampire() {
        return isVampire;
    }

    public void setVampire(boolean vampire) {
        this.isVampire = vampire;
    }

    public boolean hasHadSanguinare() {
        return hadSanguinare;
    }

    public void setHadSanguinare(boolean hadSanguinare) {
        this.hadSanguinare = hadSanguinare;
    }

    public int getVampireLevel() {
        return vampireLevel;
    }

    public void setVampireLevel(int level) {
        this.vampireLevel = Math.max(0, Math.min(5, level));
    }

    public int getLastKnownBlood() { return lastKnownBlood; }
    public void setLastKnownBlood(int blood) { this.lastKnownBlood = blood; }

    public int getBloodDecayTimer() { return bloodDecayTimer; }
    public void setBloodDecayTimer(int timer) { this.bloodDecayTimer = timer; }

    public int getSlowDecayTimer() { return slowDecayTimer; }
    public void setSlowDecayTimer(int timer) { this.slowDecayTimer = timer; }

    public int getStarvationTimer() { return starvationTimer; }
    public void setStarvationTimer(int timer) { this.starvationTimer = timer; }

    public int getAutoFeedTimer() { return autoFeedTimer; }
    public void setAutoFeedTimer(int timer) { this.autoFeedTimer = timer; }

    public UUID getAutoFeedTargetUUID() { return autoFeedTargetUUID; }
    public void setAutoFeedTargetUUID(UUID uuid) { this.autoFeedTargetUUID = uuid; }

    public int getAutoFeedState() { return autoFeedState; }
    public void setAutoFeedState(int state) { this.autoFeedState = state; }

    public int getAutoFeedMoveTimer() { return autoFeedMoveTimer; }
    public void setAutoFeedMoveTimer(int timer) { this.autoFeedMoveTimer = timer; }

    /** 重置自动觅血状态到 IDLE */
    public void resetAutoFeedState() {
        this.autoFeedState = 0;
        this.autoFeedTargetUUID = null;
        this.autoFeedMoveTimer = 0;
    }

    public static String getVampireDisplayPrefix(int level) {
        return switch (level) {
            case 1 -> "§7Ⅰ §5小血仆-";
            case 2 -> "§7Ⅱ §5血侍女仆-";
            case 3 -> "§7Ⅲ §5暗血侍女-";
            case 4 -> "§7Ⅳ §5蔷薇血侍-";
            case 5 -> "§7Ⅴ §5永夜血姬-";
            default -> "§5吸血鬼-";
        };
    }

    public void tick(LivingEntity entity) {
        // Sanguinare handling moved to InfectionHandler via MobEffectEvent.Expired / MobEffectEvent.Remove
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isVampire", isVampire);
        tag.putBoolean("hadSanguinare", hadSanguinare);
        tag.putInt("vampireLevel", vampireLevel);
        tag.putInt("lastKnownBlood", lastKnownBlood);
        tag.putInt("bloodDecayTimer", bloodDecayTimer);
        tag.putInt("slowDecayTimer", slowDecayTimer);
        tag.putInt("starvationTimer", starvationTimer);
        tag.putInt("autoFeedTimer", autoFeedTimer);
        tag.putInt("autoFeedState", autoFeedState);
        tag.putInt("autoFeedMoveTimer", autoFeedMoveTimer);
        if (autoFeedTargetUUID != null) {
            tag.putUUID("autoFeedTargetUUID", autoFeedTargetUUID);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.isVampire = tag.getBoolean("isVampire");
        // Backward compatibility: read old fields if present, but they are no longer used
        if (tag.contains("isInfected")) {
            tag.getBoolean("isInfected");
        }
        if (tag.contains("infectionTimer")) {
            tag.getInt("infectionTimer");
        }
        if (tag.contains("bloodLevel")) {
            tag.getInt("bloodLevel");
        }
        if (tag.contains("bloodTimer")) {
            tag.getInt("bloodTimer");
        }
        if (tag.contains("bitesCount")) {
            tag.getInt("bitesCount");
        }
        this.hadSanguinare = tag.getBoolean("hadSanguinare");
        this.vampireLevel = tag.getInt("vampireLevel");
        this.lastKnownBlood = tag.contains("lastKnownBlood") ? tag.getInt("lastKnownBlood") : -1;
        this.bloodDecayTimer = tag.getInt("bloodDecayTimer");
        this.slowDecayTimer = tag.getInt("slowDecayTimer");
        this.starvationTimer = tag.getInt("starvationTimer");
        this.autoFeedTimer = tag.getInt("autoFeedTimer");
        this.autoFeedState = tag.getInt("autoFeedState");
        this.autoFeedMoveTimer = tag.getInt("autoFeedMoveTimer");
        if (tag.hasUUID("autoFeedTargetUUID")) {
            this.autoFeedTargetUUID = tag.getUUID("autoFeedTargetUUID");
        } else {
            this.autoFeedTargetUUID = null;
        }
    }
}
