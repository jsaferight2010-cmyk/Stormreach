package com.stormreach.sim;

/**
 * How a tornado formed. This is about the parent storm and mechanism, not appearance or strength.
 * <ul>
 * <li>SUPERCELL: forms beneath the rotating updraft (mesocyclone) of a supercell. All strong and violent
 * tornadoes are of this kind.</li>
 * <li>LANDSPOUT: non-supercell; pre-existing low-level spin is stretched by a growing cumulus updraft.
 * Usually weak (EF0–EF1, rarely EF2), narrow and short-lived, often a dusty rope.</li>
 * <li>WATERSPOUT: the fair-weather kind, forming over water beneath developing cumulus; typically weak (EF0).
 * A supercell tornado that crosses water is a "tornadic waterspout" and keeps its SUPERCELL formation.</li>
 * </ul>
 */
public enum TornadoFormation {
    SUPERCELL(EFScale.EF5), LANDSPOUT(EFScale.EF2), WATERSPOUT(EFScale.EF1);

    /** Strongest rating this mechanism realistically produces. */
    public final EFScale maxRating;

    TornadoFormation(EFScale maxRating) {
        this.maxRating = maxRating;
    }
}
