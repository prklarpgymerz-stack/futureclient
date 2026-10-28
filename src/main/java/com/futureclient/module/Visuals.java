package com.futureclient.module;

import com.futureclient.FutureClient;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class Visuals {
    private Visuals() {}

    // ---------------------------------------------------------------- Motion Blur
    /**
     * Real frame-accumulation blur: every frame is blended with the previous (already blended)
     * frame. Frame-rate independent, no shader files needed.
     */
    public static class MotionBlur extends Module {
        public final Setting.Num strength = new Setting.Num("Strength", 5, 1, 10, 1);
        public final Setting.Bool fpsComp = new Setting.Bool("FPS Compensation", true);
        private Framebuffer prev;
        private long last;

        public MotionBlur() {
            super("Motion Blur", "Smooth frame-blending blur", Category.VISUAL, false);
            add(strength);
            add(fpsComp);
        }

        @Override
        public void onDisable() {
            if (prev != null) { prev.deleteFramebuffer(); prev = null; }
        }

        @Override
        public void onOverlayPre(RenderGameOverlayEvent.Pre e) {
            if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (!OpenGlHelper.isFramebufferEnabled()) return;
            int w = mc.displayWidth, h = mc.displayHeight;
            if (w <= 0 || h <= 0) return;
            Framebuffer main = mc.getFramebuffer();

            long now = System.nanoTime();
            double dt = (now - last) / 1.0e9;
            last = now;

            boolean fresh = dt > 0.1;
            if (prev == null) {
                prev = new Framebuffer(w, h, false);
                main.bindFramebuffer(true);
                fresh = true;
            } else if (prev.framebufferWidth != w || prev.framebufferHeight != h) {
                prev.createBindFramebuffer(w, h);
                main.bindFramebuffer(true);
                fresh = true;
            }

            ScaledResolution sr = e.resolution;
            int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
            GlStateManager.disableDepth();
            GlStateManager.disableAlpha();
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);

            if (!fresh) {
                double a60 = Math.min(0.95, 0.30 + strength.value * 0.065);
                double a = fpsComp.value ? Math.pow(a60, Math.max(0.05, Math.min(2.0, dt * 60.0))) : a60;
                quad(prev.framebufferTexture, (float) Math.min(0.97, a), sw, sh, true);
            }

            prev.bindFramebuffer(true);
            quad(main.framebufferTexture, 1f, sw, sh, false);
            main.bindFramebuffer(true);

            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }

        private void quad(int tex, float alpha, int sw, int sh, boolean blend) {
            GlStateManager.bindTexture(tex);
            GlStateManager.enableTexture2D();
            if (blend) {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            } else {
                GlStateManager.disableBlend();
            }
            GlStateManager.color(1f, 1f, 1f, alpha);
            Tessellator t = Tessellator.getInstance();
            WorldRenderer wr = t.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX);
            wr.pos(0, sh, 0).tex(0, 0).endVertex();
            wr.pos(sw, sh, 0).tex(1, 0).endVertex();
            wr.pos(sw, 0, 0).tex(1, 1).endVertex();
            wr.pos(0, 0, 0).tex(0, 1).endVertex();
            t.draw();
        }
    }

    // ---------------------------------------------------------------- Nametag logo
    public static class Nametags extends Module {
        public final Setting.Bool self, others;

        public Nametags() {
            super("Nametags", "Future logo on nametags", Category.VISUAL, true);
            self = add(new Setting.Bool("Show on yourself", true));
            others = add(new Setting.Bool("Show on others", true));
        }
    }

    // ---------------------------------------------------------------- Crosshair
    public static class Crosshair extends Module {
        final Setting.Mode style = new Setting.Mode("Style", 0, "Cross", "Dot", "Cross + Dot");
        final Setting.Num size = new Setting.Num("Length", 6, 1, 20, 1);
        final Setting.Num gap = new Setting.Num("Gap", 3, 0, 12, 1);
        final Setting.Num thick = new Setting.Num("Thickness", 1, 1, 5, 1);
        final Setting.Bool outline = new Setting.Bool("Outline", true);
        final Setting.Color color = new Setting.Color("Color", 0xFFFFFFFF);
        final Setting.Bool chroma = new Setting.Bool("Chroma", false);

        public Crosshair() {
            super("Crosshair", "Custom crosshair", Category.VISUAL, false);
            add(style); add(size); add(gap); add(thick); add(outline); add(color); add(chroma);
        }

        @Override
        public void onOverlayPre(RenderGameOverlayEvent.Pre e) {
            if (e.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
            e.setCanceled(true);
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.gameSettings.thirdPersonView != 0) return;
            int cx = e.resolution.getScaledWidth() / 2, cy = e.resolution.getScaledHeight() / 2;
            int t = thick.i(), g = gap.i(), l = size.i();
            int col = chroma.value ? Hud.rainbow(0f) : color.value;
            int[][] r = new int[5][];
            int n = 0;
            int y0 = cy - t / 2, x0 = cx - t / 2;
            if (!style.is("Dot")) {
                r[n++] = new int[]{cx - g - l, y0, cx - g, y0 + t};
                r[n++] = new int[]{cx + g, y0, cx + g + l, y0 + t};
                r[n++] = new int[]{x0, cy - g - l, x0 + t, cy - g};
                r[n++] = new int[]{x0, cy + g, x0 + t, cy + g + l};
            }
            if (!style.is("Cross")) {
                int ds = Math.max(2, t);
                r[n++] = new int[]{cx - ds / 2, cy - ds / 2, cx - ds / 2 + ds, cy - ds / 2 + ds};
            }
            if (outline.value) for (int i = 0; i < n; i++) Gui.drawRect(r[i][0] - 1, r[i][1] - 1, r[i][2] + 1, r[i][3] + 1, 0xFF000000);
            for (int i = 0; i < n; i++) Gui.drawRect(r[i][0], r[i][1], r[i][2], r[i][3], col);
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }

    // ---------------------------------------------------------------- Block Overlay
    public static class BlockOverlay extends Module {
        final Setting.Bool outline = new Setting.Bool("Outline", true);
        final Setting.Num thick = new Setting.Num("Outline Width", 2, 1, 6, 1);
        final Setting.Num fill = new Setting.Num("Fill Opacity", 20, 0, 80, 5);
        final Setting.Color color = new Setting.Color("Color", 0xFFAA77FF);
        final Setting.Bool chroma = new Setting.Bool("Chroma", false);

        public BlockOverlay() {
            super("Block Overlay", "Custom block selection outline", Category.VISUAL, false);
            add(outline); add(thick); add(fill); add(color); add(chroma);
        }

        @Override
        public void onBlockHighlight(DrawBlockHighlightEvent e) {
            if (e.target == null || e.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
            Minecraft mc = Minecraft.getMinecraft();
            World world = mc.theWorld;
            BlockPos bp = e.target.getBlockPos();
            Block b = world.getBlockState(bp).getBlock();
            if (b.getMaterial() == Material.air || !world.getWorldBorder().contains(bp)) return;
            e.setCanceled(true);

            EntityPlayer p = e.player;
            double t = e.partialTicks;
            double dx = p.lastTickPosX + (p.posX - p.lastTickPosX) * t;
            double dy = p.lastTickPosY + (p.posY - p.lastTickPosY) * t;
            double dz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * t;
            b.setBlockBoundsBasedOnState(world, bp);
            AxisAlignedBB bb = b.getSelectedBoundingBox(world, bp).expand(0.002, 0.002, 0.002).offset(-dx, -dy, -dz);

            int c = chroma.value ? Hud.rainbow(0f) : color.value;
            float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, bl = (c & 255) / 255f;

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            if (fill.value > 0) {
                GlStateManager.disableCull();
                GlStateManager.color(r, g, bl, fill.f() / 100f);
                fillBox(bb);
                GlStateManager.enableCull();
            }
            if (outline.value) {
                GL11.glLineWidth(thick.f());
                GlStateManager.color(r, g, bl, 1f);
                RenderGlobal.drawSelectionBoundingBox(bb);
            }
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GL11.glLineWidth(2.0F);
            GlStateManager.color(1f, 1f, 1f, 1f);
        }

        private static void v(WorldRenderer w, double x, double y, double z) { w.pos(x, y, z).endVertex(); }

        private static void fillBox(AxisAlignedBB b) {
            Tessellator t = Tessellator.getInstance();
            WorldRenderer w = t.getWorldRenderer();
            w.begin(7, DefaultVertexFormats.POSITION);
            v(w, b.minX, b.minY, b.minZ); v(w, b.maxX, b.minY, b.minZ); v(w, b.maxX, b.minY, b.maxZ); v(w, b.minX, b.minY, b.maxZ);
            v(w, b.minX, b.maxY, b.minZ); v(w, b.minX, b.maxY, b.maxZ); v(w, b.maxX, b.maxY, b.maxZ); v(w, b.maxX, b.maxY, b.minZ);
            v(w, b.minX, b.minY, b.minZ); v(w, b.minX, b.maxY, b.minZ); v(w, b.maxX, b.maxY, b.minZ); v(w, b.maxX, b.minY, b.minZ);
            v(w, b.minX, b.minY, b.maxZ); v(w, b.maxX, b.minY, b.maxZ); v(w, b.maxX, b.maxY, b.maxZ); v(w, b.minX, b.maxY, b.maxZ);
            v(w, b.minX, b.minY, b.minZ); v(w, b.minX, b.minY, b.maxZ); v(w, b.minX, b.maxY, b.maxZ); v(w, b.minX, b.maxY, b.minZ);
            v(w, b.maxX, b.minY, b.minZ); v(w, b.maxX, b.maxY, b.minZ); v(w, b.maxX, b.maxY, b.maxZ); v(w, b.maxX, b.minY, b.maxZ);
            t.draw();
        }
    }

    // ---------------------------------------------------------------- Fullbright
    public static class Fullbright extends Module {
        private float prev = 1f;

        public Fullbright() { super("Fullbright", "See clearly in the dark", Category.VISUAL, false); }

        @Override
        public void onEnable() {
            float g = Minecraft.getMinecraft().gameSettings.gammaSetting;
            prev = g > 5f ? 1f : g;
            Minecraft.getMinecraft().gameSettings.gammaSetting = 100f;
        }

        @Override
        public void onTick() { Minecraft.getMinecraft().gameSettings.gammaSetting = 100f; }

        @Override
        public void onDisable() { Minecraft.getMinecraft().gameSettings.gammaSetting = prev; }
    }

    // ---------------------------------------------------------------- Hide Fire
    public static class HideFire extends Module {
        public HideFire() { super("Hide Fire", "Removes the fire overlay on your screen", Category.VISUAL, false); }

        @Override
        public void onFireOverlay(RenderBlockOverlayEvent e) {
            if (e.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE) e.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- Time Changer
    public static class TimeChanger extends Module {
        final Setting.Mode time = new Setting.Mode("Time", 1, "Sunrise", "Day", "Noon", "Sunset", "Night", "Midnight");
        final Setting.Bool weather = new Setting.Bool("Clear Weather", true);
        private static final long[] TICKS = {23000L, 1000L, 6000L, 12500L, 14000L, 18000L};

        public TimeChanger() {
            super("Time Changer", "Set the time of day (client side)", Category.VISUAL, false);
            add(time); add(weather);
        }

        @Override
        public void onTick() {
            World w = Minecraft.getMinecraft().theWorld;
            if (w == null) return;
            w.setWorldTime(TICKS[time.index]);
            if (weather.value) { w.setRainStrength(0f); w.setThunderStrength(0f); }
        }
    }

    // ---------------------------------------------------------------- Zoom
    public static class Zoom extends Module {
        final Setting.Num zoom = new Setting.Num("Zoom Level", 4, 1.5, 12, 0.5);
        final Setting.Bool smooth = new Setting.Bool("Smooth Camera", true);
        final Setting.Bool scroll = new Setting.Bool("Scroll to Change", true);
        private boolean wasZoom, prevSmooth;

        public Zoom() {
            super("Zoom", "Hold C to zoom in (rebind in Controls)", Category.VISUAL, true);
            add(zoom); add(smooth); add(scroll);
        }

        private boolean active() {
            Minecraft mc = Minecraft.getMinecraft();
            if (FutureClient.zoomKey == null || mc.currentScreen != null) return false;
            int kc = FutureClient.zoomKey.getKeyCode();
            if (kc < 0) return Mouse.isButtonDown(kc + 100);
            return kc > 0 && Keyboard.isKeyDown(kc);
        }

        @Override
        public void onFov(FOVUpdateEvent e) {
            if (active()) e.newfov /= (float) zoom.value;
        }

        @Override
        public void onMouse(MouseEvent e) {
            if (e.dwheel != 0 && scroll.value && active()) {
                zoom.value = Math.max(zoom.min, Math.min(zoom.max, zoom.value + (e.dwheel > 0 ? zoom.step : -zoom.step)));
                e.setCanceled(true);
            }
        }

        @Override
        public void onTick() {
            boolean a = active();
            Minecraft mc = Minecraft.getMinecraft();
            if (smooth.value) {
                if (a && !wasZoom) { prevSmooth = mc.gameSettings.smoothCamera; mc.gameSettings.smoothCamera = true; }
                else if (!a && wasZoom) mc.gameSettings.smoothCamera = prevSmooth;
            }
            wasZoom = a;
        }

        @Override
        public void onDisable() {
            if (wasZoom) Minecraft.getMinecraft().gameSettings.smoothCamera = prevSmooth;
            wasZoom = false;
        }
    }
}
