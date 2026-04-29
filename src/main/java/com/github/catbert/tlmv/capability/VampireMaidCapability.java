package com.github.catbert.tlmv.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public class VampireMaidCapability {

    public static final Codec<VampireMaidCapability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("isVampire").forGetter(c -> c.isVampire),
            Codec.BOOL.fieldOf("hadSanguinare").forGetter(c -> c.hadSanguinare),
            Codec.INT.fieldOf("vampireLevel").forGetter(c -> c.vampireLevel),
            Codec.INT.fieldOf("lastKnownBlood").forGetter(c -> c.lastKnownBlood),
            Codec.INT.fieldOf("bloodDecayTimer").forGetter(c -> c.bloodDecayTimer),
            Codec.INT.fieldOf("slowDecayTimer").forGetter(c -> c.slowDecayTimer),
            Codec.INT.fieldOf("starvationTimer").forGetter(c -> c.starvationTimer),
            Codec.INT.fieldOf("garlicHpTicker").forGetter(c -> c.garlicHpTicker),
            Codec.INT.fieldOf("garlicBloodTicker").forGetter(c -> c.garlicBloodTicker),
            Codec.INT.fieldOf("autoFeedTimer").forGetter(c -> c.autoFeedTimer),
            Codec.STRING.optionalFieldOf("autoFeedTargetUUID").forGetter(c -> c.autoFeedTargetUUID != null ? Optional.of(c.autoFeedTargetUUID.toString()) : Optional.empty()),
            Codec.INT.fieldOf("autoFeedState").forGetter(c -> c.autoFeedState),
            Codec.INT.fieldOf("autoFeedMoveTimer").forGetter(c -> c.autoFeedMoveTimer)
    ).apply(instance, VampireMaidCapability::fromCodec));

    private static VampireMaidCapability fromCodec(
            boolean isVampire, boolean hadSanguinare, int vampireLevel,
            int lastKnownBlood, int bloodDecayTimer, int slowDecayTimer, int starvationTimer,
            int garlicHpTicker, int garlicBloodTicker,
            int autoFeedTimer, Optional<String> autoFeedTargetUUID, int autoFeedState, int autoFeedMoveTimer
    ) {
        VampireMaidCapability cap = new VampireMaidCapability();
        cap.isVampire = isVampire;
        cap.hadSanguinare = hadSanguinare;
        cap.vampireLevel = vampireLevel;
        cap.lastKnownBlood = lastKnownBlood;
        cap.bloodDecayTimer = bloodDecayTimer;
        cap.slowDecayTimer = slowDecayTimer;
        cap.starvationTimer = starvationTimer;
        cap.garlicHpTicker = garlicHpTicker;
        cap.garlicBloodTicker = garlicBloodTicker;
        cap.autoFeedTimer = autoFeedTimer;
        cap.autoFeedTargetUUID = autoFeedTargetUUID.map(UUID::fromString).orElse(null);
        cap.autoFeedState = autoFeedState;
        cap.autoFeedMoveTimer = autoFeedMoveTimer;
        return cap;
    }

    private boolean isVampire = false;
    private boolean hadSanguinare = false;
    private int vampireLevel = 0;

    // Blood decay tracking fields
    private int lastKnownBlood = 0;  // 0 表示未初始化或无血量
    private int bloodDecayTimer = 0;   // 高血量衰减计时器
    private int slowDecayTimer = 0;    // 低血量慢速衰减计时器
    private int starvationTimer = 0;   // blood=0 HP伤害计时器

    // Garlic effect tracking fields
    private int garlicHpTicker = 0;       // 大蒜HP伤害计时器
    private int garlicBloodTicker = 0;    // 大蒜额外blood消耗计时器

    // Garlic slowness tracking
    private transient boolean applyingSlowness = false;

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

    public boolean isApplyingSlowness() {
        return applyingSlowness;
    }

    public void setApplyingSlowness(boolean applyingSlowness) {
        this.applyingSlowness = applyingSlowness;
    }

    public int getLastKnownBlood() { return lastKnownBlood; }
    public void setLastKnownBlood(int blood) { this.lastKnownBlood = blood; }

    public int getBloodDecayTimer() { return bloodDecayTimer; }
    public void setBloodDecayTimer(int timer) { this.bloodDecayTimer = timer; }

    public int getSlowDecayTimer() { return slowDecayTimer; }
    public void setSlowDecayTimer(int timer) { this.slowDecayTimer = timer; }

    public int getStarvationTimer() { return starvationTimer; }
    public void setStarvationTimer(int timer) { this.starvationTimer = timer; }

    public int getGarlicHpTicker() { return garlicHpTicker; }
    public void setGarlicHpTicker(int ticker) { this.garlicHpTicker = ticker; }

    public int getGarlicBloodTicker() { return garlicBloodTicker; }
    public void setGarlicBloodTicker(int ticker) { this.garlicBloodTicker = ticker; }

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
}
