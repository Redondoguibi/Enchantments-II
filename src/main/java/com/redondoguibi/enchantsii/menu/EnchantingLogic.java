package com.redondoguibi.enchantsii.menu;

import com.redondoguibi.enchantsii.integration.QuarkIntegration;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EnchantingLogic {
    public static final int XP_COST = 150;

    private EnchantingLogic() {
    }

    public static boolean isEnchantingSource(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
            return stored != null && !stored.isEmpty();
        }

        if (QuarkIntegration.isAncientTome(stack)) {
            ItemEnchantments stored = QuarkIntegration.getTomeEnchantments(stack);
            return stored != null && !stored.isEmpty();
        }

        return false;
    }

    public static List<BookEnchantment> getBookEnchantments(ItemStack book) {
        ItemEnchantments stored;
        SourceType sourceType;

        if (book.is(Items.ENCHANTED_BOOK)) {
            stored = book.get(DataComponents.STORED_ENCHANTMENTS);
            sourceType = SourceType.ENCHANTED_BOOK;
        } else if (QuarkIntegration.isAncientTome(book)) {
            stored = QuarkIntegration.getTomeEnchantments(book);
            sourceType = SourceType.QUARK_ANCIENT_TOME;
        } else {
            return List.of();
        }

        if (stored == null || stored.isEmpty()) {
            return List.of();
        }

        List<BookEnchantment> result = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            if (entry.getIntValue() > 0) {
                result.add(new BookEnchantment(
                        entry.getKey(),
                        entry.getIntValue(),
                        sourceType
                ));
            }
        }

        result.sort(Comparator.comparing(entry -> key(entry.enchantment())));
        return List.copyOf(result);
    }

    public static Evaluation evaluate(ItemStack target, BookEnchantment source) {
        int normalMaxLevel = source.enchantment().value().getMaxLevel();
        int maxLevel = source.sourceType() == SourceType.QUARK_ANCIENT_TOME
                ? normalMaxLevel + 1
                : Math.min(source.sourceLevel(), normalMaxLevel);

        if (target.isEmpty()) {
            return new Evaluation(Status.NO_TARGET, 0, 0, maxLevel, 0);
        }

        if (maxLevel <= 0) {
            return new Evaluation(Status.INVALID_BOOK, 0, 0, 0, 0);
        }

        int currentLevel = target.getEnchantmentLevel(source.enchantment());

        if (!target.supportsEnchantment(source.enchantment())) {
            return new Evaluation(Status.NOT_SUPPORTED, currentLevel, 0, maxLevel, 0);
        }

        if (source.sourceType() == SourceType.QUARK_ANCIENT_TOME && currentLevel <= 0) {
            return new Evaluation(
                    Status.TOME_REQUIRES_ENCHANTMENT,
                    currentLevel,
                    0,
                    maxLevel,
                    0
            );
        }

        if (currentLevel >= maxLevel) {
            return new Evaluation(
                    Status.MAXED,
                    currentLevel,
                    currentLevel,
                    maxLevel,
                    currentLevel + 1
            );
        }

        if (currentLevel == 0
                && !EnchantmentHelper.isEnchantmentCompatible(
                target.getTagEnchantments().keySet(),
                source.enchantment()
        )) {
            return new Evaluation(Status.CONFLICT, 0, 0, maxLevel, 0);
        }

        int nextLevel = currentLevel + 1;
        return new Evaluation(Status.READY, currentLevel, nextLevel, maxLevel, nextLevel + 1);
    }

    public static boolean canAfford(Player player, Evaluation evaluation) {
        if (!evaluation.ready()) {
            return false;
        }

        if (player.getAbilities().instabuild) {
            return true;
        }

        return player.totalExperience >= XP_COST
                && countLapis(player.getInventory()) >= evaluation.lapisCost();
    }

    public static int countLapis(Inventory inventory) {
        int count = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.LAPIS_LAZULI)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static void consumeResources(Player player, Evaluation evaluation) {
        if (player.getAbilities().instabuild) {
            return;
        }

        removeLapis(player.getInventory(), evaluation.lapisCost());
        player.giveExperiencePoints(-XP_COST);
    }

    private static void removeLapis(Inventory inventory, int amount) {
        int remaining = amount;

        for (int slot = 0; slot < Inventory.INVENTORY_SIZE && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(Items.LAPIS_LAZULI)) {
                continue;
            }

            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;

            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        inventory.setChanged();
    }

    private static String key(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey()
                .map(resourceKey -> resourceKey.location().toString())
                .orElse(enchantment.value().description().getString());
    }

    public record BookEnchantment(
            Holder<Enchantment> enchantment,
            int sourceLevel,
            SourceType sourceType
    ) {
    }

    public record Evaluation(
            Status status,
            int currentLevel,
            int nextLevel,
            int maxLevel,
            int lapisCost
    ) {
        public boolean ready() {
            return status == Status.READY;
        }
    }

    public enum SourceType {
        ENCHANTED_BOOK,
        QUARK_ANCIENT_TOME
    }

    public enum Status {
        READY,
        NO_TARGET,
        INVALID_BOOK,
        NOT_SUPPORTED,
        CONFLICT,
        TOME_REQUIRES_ENCHANTMENT,
        MAXED
    }
}
