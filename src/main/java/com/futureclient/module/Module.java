package com.futureclient.module;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public enum Category { HUD, VISUAL, UTILITY, CLIENT }

    public final String name, desc;
    public final Category category;
    public boolean enabled, defaultEnabled;
    public float anim, hover;
    public final List<Setting> settings = new ArrayList<Setting>();

    public Module(String name, String desc, Category category, boolean enabled) {
        this.name = name; this.desc = desc; this.category = category;
        this.enabled = enabled; this.defaultEnabled = enabled;
        this.anim = enabled ? 1f : 0f;
    }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean b) {
        if (enabled == b) return;
        enabled = b;
        if (b) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onHudRender(ScaledResolution sr) {}
    public void onOverlayPre(RenderGameOverlayEvent.Pre e) {}
    public void onMouse(MouseEvent e) {}
    public void onAttack(AttackEntityEvent e) {}
    public void onChat(ClientChatReceivedEvent e) {}
    public void onFov(FOVUpdateEvent e) {}
    public void onBlockHighlight(DrawBlockHighlightEvent e) {}
    public void onFireOverlay(RenderBlockOverlayEvent e) {}

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }
}
