package com.futureclient.gui;

import com.futureclient.FutureClient;
import com.futureclient.module.Module;
import com.futureclient.module.Setting;
import com.futureclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends GuiScreen {
    private static final int ACCENT = 0xFF7C5CFF;
    private static final int W = 480, H = 280, CW = 104, CH = 60;
    private static Module selected;
    private Setting.Num dragging;
    private float dragX, dragW;

    private static class Hit {
        float x, y, w, h; Setting s;
        Hit(float x, float y, float w, float h, Setting s) { this.x = x; this.y = y; this.w = w; this.h = h; this.s = s; }
    }
    private final List<Hit> hits = new ArrayList<Hit>();

    private float ox() { return (width - W) / 2f; }
    private float oy() { return (height - H) / 2f; }
    private float cardX(int i) { return ox() + 98 + (i % 2) * (CW + 6); }
    private float cardY(int i) { return oy() + 30 + (i / 2) * (CH + 6); }
    private static boolean in(int mx, int my, float x, float y, float w, float h) { return mx >= x && my >= y && mx < x + w && my < y + h; }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        if (selected == null) selected = FutureClient.modules.all.get(0);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        FontRenderer fr = mc.fontRendererObj;
        hits.clear();
        drawRect(0, 0, width, height, 0x78000000);
        float x = ox(), y = oy();

        // window + sidebar
        RenderUtil.roundRect(x, y, W, H, 8, 0xF2121218);
        RenderUtil.roundRect(x + 6, y + 6, 84, H - 12, 6, 0xFF181821);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 16, y + 18, 0);
        GlStateManager.scale(1.6f, 1.6f, 1f);
        fr.drawStringWithShadow("FUTURE", 0, 0, 0xFFFFFFFF);
        GlStateManager.popMatrix();
        fr.drawString("CLIENT", (int) x + 17, (int) y + 34, ACCENT);
        RenderUtil.roundRect(x + 14, y + 58, 68, 20, 5, 0x337C5CFF);
        RenderUtil.roundRect(x + 14, y + 64, 2, 8, 1, ACCENT);
        fr.drawString("Mods", (int) x + 24, (int) y + 64, 0xFFFFFFFF);
        fr.drawString("v1.0", (int) x + 17, (int) (y + H - 20), 0xFF5A5A6A);
        fr.drawString("MODS", (int) x + 98, (int) y + 14, 0xFF8A8A9A);

        // slider drag
        if (dragging != null) {
            if (Mouse.isButtonDown(0)) setSlider(dragging, mx); else dragging = null;
        }

        // module cards
        List<Module> all = FutureClient.modules.all;
        for (int i = 0; i < all.size(); i++) {
            Module m = all.get(i);
            float cx = cardX(i), cy = cardY(i);
            boolean hov = in(mx, my, cx, cy, CW, CH);
            m.hover += ((hov ? 1f : 0f) - m.hover) * 0.2f;
            m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.25f;
            if (m == selected) RenderUtil.roundRect(cx - 1, cy - 1, CW + 2, CH + 2, 6, 0x887C5CFF);
            RenderUtil.roundRect(cx, cy, CW, CH, 5, RenderUtil.lerp(0xFF1C1C26, 0xFF26263A, m.hover));
            fr.drawStringWithShadow(m.name, cx + 8, cy + 8, 0xFFFFFFFF);
            fr.drawString(fr.trimStringToWidth(m.desc, CW - 14), (int) cx + 8, (int) cy + 21, 0xFF8A8A9A);
            float sx = cx + 8, sy = cy + CH - 20;
            RenderUtil.roundRect(sx, sy, 26, 12, 6, RenderUtil.lerp(0xFF2E2E3A, ACCENT, m.anim));
            RenderUtil.roundRect(sx + 1 + 14 * m.anim, sy + 1, 10, 10, 5, 0xFFFFFFFF);
            fr.drawString(m.enabled ? "On" : "Off", (int) sx + 32, (int) sy + 2, m.enabled ? 0xFFFFFFFF : 0xFF6A6A7A);
        }

        // settings pane
        float px = x + 320, pw = 154;
        RenderUtil.roundRect(px, y + 6, pw, H - 12, 6, 0xFF181821);
        if (selected != null) {
            fr.drawStringWithShadow(selected.name, px + 8, y + 14, 0xFFFFFFFF);
            fr.drawString("Settings", (int) px + 8, (int) y + 25, 0xFF8A8A9A);
            float ry = y + 42;
            for (Setting s : selected.settings) {
                if (s instanceof Setting.Bool) {
                    Setting.Bool b = (Setting.Bool) s;
                    b.anim += ((b.value ? 1f : 0f) - b.anim) * 0.25f;
                    fr.drawString(b.name, (int) px + 8, (int) ry + 2, 0xFFCFCFDC);
                    float sx = px + pw - 8 - 26;
                    RenderUtil.roundRect(sx, ry, 26, 12, 6, RenderUtil.lerp(0xFF2E2E3A, ACCENT, b.anim));
                    RenderUtil.roundRect(sx + 1 + 14 * b.anim, ry + 1, 10, 10, 5, 0xFFFFFFFF);
                    hits.add(new Hit(sx, ry, 26, 12, s));
                    ry += 22;
                } else if (s instanceof Setting.Num) {
                    Setting.Num n = (Setting.Num) s;
                    String val = n.step >= 1 ? String.valueOf((int) n.value) : String.format("%.1f", n.value);
                    fr.drawString(n.name, (int) px + 8, (int) ry, 0xFFCFCFDC);
                    fr.drawString(val, (int) (px + pw - 8 - fr.getStringWidth(val)), (int) ry, 0xFF8A8A9A);
                    float tx = px + 8, tw = pw - 16, ty = ry + 15;
                    float t = (float) ((n.value - n.min) / (n.max - n.min));
                    RenderUtil.roundRect(tx, ty, tw, 4, 2, 0xFF2E2E3A);
                    RenderUtil.roundRect(tx, ty, Math.max(4, tw * t), 4, 2, ACCENT);
                    RenderUtil.roundRect(tx + tw * t - 4, ty - 2, 8, 8, 4, 0xFFFFFFFF);
                    hits.add(new Hit(tx, ty - 5, tw, 14, s));
                    ry += 32;
                }
            }
        }
        super.drawScreen(mx, my, pt);
    }

    private void setSlider(Setting.Num n, int mx) {
        double t = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        double v = n.min + (n.max - n.min) * t;
        v = n.min + Math.round((v - n.min) / n.step) * n.step;
        n.value = Math.max(n.min, Math.min(n.max, v));
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        List<Module> all = FutureClient.modules.all;
        for (int i = 0; i < all.size(); i++) {
            if (in(mx, my, cardX(i), cardY(i), CW, CH)) {
                selected = all.get(i);
                if (btn == 0) selected.toggle();
                return;
            }
        }
        if (btn != 0) return;
        for (Hit h : hits) {
            if (in(mx, my, h.x, h.y, h.w, h.h)) {
                if (h.s instanceof Setting.Bool) {
                    Setting.Bool b = (Setting.Bool) h.s;
                    b.value = !b.value;
                } else {
                    dragging = (Setting.Num) h.s; dragX = h.x; dragW = h.w;
                    setSlider(dragging, mx);
                }
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        dragging = null;
        super.mouseReleased(mx, my, state);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == FutureClient.openGui.getKeyCode()) { mc.displayGuiScreen(null); return; }
        super.keyTyped(c, key);
    }
}
