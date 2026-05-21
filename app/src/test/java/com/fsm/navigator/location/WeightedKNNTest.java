package com.fsm.navigator.location;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class WeightedKNNTest {

    private static final double DELTA = 1e-6;

    // ── helpers ──────────────────────────────────────────────────

    private WeightedKNN.Fingerprint fp(String salleId, String nom,
                                       float x, float y,
                                       String bssid, double rssi) {
        return new WeightedKNN.Fingerprint(salleId, nom, "B3", x, y, bssid, rssi);
    }

    // ── locate() guard clauses ────────────────────────────────────

    @Test
    public void locate_emptyFilteredScan_returnsNull() {
        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        fps.add(fp("S1", "Room 1", 1f, 1f, "aa:bb:cc", -70.0));
        assertNull(WeightedKNN.locate(new HashMap<>(), fps));
    }

    @Test
    public void locate_emptyFingerprints_returnsNull() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc", -70.0);
        assertNull(WeightedKNN.locate(scan, new ArrayList<>()));
    }

    // ── Fingerprint constructor ───────────────────────────────────

    @Test
    public void fingerprint_bssidIsLowercased() {
        WeightedKNN.Fingerprint f = new WeightedKNN.Fingerprint(
                "S1", "Room", "B3", 1f, 1f, "AA:BB:CC:DD:EE:FF", -70);
        assertEquals("aa:bb:cc:dd:ee:ff", f.bssid);
    }

    @Test
    public void fingerprint_fieldsStoredCorrectly() {
        WeightedKNN.Fingerprint f = new WeightedKNN.Fingerprint(
                "S42", "Lab", "BPAL", 3.5f, 7.2f, "de:ad:be:ef", -65.0);
        assertEquals("S42", f.salleId);
        assertEquals("Lab",  f.salleNom);
        assertEquals("BPAL", f.blocId);
        assertEquals(3.5f,   f.x, 0.001f);
        assertEquals(7.2f,   f.y, 0.001f);
        assertEquals(-65.0,  f.rssiMoyen, DELTA);
    }

    // ── Single perfect match ──────────────────────────────────────

    @Test
    public void locate_singleFingerprint_exactMatch_salleAndBloc() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc", -70.0);
        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3", 5f, 10f, "aa:bb:cc", -70.0));

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);

        assertNotNull(r);
        assertEquals("S1",     r.salleId);
        assertEquals("Room 1", r.salleNom);
        assertEquals("B3",     r.blocId);
    }

    @Test
    public void locate_singleFingerprint_exactMatch_positionMatchesFP() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc", -70.0);
        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3", 5f, 10f, "aa:bb:cc", -70.0));

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);

        assertNotNull(r);
        assertEquals(5f,  r.x, 0.01f);
        assertEquals(10f, r.y, 0.01f);
    }

    @Test
    public void locate_singleFingerprint_exactMatch_confidenceIsOne() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc", -70.0);
        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3", 5f, 10f, "aa:bb:cc", -70.0));

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);

        assertNotNull(r);
        // distance == 0 → confidence = 1 - 0/30 = 1.0
        assertEquals(1.0, r.confidence, DELTA);
    }

    // ── BSSID penalty for missing scan entry ──────────────────────

    @Test
    public void locate_missingBssid_penaltyDrivesConfidenceToZero() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("zz:zz:zz", -70.0); // no matching BSSID → penalty -100

        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3", 5f, 10f, "aa:bb:cc", -70.0));

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);

        assertNotNull(r);
        // diff = -100 - (-70) = -30 → distance = 30 → confidence = max(0, 1 - 30/30) = 0
        assertEquals(0.0, r.confidence, DELTA);
    }

    // ── Confidence bounds ─────────────────────────────────────────

    @Test
    public void locate_confidenceNeverNegative() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc", -200.0); // large error

        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3", 1f, 1f, "aa:bb:cc", 0.0));

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);
        assertNotNull(r);
        assertTrue(r.confidence >= 0.0);
    }

    @Test
    public void locate_confidenceNeverAboveOne() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc", -70.0);

        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3", 1f, 1f, "aa:bb:cc", -70.0));

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);
        assertNotNull(r);
        assertTrue(r.confidence <= 1.0);
    }

    // ── Multiple rooms — best match wins ─────────────────────────

    @Test
    public void locate_twoRooms_picksBestRssiMatch() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:00:00:00:00:01", -60.0);

        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        // S1: exact match (distance = 0)
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3", 1f, 1f, "aa:00:00:00:00:01", -60.0));
        // S2: far off (distance = 30)
        fps.add(new WeightedKNN.Fingerprint("S2", "Room 2", "B3", 9f, 9f, "aa:00:00:00:00:01", -90.0));

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);
        assertNotNull(r);
        assertEquals("S1", r.salleId);
    }

    @Test
    public void locate_fourRooms_farFourthExcludedFromInterpolation() {
        // K=3: the 4th room (very far in RSSI) must not affect the interpolated position
        Map<String, Double> scan = new HashMap<>();
        scan.put("bb:cc:dd", -70.0);

        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3",  0f, 0f, "bb:cc:dd", -70.0));  // dist≈0
        fps.add(new WeightedKNN.Fingerprint("S2", "Room 2", "B3", 10f, 0f, "bb:cc:dd", -75.0));  // dist=5
        fps.add(new WeightedKNN.Fingerprint("S3", "Room 3", "B3", 20f, 0f, "bb:cc:dd", -80.0));  // dist=10
        fps.add(new WeightedKNN.Fingerprint("S4", "Room 4", "B3", 30f, 0f, "bb:cc:dd", -200.0)); // dist=130 → excluded

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);
        assertNotNull(r);
        assertEquals("S1", r.salleId);
        // S1 dominates (near-zero distance → huge weight); interpolated x must be < 5
        assertTrue("Interpolated x should be dominated by S1 (x=0)", r.x < 5f);
    }

    // ── Weighted position interpolation ──────────────────────────

    @Test
    public void locate_twoEqualDistanceRooms_interpolatesMidpoint() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc", -70.0);

        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        // Both rooms have same absolute RSSI diff (|diff|=5) → equal weights
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3",  0f, 0f, "aa:bb:cc", -65.0));
        fps.add(new WeightedKNN.Fingerprint("S2", "Room 2", "B3", 10f, 0f, "aa:bb:cc", -75.0));

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);
        assertNotNull(r);
        // Equal weights → interpolated x ≈ midpoint = 5
        assertEquals(5f, r.x, 0.5f);
        assertEquals(0f, r.y, 0.5f);
    }

    // ── Multi-BSSID fingerprint distance ─────────────────────────

    @Test
    public void locate_multiBssidFingerprints_averagedCorrectly() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("ap:01", -60.0);
        scan.put("ap:02", -80.0);

        List<WeightedKNN.Fingerprint> fps = new ArrayList<>();
        // S1: both match perfectly → distance = 0 → confidence = 1
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3", 5f, 5f, "ap:01", -60.0));
        fps.add(new WeightedKNN.Fingerprint("S1", "Room 1", "B3", 5f, 5f, "ap:02", -80.0));

        WeightedKNN.LocationResult r = WeightedKNN.locate(scan, fps);
        assertNotNull(r);
        assertEquals("S1", r.salleId);
        assertEquals(1.0, r.confidence, DELTA);
    }
}
