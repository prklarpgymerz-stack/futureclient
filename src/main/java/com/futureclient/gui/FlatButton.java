package com.futureclient.gui;

import com.futureclient.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class FlatButton extends GuiButton {
    private float hover;

    public FlatButton(int id, int x, int y, int w, int h, String text) {
        super(id, x, y, w, h, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        hover += ((hovered ? 1f : 0f) - hover) * 0.2f;
        int bg = RenderUtil.lerp(0xA0161620, 0xE07C5CFF, hover);
        RenderUtil.roundRect(xPosition, yPosition, width, height, 5, bg);
        int tc = RenderUtil.lerp(0xFFCFCFDC, 0xFFFFFFFF, hover);
        mc.fontRendererObj.drawString(displayString,
            xPosition + (width - mc.fontRendererObj.getStringWidth(displayString)) / 2f,
            yPosition + (height - 8) / 2f, tc, false);
    }
}
