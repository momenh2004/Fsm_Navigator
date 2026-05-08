package com.fsm.navigator.location;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * ImuHelper.java – Fusion des capteurs IMU
 *
 * Capteurs utilisés :
 *   - Accéléromètre  → détection des pas (PDR)
 *   - Gyroscope      → changement de direction
 *   - Boussole       → orientation absolue (azimut)
 *
 * Rôle dans la localisation :
 *   Quand le WiFi est faible (signal < -85 dBm), on estime
 *   la position en combinant :
 *     - Dernière position connue (WiFi)
 *     - Nombre de pas détectés (accéléromètre)
 *     - Direction de marche (boussole + gyroscope)
 *
 * C'est la technique PDR (Pedestrian Dead Reckoning).
 */
public class ImuHelper implements SensorEventListener {

    // Longueur moyenne d'un pas (en mètres)
    private static final float STEP_LENGTH = 0.75f;

    // Seuil de détection de pas (en m/s²)
    private static final float STEP_THRESHOLD = 11.0f;

    private SensorManager sensorManager;
    private Sensor accelSensor, gyroSensor, magnetSensor;

    // État courant
    private float   azimuth     = 0f;  // orientation en degrés (0=Nord)
    private int     stepCount   = 0;
    private float   lastAccelMag = 0f;
    private boolean stepDetected = false;

    // Position estimée par PDR
    private float pdrX = 0f;
    private float pdrY = 0f;

    // Données brutes des capteurs
    private float[] gravity     = new float[3];
    private float[] geomagnetic = new float[3];

    public ImuHelper(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    // ===== DÉMARRER / ARRÊTER =====

    public void start() {
        if (accelSensor  != null) sensorManager.registerListener(this, accelSensor,  SensorManager.SENSOR_DELAY_GAME);
        if (gyroSensor   != null) sensorManager.registerListener(this, gyroSensor,   SensorManager.SENSOR_DELAY_GAME);
        if (magnetSensor != null) sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    // ===== LECTURE DES CAPTEURS =====

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER:
                gravity = event.values.clone();
                detectStep(event.values);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = event.values.clone();
                updateAzimuth();
                break;

            case Sensor.TYPE_GYROSCOPE:
                // Le gyroscope affine l'orientation entre deux lectures de boussole
                // (correction de dérive à court terme)
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ===== DÉTECTION DE PAS =====

    /**
     * Détecte un pas en analysant la magnitude de l'accélération.
     * Algorithme simple de seuillage.
     */
    private void detectStep(float[] accel) {
        float mag = (float) Math.sqrt(
                accel[0]*accel[0] +
                        accel[1]*accel[1] +
                        accel[2]*accel[2]);

        // Détection de pic d'accélération (montée puis descente)
        if (mag > STEP_THRESHOLD && lastAccelMag <= STEP_THRESHOLD) {
            stepCount++;
            stepDetected = true;
            updatePdrPosition();
        }
        lastAccelMag = mag;
    }

    // ===== MISE À JOUR DE L'ORIENTATION =====

    /**
     * Calcule l'azimut (direction de marche) à partir de la boussole.
     */
    private void updateAzimuth() {
        float[] R = new float[9];
        float[] I = new float[9];

        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
            float[] orientation = new float[3];
            SensorManager.getOrientation(R, orientation);
            // orientation[0] = azimut en radians
            azimuth = (float) Math.toDegrees(orientation[0]);
            if (azimuth < 0) azimuth += 360;
        }
    }

    // ===== MISE À JOUR POSITION PDR =====

    /**
     * Estime le déplacement à partir d'un pas dans la direction azimuth.
     * Coordonnées : x = Est, y = Nord
     */
    private void updatePdrPosition() {
        double rad = Math.toRadians(azimuth);
        pdrX += STEP_LENGTH * (float) Math.sin(rad);
        pdrY += STEP_LENGTH * (float) Math.cos(rad);
    }

    // ===== API PUBLIQUE =====

    /**
     * Retourne le déplacement estimé depuis la dernière position WiFi connue.
     * À appeler quand le signal WiFi est trop faible.
     *
     * @param lastKnownX  Dernière position X connue (WiFi)
     * @param lastKnownY  Dernière position Y connue (WiFi)
     * @return            Position estimée { x, y }
     */
    public float[] getEstimatedPosition(float lastKnownX, float lastKnownY) {
        return new float[]{
                lastKnownX + pdrX,
                lastKnownY + pdrY
        };
    }

    /**
     * Réinitialise le PDR quand une nouvelle position WiFi est confirmée.
     */
    public void resetPdr() {
        pdrX = 0f;
        pdrY = 0f;
        stepCount = 0;
    }

    public float getAzimuth()   { return azimuth; }
    public int   getStepCount() { return stepCount; }
    public float getPdrX()      { return pdrX; }
    public float getPdrY()      { return pdrY; }
}