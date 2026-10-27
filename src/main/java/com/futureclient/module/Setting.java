package com.futureclient.module;

public abstract class Setting {
    public final String name;

    protected Setting(String name) { this.name = name; }

    public static class Bool extends Setting {
        public boolean value;
        public float anim;

        public Bool(String name, boolean value) {
            super(name);
            this.value = value;
            this.anim = value ? 1f : 0f;
        }
    }

    public static class Num extends Setting {
        public double value, min, max, step;

        public Num(String name, double value, double min, double max, double step) {
            super(name);
            this.value = value; this.min = min; this.max = max; this.step = step;
        }
    }
}
