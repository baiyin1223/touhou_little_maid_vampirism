package com.github.catbert.tlmv.blockentity;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModCapabilities;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.config.subconfig.LevelingConfig;
import com.github.catbert.tlmv.init.ModBlockEntities;
import com.github.catbert.tlmv.inventory.MaidAltarMenu;
import com.github.catbert.tlmv.network.SyncVampireMaidPacket;
import com.github.catbert.tlmv.network.TLMVNetwork;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import de.teamlapen.vampirism.blocks.AltarPillarBlock;
import de.teamlapen.vampirism.blocks.AltarTipBlock;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.core.ModBlocks;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.items.PureBloodItem;
import de.teamlapen.vampirism.particle.FlyingBloodParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MaidAltarBlockEntity extends BaseContainerBlockEntity {

    private static final int DURATION_TICK = 450;
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);

    private int runningTick;
    private Phase phase = Phase.NOT_RUNNING;
    private @Nullable UUID targetMaidUUID;
    private @Nullable EntityMaid targetMaid;
    private @Nullable UUID maidToLoadUUID;
    private @Nullable BlockPos[] tips;
    private int targetLevel;

    public MaidAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAID_ALTAR.get(), pos, state);
    }

    @Override
    @NotNull
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    @NotNull
    protected Component getDefaultName() {
        return Component.translatable("container.touhou_little_maid_vampirism.maid_altar");
    }

    @Override
    @NotNull
    protected AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory) {
        return new MaidAltarMenu(id, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @NotNull
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    @NotNull
    public ItemStack removeItem(int index, int count) {
        return ContainerHelper.removeItem(items, index, count);
    }

    @Override
    @NotNull
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(items, index);
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack) {
        items.set(index, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                (double) worldPosition.getX() + 0.5D,
                (double) worldPosition.getY() + 0.5D,
                (double) worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("runningTick", runningTick);
        tag.putString("phase", phase.name());
        if (targetMaidUUID != null) {
            tag.putUUID("targetMaidUUID", targetMaidUUID);
        }
        if (tips != null) {
            tag.putInt("tipCount", tips.length);
            for (int i = 0; i < tips.length; i++) {
                tag.putLong("tip_" + i, tips[i].asLong());
            }
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        this.runningTick = tag.getInt("runningTick");
        if (tag.contains("phase")) {
            try {
                this.phase = Phase.valueOf(tag.getString("phase"));
            } catch (IllegalArgumentException ignored) {
                this.phase = Phase.NOT_RUNNING;
            }
        }
        if (tag.hasUUID("targetMaidUUID")) {
            UUID uuid = tag.getUUID("targetMaidUUID");
            if (runningTick > 0 && targetMaid == null) {
                this.maidToLoadUUID = uuid;
            }
            this.targetMaidUUID = uuid;
        }
        if (tag.contains("tipCount")) {
            int count = tag.getInt("tipCount");
            this.tips = new BlockPos[count];
            for (int i = 0; i < count; i++) {
                this.tips[i] = BlockPos.of(tag.getLong("tip_" + i));
            }
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void onDataPacket(Connection net, @NotNull ClientboundBlockEntityDataPacket pkt) {
        if (this.hasLevel()) this.load(pkt.getTag());
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag) {
        this.load(tag);
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public @NotNull Result checkCanActivate() {
        if (runningTick > 0) {
            return Result.IS_RUNNING;
        }

        EntityMaid maid = findNearbyVampireMaid();
        if (maid == null) {
            return Result.NO_MAID;
        }

        int currentLevel = ModCapabilities.getVampireMaid(maid)
                .map(VampireMaidCapability::getVampireLevel)
                .orElse(0);
        if (currentLevel >= 5) {
            return Result.WRONG_LEVEL;
        }

        if (level == null || level.isDay()) {
            return Result.NIGHT_ONLY;
        }

        targetLevel = currentLevel + 1;
        LevelingConfig.LevelRequirements req = LevelingConfig.getRequirements(targetLevel);
        if (req == null) {
            return Result.WRONG_LEVEL;
        }

        if (!checkStructureLevel(req.structurePoints())) {
            tips = null;
            return Result.STRUCTURE_WRONG;
        }

        if (!checkItemRequirements(req)) {
            tips = null;
            return Result.ITEMS_MISSING;
        }

        return Result.OK;
    }

    public void startRitual() {
        if (level == null) return;

        EntityMaid maid = findNearbyVampireMaid();
        if (maid == null) return;

        LevelingConfig.LevelRequirements req = LevelingConfig.getRequirements(targetLevel);
        if (req == null) return;

        this.targetMaid = maid;
        this.targetMaidUUID = maid.getUUID();
        this.runningTick = DURATION_TICK;
        this.phase = Phase.WAITING;

        consumeItems(req);
        this.setChanged();

        if (!this.level.isClientSide) {
            BlockState state = this.level.getBlockState(getBlockPos());
            this.level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }

        maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICK, 10));
        this.setChanged();
    }

    public int getRunningTick() {
        return runningTick;
    }

    public @NotNull Phase getCurrentPhase() {
        if (runningTick < 1) {
            return Phase.NOT_RUNNING;
        }
        if (runningTick == 1) {
            return Phase.CLEAN_UP;
        }
        if (runningTick > (DURATION_TICK - 100)) {
            return Phase.PARTICLE_SPREAD;
        }
        if (runningTick < DURATION_TICK - 160 && runningTick >= (DURATION_TICK - 200)) {
            return Phase.BEAM1;
        }
        if (runningTick < (DURATION_TICK - 200) && runningTick > 50) {
            return Phase.BEAM2;
        }
        if (runningTick == 50) {
            return Phase.LEVELUP;
        }
        if (runningTick < 50) {
            return Phase.ENDING;
        }
        return Phase.WAITING;
    }

    public @Nullable EntityMaid getTargetMaid() {
        if (this.runningTick <= 1) {
            return null;
        }
        return this.targetMaid;
    }

    public @Nullable BlockPos[] getTips() {
        if (this.runningTick <= 1) {
            return null;
        }
        return this.tips;
    }

    // ------------------------------------------------------------------
    // Tick logic
    // ------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, MaidAltarBlockEntity blockEntity) {
        if (blockEntity.maidToLoadUUID != null) {
            if (!blockEntity.loadRitual(blockEntity.maidToLoadUUID)) return;
            blockEntity.maidToLoadUUID = null;
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }

        if (blockEntity.runningTick == DURATION_TICK && !level.isClientSide) {
            blockEntity.setChanged();
        }

        if (blockEntity.runningTick > 0) {
            blockEntity.runningTick--;
            blockEntity.phase = blockEntity.getCurrentPhase();
            blockEntity.tickRitual(level);
        }
    }

    private void tickRitual(Level level) {
        if (targetMaid == null || !targetMaid.isAlive()) {
            runningTick = 1;
        } else {
            targetMaid.setDeltaMovement(Vec3.ZERO);
        }

        Phase currentPhase = getCurrentPhase();

        if (currentPhase == Phase.CLEAN_UP) {
            targetMaid = null;
            targetMaidUUID = null;
            tips = null;
            runningTick = 0;
            phase = Phase.NOT_RUNNING;
            this.setChanged();
        }

        if (level.isClientSide) {
            if (currentPhase == Phase.PARTICLE_SPREAD && tips != null) {
                for (BlockPos tip : tips) {
                    if (level.random.nextInt(5) == 0) {
                        double targetX = worldPosition.getX() + 0.5;
                        double targetY = worldPosition.getY() + 1.0;
                        double targetZ = worldPosition.getZ() + 0.5;
                        level.addParticle(
                                new FlyingBloodParticleOptions(
                                        20 + level.random.nextInt(10), true,
                                        targetX, targetY, targetZ
                                ),
                                tip.getX() + level.random.nextDouble(),
                                tip.getY() + level.random.nextDouble(),
                                tip.getZ() + level.random.nextDouble(),
                                0, 0, 0
                        );
                    }
                }
            }

            if (currentPhase == Phase.LEVELUP && targetMaid != null) {
                level.addParticle(
                        ParticleTypes.EXPLOSION,
                        targetMaid.getX(), targetMaid.getY() + 0.5, targetMaid.getZ(),
                        0, 0, 0
                );
            }
        }

        if (currentPhase == Phase.LEVELUP) {
            if (!level.isClientSide) {
                if (targetMaid == null || !targetMaid.isAlive()) return;

                ModCapabilities.getVampireMaid(targetMaid).ifPresent(cap -> {
                    int currentLevel = cap.getVampireLevel();
                    if (currentLevel != targetLevel - 1) {
                        return;
                    }
                    cap.setVampireLevel(currentLevel + 1);
                    TLMVNetwork.INSTANCE.send(
                            PacketDistributor.TRACKING_ENTITY.with(() -> targetMaid),
                            new SyncVampireMaidPacket(targetMaid.getId(), cap.isVampire(), cap.getVampireLevel())
                    );
                    VampirismAPI.getExtendedCreatureVampirism(targetMaid).ifPresent(ext -> {
                        int maxBlood = ext.getMaxBlood();
                        ext.setBlood(maxBlood);
                        cap.setLastKnownBlood(maxBlood);
                        try {
                            ext.getClass().getMethod("sync").invoke(ext);
                        } catch (Exception e) {
                            TLMVMain.LOGGER.warn("Failed to sync blood value after level up", e);
                        }
                    });
                });

                targetMaid.setHealth(targetMaid.getMaxHealth());

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                            targetMaid.getX(), targetMaid.getY() + 0.5, targetMaid.getZ(),
                            5, 0.5, 0.5, 0.5, 0.0
                    );
                }
                level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                        4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

                targetMaid.addEffect(new MobEffectInstance(MobEffects.SATURATION, 400, 2));
                targetMaid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2));
                targetMaid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 2));
            }
        }
    }

    // ------------------------------------------------------------------
    // Maid detection
    // ------------------------------------------------------------------

    private @Nullable EntityMaid findNearbyVampireMaid() {
        if (level == null) return null;
        AABB area = new AABB(worldPosition).inflate(3.0);
        List<EntityMaid> maids = level.getEntitiesOfClass(EntityMaid.class, area);
        for (EntityMaid maid : maids) {
            boolean isVampire = ModCapabilities.getVampireMaid(maid)
                    .map(VampireMaidCapability::isVampire)
                    .orElse(false);
            if (isVampire) {
                return maid;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Structure detection
    // ------------------------------------------------------------------

    private boolean checkStructureLevel(int requiredPoints) {
        if (level == null) return false;
        BlockPos[] foundTips = findTips();

        List<ValuedTip> valuedTips = new ArrayList<>();
        for (BlockPos tipPos : foundTips) {
            int height = 0;
            AltarPillarBlock.EnumPillarType type = null;
            BlockState temp;
            while ((temp = level.getBlockState(tipPos.offset(0, -height - 1, 0))).getBlock().equals(ModBlocks.ALTAR_PILLAR.get())) {
                AltarPillarBlock.EnumPillarType t = temp.getValue(AltarPillarBlock.TYPE_PROPERTY);
                if (type == null) {
                    type = t;
                    height++;
                } else if (type.equals(t)) {
                    height++;
                } else {
                    break;
                }
            }
            int value = (int) (10 * Math.min(height, 3) * (type == null ? 0 : type.getValue()));
            valuedTips.add(new ValuedTip(tipPos, value));
        }

        valuedTips.sort((a, b) -> Integer.compare(b.value, a.value));

        int found = 0;
        int i = 0;
        List<BlockPos> selectedTips = new ArrayList<>();
        while (found < requiredPoints * 10 && i < valuedTips.size() && i < 8) {
            int v = valuedTips.get(i).value;
            if (v == 0) break;
            found += v;
            selectedTips.add(valuedTips.get(i).pos);
            i++;
        }

        this.tips = selectedTips.toArray(new BlockPos[0]);
        return found >= requiredPoints * 10;
    }

    private BlockPos @NotNull [] findTips() {
        if (level == null) return new BlockPos[0];
        List<BlockPos> list = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int cx = worldPosition.getX();
        int cy = worldPosition.getY();
        int cz = worldPosition.getZ();
        for (int x = cx - 4; x < cx + 5; x++) {
            for (int y = cy + 1; y < cy + 4; y++) {
                for (int z = cz - 4; z < cz + 5; z++) {
                    if (level.getBlockState(mutable.set(x, y, z)).getBlock() instanceof AltarTipBlock) {
                        list.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return list.toArray(new BlockPos[0]);
    }

    // ------------------------------------------------------------------
    // Item checks & consumption
    // ------------------------------------------------------------------

    private boolean checkItemRequirements(@NotNull LevelingConfig.LevelRequirements req) {
        // Slot 0: Pure Blood
        ItemStack bloodStack = getItem(0);
        boolean bloodOk = false;
        if (!bloodStack.isEmpty() && bloodStack.getItem() instanceof PureBloodItem pureBlood) {
            bloodOk = pureBlood.getLevel() >= req.pureBloodLevel() && bloodStack.getCount() >= req.pureBloodQuantity();
        }

        // Slot 1: Human Heart
        ItemStack heartStack = getItem(1);
        boolean heartOk = !heartStack.isEmpty()
                && heartStack.getItem().equals(ModItems.HUMAN_HEART.get())
                && heartStack.getCount() >= req.humanHeartQuantity();

        // Slot 2: Vampire Book
        ItemStack bookStack = getItem(2);
        boolean bookOk = !bookStack.isEmpty()
                && bookStack.getItem().equals(ModItems.VAMPIRE_BOOK.get())
                && bookStack.getCount() >= req.vampireBookQuantity();

        return bloodOk && heartOk && bookOk;
    }

    private void consumeItems(@NotNull LevelingConfig.LevelRequirements req) {
        removeItem(0, req.pureBloodQuantity());
        removeItem(1, req.humanHeartQuantity());
        removeItem(2, req.vampireBookQuantity());
    }

    // ------------------------------------------------------------------
    // Load ritual after world load
    // ------------------------------------------------------------------

    private boolean loadRitual(@NotNull UUID maidUUID) {
        if (this.level == null) return false;
        if (this.level.players().isEmpty() && !this.level.isClientSide) return false;

        Entity entity = null;
        if (this.level instanceof ServerLevel serverLevel) {
            entity = serverLevel.getEntity(maidUUID);
        } else {
            AABB searchArea = new AABB(worldPosition).inflate(10.0);
            List<EntityMaid> maids = this.level.getEntitiesOfClass(EntityMaid.class, searchArea);
            for (EntityMaid maid : maids) {
                if (maid.getUUID().equals(maidUUID)) {
                    entity = maid;
                    break;
                }
            }
        }

        if (entity instanceof EntityMaid maid && maid.isAlive()) {
            this.targetMaid = maid;
            this.targetMaidUUID = maidUUID;
            this.targetLevel = ModCapabilities.getVampireMaid(maid)
                    .map(VampireMaidCapability::getVampireLevel)
                    .orElse(0) + 1;
            LevelingConfig.LevelRequirements req = LevelingConfig.getRequirements(targetLevel);
            if (req != null) {
                checkStructureLevel(req.structurePoints());
            }
        } else if (!this.level.isClientSide) {
            runningTick = 0;
            this.tips = null;
            this.phase = Phase.NOT_RUNNING;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Enums & helpers
    // ------------------------------------------------------------------

    public enum Phase {
        NOT_RUNNING, WAITING, PARTICLE_SPREAD, BEAM1, BEAM2, LEVELUP, ENDING, CLEAN_UP
    }

    public enum Result {
        OK, IS_RUNNING, NO_MAID, WRONG_LEVEL, NIGHT_ONLY, STRUCTURE_WRONG, ITEMS_MISSING
    }

    private record ValuedTip(BlockPos pos, int value) {
    }
}
