package com.stormreach.power;

/** Something that holds captured water, in millibuckets (1000 = one bucket); it may be charged Stormwater. */
public interface WaterTank {
    int water();
    int waterCapacity();
    boolean isStormwater();
    /** Adds up to {@code mb}; Stormwater only stays Stormwater while nothing plain is mixed in. */
    int fill(int mb, boolean stormwater);
    int drain(int mb);
}
