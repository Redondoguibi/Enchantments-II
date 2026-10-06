package com.redondoguibi.enchantsii.client;

import com.redondoguibi.enchantsii.menu.EnchantingLogic;
import com.redondoguibi.enchantsii.menu.EnchantsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class EnchantsScreen extends AbstractContainerScreen<EnchantsMenu> {
    private static final int ROWS_PER_PAGE = 3;
    private static final int ROW_X = 58;
    private static final int ROW_Y = 18;
    private static final int ROW_WIDTH = 182;
    private static final int ROW_HEIGHT = 28;
    private static final int ROW_STEP = 30;
    private static final int NAV_Y = 111;

    private final Inventory playerInventory;
    private int page;

    public EnchantsScreen(EnchantsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.playerInventory = playerInventory;
        this.imageWidth = 248;
        this.imageHeight = 210;
        this.inventoryLabelY = 120;
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        int pageCount = getPageCount();
        if (page >= pageCount) {
            page = Math.max(0, pageCount - 1);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderEnchantments(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xF0181818);
        graphics.fill(left + 1, top + 1, left + imageWidth - 1, top + imageHeight - 1, 0xFF2A2A2A);

        drawSlot(graphics, left + 22, top + 35);
        drawSlot(graphics, left + 22, top + 64);

        graphics.fill(left + 54, top + 12, left + 244, top + 129, 0xFF171717);
        graphics.fill(left + 55, top + 13, left + 243, top + 128, 0xFF222222);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFE0E0E0, false);
        graphics.drawString(font, Component.translatable("screen.enchantsii.item"), 8, 39, 0xFFB8B8B8, false);
        graphics.drawString(font, Component.translatable("screen.enchantsii.book"), 8, 68, 0xFFB8B8B8, false);
        graphics.drawString(font, playerInventory.getDisplayName(), 8, inventoryLabelY, 0xFFB8B8B8, false);

        if (minecraft != null && minecraft.player != null) {
            int xp = minecraft.player.totalExperience;
            int lapis = EnchantingLogic.countLapis(minecraft.player.getInventory());

            graphics.drawString(
                    font,
                    Component.translatable("screen.enchantsii.xp", xp, EnchantingLogic.XP_COST),
                    8,
                    92,
                    xp >= EnchantingLogic.XP_COST || minecraft.player.getAbilities().instabuild
                            ? 0xFF80FF80
                            : 0xFFFF8080,
                    false
            );

            graphics.drawString(
                    font,
                    Component.translatable("screen.enchantsii.lapis", lapis),
                    8,
                    104,
                    0xFF80BFFF,
                    false
            );
        }
    }

    private void renderEnchantments(GuiGraphics graphics, int mouseX, int mouseY) {
        List<EnchantingLogic.BookEnchantment> enchantments = menu.getBookEnchantments();

        if (enchantments.isEmpty()) {
            Component message = menu.getBookStack().isEmpty()
                    ? Component.translatable("screen.enchantsii.insert_book")
                    : Component.translatable("screen.enchantsii.empty_book");
            graphics.drawString(font, message, leftPos + 64, topPos + 22, 0xFFAAAAAA, false);
            return;
        }

        int start = page * ROWS_PER_PAGE;
        int end = Math.min(start + ROWS_PER_PAGE, enchantments.size());

        for (int absoluteIndex = start; absoluteIndex < end; absoluteIndex++) {
            int row = absoluteIndex - start;
            int x = leftPos + ROW_X;
            int y = topPos + ROW_Y + row * ROW_STEP;

            EnchantingLogic.BookEnchantment source = enchantments.get(absoluteIndex);
            EnchantingLogic.Evaluation evaluation = EnchantingLogic.evaluate(menu.getTargetStack(), source);

            boolean hovered = isInside(mouseX, mouseY, x, y, ROW_WIDTH, ROW_HEIGHT);
            boolean affordable = minecraft != null
                    && minecraft.player != null
                    && EnchantingLogic.canAfford(minecraft.player, evaluation);

            int background = hovered ? 0xFF3B3B3B : 0xFF303030;
            if (evaluation.ready() && affordable) {
                background = hovered ? 0xFF34513A : 0xFF2A432F;
            }

            graphics.fill(x, y, x + ROW_WIDTH, y + ROW_HEIGHT, background);

            Component name = Enchantment.getFullname(source.enchantment(), source.sourceLevel());
            graphics.drawString(
                    font,
                    trim(name),
                    x + 4,
                    y + 3,
                    evaluation.ready() ? 0xFFF0F0F0 : 0xFFB0B0B0,
                    false
            );

            graphics.drawString(
                    font,
                    trim(statusText(evaluation)),
                    x + 4,
                    y + 15,
                    statusColor(evaluation, affordable),
                    false
            );
        }

        int pageCount = getPageCount();
        if (pageCount > 1) {
            Component previous = Component.literal(page > 0 ? "<" : "-");
            Component next = Component.literal(page + 1 < pageCount ? ">" : "-");
            Component pageText = Component.translatable("screen.enchantsii.page", page + 1, pageCount);

            graphics.drawString(font, previous, leftPos + 62, topPos + NAV_Y, 0xFFE0E0E0, false);
            graphics.drawCenteredString(font, pageText, leftPos + 149, topPos + NAV_Y, 0xFFB8B8B8);
            graphics.drawString(font, next, leftPos + 231, topPos + NAV_Y, 0xFFE0E0E0, false);
        }
    }

    private Component statusText(EnchantingLogic.Evaluation evaluation) {
        return switch (evaluation.status()) {
            case READY -> Component.translatable(
                    "screen.enchantsii.ready",
                    evaluation.currentLevel(),
                    evaluation.nextLevel(),
                    EnchantingLogic.XP_COST,
                    evaluation.lapisCost()
            );
            case NO_TARGET -> Component.translatable("screen.enchantsii.insert_item");
            case INVALID_BOOK -> Component.translatable("screen.enchantsii.invalid_book");
            case NOT_SUPPORTED -> Component.translatable("screen.enchantsii.not_supported");
            case CONFLICT -> Component.translatable("screen.enchantsii.conflict");
            case MAXED -> Component.translatable("screen.enchantsii.maxed", evaluation.maxLevel());
        };
    }

    private int statusColor(EnchantingLogic.Evaluation evaluation, boolean affordable) {
        if (evaluation.status() == EnchantingLogic.Status.READY) {
            return affordable ? 0xFF9CFF9C : 0xFFFF9C9C;
        }

        if (evaluation.status() == EnchantingLogic.Status.MAXED) {
            return 0xFFFFD36A;
        }

        return 0xFFFF8A8A;
    }

    private Component trim(Component component) {
        String text = component.getString();
        String trimmed = font.plainSubstrByWidth(text, ROW_WIDTH - 8);
        return Component.literal(trimmed);
    }

    private int getPageCount() {
        int size = menu.getBookEnchantments().size();
        return Math.max(1, (size + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<EnchantingLogic.BookEnchantment> enchantments = menu.getBookEnchantments();
            int start = page * ROWS_PER_PAGE;
            int end = Math.min(start + ROWS_PER_PAGE, enchantments.size());

            for (int absoluteIndex = start; absoluteIndex < end; absoluteIndex++) {
                int row = absoluteIndex - start;
                int x = leftPos + ROW_X;
                int y = topPos + ROW_Y + row * ROW_STEP;

                if (isInside(mouseX, mouseY, x, y, ROW_WIDTH, ROW_HEIGHT)) {
                    EnchantingLogic.Evaluation evaluation =
                            EnchantingLogic.evaluate(menu.getTargetStack(), enchantments.get(absoluteIndex));

                    if (minecraft != null
                            && minecraft.player != null
                            && EnchantingLogic.canAfford(minecraft.player, evaluation)
                            && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, absoluteIndex);
                    }

                    return true;
                }
            }

            int pageCount = getPageCount();
            if (pageCount > 1) {
                if (page > 0 && isInside(mouseX, mouseY, leftPos + 58, topPos + NAV_Y - 2, 18, 14)) {
                    page--;
                    return true;
                }

                if (page + 1 < pageCount
                        && isInside(mouseX, mouseY, leftPos + 226, topPos + NAV_Y - 2, 18, 14)) {
                    page++;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF101010);
        graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 16, y + 16, 0xFF373737);
    }
}
