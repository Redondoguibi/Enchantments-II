package com.redondoguibi.enchantsii;

import com.mojang.logging.LogUtils;
import com.redondoguibi.enchantsii.event.ModEvents;
import com.redondoguibi.enchantsii.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(EnchantsII.MOD_ID)
public final class EnchantsII {
    public static final String MOD_ID = "enchantsii";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnchantsII(IEventBus modEventBus, ModContainer modContainer) {
        ModMenus.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(ModEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ModEvents::onAnvilUpdate);

        LOGGER.info("Loading Enchants II");
    }
}
