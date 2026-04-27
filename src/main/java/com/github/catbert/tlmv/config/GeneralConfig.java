package com.github.catbert.tlmv.config;

import com.github.catbert.tlmv.config.subconfig.BloodConfig;
import com.github.catbert.tlmv.config.subconfig.InfectionConfig;
import com.github.catbert.tlmv.config.subconfig.SunDamageConfig;
import com.github.catbert.tlmv.config.subconfig.TaskConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public final class GeneralConfig {
    public static ForgeConfigSpec CONFIG;

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        InfectionConfig.init(builder);
        SunDamageConfig.init(builder);
        BloodConfig.init(builder);
        TaskConfig.init(builder);
        CONFIG = builder.build();
        return CONFIG;
    }
}
