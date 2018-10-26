package uk.ac.masts.sifids;

import org.junit.Test;

import java.util.Map;

import uk.ac.masts.sifids.entities.CatchLocation;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class LocationUnitTest {

    @Test
    public void latitudeComponents_CalculatedCorrectly() throws Exception {
        CatchLocation loc = new CatchLocation();
        loc.setLatitude(12.4);
        assertEquals('N', loc.getLatitudeDirection());
        assertEquals(12, loc.getLatitudeDegrees());
        assertEquals(24, loc.getLatitudeMinutes());
        assertEquals("12 24 N", loc.getLatitudeString());
        loc.setLatitude(-5.68);
        assertEquals('S', loc.getLatitudeDirection());
        assertEquals(5, loc.getLatitudeDegrees());
        assertEquals(41, loc.getLatitudeMinutes());
        assertEquals("05 41 S", loc.getLatitudeString());
    }

    @Test
    public void latitude_SetCorrectlyFromDegMinDir() throws Exception {
        CatchLocation loc = new CatchLocation();
        loc.setLatitude(12, 24, 'N');
        assertEquals(12.4, loc.getLatitude(), 0.01);
        loc.setLatitude(5, 41, 'S');
        assertEquals(-5.68, loc.getLatitude(), 0.01);
    }

    @Test
    public void longitudeComponents_CalculatedCorrectly() throws Exception {
        CatchLocation loc = new CatchLocation();
        loc.setLongitude(12.4);
        assertEquals('E', loc.getLongitudeDirection());
        assertEquals(12, loc.getLongitudeDegrees());
        assertEquals(24, loc.getLongitudeMinutes());
        assertEquals("12 24 E", loc.getLongitudeString());
        loc.setLongitude(-5.68);
        assertEquals('W', loc.getLongitudeDirection());
        assertEquals(5, loc.getLongitudeDegrees());
        assertEquals(41, loc.getLongitudeMinutes());
        assertEquals("05 41 W", loc.getLongitudeString());
    }

    @Test
    public void longitude_SetCorrectlyFromDegMinDir() throws Exception {
        CatchLocation loc = new CatchLocation();
        loc.setLongitude(12, 24, 'E');
        assertEquals(12.4, loc.getLongitude(), 0.01);
        loc.setLongitude(5, 41, 'W');
        assertEquals(-5.68, loc.getLongitude(), 0.01);
    }
}