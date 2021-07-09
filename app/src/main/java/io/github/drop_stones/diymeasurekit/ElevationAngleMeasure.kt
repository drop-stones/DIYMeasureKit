package io.github.drop_stones.diymeasurekit

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.text.Typography.degree

class ElevationAngleMeasure : AppCompatActivity(), SensorEventListener {
    private val TAG: String? = ElevationAngleMeasure::class.simpleName

    private lateinit var manager: SensorManager
    private lateinit var sensor: Sensor
    private val type: Int = Sensor.TYPE_GRAVITY
    private val delay: Int = SensorManager.SENSOR_DELAY_FASTEST
    private var accuracy: Int = 0

    private lateinit var angleView: TextView
    private lateinit var angleDrawView: AngleDrawView

    private var angle: Float = 0.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_elevation_angle)
        setTitle(R.string.elevation_angle_measure)

        angleView = findViewById(R.id.elevation_angle)
        angleDrawView = findViewById(R.id.angle_draw_view)
        printAngle()

        manager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = manager.getDefaultSensor(type)
    }

    override fun onResume() {
        super.onResume()
        manager.registerListener(this, sensor, delay)
    }

    override fun onPause() {
        super.onPause()
        manager.unregisterListener(this)
    }

    @SuppressLint("SetTextI18n")
    fun printAngle() {
        angleView.text = getString(R.string.angle_format).format(angle) + degree
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gx = event.values[0]
        val gy = event.values[1]
        val gz = event.values[2]
        val theta = atan2(gy, gx)
        angle = (theta * 180 / PI).toFloat()
        Log.i(TAG, "gx=$gx, gy=$gy, gz=$gz, theta=$theta, angle=$angle")

        angleDrawView.setTheta(theta)
        printAngle()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        this.accuracy = accuracy
    }
}