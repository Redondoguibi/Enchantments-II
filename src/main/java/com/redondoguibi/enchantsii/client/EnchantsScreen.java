package com.redondoguibi.enchantsii.client;

import com.redondoguibi.enchantsii.EnchantsII;
import com.redondoguibi.enchantsii.menu.EnchantingLogic;
import com.redondoguibi.enchantsii.menu.EnchantsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class EnchantsScreen extends AbstractContainerScreen<EnchantsMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(
                    EnchantsII.MOD_ID,
                    "textures/gui/enchanting_table_2.png"
            );

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int COST_BOX_CENTER_X = 141;
    private static final int XP_TEXT_Y = 29;
    private static final int LAPIS_TEXT_Y = 48;

    private static final int SELECT_Y = 63;
    private static final int SELECT_HEIGHT = 18;
    private static final int SELECT_LEFT_ARROW_X = 7;
    private static final int SELECT_RIGHT_ARROW_X = 160;
    private static final int SELECT_ARROW_WIDTH = 9;
    private static final int SELECT_TEXT_X = 20;
    private static final int SELECT_TEXT_WIDTH = 136;

    private int selectedEnchantment;

    public EnchantsScreen(EnchantsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        clampSelection();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderEnchantingControls(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The custom texture already identifies both input slots with icons.
    }

    private void renderEnchantingControls(GuiGraphics graphics, int mouseX, int mouseY) {
        List<EnchantingLogic.BookEnchantment> enchantments = menu.getBookEnchantments();

        if (enchantments.isEmpty()) {
            drawCost(graphics, "-", false, XP_TEXT_Y);
            drawCost(graphics, "-", false, LAPIS_TEXT_Y);

            Component message = menu.getBookStack().isEmpty()
                    ? Component.translatable("screen.enchantsii.insert_book")
                    : Component.translatable("screen.enchantsii.empty_book");

            drawDisabledMessage(graphics, message);
            return;
        }

        clampSelection();
        EnchantingLogic.BookEnchantment source = enchantments.get(selectedEnchantment);
        EnchantingLogic.Evaluation evaluation =
                EnchantingLogic.evaluate(menu.getTargetStack(), source);

        boolean hasPlayer = minecraft != null && minecraft.player != null;
        boolean creative = hasPlayer && minecraft.player.getAbilities().instabuild;
        boolean xpAffordable = creative
                || (hasPlayer && minecraft.player.totalExperience >= EnchantingLogic.XP_COST);
        boolean lapisAffordable = creative
                || (hasPlayer
                && evaluation.ready()
                && EnchantingLogic.countLapis(minecraft.player.getInventory()) >= evaluation.lapisCost());

        boolean canEnchant = evaluation.ready() && xpAffordable && lapisAffordable;

        drawCost(
                graphics,
                Integer.toString(EnchantingLogic.XP_COST),
                xpAffordable,
                XP_TEXT_Y
        );

        drawCost(
                graphics,
                evaluation.ready() ? Integer.toString(evaluation.lapisCost()) : "-",
                evaluation.ready() && lapisAffordable,
                LAPIS_TEXT_Y
        );

        Component enchantmentName = Enchantment.getFullname(
                source.enchantment(),
                evaluation.ready() ? evaluation.nextLevel() : evaluation.maxLevel()
        );

        boolean hovered = isInside(
                mouseX,
                mouseY,
                leftPos + SELECT_TEXT_X,
                topPos + SELECT_Y,
                SELECT_TEXT_WIDTH,
                SELECT_HEIGHT
        );

        drawEnchantButton(graphics, enchantmentName, canEnchant, hovered);

        if (enchantments.size() > 1) {
            int arrowColor = 0xFF3A332B;
            graphics.drawString(
                    font,
                    selectedEnchantment > 0 ? "<" : "-",
                    leftPos + SELECT_LEFT_ARROW_X,
                    topPos + SELECT_Y + 5,
                    arrowColor,
                    false
            );
            graphics.drawString(
                    font,
                    selectedEnchantment + 1 < enchantments.size() ? ">" : "-",
                    leftPos + SELECT_RIGHT_ARROW_X,
                    topPos + SELECT_Y + 5,
                    arrowColor,
                    false
            );
        }

        if (hovered) {
            Component tooltip = canEnchant
                    ? Component.translatable("screen.enchantsii.tooltip.click")
                    : blockedReason(evaluation, xpAffordable, lapisAffordable);
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void drawCost(GuiGraphics graphics, String value, boolean affordable, int relativeY) {
        int color = affordable ? 0xFF2F5A2F : 0xFFA12E2E;
        graphics.drawCenteredString(
                font,
                value,
                leftPos + COST_BOX_CENTER_X,
                topPos + relativeY,
                color
        );
    }

    private void drawEnchantButton(
            GuiGraphics graphics,
            Component enchantmentName,
            boolean enabled,
            boolean hovered
    ) {
        int x = leftPos + SELECT_TEXT_X;
        int y = topPos + SELECT_Y;
        int width = SELECT_TEXT_WIDTH;

        int border;
        int background;
        int textColor;

        if (enabled) {
            border = hovered ? 0xFF446D3D : 0xFF6D7A5B;
            background = hovered ? 0xFFC7D5B7 : 0xFFB8BEA6;
            textColor = hovered ? 0xFF183B18 : 0xFF2B4328;
        } else {
            border = hovered ? 0xFF8A5A55 : 0xFF81716C;
            background = hovered ? 0xFFCDB6B0 : 0xFFC0B5B0;
            textColor = 0xFF75504A;
        }

        graphics.fill(x, y, x + width, y + SELECT_HEIGHT, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + SELECT_HEIGHT - 1, background);

        String trimmedName = font.plainSubstrByWidth(enchantmentName.getString(), width - 12);
        graphics.drawCenteredString(
                font,
                Component.literal(trimmedName),
                x + width / 2,
                y + 5,
                textColor
        );
    }

    private void drawDisabledMessage(GuiGraphics graphics, Component message) {
        int x = leftPos + SELECT_TEXT_X;
        int y = topPos + SELECT_Y;
        int width = SELECT_TEXT_WIDTH;

        graphics.fill(x, y, x + width, y + SELECT_HEIGHT, 0xFF777777);
        graphics.fill(x + 1, y + 1, x + width - 1, y + SELECT_HEIGHT - 1, 0xFFC1C1C1);

        String trimmed = font.plainSubstrByWidth(message.getString(), width - 8);
        graphics.drawCenteredString(
                font,
                Component.literal(trimmed),
                x + width / 2,
                y + 5,
                0xFF555555
        );
    }

    private Component blockedReason(
            EnchantingLogic.Evaluation evaluation,
            boolean xpAffordable,
            boolean lapisAffordable
    ) {
        if (evaluation.ready()) {
            if (!xpAffordable && !lapisAffordable) {
                return Component.translatable(
                        "screen.enchantsii.tooltip.need_both",
                        EnchantingLogic.XP_COST,
                        evaluation.lapisCost()
                );
            }

            if (!xpAffordable) {
                return Component.translatable(
                        "screen.enchantsii.tooltip.need_xp",
                        EnchantingLogic.XP_COST
                );
            }

            if (!lapisAffordable) {
                return Component.translatable(
                        "screen.enchantsii.tooltip.need_lapis",
                        evaluation.lapisCost()
                );
            }
        }

        return switch (evaluation.status()) {
            case NO_TARGET -> Component.translatable("screen.enchantsii.insert_item");
            case INVALID_BOOK -> Component.translatable("screen.enchantsii.invalid_book");
            case NOT_SUPPORTED -> Component.translatable("screen.enchantsii.not_supported");
            case CONFLICT -> Component.translatable("screen.enchantsii.conflict");
            case TOME_REQUIRES_ENCHANTMENT ->
                    Component.translatable("screen.enchantsii.tome_requires_enchantment");
            case MAXED -> Component.translatable(
                    "screen.enchantsii.maxed",
                    evaluation.maxLevel()
            );
            case READY -> Component.translatable("screen.enchantsii.cannot_enchant");
        };
    }

    private void clampSelection() {
        int size = menu.getBookEnchantments().size();
        if (size <= 0) {
            selectedEnchantment = 0;
            return;
        }

        if (selectedEnchantment >= size) {
            selectedEnchantment = size - 1;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<EnchantingLogic.BookEnchantment> enchantments = menu.getBookEnchantments();

            if (!enchantments.isEmpty()) {
                clampSelection();

                if (enchantments.size() > 1) {
                    if (selectedEnchantment > 0
                            && isInside(
                            mouseX,
                            mouseY,
                            leftPos + SELECT_LEFT_ARROW_X - 2,
                            topPos + SELECT_Y,
                            SELECT_ARROW_WIDTH + 4,
                            SELECT_HEIGHT
                    )) {
                        selectedEnchantment--;
                        return true;
                    }

                    if (selectedEnchantment + 1 < enchantments.size()
                            && isInside(
                            mouseX,
                            mouseY,
                            leftPos + SELECT_RIGHT_ARROW_X - 2,
                            topPos + SELECT_Y,
                            SELECT_ARROW_WIDTH + 4,
                            SELECT_HEIGHT
                    )) {
                        selectedEnchantment++;
                        return true;
                    }
                }

                if (isInside(
                        mouseX,
                        mouseY,
                        leftPos + SELECT_TEXT_X,
                        topPos + SELECT_Y,
                        SELECT_TEXT_WIDTH,
                        SELECT_HEIGHT
                )) {
                    EnchantingLogic.Evaluation evaluation =
                            EnchantingLogic.evaluate(
                                    menu.getTargetStack(),
                                    enchantments.get(selectedEnchantment)
                            );

                    if (minecraft != null
                            && minecraft.player != null
                            && minecraft.gameMode != null
                            && EnchantingLogic.canAfford(minecraft.player, evaluation)) {
                        minecraft.gameMode.handleInventoryButtonClick(
                                menu.containerId,
                                selectedEnchantment
                        );
                    }

                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isInside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }
}
