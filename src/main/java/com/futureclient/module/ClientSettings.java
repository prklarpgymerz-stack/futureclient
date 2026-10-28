package com.futureclient.module;

/** Global client options (shown in the "Client" tab). Always enabled. */
public class ClientSettings extends Module {
    public final Setting.Color accent = new Setting.Color("Accent Color", 0xFF7C5CFF);
    public final Setting.Bool menuBg = new Setting.Bool("Menu Background", true);
    public final Setting.Mode bgStyle = new Setting.Mode("Background Style", 0, "Gradient", "Solid", "Dim");
    public final Setting.Num dim = new Setting.Num("Background Opacity", 80, 0, 100, 5);
    public final Setting.Mode hudBg = new Setting.Mode("HUD Backgrounds", 0, "Per Module", "All On", "All Off");
    public final Setting.Bool particles = new Setting.Bool("Menu Particles", true);
    public final Setting.Bool guiFit = new Setting.Bool("Auto-fit GUI to Screen", true);

    public ClientSettings() {
        super("Client Settings", "Look and feel of Future Client", Category.CLIENT, true);
        add(accent);
        add(menuBg);
        add(bgStyle);
        add(dim);
        add(hudBg);
        add(particles);
        add(guiFit);
    }
}
