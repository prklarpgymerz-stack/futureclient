package com.futureclient.module;

import com.futureclient.util.ClickTracker;
import com.futureclient.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public final List<Module> all = new ArrayList<Module>();
    public final Nametags nametags = new Nametags();

    public ModuleManager() {
        all.add(new MotionBlur());
        all.add(new Keystrokes());
        all.add(new CpsHud());
        all.add(new PingHud());
        all.add(new FpsHud());
        all.add(nametags);
    }

    public void tick() { for (Module m : all) if (m.enabled) m.onTick(); }

    public void render(ScaledResolution sr) { for (Module m : all) if (m.enabled) m.render(sr); }

    // ---------------- Motion Blur ----------------
    public static class MotionBlur extends Module {
        private static Field LIST;
        private ShaderGroup ours;
        public final Setting.Num amount;

        MotionBlur() {
            super("Motion Blur", "Smooth screen trails", false);
            amount = add(new Setting.Num("Amount", 5, 1, 10, 1));
        }

        @Override
        public void onTick() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || !OpenGlHelper.shadersSupported) return;
            ShaderGroup g = mc.entityRenderer.getShaderGroup();
            if (g == null) {
                mc.entityRenderer.loadShader(new ResourceLocation("futureclient", "shaders/post/motionblur.json"));
                ours = mc.entityRenderer.getShaderGroup();
                g = ours;
            }
            if (g == null || g != ours) return;
            try {
                if (LIST == null) LIST = ReflectionHelper.findField(ShaderGroup.class, "listShaders", "field_148031_d");
                @SuppressWarnings("unchecked")
                List<Shader> list = (List<Shader>) LIST.get(g);
                float v = 0.5f + (float) (amount.value / 10.0) * 0.45f;
                for (Shader s : list) {
                    ShaderUniform u = s.getShaderManager().getShaderUniform("Phosphor");
                    if (u != null) u.set(v, v, v);
                }
            } catch (Exception ignored) {}
        }

        @Override
        public void onDisable() {
            Minecraft mc = Minecraft.getMinecraft();
            if (ours != null && mc.entityRenderer.getShaderGroup() == ours) mc.entityRenderer.stopUseShader();
            ours = null;
        }
    }

    // ---------------- Simple text HUD ----------------
    public abstract static class Hud extends Module {
        final Setting.Num px, py;
        final Setting.Bool bg;

        Hud(String n, String d, double x, double y) {
            super(n, d, true);
            px = add(new Setting.Num("Position X", x, 0, 100, 1));
            py = add(new Setting.Num("Position Y", y, 0, 100, 1));
            bg = add(new Setting.Bool("Background", true));
        }

        abstract String text();

        @Override
        public void render(ScaledResolution sr) {
            FontRenderer f = Minecraft.getMinecraft().fontRendererObj;
            String t = text();
            int w = f.getStringWidth(t) + 12, h = 16;
            float x = (sr.getScaledWidth() - w) * (float) px.value / 100f;
            float y = (sr.getScaledHeight() - h) * (float) py.value / 100f;
            if (bg.value) RenderUtil.roundRect(x, y, w, h, 4, 0x90000000);
            f.drawStringWithShadow(t, x + 6, y + 4, 0xFFFFFFFF);
        }
    }

    public static class CpsHud extends Hud {
        CpsHud() { super("CPS", "Clicks per second", 2, 2); }
        String text() { return ClickTracker.get(true) + " | " + ClickTracker.get(false) + " CPS"; }
    }

    public static class PingHud extends Hud {
        PingHud() { super("Ping", "Server latency in ms", 2, 10); }
        String text() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.getNetHandler() == null || mc.isSingleplayer()) return "0 ms";
            NetworkPlayerInfo i = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            return (i == null ? 0 : i.getResponseTime()) + " ms";
        }
    }

    public static class FpsHud extends Hud {
        FpsHud() { super("FPS", "Frames per second", 2, 18); }
        String text() { return Minecraft.getDebugFPS() + " FPS"; }
    }

    // ---------------- Keystrokes ----------------
    public static class Keystrokes extends Module {
        final Setting.Num px, py;
        final Setting.Bool bg, cps;
        private final float[] fade = new float[7];

        Keystrokes() {
            super("Keystrokes", "WASD, mouse and space", true);
            px = add(new Setting.Num("Position X", 2, 0, 100, 1));
            py = add(new Setting.Num("Position Y", 45, 0, 100, 1));
            bg = add(new Setting.Bool("Background", true));
            cps = add(new Setting.Bool("Show CPS", true));
        }

        @Override
        public void render(ScaledResolution sr) {
            Minecraft mc = Minecraft.getMinecraft();
            FontRenderer f = mc.fontRendererObj;
            GameSettings gs = mc.gameSettings;
            KeyBinding[] b = {gs.keyBindForward, gs.keyBindLeft, gs.keyBindBack, gs.keyBindRight,
                              gs.keyBindAttack, gs.keyBindUseItem, gs.keyBindJump};
            String[] l = {"W", "A", "S", "D", "LMB", "RMB", ""};
            int[][] r = {{24, 0, 22, 22}, {0, 24, 22, 22}, {24, 24, 22, 22}, {48, 24, 22, 22},
                         {0, 48, 34, 22}, {36, 48, 34, 22}, {0, 72, 70, 12}};
            float bx = (sr.getScaledWidth() - 70) * (float) px.value / 100f;
            float by = (sr.getScaledHeight() - 84) * (float) py.value / 100f;
            for (int i = 0; i < 7; i++) {
                fade[i] += ((b[i].isKeyDown() ? 1f : 0f) - fade[i]) * 0.35f;
                float x = bx + r[i][0], y = by + r[i][1];
                float w = r[i][2], h = r[i][3];
                RenderUtil.roundRect(x, y, w, h, 3, RenderUtil.lerp(bg.value ? 0x90000000 : 0x00000000, 0xC8FFFFFF, fade[i]));
                int tc = RenderUtil.lerp(0xFFFFFFFF, 0xFF111111, fade[i]);
                boolean sh = fade[i] < 0.5f;
                if (i == 6) {
                    RenderUtil.roundRect(x + w / 2f - 12, y + h / 2f - 1, 24, 2, 1, tc);
                } else if (i >= 4 && cps.value) {
                    f.drawString(l[i], x + (w - f.getStringWidth(l[i])) / 2f, y + 4, tc, sh);
                    String c = ClickTracker.get(i == 4) + " CPS";
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(x + w / 2f, y + 14, 0);
                    GlStateManager.scale(0.5f, 0.5f, 1f);
                    f.drawString(c, -f.getStringWidth(c) / 2f, 0, tc, sh);
                    GlStateManager.popMatrix();
                } else {
                    f.drawString(l[i], x + (w - f.getStringWidth(l[i])) / 2f, y + (h - 8) / 2f, tc, sh);
                }
            }
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }

    // ---------------- Nametag logo ----------------
    public static class Nametags extends Module {
        public final Setting.Bool self, others;

        Nametags() {
            super("Nametags", "Future logo on nametags", true);
            self = add(new Setting.Bool("Show on yourself", true));
            others = add(new Setting.Bool("Show on others", true));
        }
    }
}
