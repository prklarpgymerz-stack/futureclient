package com.futureclient.gui;

import com.futureclient.FutureClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;
import java.util.Random;

public class FutureMainMenu extends GuiScreen {
    private final float[][] p = new float[70][4]; // x, y, speed, size
    private final Random rnd = new Random();

    @Override
    public void initGui() {
        buttonList.clear();
        int bw = 170, bh = 22, bx = width / 2 - bw / 2, by = height / 2 - 12;
        buttonList.add(new FlatButton(0, bx, by, bw, bh, "Singleplayer"));
        buttonList.add(new FlatButton(1, bx, by + 28, bw, bh, "Multiplayer"));
        buttonList.add(new FlatButton(2, bx, by + 56, bw / 2 - 2, bh, "Options"));
        buttonList.add(new FlatButton(3, bx + bw / 2 + 2, by + 56, bw / 2 - 2, bh, "Quit"));
        for (float[] q : p) {
            q[0] = rnd.nextFloat() * width; q[1] = rnd.nextFloat() * height;
            q[2] = 0.1f + rnd.nextFloat() * 0.4f; q[3] = 1 + rnd.nextInt(2);
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case 0: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
            case 1: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
            case 2: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
            case 3: mc.shutdown(); break;
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawGradientRect(0, 0, width, height, 0xFF090912, 0xFF1B1038);
        int acc = FutureClient.modules.client.accent.value;
        if (FutureClient.modules.client.particles.value) {
            for (float[] q : p) {
                q[1] -= q[2];
                if (q[1] < -2) { q[1] = height + 2; q[0] = rnd.nextFloat() * width; }
                drawRect((int) q[0], (int) q[1], (int) (q[0] + q[3]), (int) (q[1] + q[3]), (acc & 0xFFFFFF) | 0x59000000);
            }
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f - 72, 0);
        GlStateManager.scale(4f, 4f, 1f);
        drawCenteredString(fontRendererObj, "FUTURE", 0, 0, 0xFFFFFFFF);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f - 36, 0);
        GlStateManager.scale(1.5f, 1.5f, 1f);
        drawCenteredString(fontRendererObj, "C L I E N T", 0, 0, acc);
        GlStateManager.popMatrix();
        super.drawScreen(mx, my, pt);
        fontRendererObj.drawString("Future Client 1.2", 6, height - 12, 0xFF5A5A6A);
        String v = "Minecraft 1.8.9";
        fontRendererObj.drawString(v, width - fontRendererObj.getStringWidth(v) - 6, height - 12, 0xFF5A5A6A);
    }
}
