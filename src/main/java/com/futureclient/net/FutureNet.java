package com.futureclient.net;

import net.minecraft.client.Minecraft;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Tells a small server "I'm using Future Client" and asks who else is.
 * Set API to your server (see server/server.py), e.g. "http://1.2.3.4:8765".
 * While API is empty only YOU get the logo.
 */
public class FutureNet {
    public static final String API = "";

    private static volatile Set<String> users = Collections.emptySet();

    public static boolean isUser(UUID id) {
        return id != null && users.contains(id.toString().toLowerCase());
    }

    public static void start() {
        if (API.isEmpty()) return;
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        UUID me = Minecraft.getMinecraft().getSession().getProfile().getId();
                        if (me != null) http(API + "/hb", me.toString());
                        Set<String> s = new HashSet<String>();
                        for (String line : http(API + "/users", null).split("\n")) {
                            line = line.trim().toLowerCase();
                            if (!line.isEmpty()) s.add(line);
                        }
                        users = s;
                    } catch (Exception ignored) {}
                    try { Thread.sleep(60000); } catch (InterruptedException e) { return; }
                }
            }
        }, "FutureClient-Net");
        t.setDaemon(true);
        t.start();
    }

    private static String http(String url, String body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(4000);
        c.setReadTimeout(4000);
        if (body != null) {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            OutputStream o = c.getOutputStream();
            o.write(body.getBytes("UTF-8"));
            o.close();
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = r.readLine()) != null) sb.append(l).append('\n');
        r.close();
        return sb.toString();
    }
}
