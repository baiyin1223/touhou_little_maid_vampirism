package com.github.catbert.tlmv.config;

import com.github.catbert.tlmv.config.subconfig.BloodConfig;
import com.github.catbert.tlmv.config.subconfig.InfectionConfig;
import com.github.catbert.tlmv.config.subconfig.LevelingConfig;
import com.github.catbert.tlmv.config.subconfig.SunDamageConfig;
import com.github.catbert.tlmv.config.subconfig.TaskConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class GeneralConfig {
    public static ModConfigSpec CONFIG;

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        InfectionConfig.init(builder);
        SunDamageConfig.init(builder);
        BloodConfig.init(builder);
        TaskConfig.init(builder);
        LevelingConfig.init(builder);
        CONFIG = builder.build();
        return CONFIG;
    }
}
