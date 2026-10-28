package com.futureclient.gui;

import com.futureclient.FutureClient;
import com.futureclient.module.ClientSettings;
import com.futureclient.util.RenderUtil;
import net.minecraft.client.gui.Gui;

/** The full-screen backdrop behind Future Client menus (Client tab: Menu Background). */
public final class GuiBackground {
    private GuiBackground() {}

    public static void draw(int w, int h) {
        ClientSettings c = FutureClient.modules.client;
        if (!c.menuBg.value) return;
        int a = Math.max(0, Math.min(255, (int) (c.dim.value * 2.55)));
        if (c.bgStyle.is("Solid")) {
            Gui.drawRect(0, 0, w, h, (a << 24) | 0x0B0B12);
        } else if (c.bgStyle.is("Dim")) {
            Gui.drawRect(0, 0, w, h, a << 24);
        } else {
            int acc = c.accent.value;
            int tint = (((acc >> 16 & 255) / 5) << 16) | (((acc >> 8 & 255) / 5) << 8) | ((acc & 255) / 5);
            RenderUtil.gradient(0, 0, w, h, (a << 24) | 0x080810, (a << 24) | tint);
        }
    }

    /** Lighter version for the HUD editor so you can still judge placement against the game view. */
    public static void drawLight(int w, int h) {
        ClientSettings c = FutureClient.modules.client;
        if (!c.menuBg.value) return;
        Gui.drawRect(0, 0, w, h, Math.max(0, Math.min(150, (int) (c.dim.value * 1.2))) << 24);
    }
}
