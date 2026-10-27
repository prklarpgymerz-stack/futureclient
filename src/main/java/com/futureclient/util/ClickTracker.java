package com.futureclient.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClickTracker {
    private static final List<Long> L = new ArrayList<Long>();
    private static final List<Long> R = new ArrayList<Long>();

    public static void click(boolean left) {
        (left ? L : R).add(System.currentTimeMillis());
    }

    public static int get(boolean left) {
        List<Long> l = left ? L : R;
        long now = System.currentTimeMillis();
        Iterator<Long> it = l.iterator();
        while (it.hasNext()) if (now - it.next() > 1000) it.remove();
        return l.size();
    }
}
