package io.github.drop_stones.diymeasurekit

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.util.*
import java.util.TimerTask
import kotlin.math.abs

open class DistanceMeasure : AppCompatActivity(), SensorEventListener, Runnable {
    val TAG: String? = DistanceMeasure::class.simpleName
    val DISTANCE_REFRESH_PERIOD_MS: Long = 20
    val alpha: Double = 0.75
    val secToNano = 1000000000

    lateinit var manager: SensorManager
    lateinit var sensor: Sensor

    val handler: Handler = Handler()
    var timer: Timer = Timer()

    var interval: Double = 0.0
    var accuracy: Int = 0
    var prevTimeStamp: Long = 0

    //val delay: Int = SensorManager.SENSOR_DELAY_NORMAL
    val delay: Int = SensorManager.SENSOR_DELAY_FASTEST
    val type: Int = Sensor.TYPE_LINEAR_ACCELERATION
    val thresholdX = 0.01
    val thresholdY = 0.008
    val thresholdZ = 0.01
    val stopAccelerationThreshold = 0.001
    val stopVelocityThreshold = 0.02
    val beta: Double = 0.99

    lateinit var distanceView: TextView
    var distance: Double = 0.0
    var x: Double = 0.0
    var y: Double = 0.0
    var z: Double = 0.0
    var dx: Double = 0.0
    var dy: Double = 0.0
    var dz: Double = 0.0
    var vx: Double = 0.0
    var vy: Double = 0.0
    var vz: Double = 0.0
    var ax: Double = 0.0
    var ay: Double = 0.0
    var az: Double = 0.0

    // Slide Window
    val windowSize: Int = 4
    var windowIndex: Int = 0
    var slideWindowX: DoubleArray = DoubleArray(windowSize)
    var slideWindowY: DoubleArray = DoubleArray(windowSize)
    var slideWindowZ: DoubleArray = DoubleArray(windowSize)

    // For removing gravity
    var gx: Double = 0.0
    var gy: Double = 0.0
    var gz: Double = 0.0

    // Remove noise
    var stabilityCount: Int = 0
    var stabilityX: FloatArray = FloatArray(3 * 60 * 200)
    var stabilityY: FloatArray = FloatArray(3 * 60 * 200)
    var stabilityZ: FloatArray = FloatArray(3 * 60 * 200)
    var stabilityMaxX: Float = 0F
    var stabilityMaxY: Float = 0F
    var stabilityMaxZ: Float = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        distanceView = findViewById(R.id.distance)
        val measureButton: Button = findViewById(R.id.measure_button)
        var measureStart: Boolean = false
        measureButton.setOnClickListener {
            if (!measureStart) {
                manager.registerListener(this, sensor, delay)
                measureStart = true
                prevTimeStamp = 0
                measureButton.text = "Stop"
            } else {
                manager.unregisterListener(this)
                measureStart = false
                measureButton.text = "Measure"
            }
        }
        val clearButton: Button = findViewById(R.id.clear_button)
        clearButton.setOnClickListener {
            clear()
            printDistance()
        }
        printDistance()

