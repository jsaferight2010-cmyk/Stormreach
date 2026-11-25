package com.stormreach.sim;

/**
 * Relationship to another tornado. These describe separate tornadoes, not one tornado's shape:
 * <ul>
 * <li>SATELLITE: a smaller, separate tornado orbiting a larger "parent" tornado within the same mesocyclone.</li>
 * <li>COMPANION: a separate tornado from the same parent storm (or its rear-flank region), occurring at the same
 * time but not orbiting the main tornado.</li>
 * </ul>
 */
public enum TornadoRelation {
    NONE, SATELLITE, COMPANION
}
