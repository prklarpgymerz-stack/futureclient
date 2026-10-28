package com.futureclient.module;

import com.futureclient.util.ClickTracker;
import com.futureclient.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class Huds {
    private Huds() {}

    // ---------------------------------------------------------------- FPS
    public static class Fps extends Hud.Text {
        final Setting.Bool label = new Setting.Bool("Show Label", true);

        public Fps() {
            super("FPS", "Frames per second", 1, 1);
            add(label);
        }

        @Override
        protected List<String> lines() {
            return Collections.singletonList(net.minecraft.client.Minecraft.getDebugFPS() + (label.value ? " FPS" : ""));
        }
    }

    // ---------------------------------------------------------------- CPS
    public static class Cps extends Hud.Text {
        final Setting.Mode mode = new Setting.Mode("Display", 0, "Left | Right", "Left", "Right", "Total");

        public Cps() {
            super("CPS", "Clicks per second", 1, 7);
            add(mode);
        }

        @Override
        protected List<String> lines() {
            int l = ClickTracker.get(true), r = ClickTracker.get(false);
            String s;
            switch (mode.index) {
                case 1: s = l + " CPS"; break;
                case 2: s = r + " CPS"; break;
                case 3: s = (l + r) + " CPS"; break;
                default: s = l + " | " + r + " CPS"; break;
            }
            return Collections.singletonList(s);
        }
    }

    // ---------------------------------------------------------------- Ping
    public static class Ping extends Hud.Text {
        final Setting.Bool dynamic = new Setting.Bool("Color by Latency", true);
        private int ping;

        public Ping() {
            super("Ping", "Server latency in ms", 1, 13);
            add(dynamic);
        }

        @Override
        protected List<String> lines() {
            ping = 0;
            if (mc.thePlayer != null && mc.getNetHandler() != null && !mc.isSingleplayer()) {
                NetworkPlayerInfo i = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                if (i != null) ping = i.getResponseTime();
            }
            return Collections.singletonList(ping + " ms");
        }

        @Override
        protected int lineColor(int i, int base) {
            if (!dynamic.value) return base;
            return ping < 80 ? 0xFF55FF55 : ping < 150 ? 0xFFFFFF55 : 0xFFFF5555;
        }
    }

    // ---------------------------------------------------------------- Keystrokes
    public static class Keystrokes extends Hud {
        final Setting.Bool mouse = new Setting.Bool("Mouse Buttons", true);
        final Setting.Bool cps = new Setting.Bool("Show CPS", false);
        final Setting.Bool space = new Setting.Bool("Space Bar", true);
        final Setting.Num fade = new Setting.Num("Fade Speed", 5, 1, 10, 1);
        final Setting.Num round = new Setting.Num("Corner Radius", 3, 0, 8, 1);
        final Setting.Color pressed = new Setting.Color("Pressed Color", 0xFFFFFFFF);
        private final float[] fadeA = new float[7];

        public Keystrokes() {
            super("Keystrokes", "WASD, mouse and space", Category.HUD, 1, 45, true);
            add(mouse); add(cps); add(space); add(fade); add(round); add(pressed);
        }

        @Override
        protected void measure() {
            w = 70;
            h = mouse.value ? (space.value ? 88 : 70) : (space.value ? 64 : 46);
        }

        private static int lum(int c) {
            return ((c >> 16 & 255) * 299 + (c >> 8 & 255) * 587 + (c & 255) * 114) / 1000;
        }

        @Override
        protected void paint(int c) {
            GameSettings gs = mc.gameSettings;
            FontRenderer f = mc.fontRendererObj;
            KeyBinding[] b = {gs.keyBindForward, gs.keyBindLeft, gs.keyBindBack, gs.keyBindRight,
                              gs.keyBindAttack, gs.keyBindUseItem, gs.keyBindJump};
            String[] l = {"W", "A", "S", "D", "LMB", "RMB", ""};
            float my = 48f, sy = mouse.value ? 72f : 48f;
            float[][] r = {{24, 0, 22, 22}, {0, 24, 22, 22}, {24, 24, 22, 22}, {48, 24, 22, 22},
                           {0, my, 34, 22}, {36, my, 34, 22}, {0, sy, 70, 16}};
            int dark = lum(pressed.value) > 140 ? 0xFF111111 : 0xFFFFFFFF;
            int base = bgOn() ? ((int) (bgAlpha.value * 2.55) << 24) : 0;
            int on = (pressed.value & 0xFFFFFF) | 0xC8000000;
            for (int i = 0; i < 7; i++) {
                if ((i == 4 || i == 5) && !mouse.value) continue;
                if (i == 6 && !space.value) continue;
                fadeA[i] += ((b[i].isKeyDown() ? 1f : 0f) - fadeA[i]) * (0.12f + fade.f() * 0.04f);
                float x = r[i][0], y = r[i][1], kw = r[i][2], kh = r[i][3];
                RenderUtil.roundRect(x, y, kw, kh, round.f(), RenderUtil.lerp(base, on, fadeA[i]));
                int tc = RenderUtil.lerp(c, dark, fadeA[i]);
                boolean sh = shadow.value && fadeA[i] < 0.5f;
                if (i == 6) {
                    RenderUtil.roundRect(x + kw / 2f - 14, y + kh / 2f - 1, 28, 2, 1, tc);
                } else if (i >= 4 && cps.value) {
                    f.drawString(l[i], x + (kw - f.getStringWidth(l[i])) / 2f, y + 4, tc, sh);
                    String s = ClickTracker.get(i == 4) + " CPS";
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(x + kw / 2f, y + 14, 0);
                    GlStateManager.scale(0.5f, 0.5f, 1f);
                    f.drawString(s, -f.getStringWidth(s) / 2f, 0, tc, sh);
                    GlStateManager.popMatrix();
                } else {
                    f.drawString(l[i], x + (kw - f.getStringWidth(l[i])) / 2f, y + (kh - 8) / 2f, tc, sh);
                }
            }
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }

    // ---------------------------------------------------------------- Coordinates
    public static class Coords extends Hud.Text {
        final Setting.Mode layout = new Setting.Mode("Layout", 0, "Stacked", "Single Line");
        final Setting.Num decimals = new Setting.Num("Decimals", 0, 0, 2, 1);

        public Coords() {
            super("Coordinates", "Your XYZ position", 1, 19);
            add(layout); add(decimals);
        }

        private String f(double v) {
            int d = decimals.i();
            return d == 0 ? String.valueOf((int) Math.floor(v)) : String.format(Locale.US, "%." + d + "f", v);
        }

        @Override
        protected List<String> lines() {
            EntityPlayer p = mc.thePlayer;
            if (p == null) return Collections.emptyList();
            if (layout.is("Single Line")) return Collections.singletonList("XYZ: " + f(p.posX) + " / " + f(p.posY) + " / " + f(p.posZ));
            return Arrays.asList("X: " + f(p.posX), "Y: " + f(p.posY), "Z: " + f(p.posZ));
        }
    }

    // ---------------------------------------------------------------- Direction
    public static class Direction extends Hud.Text {
        final Setting.Bool axis = new Setting.Bool("Show Axis", true);
        final Setting.Bool label = new Setting.Bool("Show Label", true);

        public Direction() {
            super("Direction", "Which way you are facing", 1, 27);
            add(axis); add(label);
        }

        @Override
        protected List<String> lines() {
            EntityPlayer p = mc.thePlayer;
            if (p == null) return Collections.emptyList();
            int d = MathHelper.floor_double(p.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
            String[] n = {"South", "West", "North", "East"};
            String[] a = {"+Z", "-X", "-Z", "+X"};
            String s = n[d] + (axis.value ? " (" + a[d] + ")" : "");
            return Collections.singletonList(label.value ? "Facing: " + s : s);
        }
    }

    // ---------------------------------------------------------------- Clock
    public static class Clock extends Hud.Text {
        final Setting.Mode format = new Setting.Mode("Format", 0, "24 Hour", "12 Hour");
        final Setting.Bool seconds = new Setting.Bool("Show Seconds", false);

        public Clock() {
            super("Clock", "Your local time", 50, 1);
            add(format); add(seconds);
        }

        @Override
        protected List<String> lines() {
            String p = format.is("24 Hour") ? (seconds.value ? "HH:mm:ss" : "HH:mm") : (seconds.value ? "hh:mm:ss a" : "hh:mm a");
            return Collections.singletonList(new SimpleDateFormat(p, Locale.US).format(new Date()));
        }
    }

    // ---------------------------------------------------------------- Speed
    public static class Speed extends Hud.Text {
        final Setting.Bool vertical = new Setting.Bool("Include Vertical", false);
        final Setting.Bool label = new Setting.Bool("Show Label", true);
        private double bps;

        public Speed() {
            super("Speed", "Blocks per second", 1, 33);
            add(vertical); add(label);
        }

        @Override
        public void onTick() {
            EntityPlayer p = mc.thePlayer;
            if (p == null) return;
            double dx = p.posX - p.prevPosX, dz = p.posZ - p.prevPosZ, dy = p.posY - p.prevPosY;
            double v = Math.sqrt(dx * dx + dz * dz + (vertical.value ? dy * dy : 0)) * 20.0;
            bps += (v - bps) * 0.3;
        }

        @Override
        protected List<String> lines() {
            return Collections.singletonList((label.value ? "Speed: " : "") + String.format(Locale.US, "%.1f", bps) + " b/s");
        }
    }

    // ---------------------------------------------------------------- Armor Status
    public static class Armor extends Hud {
        final Setting.Mode layout = new Setting.Mode("Layout", 0, "Vertical", "Horizontal");
        final Setting.Mode dur = new Setting.Mode("Durability", 0, "Number", "Percent", "None");
        final Setting.Bool held = new Setting.Bool("Show Held Item", true);
        private final List<ItemStack> stacks = new ArrayList<ItemStack>();

        public Armor() {
            super("Armor Status", "Armor and held item durability", Category.HUD, 99, 60, true);
            add(layout); add(dur); add(held);
        }

        @Override
        public void addCommon() {
            super.addCommon();
            bg.value = false;
            bg.anim = 0f;
            bg.def = bg.save();
        }

        private boolean vert() { return layout.is("Vertical"); }
        private boolean txt() { return !dur.is("None"); }

        @Override
        protected void measure() {
            stacks.clear();
            EntityPlayer p = mc.thePlayer;
            if (p != null) {
                for (int i = 3; i >= 0; i--) {
                    ItemStack s = p.inventory.armorInventory[i];
                    if (s != null) stacks.add(s);
                }
                if (held.value && p.getHeldItem() != null) stacks.add(p.getHeldItem());
            }
            if (stacks.isEmpty() && editor) {
                stacks.add(new ItemStack(Items.iron_helmet));
                stacks.add(new ItemStack(Items.iron_chestplate));
                stacks.add(new ItemStack(Items.iron_leggings));
                stacks.add(new ItemStack(Items.iron_boots));
            }
            int n = stacks.size();
            if (vert()) { w = n == 0 ? 0 : 18 + (txt() ? 34 : 0); h = n * 18; }
            else { w = n * (txt() ? 32 : 20); h = n == 0 ? 0 : 18 + (txt() ? 10 : 0); }
        }

        private String durText(ItemStack s) {
            if (!txt()) return null;
            if (s.isItemStackDamageable()) {
                int max = s.getMaxDamage(), left = max - s.getItemDamage();
                return dur.is("Percent") ? (left * 100 / Math.max(1, max)) + "%" : String.valueOf(left);
            }
            return s.stackSize > 1 ? String.valueOf(s.stackSize) : null;
        }

        private int durColor(ItemStack s) {
            if (!s.isItemStackDamageable()) return 0xFFFFFFFF;
            float frac = (s.getMaxDamage() - s.getItemDamage()) / (float) Math.max(1, s.getMaxDamage());
            return 0xFF000000 | java.awt.Color.HSBtoRGB(Math.max(0f, Math.min(1f, frac)) * 0.33f, 0.85f, 1f);
        }

        private void item(ItemStack s, int x, int y) {
            GlStateManager.pushMatrix();
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(s, x, y);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }

        @Override
        protected void paint(int c) {
            if (stacks.isEmpty()) return;
            bgBox(0, 0, w, h);
            boolean v = vert();
            int slot = txt() ? 32 : 20;
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack s = stacks.get(i);
                String t = durText(s);
                if (v) {
                    item(s, 0, i * 18 + 1);
                    if (t != null) text(t, 20, i * 18 + 5, durColor(s));
                } else {
                    item(s, i * slot + (slot - 16) / 2, 0);
                    if (t != null) text(t, i * slot + slot / 2f - mc.fontRendererObj.getStringWidth(t) / 2f, 18, durColor(s));
                }
            }
        }
    }

    // ---------------------------------------------------------------- Potion Status
    public static class Potions extends Hud.Text {
        final Setting.Bool showDur = new Setting.Bool("Show Duration", true);
        final Setting.Bool effectColors = new Setting.Bool("Use Effect Colors", true);
        private final List<Integer> colors = new ArrayList<Integer>();

        public Potions() {
            super("Potion Status", "Active potion effects", 99, 10);
            add(showDur); add(effectColors);
        }

        @Override
        protected List<String> lines() {
            colors.clear();
            EntityPlayer p = mc.thePlayer;
            if (p == null) return Collections.emptyList();
            Collection<PotionEffect> fx = p.getActivePotionEffects();
            List<String> out = new ArrayList<String>();
            for (PotionEffect e : fx) {
                String s = I18n.format(e.getEffectName()) + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
                if (showDur.value) s += " " + Potion.getDurationString(e);
                out.add(s);
                Potion type = Potion.potionTypes[e.getPotionID()];
                colors.add(type == null ? 0xFFFFFFFF : RenderUtil.lerp(0xFF000000 | type.getLiquidColor(), 0xFFFFFFFF, 0.35f));
            }
            return out;
        }

        @Override
        protected int lineColor(int i, int base) {
            return effectColors.value && i < colors.size() ? colors.get(i) : base;
        }
    }

    // ---------------------------------------------------------------- Combo
    public static class Combo extends Hud.Text {
        final Setting.Bool label = new Setting.Bool("Show Label", true);
        private int count, lastHurt;

        public Combo() {
            super("Combo Counter", "Hits in a row without being hit", 50, 7);
            add(label);
        }

        @Override
        public void onTick() {
            if (mc.thePlayer == null) return;
            int ht = mc.thePlayer.hurtTime;
            if (ht > 0 && lastHurt == 0) count = 0;
            lastHurt = ht;
        }

        @Override
        public void onAttack(AttackEntityEvent e) {
            if (e.entityPlayer != mc.thePlayer || !(e.target instanceof EntityLivingBase)) return;
            EntityLivingBase t = (EntityLivingBase) e.target;
            if (t.hurtResistantTime <= t.maxHurtResistantTime / 2.0F) count++;
        }

        @Override
        protected List<String> lines() {
            return Collections.singletonList((label.value ? "Combo: " : "") + count);
        }
    }

    // ---------------------------------------------------------------- Reach
    public static class Reach extends Hud.Text {
        final Setting.Bool label = new Setting.Bool("Show Label", true);
        private double reach = -1;

        public Reach() {
            super("Reach Display", "Distance of your last hit", 50, 13);
            add(label);
        }

        @Override
        public void onMouse(MouseEvent e) {
            if (!e.buttonstate || e.button != 0 || mc.thePlayer == null) return;
            MovingObjectPosition o = mc.objectMouseOver;
            if (o != null && o.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && o.hitVec != null) {
                reach = mc.thePlayer.getPositionEyes(1.0F).distanceTo(o.hitVec);
            }
        }

        @Override
        protected List<String> lines() {
            String v = reach < 0 ? "-" : String.format(Locale.US, "%.2f", reach) + " blocks";
            return Collections.singletonList((label.value ? "Reach: " : "") + v);
        }
    }

    // ---------------------------------------------------------------- Memory
    public static class Memory extends Hud.Text {
        final Setting.Mode display = new Setting.Mode("Display", 0, "Used / Max", "Used", "Percent");

        public Memory() {
            super("Memory", "Game memory usage", 50, 19);
            add(display);
        }

        @Override
        protected List<String> lines() {
            Runtime rt = Runtime.getRuntime();
            long max = rt.maxMemory() / 1048576L, used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            String s;
            if (display.is("Percent")) s = "Mem: " + (used * 100 / Math.max(1, max)) + "%";
            else if (display.is("Used")) s = "Mem: " + used + " MB";
            else s = "Mem: " + used + "/" + max + " MB";
            return Collections.singletonList(s);
        }
    }

    // ---------------------------------------------------------------- Server IP
    public static class ServerIp extends Hud.Text {
        public ServerIp() { super("Server IP", "Address of the server you are on", 50, 25); }

        @Override
        protected List<String> lines() {
            return Collections.singletonList(mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer");
        }
    }

    // ---------------------------------------------------------------- Pot Counter
    public static class PotCounter extends Hud.Text {
        final Setting.Bool label = new Setting.Bool("Show Label", true);

        public PotCounter() {
            super("Pot Counter", "Healing splash potions in your inventory", 50, 31);
            add(label);
        }

        @Override
        protected List<String> lines() {
            EntityPlayer p = mc.thePlayer;
            if (p == null) return Collections.emptyList();
            int n = 0;
            for (ItemStack s : p.inventory.mainInventory) {
                if (s == null || !(s.getItem() instanceof ItemPotion) || !ItemPotion.isSplash(s.getMetadata())) continue;
                List<PotionEffect> fx = ((ItemPotion) s.getItem()).getEffects(s);
                if (fx == null) continue;
                for (PotionEffect e : fx) {
                    if (e.getPotionID() == Potion.heal.id) { n += s.stackSize; break; }
                }
            }
            return Collections.singletonList((label.value ? "Pots: " : "") + n);
        }
    }
}
