package com.futureclient.util;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class RenderUtil {
    public static void color(int c) {
        GlStateManager.color((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, (c >>> 24) / 255f);
    }

    public static void roundRect(float x, float y, float w, float h, float r, int c) {
        r = Math.min(r, Math.min(w, h) / 2f);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        color(c);
        GL11.glBegin(GL11.GL_POLYGON);
        corner(x + w - r, y + r, r, 0, 90);
        corner(x + w - r, y + h - r, r, 90, 180);
        corner(x + r, y + h - r, r, 180, 270);
        corner(x + r, y + r, r, 270, 360);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void corner(float cx, float cy, float r, int a0, int a1) {
        for (int a = a0; a <= a1; a += 10) {
            double rad = Math.toRadians(a);
            GL11.glVertex2d(cx + Math.sin(rad) * r, cy - Math.cos(rad) * r);
        }
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
