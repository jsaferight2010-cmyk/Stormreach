package com.stormreach.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 6.0: Doppler velocity geometry, radar colour scales and display units (overhaul test list items 14 and 15). */
class RadarAndUnitsTest {
    @Test
    void radialVelocityIsTheWindComponentAlongTheBeam() {
        // wind blowing east at 20 m/s; a target due east of the radar sees it moving away (+20)
        assertEquals(20, RadarMath.radial(20, 0, 100, 0), 1e-9);
        // due west of the radar the same wind blows toward it (-20)
        assertEquals(-20, RadarMath.radial(20, 0, -100, 0), 1e-9);
        // due north (dz negative) the beam is perpendicular to the wind: no radial component
        assertEquals(0, RadarMath.radial(20, 0, 0, -100), 1e-9);
        // 45 degrees off the wind: cos 45
        assertEquals(20 * Math.cos(Math.PI / 4), RadarMath.radial(20, 0, 100, 100), 1e-9);
    }

    @Test
    void aTornadoShowsAnInboundOutboundCouplet() {
        // a cyclonic vortex north of the radar: one side of it moves toward the radar, the other away
        WindField f = new WindField(40, 60, 0, 0, 1, 0, 0);
        double cx = 0, cz = -500;   // tornado 500 m north of the radar
        double[] west = {cx - 40, cz}, east = {cx + 40, cz};
        WindField.Sample sw = f.sample(west[0] - cx, west[1] - cz, 30, 0), se = f.sample(east[0] - cx, east[1] - cz, 30, 0);
        double vw = RadarMath.radial(sw.u(), sw.w(), west[0], west[1]), ve = RadarMath.radial(se.u(), se.w(), east[0], east[1]);
        assertTrue(Math.signum(vw) != Math.signum(ve), "the two sides of the vortex must have opposite radial velocity: " + vw + " / " + ve);
        assertTrue(Math.abs(vw) > 20 && Math.abs(ve) > 20, "a strong couplet: " + vw + " / " + ve);
    }

    @Test
    void gatesRoundTripAndBearingsFollowTheCompass() {
        assertEquals(90, RadarMath.bearing(1, 0), 1e-9);    // +x is east
        assertEquals(180, RadarMath.bearing(0, 1), 1e-9);   // +z is south
        assertEquals(0, RadarMath.bearing(0, -1), 1e-9);    // -z is north
        for (int az = 0; az < RadarMath.AZIMUTHS; az += 7) for (int g = 0; g < RadarMath.GATES; g += 5) {
            double[] o = RadarMath.gateOffset(az, g, 1000);
            int[] back = RadarMath.gateOf(o[0], o[1], 1000);
            assertNotNull(back);
            assertEquals(az, back[0]); assertEquals(g, back[1]);
        }
        assertNull(RadarMath.gateOf(2000, 0, 1000));
    }

    @Test
    void velocityPackingNeverCollidesWithNoData() {
        assertEquals(-127, RadarMath.packVelocity(-500));
        assertEquals(127, RadarMath.packVelocity(500));
        assertNotEquals(RadarMath.NO_DATA, RadarMath.packVelocity(-128));
        assertEquals(0, RadarMath.dbzColour(2));
        assertNotEquals(0, RadarMath.dbzColour(40));
        assertNotEquals(RadarMath.velocityColour(-30), RadarMath.velocityColour(30));
    }

    @Test
    void reflectivityCombinesAsPowerNotDecibels() {
        // two equal 40 dBZ echoes make 43 dBZ, not 80
        assertEquals(43.01, RadarMath.combineDbz(40, 40), 0.01);
        assertEquals(40, RadarMath.combineDbz(40, 0), 1e-9);
    }

    @Test
    void unitsDefaultToUsCustomaryAndConvertCorrectly() {
        assertEquals(Units.System.IMPERIAL, Units.System.parse(null));
        assertEquals(Units.System.IMPERIAL, Units.System.parse("imperial"));
        assertEquals(Units.System.METRIC, Units.System.parse("Metric"));
        assertEquals("72°F", Units.temperature(22.2, Units.System.IMPERIAL));
        assertEquals("22°C", Units.temperature(22.2, Units.System.METRIC));
        assertEquals("60 mph", Units.speed(60, Units.System.IMPERIAL));
        assertEquals("97 km/h", Units.speed(60, Units.System.METRIC));
        assertEquals("29.92 inHg", Units.pressure(1013.25, Units.System.IMPERIAL));
        assertEquals("1013 hPa", Units.pressure(1013.25, Units.System.METRIC));
        assertEquals("1.00 in", Units.smallLength(25.4, Units.System.IMPERIAL));
        assertEquals("328 ft", Units.distance(100, Units.System.IMPERIAL));
        assertEquals("0.6 mi", Units.distance(1000, Units.System.IMPERIAL));
        assertEquals("1.5 km", Units.distance(1500, Units.System.METRIC));
        assertEquals(32, Units.cToF(0), 1e-9);
        assertEquals(0, Units.fToC(32), 1e-9);
    }
}
