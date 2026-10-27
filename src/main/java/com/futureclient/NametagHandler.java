package com.futureclient;

import com.futureclient.net.FutureNet;
import com.futureclient.util.LogoTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class NametagHandler {
    private final Minecraft mc = Minecraft.getMinecraft();

    /** Other players: vanilla nametag stays, we only add the logo in front of it. */
    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public void onSpecials(RenderLivingEvent.Specials.Post e) {
        if (!FutureClient.modules.nametags.enabled || !FutureClient.modules.nametags.others.value) return;
        if (!(e.entity instanceof EntityPlayer) || e.entity == mc.thePlayer) return;
        EntityPlayer p = (EntityPlayer) e.entity;
        if (!FutureNet.isUser(p.getUniqueID())) return;
        double d = p.getDistanceSqToEntity(mc.thePlayer);
        double range = p.isSneaking() ? 32 : 64;
        if (d >= range * range || p.isInvisibleToPlayer(mc.thePlayer) || p.riddenByEntity != null || !Minecraft.isGuiEnabled()) return;
        draw(p, e.x, e.y, e.z, p.getDisplayName().getFormattedText(), false, d);
    }

    /** Yourself: vanilla never draws your own tag, so we draw a vanilla-style one (third person only). */
    @SubscribeEvent
    public void onWorldLast(RenderWorldLastEvent e) {
        if (!FutureClient.modules.nametags.enabled || !FutureClient.modules.nametags.self.value) return;
        EntityPlayer p = mc.thePlayer;
        if (p == null || mc.gameSettings.thirdPersonView == 0 || !Minecraft.isGuiEnabled() || mc.getRenderViewEntity() == null) return;
        Entity v = mc.getRenderViewEntity();
        double t = e.partialTicks;
        double x = p.lastTickPosX + (p.posX - p.lastTickPosX) * t - (v.lastTickPosX + (v.posX - v.lastTickPosX) * t);
        double y = p.lastTickPosY + (p.posY - p.lastTickPosY) * t - (v.lastTickPosY + (v.posY - v.lastTickPosY) * t);
        double z = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * t - (v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * t);
        draw(p, x, y, z, p.getDisplayName().getFormattedText(), true, 0);
        GlStateManager.enableAlpha();
    }

    private void draw(EntityPlayer p, double x, double y, double z, String name, boolean full, double dist) {
        RenderManager rm = mc.getRenderManager();
        FontRenderer fr = mc.fontRendererObj;
        float s = 0.02666667F;
        boolean sneak = p.isSneaking();
        double yy = y - (p.isChild() ? p.height / 2.0 : 0.0);
        if (!full && !sneak && dist < 100) {
            ScoreObjective o = p.getWorldScoreboard().getObjectiveInDisplaySlot(2);
            if (o != null) yy += fr.FONT_HEIGHT * 1.15F * s;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) yy + p.height + 0.5F, (float) z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate(rm.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-s, -s, s);
        GlStateManager.disableLighting();
        if (sneak) GlStateManager.translate(0f, 9.374999F, 0f);
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int w = fr.getStringWidth(name);
        int j = w / 2;
        if (!sneak) GlStateManager.disableDepth();

        if (full) {
            GlStateManager.disableTexture2D();
            Tessellator t = Tessellator.getInstance();
            WorldRenderer wr = t.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(-j - 1, -1, 0).color(0f, 0f, 0f, 0.25f).endVertex();
            wr.pos(-j - 1, 8, 0).color(0f, 0f, 0f, 0.25f).endVertex();
            wr.pos(j + 1, 8, 0).color(0f, 0f, 0f, 0.25f).endVertex();
            wr.pos(j + 1, -1, 0).color(0f, 0f, 0f, 0.25f).endVertex();
            t.draw();
            GlStateManager.enableTexture2D();
            fr.drawString(name, -w / 2, 0, 553648127);
        }

        int lx = -w / 2 - 11;
        logo(lx, sneak ? 0.5f : 0.35f);
        if (!sneak) {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            if (full) fr.drawString(name, -w / 2, 0, -1);
            logo(lx, 1f);
        } else if (full) {
            // sneaking vanilla tag is only the dim pass; already drawn above
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    private void logo(int x, float alpha) {
        mc.getTextureManager().bindTexture(LogoTexture.get());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, 0, 0, 0, 9, 9, 9, 9);
    }
}
