package com.futureclient.module;

import java.util.Locale;

public abstract class Setting {
    public final String name;
    public boolean hidden;
    public String def;

    protected Setting(String name) { this.name = name; }

    public abstract String save();
    public abstract void load(String s);

    public static class Bool extends Setting {
        public boolean value;
        public float anim;

        public Bool(String name, boolean value) {
            super(name);
            this.value = value;
            this.anim = value ? 1f : 0f;
        }

        public String save() { return String.valueOf(value); }
        public void load(String s) { value = Boolean.parseBoolean(s); anim = value ? 1f : 0f; }
    }

    public static class Num extends Setting {
        public double value, min, max, step;

        public Num(String name, double value, double min, double max, double step) {
            super(name);
            this.value = value; this.min = min; this.max = max; this.step = step;
        }

        public int i() { return (int) Math.round(value); }
        public float f() { return (float) value; }

        public String display() {
            if (step >= 1) return String.valueOf((int) Math.round(value));
            if (step >= 0.1) return String.format(Locale.US, "%.1f", value);
            return String.format(Locale.US, "%.2f", value);
        }

        public String save() { return Double.toString(value); }
        public void load(String s) {
            try { value = Math.max(min, Math.min(max, Double.parseDouble(s))); } catch (NumberFormatException ignored) {}
        }
    }

    public static class Mode extends Setting {
        public final String[] options;
        public int index;

        public Mode(String name, int index, String... options) {
            super(name);
            this.index = index;
            this.options = options;
        }

        public String get() { return options[index]; }
        public boolean is(String s) { return options[index].equals(s); }
        public void cycle(int d) { index = (index + d + options.length) % options.length; }

        public String save() { return String.valueOf(index); }
        public void load(String s) {
            try {
                int v = Integer.parseInt(s.trim());
                if (v >= 0 && v < options.length) index = v;
            } catch (NumberFormatException ignored) {}
        }
    }

    public static class Color extends Setting {
        public static final int[] PALETTE = {
            0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55,
            0xFF55FFFF, 0xFF5599FF, 0xFFAA77FF, 0xFFFF77CC
        };
        public int value;

        public Color(String name, int value) { super(name); this.value = value; }

        public String save() { return Integer.toString(value); }
        public void load(String s) {
            try { value = Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
    }
}
