package com.futureclient.gui;

import com.futureclient.ConfigManager;
import com.futureclient.FutureClient;
import com.futureclient.module.Hud;
import com.futureclient.module.Module;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Drag HUD elements around, scroll over one to resize it. */
public class HudEditor extends GuiScreen {
    private final GuiScreen parent;
    private Hud drag;
    private float offX, offY;
    private boolean snapX, snapY;

    public HudEditor(GuiScreen parent) { this.parent = parent; }

    @Override public boolean doesGuiPauseGame() { return false; }

    private List<Hud> huds() {
        List<Hud> out = new ArrayList<Hud>();
        for (Module m : FutureClient.modules.all) if (m.enabled && m instanceof Hud) out.add((Hud) m);
        return out;
    }

    private static boolean in(float mx, float my, Hud h) {
        return mx >= h.rx - 3 && my >= h.ry - 3 && mx <= h.rx + h.rw + 3 && my <= h.ry + h.rh + 3;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0x60000000);
        ScaledResolution sr = new ScaledResolution(mc);
        int acc = FutureClient.modules.client.accent.value;
        if (drag != null) {
            if (Mouse.isButtonDown(0)) move(sr, mx, my); else drag = null;
        }
        List<Hud> list = huds();
        Hud.editor = true;
        for (Hud h : list) h.draw(sr);
        Hud.editor = false;
        for (Hud h : list) {
            boolean hot = h == drag || (drag == null && in(mx, my, h));
            outline(h.rx, h.ry, h.rw, h.rh, hot ? acc : 0x66FFFFFF);
            if (hot) mc.fontRendererObj.drawStringWithShadow(h.name, h.rx, Math.max(2, h.ry - 12), 0xFFFFFFFF);
        }
        if (drag != null && snapX) drawRect(width / 2, 0, width / 2 + 1, height, 0x99FFFFFF);
        if (drag != null && snapY) drawRect(0, height / 2, width, height / 2 + 1, 0x99FFFFFF);
        String t = list.isEmpty() ? "No HUD elements enabled - turn some on in the Mods tab" : "Drag to move  |  Scroll over an element to resize  |  ESC to go back";
        drawCenteredString(fontRendererObj, t, width / 2, 6, 0xFFDDDDEE);
    }

    private void outline(float x, float y, float w, float h, int c) {
        int x0 = (int) x - 2, y0 = (int) y - 2, x1 = (int) (x + w) + 2, y1 = (int) (y + h) + 2;
        drawRect(x0, y0, x1, y0 + 1, c);
        drawRect(x0, y1 - 1, x1, y1, c);
        drawRect(x0, y0, x0 + 1, y1, c);
        drawRect(x1 - 1, y0, x1, y1, c);
    }

    private void move(ScaledResolution sr, int mx, int my) {
        float freeW = Math.max(1f, sr.getScaledWidth() - drag.rw), freeH = Math.max(1f, sr.getScaledHeight() - drag.rh);
        float nx = mx - offX, ny = my - offY;
        snapX = snapY = false;
        if (Math.abs(nx - freeW / 2f) < 4) { nx = freeW / 2f; snapX = true; }
        if (Math.abs(ny - freeH / 2f) < 4) { ny = freeH / 2f; snapY = true; }
        if (nx < 3) nx = 0;
        if (nx > freeW - 3) nx = freeW;
        if (ny < 3) ny = 0;
        if (ny > freeH - 3) ny = freeH;
        drag.posX.value = Math.max(0, Math.min(1, nx / freeW));
        drag.posY.value = Math.max(0, Math.min(1, ny / freeH));
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;
        List<Hud> list = huds();
        for (int i = list.size() - 1; i >= 0; i--) {
            Hud h = list.get(i);
            if (in(mx, my, h)) { drag = h; offX = mx - h.rx; offY = my - h.ry; return; }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        for (Hud h : huds()) {
            if (in(mx, my, h)) {
                h.scale.value = Math.max(h.scale.min, Math.min(h.scale.max, h.scale.value + (d > 0 ? 0.1 : -0.1)));
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        super.keyTyped(c, key);
    }

    @Override
    public void onGuiClosed() {
        Hud.editor = false;
        ConfigManager.save();
    }
}
