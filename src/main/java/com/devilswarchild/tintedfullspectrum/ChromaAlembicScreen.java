package com.devilswarchild.tintedfullspectrum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

// GUI for the Chroma Alembic: a hex field + 3 RGB sliders that stay in sync with each other and
// live-push their value to the block entity's shared "Selected" color, plus a craft section (input
// slot / progress arrow / output slot) that mirrors ChromaAlembicBlockEntity's server-authoritative
// state. See chroma_alembic_full_build.md.
//
// The progress arrow's left-to-right fill is drawn procedurally (a shaft rectangle + triangular
// head) rather than blitted from a second texture, since only one (idle-state) arrow graphic was
// supplied in alembic_gui.png -- a dedicated "filled" arrow asset would let this be a simple blit
// instead and should look better; this is a reasonable stand-in until one exists.
public class ChromaAlembicScreen extends AbstractContainerScreen<ChromaAlembicMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TintedFullSpectrum.MODID, "textures/gui/alembic_gui.png");
    private static final ResourceLocation KNOB_TEXTURE = ResourceLocation.fromNamespaceAndPath(TintedFullSpectrum.MODID, "textures/gui/slider_knob.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    // Coordinates below were measured directly off alembic_gui.png (pixel edge-detection plus a
    // user-supplied close-up of the hex field's location) -- see the "GUI issue" feedback pass.
    // The hex field sits in the flatter panel area to the left of the slider bracket (x:8-64),
    // sharing the sliders' vertical band, not a separately-outlined box of its own. The 3 slider
    // "tracks" are single 1px painted guide lines at y=40/45/50 (x=66..134), not thick rails --
    // the knob is rendered small (KNOB_SIZE) to actually sit on them.
    private static final int HEX_X = 16, HEX_Y = 42, HEX_W = 43, HEX_H = 12;
    private static final int SWATCH_X = 143, SWATCH_Y = 37, SWATCH_W = 25, SWATCH_H = 20;
    private static final int SLIDER_X = 66, SLIDER_W = 68, SLIDER_H = 8;
    private static final int SLIDER_R_Y = 36, SLIDER_G_Y = 41, SLIDER_B_Y = 46;
    private static final int KNOB_SIZE = 8;
    private static final int ARROW_X = 77, ARROW_Y = 98, ARROW_SHAFT_W = 11, ARROW_HEAD_W = 11, ARROW_H = 15;
    private static final int ARROW_COLOR = 0xFFC56F36;

    private int r, g, b;
    private EditBox hexBox;
    private ColorSlider rSlider, gSlider, bSlider;
    private boolean syncingWidgets;

    public ChromaAlembicScreen(ChromaAlembicMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 130;
        int packed = menu.getBlockEntity().getSelectedColor();
        r = FastColor.ARGB32.red(packed);
        g = FastColor.ARGB32.green(packed);
        b = FastColor.ARGB32.blue(packed);
    }

    @Override
    protected void init() {
        super.init();

        hexBox = new EditBox(font, leftPos + HEX_X, topPos + HEX_Y, HEX_W, HEX_H, Component.literal("hex"));
        hexBox.setMaxLength(7);
        hexBox.setTextColor(0xFFFFFF);
        hexBox.setValue(String.format("#%06X", (r << 16) | (g << 8) | b));
        hexBox.setResponder(this::onHexChanged);
        addRenderableWidget(hexBox);

        rSlider = new ColorSlider(leftPos + SLIDER_X, topPos + SLIDER_R_Y, r, value -> setColor(value, g, b));
        gSlider = new ColorSlider(leftPos + SLIDER_X, topPos + SLIDER_G_Y, g, value -> setColor(r, value, b));
        bSlider = new ColorSlider(leftPos + SLIDER_X, topPos + SLIDER_B_Y, b, value -> setColor(r, g, value));
        addRenderableWidget(rSlider);
        addRenderableWidget(gSlider);
        addRenderableWidget(bSlider);
    }

    private void onHexChanged(String text) {
        if (syncingWidgets) {
            return;
        }
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6) {
            return;
        }
        try {
            int parsed = Integer.parseInt(hex, 16);
            setColor(FastColor.ARGB32.red(parsed), FastColor.ARGB32.green(parsed), FastColor.ARGB32.blue(parsed));
        } catch (NumberFormatException ignored) {
            // Incomplete/invalid hex while the player is still typing -- ignore until it parses.
        }
    }

    private void setColor(int newR, int newG, int newB) {
        r = Mth.clamp(newR, 0, 255);
        g = Mth.clamp(newG, 0, 255);
        b = Mth.clamp(newB, 0, 255);

        syncingWidgets = true;
        if (hexBox != null) {
            hexBox.setValue(String.format("#%06X", (r << 16) | (g << 8) | b));
        }
        if (rSlider != null) {
            rSlider.setValueSilently(r);
            gSlider.setValueSilently(g);
            bSlider.setValueSilently(b);
        }
        syncingWidgets = false;

        int packed = FastColor.ARGB32.color(255, r, g, b);
        PacketDistributor.sendToServer(new ChromaAlembicSetColorPayload(menu.getBlockEntity().getBlockPos(), packed));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEX_W, TEX_H);

        int sx = leftPos + SWATCH_X, sy = topPos + SWATCH_Y;
        int packed = FastColor.ARGB32.color(255, r, g, b);
        guiGraphics.fill(sx - 1, sy - 1, sx + SWATCH_W + 1, sy + SWATCH_H + 1, 0xFF28180B);
        guiGraphics.fill(sx, sy, sx + SWATCH_W, sy + SWATCH_H, packed);

        renderProgressArrow(guiGraphics);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics) {
        ChromaAlembicBlockEntity be = menu.getBlockEntity();
        float fraction = 0f;
        if (be.isProcessing() && minecraft != null && minecraft.level != null) {
            long elapsed = minecraft.level.getGameTime() - be.getCraftStartGameTime();
            fraction = Mth.clamp(elapsed / (float) ChromaAlembicBlockEntity.TOTAL_TICKS, 0f, 1f);
        }
        if (fraction <= 0f) {
            return;
        }

        int totalWidth = ARROW_SHAFT_W + ARROW_HEAD_W;
        int revealed = Math.round(totalWidth * fraction);
        int ax = leftPos + ARROW_X;
        int ay = topPos + ARROW_Y;
        int shaftTop = ay + ARROW_H / 2 - 2;
        int shaftBottom = ay + ARROW_H / 2 + 2;

        for (int col = 0; col < revealed; col++) {
            int top, bottom;
            if (col < ARROW_SHAFT_W) {
                top = shaftTop;
                bottom = shaftBottom;
            } else {
                float headProgress = (col - ARROW_SHAFT_W) / (float) ARROW_HEAD_W;
                int halfHeight = Math.round((ARROW_H / 2f) * (1f - headProgress));
                top = ay + ARROW_H / 2 - halfHeight;
                bottom = ay + ARROW_H / 2 + halfHeight;
            }
            if (bottom > top) {
                guiGraphics.fill(ax + col, top, ax + col + 1, bottom, ARROW_COLOR);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // Vanilla's default label color (0x404040, a dark gray tuned for the light vanilla inventory
    // texture) is nearly unreadable against this GUI's dark green/brown panel -- draw both labels
    // light with a shadow instead.
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE0E0E0, true);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE0E0E0, true);
    }

    // AbstractContainerScreen.mouseDragged completely replaces the normal widget-dispatch logic
    // with its own slot-drag-across-multiple-slots handling, and never forwards to the focused
    // child widget -- so without this override, a slider registers the initial click (which jumps
    // it to that position) but never updates again once the mouse actually moves while held down.
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging() && getFocused() instanceof ColorSlider slider) {
            return slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // Draws only the knob (slider_knob.png) at the current value's position -- the track itself is
    // already painted into the GUI background, and vanilla's default slider chrome doesn't apply here.
    private class ColorSlider extends AbstractSliderButton {
        private final java.util.function.IntConsumer onChange;

        ColorSlider(int x, int y, int initial, java.util.function.IntConsumer onChange) {
            super(x, y, SLIDER_W, SLIDER_H, Component.empty(), initial / 255.0);
            this.onChange = onChange;
        }

        void setValueSilently(int value) {
            this.value = Mth.clamp(value, 0, 255) / 255.0;
        }

        @Override
        protected void updateMessage() {
        }

        @Override
        protected void applyValue() {
            if (!syncingWidgets) {
                onChange.accept((int) Math.round(this.value * 255.0));
            }
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int knobX = getX() + (int) (this.value * (getWidth() - KNOB_SIZE));
            int knobY = getY() + (getHeight() - KNOB_SIZE) / 2;
            guiGraphics.blit(KNOB_TEXTURE, knobX, knobY, KNOB_SIZE, KNOB_SIZE, 0f, 0f, 16, 16, 16, 16);
        }
    }
}
