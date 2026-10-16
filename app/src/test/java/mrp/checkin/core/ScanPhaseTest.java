package mrp.checkin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScanPhaseTest {
    @Test
    public void fromCodeMapsKnownCodes() {
        assertEquals(ScanPhase.SCANNING, ScanPhase.fromCode(0));
        assertEquals(ScanPhase.VERIFYING, ScanPhase.fromCode(1));
        assertEquals(ScanPhase.CONFIRMED, ScanPhase.fromCode(2));
        assertEquals(ScanPhase.ERROR, ScanPhase.fromCode(3));
    }

    @Test
    public void unknownCodeFallsBackToScanning() {
        assertEquals(ScanPhase.SCANNING, ScanPhase.fromCode(99));
        assertEquals(ScanPhase.SCANNING, ScanPhase.fromCode(-1));
    }

    @Test
    public void onlyScanningAllowsDecode() {
        assertTrue(ScanPhase.SCANNING.allowsDecode());
        assertFalse(ScanPhase.VERIFYING.allowsDecode());
        assertFalse(ScanPhase.CONFIRMED.allowsDecode());
        assertFalse(ScanPhase.ERROR.allowsDecode());
    }

    @Test
    public void codesAreStable() {
        assertEquals(0, ScanPhase.SCANNING.code());
        assertEquals(3, ScanPhase.ERROR.code());
    }
}