        manager = getSystemService(SENSOR_SERVICE) as SensorManager
        if (manager == null) {
            return;
        }
        sensor = manager.getDefaultSensor(type)
        if (sensor == null) {
            return;
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun run() {
        printDistance()
        Log.i(TAG, "Running!!!!!")
    }

    override fun onPause() {
        super.onPause()
        manager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        //samplingStability(event)
        //return

        if (prevTimeStamp == 0L) {
            prevTimeStamp = event.timestamp
        } else {
            // nano sec
            val timeDiff: Double = (event.timestamp.toDouble() - prevTimeStamp.toDouble())
            interval = timeDiff / secToNano
            prevTimeStamp = event.timestamp
        }
        calculateXYZ(event, interval)
        calculateDistance()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        this.accuracy = accuracy
    }

    fun calculateXYZ(event: SensorEvent, interval: Double) {
        //gx = alpha * gx + (1 - alpha) * event.values[0]
        //gy = alpha * gy + (1 - alpha) * event.values[1]
        //gz = alpha * gz + (1 - alpha) * event.values[2]
        //ax = event.values[0] - gx
        //ay = event.values[1] - gy
        //az = event.values[2] - gz
        //ax = if (abs(ax) > abs(thresholdX)) event.values[0].toDouble() else 0.0
        //ay = if (abs(ay) > abs(thresholdY)) event.values[1].toDouble() else 0.0
        //az = if (abs(az) > abs(thresholdZ)) event.values[2].toDouble() else 0.0
        //ax = alpha * ax + (1 - alpha) * event.values[0]
        //ay = alpha * ay + (1 - alpha) * event.values[1]
        //az = alpha * az + (1 - alpha) * event.values[2]

        slideWindow(event, interval)

        dx = vx * interval + (ax * interval * interval) / 2
        dy = vy * interval + (ay * interval * interval) / 2
        dz = vz * interval + (az * interval * interval) / 2
        x += dx
        y += dy
        z += dz
        Log.i(TAG, "interval=$interval")
        Log.i(TAG, "ax=$ax, ay=$ay, az=$az")
        Log.i(TAG, "vx=$vx, vy=$vy, vz=$vz")
        Log.i(TAG, "dx=$dx, dy=$dy, dz=$dz")
        Log.i(TAG, "x=$x, y=$y, z=$z")
    }

    fun slideWindow(event: SensorEvent, interval: Double) {
        val filteredX = when {
            event.values[0] > thresholdX   -> event.values[0] - thresholdX / 2
            event.values[0] < - thresholdX -> event.values[0] + thresholdX / 2
            else -> 0.0
        }
        val filteredY = when {
            event.values[1] > thresholdY   -> event.values[1] - thresholdY / 2
            event.values[1] < - thresholdY -> event.values[1] + thresholdY / 2
            else -> 0.0
        }
        val filteredZ = when {
            event.values[2] > thresholdZ   -> event.values[2] - thresholdZ / 2
            event.values[2] < - thresholdZ -> event.values[2] + thresholdZ / 2
            else -> 0.0
        }

        slideWindowX[windowIndex] = filteredX
        slideWindowY[windowIndex] = filteredY
        slideWindowZ[windowIndex] = filteredZ
        windowIndex = (windowIndex + 1) % windowSize

        ax = slideWindowX.sum() / windowSize
        ay = slideWindowY.sum() / windowSize
        az = slideWindowZ.sum() / windowSize

        //vx = if (abs(ax) < stopAccelerationThreshold && abs(vx) < stopVelocityThreshold) 0.0 else vx + ax * interval
        //vy = if (abs(ay) < stopAccelerationThreshold && abs(vy) < stopVelocityThreshold) 0.0 else vy + ay * interval
        //vz = if (abs(az) < stopAccelerationThreshold && abs(vz) < stopVelocityThreshold) 0.0 else vz + az * interval
        vx = if (abs(ax) > stopAccelerationThreshold) vx + ax * interval else 0.0
        vy = if (abs(ay) > stopAccelerationThreshold) vy + ay * interval else 0.0
        vz = if (abs(az) > stopAccelerationThreshold) vz + az * interval else 0.0
    }

    open fun calculateDistance() {
        distance += dx + dy + dz;
    }

    fun clear() {
        distance = 0.0
        ax = 0.0
        ay = 0.0
        az = 0.0
        vx = 0.0
        vy = 0.0
        vz = 0.0
        dx = 0.0
        dy = 0.0
        dz = 0.0
        x = 0.0
        y = 0.0
        z = 0.0
        var i = 0
        for (i in 0 until windowSize) {
            slideWindowX[i] = 0.0
            slideWindowY[i] = 0.0
            slideWindowZ[i] = 0.0
        }
        prevTimeStamp = 0
    }

    fun printDistance() {
        distanceView.text = "%.2f m".format(distance)
    }

    fun samplingStability(event: SensorEvent) {
        if (stabilityCount == stabilityX.size)
            return
        stabilityX[stabilityCount] = event.values[0]
        stabilityY[stabilityCount] = event.values[1]
        stabilityZ[stabilityCount] = event.values[2]
        stabilityMaxX = if (abs(event.values[0]) > abs(stabilityMaxX)) event.values[0] else stabilityMaxX
        stabilityMaxY = if (abs(event.values[1]) > abs(stabilityMaxY)) event.values[1] else stabilityMaxY
        stabilityMaxZ = if (abs(event.values[2]) > abs(stabilityMaxZ)) event.values[2] else stabilityMaxZ
        stabilityCount += 1
        return
    }

    fun printStability() {
        val aveX = stabilityX.average()
        val aveY = stabilityY.average()
        val aveZ = stabilityZ.average()
        Log.i(TAG, "maxX=$stabilityMaxX, maxY=$stabilityMaxY, maxZ=$stabilityMaxZ, aveX=$aveX, aveY=$aveY, aveZ=$aveZ")
    }
}
