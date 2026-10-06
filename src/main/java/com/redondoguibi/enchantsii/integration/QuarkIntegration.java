package com.redondoguibi.enchantsii.integration;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

public final class QuarkIntegration {
    private static final ResourceLocation ANCIENT_TOME_ID =
            ResourceLocation.fromNamespaceAndPath("quark", "ancient_tome");
    private static final ResourceLocation TOME_ENCHANTMENTS_ID =
            ResourceLocation.fromNamespaceAndPath("quark", "tome_enchantments");

    private QuarkIntegration() {
    }

    public static boolean isAncientTome(ItemStack stack) {
        return !stack.isEmpty()
                && ANCIENT_TOME_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Nullable
    public static ItemEnchantments getTomeEnchantments(ItemStack stack) {
        if (!isAncientTome(stack)) {
            return null;
        }

        DataComponentType<?> rawType =
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(TOME_ENCHANTMENTS_ID);
        if (rawType == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        DataComponentType<ItemEnchantments> tomeType =
                (DataComponentType<ItemEnchantments>) rawType;

        return stack.get(tomeType);
    }
}
