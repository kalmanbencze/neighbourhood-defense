package me.kalmanbncz.nd.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * the sensor fusion class, responsible for the rotation matrix calculation
 */
public class SensorFusion implements SensorEventListener {

    float[] mRotationMatrix = new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};

    Context context;

    private SensorManager mSensorManager = null;

    private LowPassFilter lowpass = new LowPassFilter();

    private float[] values = new float[3];

    private float[] floatValues = new float[3];

    public SensorFusion(Context context) {
        // get sensorManager and initialise sensor listeners
        this.context = context;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        initListeners();
    }

    public void onPauseOrStop() {
        mSensorManager.unregisterListener(this);
    }

    public void onResume() {
        // restore the sensor listeners when user resumes the application.
        initListeners();
    }

    // This function registers sensor listeners for the accelerometer, magnetometer and gyroscope.
    public void initListeners() {
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public synchronized void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // Get rotation matrix from angles
            values = lowpass.lowPass(event.values, 0.05f);
            floatValues[0] = values[1];
            floatValues[1] = values[0] * -1;
            floatValues[2] = values[2] * -1;
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, floatValues);

            // Remap the axes
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                    SensorManager.AXIS_MINUS_Z, mRotationMatrix);
        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public synchronized float[] getRotationMatrix() {
        return mRotationMatrix;
    }

    public class LowPassFilter {

        float[] output;

        public LowPassFilter() {

        }

        // LowPass Filter
        public float[] lowPass(float[] input, float alpha) {
            if (output == null) {
                output = input;
                return output;
            }

            for (int i = 0; i < input.length; i++) {
                output[i] = output[i] + alpha * (input[i] - output[i]);
            }
            return output;
        }

    }
}
