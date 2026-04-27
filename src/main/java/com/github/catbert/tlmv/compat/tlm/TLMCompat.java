package com.github.catbert.tlmv.compat.tlm;

import com.github.catbert.tlmv.TLMVMain;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.util.List;

public class TLMCompat {

    private static final String TLM_EXTENSION_CLASS = "com.github.catbert.tlmv.compat.tlm.TLMExtension";
    private static boolean loaded = false;

    public static void init() {
        loaded = ModList.get().isLoaded("touhou_little_maid");
        if (loaded) {
            TLMVMain.LOGGER.info("TouhouLittleMaid compatibility initialized");
            safelyRegisterExtension();
        }
    }

    private static void safelyRegisterExtension() {
        try {
            Class<?> tlmClass = Class.forName("com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid");
            Field extensionsField = tlmClass.getField("EXTENSIONS");
            @SuppressWarnings("unchecked")
            List<Object> extensions = (List<Object>) extensionsField.get(null);

            boolean alreadyRegistered = extensions.stream()
                    .anyMatch(e -> e.getClass().getName().equals(TLM_EXTENSION_CLASS));
            if (!alreadyRegistered) {
                Class<?> extensionClass = Class.forName(TLM_EXTENSION_CLASS);
                extensions.add(extensionClass.getDeclaredConstructor().newInstance());
                TLMVMain.LOGGER.info("Manually registered TLMExtension to TouhouLittleMaid.EXTENSIONS");
            }
        } catch (Exception e) {
            TLMVMain.LOGGER.warn("Failed to manually register TLM extension: {}", e.getMessage());
        }
    }

    public static boolean isTLMLoaded() {
        return loaded;
    }
}
