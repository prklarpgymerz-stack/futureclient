package com.futureclient;

import com.futureclient.module.Module;
import com.futureclient.module.Setting;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Profiles live in .minecraft/config/futureclient/profiles/NAME.properties.
 * A profile stores every module's on/off state, every setting and every HUD position.
 */
public class ConfigManager {
    private static String active = "default";

    private static File root() {
        File d = new File(Minecraft.getMinecraft().mcDataDir, "config/futureclient");
        new File(d, "profiles").mkdirs();
        return d;
    }

    private static File file(String name) { return new File(new File(root(), "profiles"), name + ".properties"); }

    public static String active() { return active; }

    /** Keeps only letters, digits, space, dash and underscore (max 16 chars). */
    public static String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c < 128 && (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_')) sb.append(c);
        }
        String r = sb.toString().trim();
        return r.length() > 16 ? r.substring(0, 16).trim() : r;
    }

    public static List<String> list() {
        List<String> out = new ArrayList<String>();
        File[] fs = new File(root(), "profiles").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String n) { return n.endsWith(".properties"); }
        });
        if (fs != null) for (File f : fs) out.add(f.getName().substring(0, f.getName().length() - 11));
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static boolean exists(String name) { return file(name).exists(); }

    /** Called once at startup after the modules exist. */
    public static void init() {
        try {
            File a = new File(root(), "active.txt");
            if (a.exists()) {
                Properties p = new Properties();
                InputStream in = new FileInputStream(a);
                try { p.load(in); } finally { in.close(); }
                String n = sanitize(p.getProperty("active", "default"));
                if (!n.isEmpty()) active = n;
            }
        } catch (Exception ignored) {}
        if (!load(active)) {
            active = "default";
            saveAs("default");
        }
    }

    /** Auto-save into the active profile (GUI close / game exit). */
    public static void save() { saveAs(active); }

    /** Same as save() but reports success. */
    public static boolean save2() { return saveAs(active); }

    public static boolean saveAs(String name) {
        name = sanitize(name);
        if (name.isEmpty() || FutureClient.modules == null) return false;
        Properties p = new Properties();
        for (Module m : FutureClient.modules.all) {
            p.setProperty(key(m, null), String.valueOf(m.enabled));
            for (Setting s : m.settings) p.setProperty(key(m, s), s.save());
        }
        try {
            OutputStream o = new FileOutputStream(file(name));
            try { p.store(o, "Future Client profile: " + name); } finally { o.close(); }
            active = name;
            writeActive();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean load(String name) {
        name = sanitize(name);
        File f = file(name);
        if (name.isEmpty() || !f.exists() || FutureClient.modules == null) return false;
        Properties p = new Properties();
        try {
            InputStream in = new FileInputStream(f);
            try { p.load(in); } finally { in.close(); }
        } catch (IOException e) {
            return false;
        }
        resetDefaults();
        for (Module m : FutureClient.modules.all) {
            String en = p.getProperty(key(m, null));
            if (en != null) m.setEnabled(Boolean.parseBoolean(en));
            for (Setting s : m.settings) {
                String v = p.getProperty(key(m, s));
                if (v != null) s.load(v);
            }
        }
        active = name;
        writeActive();
        return true;
    }

    public static boolean delete(String name) {
        name = sanitize(name);
        if (name.isEmpty() || !file(name).exists()) return false;
        boolean ok = file(name).delete();
        if (ok && name.equals(active)) {
            active = "default";
            if (!exists("default")) saveAs("default");
            writeActive();
        }
        return ok;
    }

    /** Back to factory settings (in memory; the active profile file is only changed when you save). */
    public static void resetDefaults() {
        if (FutureClient.modules == null) return;
        for (Module m : FutureClient.modules.all) {
            m.setEnabled(m.defaultEnabled);
            for (Setting s : m.settings) if (s.def != null) s.load(s.def);
        }
    }

    private static void writeActive() {
        try {
            Properties p = new Properties();
            p.setProperty("active", active);
            OutputStream o = new FileOutputStream(new File(root(), "active.txt"));
            try { p.store(o, null); } finally { o.close(); }
        } catch (IOException ignored) {}
    }

    private static String key(Module m, Setting s) {
        return (m.name + (s == null ? ".enabled" : "." + s.name)).replace(' ', '_');
    }
}
