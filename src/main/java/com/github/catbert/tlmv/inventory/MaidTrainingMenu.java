package com.github.catbert.tlmv.inventory;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.config.subconfig.LevelingConfig;
import com.github.catbert.tlmv.level.HunterLevelManager;
import com.github.catbert.tlmv.network.SyncVampireMaidPacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MaidTrainingMenu extends AbstractContainerMenu {

    private final Player player;
    private final BlockPos trainerPos;
    private final SimpleContainer inputSlots = new SimpleContainer(5);

    public MaidTrainingMenu(int containerId, Inventory playerInventory, BlockPos trainerPos) {
        super(com.github.catbert.tlmv.init.ModMenuTypes.MAID_TRAINING.get(), containerId);
        this.player = playerInventory.player;
        this.trainerPos = trainerPos;

        // 5 slots matching Vampirism hunter trainer layout:
        // Top row: iron(27,26), gold(57,26), vampBook(86,26) — 匹配原教官 GUI 三槽
        // Bottom row: soulOrb(42,50), pureBlood(71,50) — 居两两间隙中下方
        this.addSlot(new Slot(inputSlots, 0, 27, 26) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(Items.IRON_INGOT); }
        });
        this.addSlot(new Slot(inputSlots, 1, 57, 26) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(Items.GOLD_INGOT); }
        });
        this.addSlot(new Slot(inputSlots, 2, 86, 26) {
            @Override public boolean mayPlace(ItemStack s) {
                var key = BuiltInRegistries.ITEM.getKey(s.getItem());
                return key != null && "vampirism:vampire_book".equals(key.toString());
            }
        });
        this.addSlot(new Slot(inputSlots, 3, 42, 50) {
            @Override public boolean mayPlace(ItemStack s) {
                var key = BuiltInRegistries.ITEM.getKey(s.getItem());
                return key != null && "vampirism:soul_orb_vampire".equals(key.toString());
            }
        });
        this.addSlot(new Slot(inputSlots, 4, 71, 50) {
            @Override public boolean mayPlace(ItemStack s) {
                var key = BuiltInRegistries.ITEM.getKey(s.getItem());
                return key != null && "vampirism".equals(key.getNamespace()) && key.getPath().startsWith("pure_blood");
            }
        });

        // Player inventory slots (standard 27 + 9)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    public boolean canPromote() {
        LevelingConfig.HunterLevelRequirements req = getReqs();
        if (req == null) return false;
        return count(Items.IRON_INGOT) >= req.ironQuantity()
                && count(Items.GOLD_INGOT) >= req.goldQuantity()
                && countVampBook() >= req.vampireBookQuantity()
                && countSoulOrb() >= req.soulOrbQuantity()
                && countPureBlood(req.pureBloodLevel()) >= req.pureBloodQuantity();
    }

    private LevelingConfig.HunterLevelRequirements getReqs() {
        Level level = player.level();
        AABB area = new AABB(trainerPos).inflate(5.0);
        List<EntityMaid> maids = level.getEntitiesOfClass(EntityMaid.class, area);
        for (EntityMaid maid : maids) {
            if (maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(player.getUUID())) {
                var cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
                if (cap.isHunter()) {
                    return LevelingConfig.getHunterRequirements(cap.getHunterLevel() + 1);
                }
            }
        }
        return null;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        // Return unused items to player
        if (!player.level().isClientSide()) {
            for (int i = 0; i < 5; i++) {
                var stack = inputSlots.getItem(i);
                if (!stack.isEmpty()) {
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    inputSlots.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private int count(net.minecraft.world.item.Item item) {
        int c = 0;
        for (int i = 0; i < 5; i++) {
            var s = inputSlots.getItem(i);
            if (s.is(item)) c += s.getCount();
        }
        return c;
    }

    private int countVampBook() {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("vampirism", "vampire_book"));
        return item == null ? 0 : count(item);
    }

    private int countSoulOrb() {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("vampirism", "soul_orb_vampire"));
        return item == null ? 0 : count(item);
    }

    private int countPureBlood(int requiredLevel) {
        int c = 0;
        for (int i = 0; i < 5; i++) {
            var s = inputSlots.getItem(i);
            if (!s.isEmpty()) {
                var key = BuiltInRegistries.ITEM.getKey(s.getItem());
                if (key != null && "vampirism".equals(key.getNamespace()) && key.getPath().startsWith("pure_blood")) {
                    try {
                        int level = Integer.parseInt(key.getPath().substring(key.getPath().lastIndexOf('_') + 1));
                        if (level >= requiredLevel) c += s.getCount();
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return c;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        // Simple shift-click: move between input slots (0-4) and player inventory (5+)
        var slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            var stack = slot.getItem().copy();
            if (index < 5) {
                if (!this.moveItemStackTo(stack, 5, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 5, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.distanceToSqr(trainerPos.getX() + 0.5, trainerPos.getY() + 0.5, trainerPos.getZ() + 0.5) <= 64.0;
    }
}
