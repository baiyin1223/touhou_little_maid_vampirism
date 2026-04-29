package com.github.catbert.tlmv.capability;

import com.github.catbert.tlmv.TLMVMain;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TLMVMain.MOD_ID);

    public static final Supplier<AttachmentType<VampireMaidCapability>> VAMPIRE_MAID =
            ATTACHMENT_TYPES.register("vampire_maid",
                    () -> AttachmentType.builder(VampireMaidCapability::new)
                            .serialize(VampireMaidCapability.CODEC)
                            .build());
}
