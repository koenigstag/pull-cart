package com.koenigstag.pull_cart.client;

import com.koenigstag.pull_cart.LoomMenu;
import com.koenigstag.pull_cart.PullCartMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** MC+ Loom's GuiLoom: the spindles' time left as the needle over them, the spinning as the arrow. */
public class LoomScreen extends AbstractContainerScreen<LoomMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PullCartMod.MOD_ID, "textures/gui/container/wooden_loom.png");

    public LoomScreen(LoomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        if (menu.isBurning()) {
            int left = menu.burnLeft(12);
            graphics.blit(TEXTURE, x + 57, y + 36 + 12 - left, 176, 12 - left, 14, left + 2);
        }
        graphics.blit(TEXTURE, x + 79, y + 34, 176, 14, menu.spinProgress(24) + 1, 16);
    }
}
