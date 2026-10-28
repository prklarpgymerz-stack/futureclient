package com.futureclient.module;

/** Global client options (shown in the "Client" tab). Always enabled. */
public class ClientSettings extends Module {
    public final Setting.Color accent = new Setting.Color("Accent Color", 0xFF7C5CFF);
    public final Setting.Num dim = new Setting.Num("Background Dim", 47, 0, 90, 5);
    public final Setting.Bool particles = new Setting.Bool("Menu Particles", true);
    public final Setting.Bool guiFit = new Setting.Bool("Auto-fit GUI to Screen", true);

    public ClientSettings() {
        super("Client Settings", "Look and feel of Future Client", Category.CLIENT, true);
        add(accent);
        add(dim);
        add(particles);
        add(guiFit);
    }
}
