package com.github.catbert.tlmv.compat.jade;

import com.github.catbert.tlmv.TLMVMain;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(TLMVMain.MOD_ID)
public class JadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(VampireMaidComponentProvider.INSTANCE, EntityMaid.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(VampireMaidComponentProvider.INSTANCE, EntityMaid.class);
    }
}
