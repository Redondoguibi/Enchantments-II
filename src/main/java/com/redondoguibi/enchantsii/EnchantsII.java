package com.redondoguibi.enchantsii;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EnchantsII.MOD_ID)
public final class EnchantsII {
    public static final String MOD_ID = "enchantsii";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnchantsII(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Loading Enchants II");
    }
}
