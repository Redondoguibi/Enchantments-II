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

    // Cost boxes drawn into enchanting_table_2.png.
    // Top: raw XP points. Bottom: lapis lazuli.
    private static final int COST_BOX_CENTER_X = 141;
    private static final int XP_TEXT_Y = 29;
    private static final int LAPIS_TEXT_Y = 48;

    // Free strip between the custom controls and the player inventory.
    // It is used to select/apply an enchantment while preserving support
    // for enchanted books that contain more than one enchantment.
    private static final int SELECT_Y = 65;
    private static final int SELECT_HEIGHT = 17;
    private static final int SELECT_LEFT_ARROW_X = 8;
    private static final int SELECT_RIGHT_ARROW_X = 158;
    private static final int SELECT_ARROW_WIDTH = 10;
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
        // Intentionally omit the vanilla title/inventory labels so the layout
        // stays identical to the supplied texture.
    }

    private void renderEnchantingControls(GuiGraphics graphics, int mouseX, int mouseY) {
        List<EnchantingLogic.BookEnchantment> enchantments = menu.getBookEnchantments();

        if (enchantments.isEmpty()) {
            drawCost(graphics, "-", false, XP_TEXT_Y);
            drawCost(graphics, "-", false, LAPIS_TEXT_Y);

            Component message = menu.getBookStack().isEmpty()
                    ? Component.translatable("screen.enchantsii.insert_book")
                    : Component.translatable("screen.enchantsii.empty_book");
            drawSelectionText(graphics, message, 0xFF606060);
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

        // Raw XP is a fixed price whenever there is a valid enchanted-book source.
        drawCost(
                graphics,
                Integer.toString(EnchantingLogic.XP_COST),
                xpAffordable,
                XP_TEXT_Y
        );

        // Lapis depends on the next level being applied, so only show a numeric
        // price when the current item/enchantment combination can actually be applied.
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

        int selectionColor = selectionColor(evaluation, xpAffordable && lapisAffordable);
        drawSelectionText(graphics, enchantmentName, selectionColor);

        if (enchantments.size() > 1) {
            int arrowColor = 0xFF404040;
            graphics.drawString(
                    font,
                    selectedEnchantment > 0 ? "<" : "-",
                    leftPos + SELECT_LEFT_ARROW_X,
                    topPos + SELECT_Y + 4,
                    arrowColor,
                    false
            );
            graphics.drawString(
                    font,
                    selectedEnchantment + 1 < enchantments.size() ? ">" : "-",
                    leftPos + SELECT_RIGHT_ARROW_X,
                    topPos + SELECT_Y + 4,
                    arrowColor,
                    false
            );
        }
    }

    private void drawCost(GuiGraphics graphics, String value, boolean affordable, int relativeY) {
        int color = affordable ? 0xFF40372A : 0xFF9A3535;
        graphics.drawCenteredString(
                font,
                value,
                leftPos + COST_BOX_CENTER_X,
                topPos + relativeY,
                color
        );
    }

    private void drawSelectionText(GuiGraphics graphics, Component text, int color) {
        String trimmed = font.plainSubstrByWidth(text.getString(), SELECT_TEXT_WIDTH);
        graphics.drawCenteredString(
                font,
                Component.literal(trimmed),
                leftPos + SELECT_TEXT_X + SELECT_TEXT_WIDTH / 2,
                topPos + SELECT_Y + 4,
                color
        );
    }

    private int selectionColor(EnchantingLogic.Evaluation evaluation, boolean affordable) {
        return switch (evaluation.status()) {
            case READY -> affordable ? 0xFF3F513A : 0xFF9A3535;
            case MAXED -> 0xFF8A681E;
            default -> 0xFF7A3A3A;
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
