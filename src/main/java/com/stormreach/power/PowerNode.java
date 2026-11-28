package com.stormreach.power;

/**
 * A block entity that holds Storm Units (SU), the mod's unit of electricity. Generators make SU and push it out along
 * Copper Cable (consumers first, then storage); storage (the Storm Capacitor) pushes to consumers; consumers spend it.
 */
public interface PowerNode {
    enum Role { GENERATOR, STORAGE, CONSUMER }

    Role role();
    int stored();
    int capacity();
    /** Takes up to {@code amount} SU; returns how much was accepted. */
    int insert(int amount);
    /** Gives up to {@code amount} SU; returns how much was removed. */
    int extract(int amount);
    /** SU per tick this node is making (generators) or would use (consumers), for the Multimeter. */
    default int rate() { return 0; }
}
