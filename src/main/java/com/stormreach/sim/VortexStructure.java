package com.stormreach.sim;

/**
 * Internal vortex structure. A multiple-vortex tornado has two or more subvortices (suction vortices)
 * orbiting inside one broad parent circulation; their winds add to the parent's, producing narrow swaths of
 * the most intense damage. This is independent of the tornado's visual morphology.
 */
public enum VortexStructure {
    SINGLE, MULTIPLE_VORTEX
}
