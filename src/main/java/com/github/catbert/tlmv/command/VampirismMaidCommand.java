package com.github.catbert.tlmv.command;

import com.github.catbert.tlmv.capability.ModAttachments;
import com.github.catbert.tlmv.capability.VampireMaidCapability;
import com.github.catbert.tlmv.level.VampireLevelManager;
import com.github.catbert.tlmv.network.SyncVampireMaidPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Optional;

public class VampirismMaidCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("vampirismmaid")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.literal("vampire")
                        .executes(context -> setVampire(context.getSource(), true)))
                    .then(Commands.literal("none")
                        .executes(context -> setVampire(context.getSource(), false)))
                )
                .then(Commands.literal("level")
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                        .executes(context -> {
                            int level = IntegerArgumentType.getInteger(context, "level");
                            return setLevel(context.getSource(), level);
                        })
                    )
                )
        );
    }

    private static int setVampire(CommandSourceStack source, boolean vampire) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("该指令只能由玩家执行"));
            return 0;
        }

        Entity target = getTargetEntity(player);

        if (target == null) {
            source.sendFailure(Component.literal("没有指向任何实体"));
            return 0;
        }

        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (entityKey == null || !"touhou_little_maid".equals(entityKey.getNamespace())) {
            source.sendFailure(Component.literal("目标不是女仆实体"));
            return 0;
        }

        VampireMaidCapability cap = target.getData(ModAttachments.VAMPIRE_MAID.get());
        if (vampire) {
            cap.setVampire(true);
            cap.setVampireLevel(1);
            source.sendSuccess(() -> Component.literal("已将女仆转变为吸血鬼"), true);
        } else {
            cap.setVampire(false);
            cap.setVampireLevel(0);
            cap.setHadSanguinare(false);
            source.sendSuccess(() -> Component.literal("已将女仆恢复为普通状态"), true);
        }

        PacketDistributor.sendToPlayersTrackingEntity(target,
                new SyncVampireMaidPacket(target.getId(), cap.isVampire(), cap.getVampireLevel())
        );

        return 1;
    }

    private static int setLevel(CommandSourceStack source, int level) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("该指令只能由玩家执行"));
            return 0;
        }

        Entity target = getTargetEntity(player);

        if (target == null) {
            source.sendFailure(Component.literal("没有指向任何实体"));
            return 0;
        }

        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (entityKey == null || !"touhou_little_maid".equals(entityKey.getNamespace())) {
            source.sendFailure(Component.literal("目标不是女仆实体"));
            return 0;
        }

        VampireMaidCapability cap = target.getData(ModAttachments.VAMPIRE_MAID.get());
        if (!cap.isVampire()) {
            source.sendFailure(Component.translatable("message.touhou_little_maid_vampirism.not_vampire"));
            return 0;
        }

        cap.setVampireLevel(level);
        if (target instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid) {
            VampireLevelManager.applyLevel(maid, level);
        }

        PacketDistributor.sendToPlayersTrackingEntity(target,
                new SyncVampireMaidPacket(target.getId(), cap.isVampire(), cap.getVampireLevel())
        );

        source.sendSuccess(() -> Component.translatable("message.touhou_little_maid_vampirism.level_set", level), true);
        return 1;
    }

    private static Entity getTargetEntity(ServerPlayer player) {
        double reach = 5.0;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(reach));
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(reach)).inflate(1.0);

        Entity closest = null;
        double closestDist = reach * reach;

        for (Entity entity : player.level().getEntities(player, searchBox)) {
            AABB entityBB = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hit = entityBB.clip(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closest = entity;
                    closestDist = dist;
                }
            }
        }

        return closest;
    }
}
