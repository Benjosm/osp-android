package com.doublethinksolutions.osp.tasks

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.doublethinksolutions.osp.data.DeviceOrientation

/**
 * A Singleton object responsible for providing the device's real-time orientation.
 *
 * It listens to the device's rotation vector sensor and continuously updates the
 * latest orientation data. Start listening in onResume() and stop in onPause()
 * to conserve battery.
 */
object OrientationProvider : SensorEventListener {

    // The most recent orientation data. Volatile ensures reads/writes are atomic
    // and visible across threads (sensor thread and UI/main thread).
    @Volatile
    var latestOrientation: DeviceOrientation? = null
        private set // Can only be set from within this object

    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null

    /**
     * Starts listening for orientation updates.
     * @param context The application context is preferred to avoid memory leaks.
     */
    fun start(context: Context) {
        if (sensorManager != null) {
            // Already started
            return
        }

        sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationVectorSensor == null) {
            Log.w("OrientationProvider", "Rotation Vector Sensor not available. Cannot provide orientation.")
            return
        }

        sensorManager?.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
    }

    /**
     * Stops listening for orientation updates to save battery.
     */
    fun stop() {
        sensorManager?.unregisterListener(this)
        // Nullify to allow for restart and to release resources
        sensorManager = null
        rotationVectorSensor = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Values are in radians, convert to degrees
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

            // Azimuth is often -180 to 180. Normalize to 0-360 for consistency.
            val adjustedAzimuth = (azimuth + 360) % 360

            latestOrientation = DeviceOrientation(adjustedAzimuth, pitch, roll)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for this implementation
    }
}
