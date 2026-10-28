package com.futureclient.module;

import com.futureclient.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.util.Collections;
import java.util.List;

/** Base for every on-screen element. Default x/y are percentages; stored as a 0..1 fraction of the free screen space. */
public abstract class Hud extends Module {
    public static boolean editor;

    protected final Minecraft mc = Minecraft.getMinecraft();
    public final Setting.Num posX, posY;
    public final Setting.Num scale = new Setting.Num("Scale", 1, 0.5, 2.5, 0.1);
    public final Setting.Bool bg = new Setting.Bool("Background", true);
    public final Setting.Num bgAlpha = new Setting.Num("Background Opacity", 55, 0, 100, 5);
    public final Setting.Color color = new Setting.Color("Text Color", 0xFFFFFFFF);
    public final Setting.Bool chroma = new Setting.Bool("Chroma", false);
    public final Setting.Bool shadow = new Setting.Bool("Text Shadow", true);

    public float w, h, rx, ry, rw, rh;

    protected Hud(String n, String d, Category c, double x, double y, boolean on) {
        super(n, d, c, on);
        posX = add(new Setting.Num("x", x / 100.0, 0, 1, 0.001));
        posY = add(new Setting.Num("y", y / 100.0, 0, 1, 0.001));
        posX.hidden = true;
        posY.hidden = true;
    }

    /** Called by the manager so module-specific settings are listed first. */
    public void addCommon() {
        add(scale); add(bg); add(bgAlpha); add(color); add(chroma); add(shadow);
    }

    protected abstract void measure();
    protected abstract void paint(int color);

    @Override
    public void onHudRender(ScaledResolution sr) { draw(sr); }

    public void draw(ScaledResolution sr) {
        measure();
        float s = scale.f();
        float sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
        float x = posX.f() * Math.max(0f, sw - w * s);
        float y = posY.f() * Math.max(0f, sh - h * s);
        rx = x; ry = y; rw = w * s; rh = h * s;
        if (w <= 0 || h <= 0) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(s, s, 1f);
        paint(textColor());
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    protected int textColor() { return chroma.value ? rainbow(0f) : color.value; }

    public static int rainbow(float offset) {
        float hue = ((System.currentTimeMillis() % 4000L) / 4000f + offset) % 1f;
        return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.7f, 1f);
    }

    protected void bgBox(float x, float y, float bw, float bh) {
        if (bg.value) RenderUtil.roundRect(x, y, bw, bh, 3, ((int) (bgAlpha.value * 2.55) << 24));
    }

    protected void text(String s, float x, float y, int c) {
        mc.fontRendererObj.drawString(s, x, y, c, shadow.value);
    }

    /** Simple multi-line text element. */
    public abstract static class Text extends Hud {
        private List<String> cur = Collections.emptyList();

        protected Text(String n, String d, double x, double y) { this(n, d, Category.HUD, x, y); }

        protected Text(String n, String d, Category c, double x, double y) { super(n, d, c, x, y, true); }

        protected abstract List<String> lines();

        protected int lineColor(int i, int base) { return base; }

        @Override
        protected void measure() {
            cur = lines();
            if (cur.isEmpty() && editor) cur = Collections.singletonList(name);
            FontRenderer f = mc.fontRendererObj;
            float m = 0;
            for (String s : cur) m = Math.max(m, f.getStringWidth(s));
            w = cur.isEmpty() ? 0 : m + 10;
            h = cur.isEmpty() ? 0 : cur.size() * 10 + 3;
        }

        @Override
        protected void paint(int c) {
            if (cur.isEmpty()) return;
            bgBox(0, 0, w, h);
            for (int i = 0; i < cur.size(); i++) text(cur.get(i), 5, 3 + i * 10, lineColor(i, c));
        }
    }
}
