package com.futureclient.module;

import net.minecraft.client.Minecraft;
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

public class ModuleManager {
    public final List<Module> all = new ArrayList<Module>();
    public final Visuals.Nametags nametags = new Visuals.Nametags();
    public final ClientSettings client = new ClientSettings();

    public ModuleManager() {
        // HUD
        all.add(new Huds.Fps());
        all.add(new Huds.Cps());
        all.add(new Huds.Ping());
        all.add(new Huds.Keystrokes());
        all.add(new Huds.Coords());
        all.add(new Huds.Direction());
        all.add(new Huds.Clock());
        all.add(new Huds.Speed());
        all.add(new Huds.Armor());
        all.add(new Huds.Potions());
        all.add(new Huds.Combo());
        all.add(new Huds.Reach());
        all.add(new Huds.Memory());
        all.add(new Huds.ServerIp());
        all.add(new Huds.PotCounter());
        // Visual
        all.add(new Visuals.MotionBlur());
        all.add(nametags);
        all.add(new Visuals.Crosshair());
        all.add(new Visuals.BlockOverlay());
        all.add(new Visuals.Fullbright());
        all.add(new Visuals.HideFire());
        all.add(new Visuals.TimeChanger());
        all.add(new Visuals.Zoom());
        // Utility
        all.add(new Utils.ToggleSprint());
        all.add(new Utils.ChatTimestamps());
        // Client
        all.add(client);

        for (Module m : all) if (m instanceof Hud) ((Hud) m).addCommon();
        // remember factory defaults (used by profile reset)
        for (Module m : all) {
            m.defaultEnabled = m.enabled;
            for (Setting s : m.settings) s.def = s.save();
        }
    }

    public void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        for (Module m : all) if (m.enabled) m.onTick();
    }

    public void renderHud(ScaledResolution sr) { for (Module m : all) if (m.enabled) m.onHudRender(sr); }
    public void overlayPre(RenderGameOverlayEvent.Pre e) { for (Module m : all) if (m.enabled) m.onOverlayPre(e); }
    public void mouse(MouseEvent e) { for (Module m : all) if (m.enabled) m.onMouse(e); }
    public void attack(AttackEntityEvent e) { for (Module m : all) if (m.enabled) m.onAttack(e); }
    public void chat(ClientChatReceivedEvent e) { for (Module m : all) if (m.enabled) m.onChat(e); }
    public void fov(FOVUpdateEvent e) { for (Module m : all) if (m.enabled) m.onFov(e); }
    public void blockHighlight(DrawBlockHighlightEvent e) { for (Module m : all) if (m.enabled) m.onBlockHighlight(e); }
    public void fire(RenderBlockOverlayEvent e) { for (Module m : all) if (m.enabled) m.onFireOverlay(e); }
}
