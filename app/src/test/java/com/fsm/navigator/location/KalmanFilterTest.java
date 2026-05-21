package com.fsm.navigator.location;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class KalmanFilterTest {

    private static final double Q = 0.008;
    private static final double R = 0.1;
    private static final double DELTA = 1e-9;

    private KalmanFilter kalman;

    @Before
    public void setUp() {
        kalman = new KalmanFilter();
    }

    // ── filter() ────────────────────────────────────────────────────────────

    @Test
    public void filter_emptyInput_returnsEmptyMap() {
        Map<String, Double> result = kalman.filter(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void filter_firstScan_returnsRawValue() {
        Map<String, Integer> scan = new HashMap<>();
        scan.put("AA:BB:CC:DD:EE:FF", -70);

        Map<String, Double> result = kalman.filter(scan);

        assertEquals(1, result.size());
        assertEquals(-70.0, result.get("aa:bb:cc:dd:ee:ff"), DELTA);
    }

    @Test
    public void filter_bssidKeyIsLowercased() {
        Map<String, Integer> scan = new HashMap<>();
        scan.put("AA:BB:CC:DD:EE:FF", -70);

        Map<String, Double> result = kalman.filter(scan);

        assertFalse(result.containsKey("AA:BB:CC:DD:EE:FF"));
        assertTrue(result.containsKey("aa:bb:cc:dd:ee:ff"));
    }

    @Test
    public void filter_secondScan_appliesKalmanCorrection() {
        String bssid = "aa:bb:cc:dd:ee:ff";
        Map<String, Integer> scan1 = new HashMap<>();
        scan1.put(bssid, -70);
        kalman.filter(scan1); // initialise state

        Map<String, Integer> scan2 = new HashMap<>();
        scan2.put(bssid, -80);
        Map<String, Double> result = kalman.filter(scan2);

        double predCov = 1.0 + Q;                   // 1.008
        double gain = predCov / (predCov + R);       // 1.008 / 1.108
        double expected = -70.0 + gain * (-80.0 - (-70.0));

        assertEquals(expected, result.get(bssid), DELTA);
    }

    @Test
    public void filter_sameMeasurementTwice_valueUnchanged() {
        String bssid = "aa:bb:cc:dd:ee:ff";
        Map<String, Integer> scan = new HashMap<>();
        scan.put(bssid, -70);
        kalman.filter(scan);

        Map<String, Integer> scan2 = new HashMap<>();
        scan2.put(bssid, -70);
        Map<String, Double> result = kalman.filter(scan2);

        // correction term is zero when measured == estimate
        assertEquals(-70.0, result.get(bssid), DELTA);
    }

    @Test
    public void filter_multipleBssids_trackedIndependently() {
        String b1 = "aa:00:00:00:00:01";
        String b2 = "aa:00:00:00:00:02";

        Map<String, Integer> scan1 = new HashMap<>();
        scan1.put(b1, -60);
        scan1.put(b2, -75);
        kalman.filter(scan1);

        Map<String, Integer> scan2 = new HashMap<>();
        scan2.put(b1, -65);
        scan2.put(b2, -80);
        Map<String, Double> result = kalman.filter(scan2);

        double predCov = 1.0 + Q;
        double gain = predCov / (predCov + R);

        assertEquals(-60.0 + gain * (-65.0 - (-60.0)), result.get(b1), DELTA);
        assertEquals(-75.0 + gain * (-80.0 - (-75.0)), result.get(b2), DELTA);
    }

    @Test
    public void filter_newBssidMidSession_treatedAsFirst() {
        Map<String, Integer> scan1 = new HashMap<>();
        scan1.put("aa:00:00:00:00:01", -60);
        kalman.filter(scan1);

        Map<String, Integer> scan2 = new HashMap<>();
        scan2.put("aa:00:00:00:00:02", -55);
        Map<String, Double> result = kalman.filter(scan2);

        assertEquals(-55.0, result.get("aa:00:00:00:00:02"), DELTA);
    }

    @Test
    public void filter_threeScans_errorCovStrictlyDecreasing() {
        String bssid = "aa:bb:cc:dd:ee:ff";
        Map<String, Integer> scan = new HashMap<>();
        scan.put(bssid, -70);

        // Scan 1 – initialises (errorCov = 1.0)
        kalman.filter(scan);

        // Compute expected errorCov after scan 2
        double cov = 1.0;
        double predCov = cov + Q;
        double gain = predCov / (predCov + R);
        double cov2 = (1 - gain) * predCov;

        // Scan 2
        kalman.filter(scan);

        // Compute expected errorCov after scan 3
        predCov = cov2 + Q;
        gain = predCov / (predCov + R);
        double cov3 = (1 - gain) * predCov;

        // Scan 3
        kalman.filter(scan);

        assertTrue("errorCov should decrease over repeated scans", cov3 < cov2);
    }

    // ── reset() ─────────────────────────────────────────────────────────────

    @Test
    public void reset_clearsState_nextFilterRaw() {
        String bssid = "aa:bb:cc:dd:ee:ff";
        Map<String, Integer> scan1 = new HashMap<>();
        scan1.put(bssid, -70);
        kalman.filter(scan1);

        kalman.reset();

        Map<String, Integer> scan2 = new HashMap<>();
        scan2.put(bssid, -80);
        Map<String, Double> result = kalman.filter(scan2);

        // State was cleared, so this is a first-time observation
        assertEquals(-80.0, result.get(bssid), DELTA);
    }

    @Test
    public void reset_onFreshFilter_isNoOp() {
        kalman.reset(); // must not throw

        Map<String, Integer> scan = new HashMap<>();
        scan.put("aa:bb:cc:dd:ee:ff", -70);
        Map<String, Double> result = kalman.filter(scan);
        assertEquals(-70.0, result.get("aa:bb:cc:dd:ee:ff"), DELTA);
    }

    // ── isSignalWeak() ───────────────────────────────────────────────────────

    @Test
    public void isSignalWeak_null_returnsTrue() {
        assertTrue(KalmanFilter.isSignalWeak(null));
    }

    @Test
    public void isSignalWeak_empty_returnsTrue() {
        assertTrue(KalmanFilter.isSignalWeak(Collections.emptyMap()));
    }

    @Test
    public void isSignalWeak_allBelowThreshold_returnsTrue() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc:dd:ee:ff", -90.0);
        scan.put("11:22:33:44:55:66", -87.0);
        assertTrue(KalmanFilter.isSignalWeak(scan));
    }

    @Test
    public void isSignalWeak_oneAboveThreshold_returnsFalse() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc:dd:ee:ff", -80.0);
        assertFalse(KalmanFilter.isSignalWeak(scan));
    }

    @Test
    public void isSignalWeak_exactlyAtThreshold_returnsFalse() {
        // -85.0 is NOT < -85.0, so the signal is NOT weak
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc:dd:ee:ff", -85.0);
        assertFalse(KalmanFilter.isSignalWeak(scan));
    }

    @Test
    public void isSignalWeak_mixedStrengths_usesMaxRssi() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("weak",   -95.0);
        scan.put("strong", -50.0);
        assertFalse(KalmanFilter.isSignalWeak(scan));
    }

    @Test
    public void isSignalWeak_justBelowThreshold_returnsTrue() {
        Map<String, Double> scan = new HashMap<>();
        scan.put("aa:bb:cc:dd:ee:ff", -85.01);
        assertTrue(KalmanFilter.isSignalWeak(scan));
    }
}
