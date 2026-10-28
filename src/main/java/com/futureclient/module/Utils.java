package com.futureclient.module;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class Utils {
    private Utils() {}

    // ---------------------------------------------------------------- Toggle Sprint
    public static class ToggleSprint extends Hud.Text {
        final Setting.Bool status = new Setting.Bool("Show Status", true);

        public ToggleSprint() {
            super("Toggle Sprint", "Always sprint while moving forward", Category.UTILITY, 1, 90);
            add(status);
            enabled = false;
            defaultEnabled = false;
            anim = 0f;
        }

        @Override
        public void onTick() {
            if (mc.thePlayer == null || mc.thePlayer.isSneaking()) return;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }

        @Override
        public void onDisable() {
            if (mc.gameSettings != null) KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        }

        @Override
        protected List<String> lines() {
            if (!status.value || mc.thePlayer == null) return Collections.emptyList();
            return Collections.singletonList(mc.thePlayer.isSprinting() ? "Sprinting (Toggled)" : "Walking (Toggled)");
        }
    }

    // ---------------------------------------------------------------- Chat timestamps
    public static class ChatTimestamps extends Module {
        final Setting.Mode format = new Setting.Mode("Format", 0, "24 Hour", "12 Hour");
        final Setting.Bool seconds = new Setting.Bool("Show Seconds", false);

        public ChatTimestamps() {
            super("Chat Timestamps", "Adds [time] before chat messages", Category.UTILITY, false);
            add(format); add(seconds);
        }

        @Override
        public void onChat(ClientChatReceivedEvent e) {
            if (e.type == 2) return;
            String p = format.is("24 Hour") ? (seconds.value ? "HH:mm:ss" : "HH:mm") : (seconds.value ? "hh:mm:ss a" : "hh:mm a");
            ChatComponentText prefix = new ChatComponentText("\u00a77[" + new SimpleDateFormat(p, Locale.US).format(new Date()) + "] \u00a7r");
            prefix.appendSibling(e.message);
            e.message = prefix;
        }
    }
}
