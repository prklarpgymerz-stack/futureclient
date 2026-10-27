package com.futureclient.module;

import net.minecraft.client.gui.ScaledResolution;
import java.util.ArrayList;
import java.util.List;

public class Module {
    public final String name, desc;
    public boolean enabled;
    public float anim, hover;
    public final List<Setting> settings = new ArrayList<Setting>();

    public Module(String name, String desc, boolean enabled) {
        this.name = name; this.desc = desc; this.enabled = enabled; this.anim = enabled ? 1f : 0f;
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
    public void render(ScaledResolution sr) {}

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }
}
