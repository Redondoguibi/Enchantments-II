package com.redondoguibi.enchantsii.registry;

import com.redondoguibi.enchantsii.EnchantsII;
import com.redondoguibi.enchantsii.menu.EnchantsMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, EnchantsII.MOD_ID);

    public static final Supplier<MenuType<EnchantsMenu>> ENCHANTS_MENU =
            MENUS.register("enchants_menu", () -> IMenuTypeExtension.create(EnchantsMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
