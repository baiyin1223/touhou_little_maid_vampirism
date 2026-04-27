package com.github.catbert.tlmv.capability;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = TLMVMain.MOD_ID)
public class ModCapabilities {
    public static final Capability<VampireMaidCapability> VAMPIRE_MAID = CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation VAMPIRE_MAID_CAP_ID = new ResourceLocation(TLMVMain.MOD_ID, "vampire_maid");

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(VampireMaidCapability.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!ModList.get().isLoaded("touhou_little_maid")) {
            return;
        }

        Entity entity = event.getObject();
        ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityKey != null && "touhou_little_maid".equals(entityKey.getNamespace())) {
            event.addCapability(VAMPIRE_MAID_CAP_ID, new VampireMaidCapabilityProvider());
        }
    }

    public static LazyOptional<VampireMaidCapability> getVampireMaid(Entity entity) {
        return entity.getCapability(VAMPIRE_MAID);
    }
}
