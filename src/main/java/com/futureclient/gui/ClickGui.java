package com.futureclient.gui;

import com.futureclient.ConfigManager;
import com.futureclient.FutureClient;
import com.futureclient.module.ClientSettings;
import com.futureclient.module.Module;
import com.futureclient.module.Setting;
import com.futureclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends GuiScreen {
    private static final int W = 580, H = 320, CW = 100, CH = 56, GAP = 6;
    private static final String[] TABS = {"All", "HUD", "Visual", "Utility", "Client", "Profiles"};
    private static final int T_CLIENT = 4, T_PROFILES = 5;
    private static final int K_MODULE = 0, K_BOOL = 1, K_NUM = 2, K_MODE = 3, K_COLOR = 4;
    private static final int K_SAVE = 10, K_LOAD = 11, K_DELETE = 12, K_CREATE = 13;
    private static final int K_SAVE_ACTIVE = 20, K_RESET = 21;

    private static int tab = 0;
    private static Module selected;
    private static String status = "";
    private static long statusTime;

    private String search = "", nameInput = "";
    private float scroll, sscroll, fit = 1f, clipTop;
    private Setting.Num dragging;
    private float dragX, dragW;
    private List<String> profileCache = new ArrayList<String>();

    private static class Hit {
        float x, y, w, h;
        Setting s;
        int kind, idx;

        Hit(float x, float y, float w, float h, Setting s, int kind, int idx) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.s = s; this.kind = kind; this.idx = idx;
        }
    }

    private final List<Hit> hits = new ArrayList<Hit>();

    // ------------------------------------------------------------ helpers
    private int acc() { return FutureClient.modules.client.accent.value; }
    private float ox() { return (width - W) / 2f; }
    private float oy() { return (height - H) / 2f; }
    private float tabY(float y, int i) { return y + 58 + i * 24 + (i >= T_CLIENT ? 8 : 0); }
    private static boolean in(float mx, float my, float x, float y, float w, float h) { return mx >= x && my >= y && mx < x + w && my < y + h; }

    private void computeFit() {
        fit = FutureClient.modules.client.guiFit.value ? Math.min(1f, Math.min((width - 8f) / W, (height - 8f) / H)) : 1f;
    }
    private float lx(float mx) { return (mx - width / 2f) / fit + width / 2f; }
    private float ly(float my) { return (my - height / 2f) / fit + height / 2f; }

    private void scissor(float x, float y, float w, float h) {
        int f = new ScaledResolution(mc).getScaleFactor();
        float sx = (x - width / 2f) * fit + width / 2f, sy = (y - height / 2f) * fit + height / 2f;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (sx * f), (int) ((height - (sy + h * fit)) * f), Math.max(0, (int) (w * fit * f)), Math.max(0, (int) (h * fit * f)));
    }
    private void noScissor() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }

    private List<Module> visible() {
        List<Module> out = new ArrayList<Module>();
        String q = search.toLowerCase();
        for (Module m : FutureClient.modules.all) {
            if (!q.isEmpty()) {
                if (m.category != Module.Category.CLIENT && m.name.toLowerCase().contains(q)) out.add(m);
                continue;
            }
            if (tab == 0) { if (m.category != Module.Category.CLIENT) out.add(m); }
            else if (tab >= 1 && tab <= T_CLIENT && m.category.ordinal() == tab - 1) out.add(m);
        }
        return out;
    }

    private void fixSelected() {
        if (tab == T_CLIENT) { selected = FutureClient.modules.client; return; }
        if (selected == null || selected.category == Module.Category.CLIENT) {
            List<Module> v = visible();
            selected = v.isEmpty() ? null : v.get(0);
        }
    }

    private void refreshProfiles() { profileCache = ConfigManager.list(); }

    private static void setStatus(String s) { status = s; statusTime = System.currentTimeMillis(); }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        fixSelected();
        refreshProfiles();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        ConfigManager.save();
    }

    // ------------------------------------------------------------ drawing
    @Override
    public void drawScreen(int mxi, int myi, float pt) {
        computeFit();
        float mx = lx(mxi), my = ly(myi);
        FontRenderer fr = mc.fontRendererObj;
        hits.clear();
        ClientSettings cs = FutureClient.modules.client;
        GuiBackground.draw(width, height);

        if (dragging != null) {
            if (Mouse.isButtonDown(0)) setSlider(dragging, mx); else dragging = null;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f, 0f);
        GlStateManager.scale(fit, fit, 1f);
        GlStateManager.translate(-width / 2f, -height / 2f, 0f);

        float x = ox(), y = oy();
        RenderUtil.roundRect(x, y, W, H, 8, 0xF2121218);
        drawSidebar(fr, x, y, mx, my);
        if (tab == T_PROFILES) { drawProfiles(fr, x, y, mx, my); drawProfilePane(fr, x, y, mx, my); }
        else { drawGrid(fr, x, y, mx, my); drawSettings(fr, x, y, mx, my); }
        GlStateManager.popMatrix();
    }

    private void drawSidebar(FontRenderer fr, float x, float y, float mx, float my) {
        int acc = acc();
        RenderUtil.roundRect(x + 6, y + 6, 88, H - 12, 6, 0xFF181821);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 16, y + 18, 0);
        GlStateManager.scale(1.6f, 1.6f, 1f);
        fr.drawStringWithShadow("FUTURE", 0, 0, 0xFFFFFFFF);
        GlStateManager.popMatrix();
        fr.drawString("CLIENT", (int) x + 17, (int) y + 34, acc);
        for (int i = 0; i < TABS.length; i++) {
            float ty = tabY(y, i);
            boolean sel = tab == i && (search.isEmpty() || i == T_PROFILES);
            boolean hov = in(mx, my, x + 10, ty, 80, 20);
            if (sel) {
                RenderUtil.roundRect(x + 10, ty, 80, 20, 5, (acc & 0xFFFFFF) | 0x33000000);
                RenderUtil.roundRect(x + 10, ty + 6, 2, 8, 1, acc);
            } else if (hov) {
                RenderUtil.roundRect(x + 10, ty, 80, 20, 5, 0x22FFFFFF);
            }
            fr.drawString(TABS[i], (int) x + 22, (int) ty + 6, sel ? 0xFFFFFFFF : 0xFF9A9AAA);
        }
        RenderUtil.roundRect(x + 14, y + 155, 72, 1, 0, 0x22FFFFFF);
        boolean hb = in(mx, my, x + 12, y + H - 54, 76, 20);
        RenderUtil.roundRect(x + 12, y + H - 54, 76, 20, 5, hb ? RenderUtil.lerp(acc, 0xFFFFFFFF, 0.2f) : acc);
        String t = "HUD Editor";
        fr.drawString(t, (int) (x + 12 + (76 - fr.getStringWidth(t)) / 2f), (int) (y + H - 48), 0xFFFFFFFF);
        fr.drawString("v1.2", (int) x + 17, (int) (y + H - 24), 0xFF5A5A6A);
    }

    private void scrollbar(float x, float y, float h, float scrollV, float max) {
        if (max <= 0) return;
        float bar = 30f;
        RenderUtil.roundRect(x, y + (scrollV / max) * (h - bar), 3, bar, 1.5f, 0x55FFFFFF);
    }

    private void drawGrid(FontRenderer fr, float x, float y, float mx, float my) {
        int acc = acc();
        float sx = x + 104, sy = y + 10;
        RenderUtil.roundRect(sx, sy, 220, 18, 5, 0xFF181821);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        if (search.isEmpty()) fr.drawString("Search mods...", (int) sx + 8, (int) sy + 5, 0xFF5A5A6A);
        else fr.drawString(search + (blink ? "_" : ""), (int) sx + 8, (int) sy + 5, 0xFFFFFFFF);

        List<Module> mods = visible();
        String cnt = mods.size() + (mods.size() == 1 ? " mod" : " mods");
        fr.drawString(cnt, (int) (x + 416 - fr.getStringWidth(cnt)), (int) sy + 5, 0xFF5A5A6A);

        float gx = x + 104, gy = y + 36, gw = 3 * CW + 2 * GAP, gh = H - 44;
        int rows = (mods.size() + 2) / 3;
        float maxScroll = Math.max(0f, rows * (CH + GAP) - gh);
        scroll = Math.max(0f, Math.min(maxScroll, scroll));
        scissor(gx - 2, gy - 2, gw + 4, gh + 4);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            float cx = gx + (i % 3) * (CW + GAP), cy = gy + (i / 3) * (CH + GAP) - scroll;
            if (cy + CH < gy || cy > gy + gh) continue;
            boolean hov = in(mx, my, cx, cy, CW, CH) && in(mx, my, gx, gy, gw, gh);
            m.hover += ((hov ? 1f : 0f) - m.hover) * 0.2f;
            m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.25f;
            if (m == selected) RenderUtil.roundRect(cx - 1, cy - 1, CW + 2, CH + 2, 6, (acc & 0xFFFFFF) | 0x88000000);
            RenderUtil.roundRect(cx, cy, CW, CH, 5, RenderUtil.lerp(0xFF1C1C26, 0xFF26263A, m.hover));
            fr.drawStringWithShadow(fr.trimStringToWidth(m.name, CW - 14), cx + 8, cy + 8, 0xFFFFFFFF);
            fr.drawString(fr.trimStringToWidth(m.desc, CW - 14), (int) cx + 8, (int) cy + 21, 0xFF8A8A9A);
            float sw = cx + 8, sh = cy + CH - 20;
            if (m.category == Module.Category.CLIENT) {
                fr.drawString("Always on", (int) sw, (int) sh + 2, 0xFF6A6A7A);
            } else {
                RenderUtil.roundRect(sw, sh, 26, 12, 6, RenderUtil.lerp(0xFF2E2E3A, acc, m.anim));
                RenderUtil.roundRect(sw + 1 + 14 * m.anim, sh + 1, 10, 10, 5, 0xFFFFFFFF);
                fr.drawString(m.enabled ? "On" : "Off", (int) sw + 32, (int) sh + 2, m.enabled ? 0xFFFFFFFF : 0xFF6A6A7A);
            }
        }
        noScissor();
        if (mods.isEmpty()) fr.drawString("No mods found", (int) gx + 4, (int) gy + 6, 0xFF6A6A7A);
        scrollbar(gx + gw + 3, gy, gh, scroll, maxScroll);
    }

    private void button(FontRenderer fr, float x, float y, float w, float h, String label, int kind, int idx, int col, float mx, float my) {
        boolean hov = in(mx, my, x, y, w, h);
        RenderUtil.roundRect(x, y, w, h, 4, hov ? RenderUtil.lerp(col, 0xFFFFFFFF, 0.18f) : col);
        fr.drawString(label, (int) (x + (w - fr.getStringWidth(label)) / 2f), (int) (y + (h - 8) / 2f), 0xFFFFFFFF);
        hits.add(new Hit(x, y, w, h, null, kind, idx));
    }

    private void drawProfiles(FontRenderer fr, float x, float y, float mx, float my) {
        int acc = acc();
        float sx = x + 104, sy = y + 10;
        RenderUtil.roundRect(sx, sy, 220, 18, 5, 0xFF181821);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        if (nameInput.isEmpty()) fr.drawString("New profile name...", (int) sx + 8, (int) sy + 5, 0xFF5A5A6A);
        else fr.drawString(nameInput + (blink ? "_" : ""), (int) sx + 8, (int) sy + 5, 0xFFFFFFFF);
        button(fr, x + 330, sy, 86, 18, "Save as New", K_CREATE, 0, acc, mx, my);

        float gx = x + 104, gy = y + 36, gw = 312, gh = H - 44;
        int rh = 34;
        float maxScroll = Math.max(0f, profileCache.size() * rh - gh);
        scroll = Math.max(0f, Math.min(maxScroll, scroll));
        scissor(gx - 2, gy - 2, gw + 4, gh + 4);
        for (int i = 0; i < profileCache.size(); i++) {
            String name = profileCache.get(i);
            float ry = gy + i * rh - scroll;
            if (ry + 30 < gy || ry > gy + gh) continue;
            boolean isActive = name.equals(ConfigManager.active());
            if (isActive) RenderUtil.roundRect(gx - 1, ry - 1, gw + 2, 32, 6, (acc & 0xFFFFFF) | 0x88000000);
            RenderUtil.roundRect(gx, ry, gw, 30, 5, 0xFF1C1C26);
            fr.drawStringWithShadow(fr.trimStringToWidth(name, 120), gx + 10, ry + 6, 0xFFFFFFFF);
            fr.drawString(isActive ? "Active" : "Saved profile", (int) gx + 10, (int) ry + 18, isActive ? acc : 0xFF6A6A7A);
            button(fr, gx + gw - 150, ry + 6, 44, 18, "Save", K_SAVE, i, 0xFF2E2E3A, mx, my);
            button(fr, gx + gw - 100, ry + 6, 44, 18, "Load", K_LOAD, i, acc, mx, my);
            button(fr, gx + gw - 50, ry + 6, 44, 18, "Delete", K_DELETE, i, 0xFF7A2E2E, mx, my);
        }
        noScissor();
        if (profileCache.isEmpty()) fr.drawString("No profiles yet - type a name and press Save as New", (int) gx + 4, (int) gy + 6, 0xFF6A6A7A);
        scrollbar(gx + gw + 3, gy, gh, scroll, maxScroll);
    }

    private void drawProfilePane(FontRenderer fr, float x, float y, float mx, float my) {
        int acc = acc();
        float px = x + 426, pw = 148, py = y + 6, ph = H - 12;
        RenderUtil.roundRect(px, py, pw, ph, 6, 0xFF181821);
        fr.drawStringWithShadow("Profiles", px + 8, py + 9, 0xFFFFFFFF);
        fr.drawString("Active profile", (int) px + 8, (int) py + 26, 0xFF8A8A9A);
        fr.drawString(fr.trimStringToWidth(ConfigManager.active(), (int) pw - 16), (int) px + 8, (int) py + 37, acc);
        List<String> d = fr.listFormattedStringToWidth("A profile stores every mod's on/off state, its settings and your HUD positions. The active profile saves itself when you close this menu.", (int) pw - 16);
        for (int i = 0; i < d.size(); i++) fr.drawString(d.get(i), (int) px + 8, (int) py + 54 + i * 10, 0xFF8A8A9A);
        button(fr, px + 8, py + 150, pw - 16, 18, "Save Active Profile", K_SAVE_ACTIVE, 0, acc, mx, my);
        button(fr, px + 8, py + 172, pw - 16, 18, "Reset to Defaults", K_RESET, 0, 0xFF7A2E2E, mx, my);
        if (System.currentTimeMillis() - statusTime < 3500 && !status.isEmpty()) {
            List<String> sl = fr.listFormattedStringToWidth(status, (int) pw - 16);
            for (int i = 0; i < Math.min(3, sl.size()); i++) fr.drawString(sl.get(i), (int) px + 8, (int) (py + ph - 36 + i * 10), 0xFFDDDDEE);
        }
    }

    private void drawSettings(FontRenderer fr, float x, float y, float mx, float my) {
        int acc = acc();
        float px = x + 426, pw = 148, py = y + 6, ph = H - 12;
        RenderUtil.roundRect(px, py, pw, ph, 6, 0xFF181821);
        clipTop = py + 52;
        if (selected == null) return;
        Module m = selected;
        m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.25f;
        fr.drawStringWithShadow(fr.trimStringToWidth(m.name, (int) pw - 52), px + 8, py + 9, 0xFFFFFFFF);
        if (m.category != Module.Category.CLIENT) {
            float sx = px + pw - 8 - 26, sy = py + 8;
            RenderUtil.roundRect(sx, sy, 26, 12, 6, RenderUtil.lerp(0xFF2E2E3A, acc, m.anim));
            RenderUtil.roundRect(sx + 1 + 14 * m.anim, sy + 1, 10, 10, 5, 0xFFFFFFFF);
            hits.add(new Hit(sx, sy, 26, 12, null, K_MODULE, 0));
        }
        List<String> d = fr.listFormattedStringToWidth(m.desc, (int) pw - 16);
        for (int i = 0; i < Math.min(2, d.size()); i++) fr.drawString(d.get(i), (int) px + 8, (int) py + 24 + i * 10, 0xFF8A8A9A);
        RenderUtil.roundRect(px + 8, py + 46, pw - 16, 1, 0, 0x22FFFFFF);

        float top = clipTop, bottom = py + ph - 4;
        float ry = top - sscroll, start = ry;
        scissor(px, top, pw, bottom - top);
        for (Setting s : m.settings) {
            if (s.hidden) continue;
            boolean vis = ry + 32 > top && ry < bottom;
            if (s instanceof Setting.Bool) {
                Setting.Bool b = (Setting.Bool) s;
                b.anim += ((b.value ? 1f : 0f) - b.anim) * 0.25f;
                fr.drawString(fr.trimStringToWidth(b.name, (int) pw - 52), (int) px + 8, (int) ry + 2, 0xFFCFCFDC);
                float sx = px + pw - 8 - 26;
                RenderUtil.roundRect(sx, ry, 26, 12, 6, RenderUtil.lerp(0xFF2E2E3A, acc, b.anim));
                RenderUtil.roundRect(sx + 1 + 14 * b.anim, ry + 1, 10, 10, 5, 0xFFFFFFFF);
                if (vis) hits.add(new Hit(sx, ry, 26, 12, s, K_BOOL, 0));
                ry += 22;
            } else if (s instanceof Setting.Num) {
                Setting.Num n = (Setting.Num) s;
                String val = n.display();
                fr.drawString(fr.trimStringToWidth(n.name, (int) pw - 16 - fr.getStringWidth(val) - 4), (int) px + 8, (int) ry, 0xFFCFCFDC);
                fr.drawString(val, (int) (px + pw - 8 - fr.getStringWidth(val)), (int) ry, 0xFF8A8A9A);
                float tx = px + 8, tw = pw - 16, ty = ry + 15;
                float t = (float) ((n.value - n.min) / (n.max - n.min));
                RenderUtil.roundRect(tx, ty, tw, 4, 2, 0xFF2E2E3A);
                RenderUtil.roundRect(tx, ty, Math.max(4, tw * t), 4, 2, acc);
                RenderUtil.roundRect(tx + tw * t - 4, ty - 2, 8, 8, 4, 0xFFFFFFFF);
                if (vis) hits.add(new Hit(tx, ry + 10, tw, 14, s, K_NUM, 0));
                ry += 32;
            } else if (s instanceof Setting.Mode) {
                Setting.Mode md = (Setting.Mode) s;
                fr.drawString(fr.trimStringToWidth(md.name, (int) pw - 16), (int) px + 8, (int) ry, 0xFFCFCFDC);
                boolean hov = in(mx, my, px + 8, ry + 11, pw - 16, 14) && my >= top;
                RenderUtil.roundRect(px + 8, ry + 11, pw - 16, 14, 4, hov ? 0xFF2C2C38 : 0xFF22222C);
                String o = "< " + md.get() + " >";
                fr.drawString(fr.trimStringToWidth(o, (int) pw - 22), (int) (px + 8 + (pw - 16 - fr.getStringWidth(fr.trimStringToWidth(o, (int) pw - 22))) / 2f), (int) ry + 15, 0xFFFFFFFF);
                if (vis) hits.add(new Hit(px + 8, ry + 11, pw - 16, 14, s, K_MODE, 0));
                ry += 32;
            } else if (s instanceof Setting.Color) {
                Setting.Color c = (Setting.Color) s;
                fr.drawString(fr.trimStringToWidth(c.name, (int) pw - 16), (int) px + 8, (int) ry, 0xFFCFCFDC);
                for (int i = 0; i < Setting.Color.PALETTE.length; i++) {
                    float cx = px + 8 + i * 14;
                    int col = Setting.Color.PALETTE[i];
                    if (col == c.value) RenderUtil.roundRect(cx - 1, ry + 10, 14, 14, 4, 0xFFFFFFFF);
                    RenderUtil.roundRect(cx, ry + 11, 12, 12, 3, col);
                    if (vis) hits.add(new Hit(cx, ry + 11, 12, 12, s, K_COLOR, i));
                }
                ry += 30;
            }
        }
        noScissor();
        float view = bottom - top, content = ry - start;
        float maxS = Math.max(0f, content - view);
        sscroll = Math.max(0f, Math.min(maxS, sscroll));
        scrollbar(px + pw - 5, top, view, sscroll, maxS);
    }

    private void setSlider(Setting.Num n, float mx) {
        double t = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        double v = n.min + (n.max - n.min) * t;
        if (n.step > 0) v = n.min + Math.round((v - n.min) / n.step) * n.step;
        n.value = Math.max(n.min, Math.min(n.max, v));
    }

    // ------------------------------------------------------------ input
    @Override
    protected void mouseClicked(int mxi, int myi, int btn) throws IOException {
        computeFit();
        float mx = lx(mxi), my = ly(myi), x = ox(), y = oy();

        for (int i = 0; i < TABS.length; i++) {
            if (in(mx, my, x + 10, tabY(y, i), 80, 20)) {
                tab = i; search = ""; scroll = 0; sscroll = 0;
                fixSelected();
                if (tab == T_PROFILES) refreshProfiles();
                return;
            }
        }
        if (in(mx, my, x + 12, y + H - 54, 76, 20)) { mc.displayGuiScreen(new HudEditor(this)); return; }

        float gx = x + 104, gy = y + 36, gw = 3 * CW + 2 * GAP, gh = H - 44;
        boolean inGrid = in(mx, my, gx, gy, gw, gh);
        if (tab != T_PROFILES && inGrid) {
            List<Module> mods = visible();
            for (int i = 0; i < mods.size(); i++) {
                float cx = gx + (i % 3) * (CW + GAP), cy = gy + (i / 3) * (CH + GAP) - scroll;
                if (in(mx, my, cx, cy, CW, CH)) {
                    Module m = mods.get(i);
                    selected = m;
                    sscroll = 0;
                    if (btn == 0 && m.category != Module.Category.CLIENT) m.toggle();
                    return;
                }
            }
        }
        for (Hit h : hits) {
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            if (h.kind >= K_BOOL && h.kind <= K_COLOR && my < clipTop) continue;
            if (h.kind >= K_SAVE && h.kind <= K_DELETE && !inGrid) continue;
            act(h, btn, mx);
            return;
        }
    }

    private void act(Hit h, int btn, float mx) {
        switch (h.kind) {
            case K_MODULE:
                if (selected != null) selected.toggle();
                break;
            case K_BOOL:
                Setting.Bool b = (Setting.Bool) h.s;
                b.value = !b.value;
                break;
            case K_NUM:
                dragging = (Setting.Num) h.s;
                dragX = h.x; dragW = h.w;
                setSlider(dragging, mx);
                break;
            case K_MODE:
                ((Setting.Mode) h.s).cycle(btn == 1 ? -1 : 1);
                break;
            case K_COLOR:
                ((Setting.Color) h.s).value = Setting.Color.PALETTE[h.idx];
                break;
            case K_SAVE:
                setStatus(ConfigManager.saveAs(profileCache.get(h.idx)) ? "Saved '" + profileCache.get(h.idx) + "'" : "Could not save profile");
                refreshProfiles();
                break;
            case K_LOAD:
                setStatus(ConfigManager.load(profileCache.get(h.idx)) ? "Loaded '" + profileCache.get(h.idx) + "'" : "Could not load profile");
                break;
            case K_DELETE:
                String n = profileCache.get(h.idx);
                if (n.equals("default")) setStatus("The default profile cannot be deleted");
                else setStatus(ConfigManager.delete(n) ? "Deleted '" + n + "'" : "Could not delete profile");
                refreshProfiles();
                break;
            case K_CREATE:
                createProfile();
                break;
            case K_SAVE_ACTIVE:
                setStatus(ConfigManager.save2() ? "Saved '" + ConfigManager.active() + "'" : "Could not save profile");
                refreshProfiles();
                break;
            case K_RESET:
                ConfigManager.resetDefaults();
                setStatus("Everything reset to defaults. Save to keep it.");
                break;
            default:
                break;
        }
    }

    private void createProfile() {
        String n = ConfigManager.sanitize(nameInput);
        if (n.isEmpty()) { setStatus("Type a name first"); return; }
        if (ConfigManager.exists(n)) { setStatus("'" + n + "' already exists - use its Save button"); return; }
        if (ConfigManager.saveAs(n)) { setStatus("Created '" + n + "' from your current setup"); nameInput = ""; }
        else setStatus("Could not create profile");
        refreshProfiles();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        computeFit();
        float mx = lx(Mouse.getEventX() * width / mc.displayWidth);
        float x = ox();
        int dir = d > 0 ? -1 : 1;
        if (tab != T_PROFILES && mx >= x + 426) sscroll += dir * 18;
        else scroll += dir * 24;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        boolean typing = tab == T_PROFILES || !search.isEmpty();
        if (key == Keyboard.KEY_ESCAPE) { super.keyTyped(c, key); return; }
        if (!typing && key == FutureClient.openGui.getKeyCode()) { mc.displayGuiScreen(null); return; }
        if (tab == T_PROFILES) {
            if (key == Keyboard.KEY_BACK) { if (!nameInput.isEmpty()) nameInput = nameInput.substring(0, nameInput.length() - 1); }
            else if (key == Keyboard.KEY_RETURN) createProfile();
            else if (nameInput.length() < 16 && c >= 32 && c < 127 && ConfigManager.sanitize(String.valueOf(c)).length() == 1) nameInput += c;
            return;
        }
        if (key == Keyboard.KEY_BACK) {
            if (!search.isEmpty()) { search = search.substring(0, search.length() - 1); scroll = 0; }
        } else if (search.length() < 20 && ChatAllowedCharacters.isAllowedCharacter(c) && c != ' ' || (c == ' ' && !search.isEmpty())) {
            if (search.length() < 20) { search += c; scroll = 0; }
        }
    }
}
