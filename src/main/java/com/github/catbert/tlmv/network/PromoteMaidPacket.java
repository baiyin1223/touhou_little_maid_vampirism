package com.github.catbert.tlmv.network;

import com.github.catbert.tlmv.TLMVMain;
import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.config.subconfig.LevelingConfig;
import com.github.catbert.tlmv.inventory.MaidTrainingMenu;
import com.github.catbert.tlmv.level.HunterLevelManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class PromoteMaidPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PromoteMaidPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TLMVMain.MOD_ID, "promote_maid"));

    public static final StreamCodec<FriendlyByteBuf, PromoteMaidPacket> STREAM_CODEC =
            StreamCodec.ofMember(PromoteMaidPacket::write, PromoteMaidPacket::new);

    private final BlockPos trainerPos;

    public PromoteMaidPacket(BlockPos trainerPos) {
        this.trainerPos = trainerPos;
    }

    public PromoteMaidPacket(FriendlyByteBuf buf) {
        this.trainerPos = buf.readBlockPos();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(trainerPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PromoteMaidPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();
            BlockPos pos = packet.trainerPos;

            // Verify player hunter level >= 4
            int hunterLevel = getHunterLevel(player);
            if (hunterLevel < 4) return;

            // Find owned hunter maid near trainer
            AABB area = new AABB(pos).inflate(5.0);
            List<EntityMaid> maids = level.getEntitiesOfClass(EntityMaid.class, area);
            EntityMaid targetMaid = null;
            for (EntityMaid maid : maids) {
                if (maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(player.getUUID())) {
                    var cap = maid.getData(ModAttachments.VAMPIRE_MAID.get());
                    if (cap.isHunter()) {
                        targetMaid = maid;
                        break;
                    }
                }
            }
            if (targetMaid == null) {
                player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.maid_promotion_no_maid"));
                return;
            }

            var cap = targetMaid.getData(ModAttachments.VAMPIRE_MAID.get());
            int currentLevel = cap.getHunterLevel();
            int targetLevel = currentLevel + 1;
            if (targetLevel > 5) return;

            LevelingConfig.HunterLevelRequirements req = LevelingConfig.getHunterRequirements(targetLevel);
            if (req == null) return;

            // Check items in player's open MaidTrainingMenu (if any), else player inventory
            boolean consumed = false;
            if (player.containerMenu instanceof MaidTrainingMenu mtm) {
                consumed = consumeFromMenu(mtm, req, player);
            }
            if (!consumed) {
                consumed = consumeFromInventory(player, req);
            }
            if (!consumed) {
                player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.maid_promotion_no_items",
                        req.ironQuantity(), req.goldQuantity(), req.vampireBookQuantity(),
                        req.soulOrbQuantity(), req.pureBloodLevel(), req.pureBloodQuantity()));
                return;
            }

            // Promote
            cap.setHunterLevel(targetLevel);
            HunterLevelManager.applyLevel(targetMaid, targetLevel);

            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(targetMaid,
                    new SyncVampireMaidPacket(targetMaid.getId(), cap.isVampire(), cap.getVampireLevel(), cap.isHunter(), cap.getHunterLevel()));

            if (level instanceof ServerLevel serverLevel) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
                if (bolt != null) {
                    bolt.moveTo(targetMaid.getX(), targetMaid.getY(), targetMaid.getZ());
                    bolt.setVisualOnly(true);
                    serverLevel.addFreshEntity(bolt);
                }
            }

            // Promotion buffs: Regen III, Resistance II, Absorption IV for 30s
            targetMaid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2));
            targetMaid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
            targetMaid.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));

            player.sendSystemMessage(Component.translatable("message.touhou_little_maid_vampirism.maid_promoted", targetLevel));
            player.closeContainer();
        });
    }

    private static boolean consumeFromMenu(MaidTrainingMenu menu, LevelingConfig.HunterLevelRequirements req, ServerPlayer player) {
        // Access SimpleContainer via reflection
        try {
            var field = MaidTrainingMenu.class.getDeclaredField("inputSlots");
            field.setAccessible(true);
            var slots = (net.minecraft.world.SimpleContainer) field.get(menu);
            if (!checkSlots(slots, req)) return false;
            removeSlots(slots, req);
            menu.broadcastChanges();
            return true;
        } catch (Exception e) {
            TLMVMain.LOGGER.warn("[PromoteMaidPacket] Failed to access menu slots: {}", e.getMessage());
        }
        return false;
    }

    private static boolean checkSlots(net.minecraft.world.SimpleContainer slots, LevelingConfig.HunterLevelRequirements req) {
        return count(slots, Items.IRON_INGOT) >= req.ironQuantity()
                && count(slots, Items.GOLD_INGOT) >= req.goldQuantity()
                && countReg(slots, "vampirism:vampire_book") >= req.vampireBookQuantity()
                && countReg(slots, "vampirism:soul_orb_vampire") >= req.soulOrbQuantity()
                && countPure(slots, req.pureBloodLevel()) >= req.pureBloodQuantity();
    }

    private static void removeSlots(net.minecraft.world.SimpleContainer slots, LevelingConfig.HunterLevelRequirements req) {
        remove(slots, Items.IRON_INGOT, req.ironQuantity());
        remove(slots, Items.GOLD_INGOT, req.goldQuantity());
        removeReg(slots, "vampirism:vampire_book", req.vampireBookQuantity());
        removeReg(slots, "vampirism:soul_orb_vampire", req.soulOrbQuantity());
        removePure(slots, req.pureBloodLevel(), req.pureBloodQuantity());
    }

    private static int count(net.minecraft.world.SimpleContainer c, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            var s = c.getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static int countReg(net.minecraft.world.SimpleContainer c, String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == null ? 0 : count(c, item);
    }

    private static int countPure(net.minecraft.world.SimpleContainer c, int lvl) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            var s = c.getItem(i);
            if (!s.isEmpty()) {
                var key = BuiltInRegistries.ITEM.getKey(s.getItem());
                if (key != null && "vampirism".equals(key.getNamespace()) && key.getPath().startsWith("pure_blood")) {
                    try {
                        int itemLvl = Integer.parseInt(key.getPath().substring(key.getPath().lastIndexOf('_') + 1));
                        if (itemLvl >= lvl) n += s.getCount();
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return n;
    }

    private static void remove(net.minecraft.world.SimpleContainer c, net.minecraft.world.item.Item item, int amount) {
        for (int i = 0; i < c.getContainerSize() && amount > 0; i++) {
            var s = c.getItem(i);
            if (s.is(item)) {
                int take = Math.min(amount, s.getCount());
                s.shrink(take);
                c.setItem(i, s);
                amount -= take;
            }
        }
    }

    private static void removeReg(net.minecraft.world.SimpleContainer c, String id, int amount) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item != null) remove(c, item, amount);
    }

    private static void removePure(net.minecraft.world.SimpleContainer c, int lvl, int amount) {
        for (int i = 0; i < c.getContainerSize() && amount > 0; i++) {
            var s = c.getItem(i);
            if (!s.isEmpty()) {
                var key = BuiltInRegistries.ITEM.getKey(s.getItem());
                if (key != null && "vampirism".equals(key.getNamespace()) && key.getPath().startsWith("pure_blood")) {
                    try {
                        int itemLvl = Integer.parseInt(key.getPath().substring(key.getPath().lastIndexOf('_') + 1));
                        if (itemLvl >= lvl) {
                            int take = Math.min(amount, s.getCount());
                            s.shrink(take);
                            c.setItem(i, s);
                            amount -= take;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private static boolean consumeFromInventory(ServerPlayer player, LevelingConfig.HunterLevelRequirements req) {
        var inv = player.getInventory();
        if (!checkInv(inv, req)) return false;
        removeInv(inv, req);
        return true;
    }

    private static boolean checkInv(net.minecraft.world.entity.player.Inventory inv, LevelingConfig.HunterLevelRequirements req) {
        var soulOrb = BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:soul_orb_vampire"));
        var vampBook = BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:vampire_book"));
        return countInv(inv, Items.IRON_INGOT) >= req.ironQuantity()
                && countInv(inv, Items.GOLD_INGOT) >= req.goldQuantity()
                && (vampBook == null || countInv(inv, vampBook) >= req.vampireBookQuantity())
                && (soulOrb == null || countInv(inv, soulOrb) >= req.soulOrbQuantity())
                && countPureInv(inv, req.pureBloodLevel()) >= req.pureBloodQuantity();
    }

    private static void removeInv(net.minecraft.world.entity.player.Inventory inv, LevelingConfig.HunterLevelRequirements req) {
        var soulOrb = BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:soul_orb_vampire"));
        var vampBook = BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:vampire_book"));
        removeInvItem(inv, Items.IRON_INGOT, req.ironQuantity());
        removeInvItem(inv, Items.GOLD_INGOT, req.goldQuantity());
        if (vampBook != null) removeInvItem(inv, vampBook, req.vampireBookQuantity());
        if (soulOrb != null) removeInvItem(inv, soulOrb, req.soulOrbQuantity());
        removePureInv(inv, req.pureBloodLevel(), req.pureBloodQuantity());
    }

    private static int countInv(net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.item.Item item) {
        int c = 0;
        for (var stack : inv.items) if (stack.is(item)) c += stack.getCount();
        for (var stack : inv.offhand) if (stack.is(item)) c += stack.getCount();
        return c;
    }

    private static int countPureInv(net.minecraft.world.entity.player.Inventory inv, int lvl) {
        int c = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (key != null && "vampirism".equals(key.getNamespace()) && key.getPath().startsWith("pure_blood")) {
                    try {
                        int itemLvl = Integer.parseInt(key.getPath().substring(key.getPath().lastIndexOf('_') + 1));
                        if (itemLvl >= lvl) c += stack.getCount();
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return c;
    }

    private static void removeInvItem(net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.item.Item item, int amount) {
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            var stack = inv.getItem(i);
            if (stack.is(item)) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
            }
        }
    }

    private static void removePureInv(net.minecraft.world.entity.player.Inventory inv, int lvl, int amount) {
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (key != null && "vampirism".equals(key.getNamespace()) && key.getPath().startsWith("pure_blood")) {
                    try {
                        int itemLvl = Integer.parseInt(key.getPath().substring(key.getPath().lastIndexOf('_') + 1));
                        if (itemLvl >= lvl) {
                            int take = Math.min(amount, stack.getCount());
                            stack.shrink(take);
                            amount -= take;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private static int getHunterLevel(ServerPlayer player) {
        try {
            Class<?> fphClass = Class.forName("de.teamlapen.vampirism.entity.factions.FactionPlayerHandler");
            var inst = fphClass.getMethod("get", net.minecraft.world.entity.player.Player.class).invoke(null, player);
            Class<?> factionClass = Class.forName("de.teamlapen.vampirism.api.entity.factions.IPlayableFaction");
            Class<?> vrefClass = Class.forName("de.teamlapen.vampirism.api.VReference");
            Object hunterFaction = vrefClass.getField("HUNTER_FACTION").get(null);
            Object level = inst.getClass().getMethod("getCurrentLevel", factionClass).invoke(inst, hunterFaction);
            if (level instanceof Integer i) return i;
        } catch (Exception ignored) {}
        return 0;
    }
}
