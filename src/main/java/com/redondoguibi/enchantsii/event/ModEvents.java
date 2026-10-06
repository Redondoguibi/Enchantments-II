package com.redondoguibi.enchantsii.event;

import com.redondoguibi.enchantsii.menu.EnchantsMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class ModEvents {
    private ModEvents() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));

        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (serverPlayer.isSpectator()) {
            return;
        }

        var pos = event.getPos().immutable();
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, player) -> new EnchantsMenu(containerId, inventory, pos),
                        Component.translatable("menu.enchantsii.enchanting")
                ),
                buffer -> buffer.writeBlockPos(pos)
        );
    }

    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (event.getLeft().is(Items.ENCHANTED_BOOK)) {
            return;
        }

        if (event.getRight().is(Items.ENCHANTED_BOOK)) {
            event.setCanceled(true);
            return;
        }

        if (!event.getOutput().isEmpty()
                && !event.getOutput().getEnchantments().equals(event.getLeft().getEnchantments())) {
            event.setCanceled(true);
        }
    }
}
