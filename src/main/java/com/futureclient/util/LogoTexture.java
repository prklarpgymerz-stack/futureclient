package com.futureclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Generates the Future Client logo at runtime (no image file needed). */
public class LogoTexture {
    private static ResourceLocation loc;

    public static ResourceLocation get() {
        if (loc == null) {
            loc = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("futureclient_logo", new DynamicTexture(make()));
        }
        return loc;
    }

    private static BufferedImage make() {
        int s = 64;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // rounded hexagon with violet -> cyan gradient
        Path2D.Double hex = new Path2D.Double();
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(60 * i - 90);
            double px = 32 + 26 * Math.cos(a), py = 32 + 26 * Math.sin(a);
            if (i == 0) hex.moveTo(px, py); else hex.lineTo(px, py);
        }
        hex.closePath();
        g.setPaint(new GradientPaint(8, 4, new Color(0x8B5CFF), 56, 60, new Color(0x22D3EE)));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(hex);
        g.fill(hex);

        // soft inner ring
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(1.5f));
        Path2D.Double ring = new Path2D.Double();
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(60 * i - 90);
            double px = 32 + 21 * Math.cos(a), py = 32 + 21 * Math.sin(a);
            if (i == 0) ring.moveTo(px, py); else ring.lineTo(px, py);
        }
        ring.closePath();
        g.draw(ring);

        // slanted "F"
        Path2D.Double f = new Path2D.Double();
        f.moveTo(22, 47); f.lineTo(22, 17); f.lineTo(47, 17); f.lineTo(43.5, 25);
        f.lineTo(30, 25); f.lineTo(30, 30.5); f.lineTo(41, 30.5); f.lineTo(37.5, 38);
        f.lineTo(30, 38); f.lineTo(30, 47); f.closePath();
        AffineTransform at = new AffineTransform();
        at.translate(32, 32); at.shear(-0.18, 0); at.translate(-32, -32);
        g.setColor(Color.WHITE);
        g.fill(at.createTransformedShape(f));

        g.dispose();
        return img;
    }
}
