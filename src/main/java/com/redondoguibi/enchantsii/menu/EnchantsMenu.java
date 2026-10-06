package com.redondoguibi.enchantsii.menu;

import com.redondoguibi.enchantsii.registry.ModMenus;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public final class EnchantsMenu extends AbstractContainerMenu {
    private static final int TARGET_SLOT = 0;
    private static final int BOOK_SLOT = 1;

    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_MAIN_END = PLAYER_INV_START + 27;
    private static final int PLAYER_INV_END = PLAYER_MAIN_END + 9;

    private final Container inputs;
    private final BlockPos tablePos;

    public EnchantsMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    public EnchantsMenu(int containerId, Inventory playerInventory, BlockPos tablePos) {
        super(ModMenus.ENCHANTS_MENU.get(), containerId);
        this.tablePos = tablePos.immutable();

        this.inputs = new SimpleContainer(2) {
            @Override
            public void setChanged() {
                super.setChanged();
                EnchantsMenu.this.slotsChanged(this);
            }
        };

        addSlot(new Slot(inputs, TARGET_SLOT, 22, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.is(Items.BOOK)
                        && !stack.is(Items.ENCHANTED_BOOK)
                        && EnchantmentHelper.canStoreEnchantments(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addSlot(new Slot(inputs, BOOK_SLOT, 22, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.ENCHANTED_BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 132 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 190));
        }
    }

    public ItemStack getTargetStack() {
        return inputs.getItem(TARGET_SLOT);
    }

    public ItemStack getBookStack() {
        return inputs.getItem(BOOK_SLOT);
    }

    public List<EnchantingLogic.BookEnchantment> getBookEnchantments() {
        return EnchantingLogic.getBookEnchantments(getBookStack());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) {
            return false;
        }

        List<EnchantingLogic.BookEnchantment> enchantments = getBookEnchantments();
        if (id < 0 || id >= enchantments.size()) {
            return false;
        }

        ItemStack target = getTargetStack();
        EnchantingLogic.BookEnchantment source = enchantments.get(id);
        EnchantingLogic.Evaluation evaluation = EnchantingLogic.evaluate(target, source);

        if (!evaluation.ready() || !EnchantingLogic.canAfford(player, evaluation)) {
            return false;
        }

        EnchantmentHelper.updateEnchantments(
                target,
                mutable -> mutable.set(source.enchantment(), evaluation.nextLevel())
        );

        EnchantingLogic.consumeResources(player, evaluation);
        inputs.setChanged();
        broadcastChanges();

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardStat(Stats.ENCHANT_ITEM);
            CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, target, evaluation.nextLevel());
            serverPlayer.level().playSound(
                    null,
                    tablePos,
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.9F + serverPlayer.getRandom().nextFloat() * 0.2F
            );
        }

        return true;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.level().getBlockState(tablePos).is(Blocks.ENCHANTING_TABLE)) {
            return false;
        }

        double x = tablePos.getX() + 0.5D;
        double y = tablePos.getY() + 0.5D;
        double z = tablePos.getZ() + 0.5D;
        return player.distanceToSqr(x, y, z) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();

        if (index < PLAYER_INV_START) {
            if (!moveItemStackTo(source, PLAYER_INV_START, PLAYER_INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (source.is(Items.ENCHANTED_BOOK)) {
            if (!moveItemStackTo(source, BOOK_SLOT, BOOK_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!source.is(Items.BOOK)
                && EnchantmentHelper.canStoreEnchantments(source)
                && !source.is(Items.ENCHANTED_BOOK)) {
            if (!moveItemStackTo(source, TARGET_SLOT, TARGET_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_MAIN_END) {
            if (!moveItemStackTo(source, PLAYER_MAIN_END, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, PLAYER_INV_START, PLAYER_MAIN_END, false)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (source.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, source);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, inputs);
    }
}
