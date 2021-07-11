package io.github.drop_stones.diymeasurekit

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import java.util.*
import java.util.TimerTask
import kotlin.math.abs

open class DistanceMeasure : AppCompatActivity(), SensorEventListener {
    private val TAG: String? = DistanceMeasure::class.simpleName
    //private val alpha: Double = 0.75
    private val secToNano = 1000000000

    private lateinit var manager: SensorManager
    private lateinit var sensor: Sensor

    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var timer: Timer

    private var interval: Double = 0.0
    private var accuracy: Int = 0
    private var prevTimeStamp: Long = 0

    //private val delay: Int = SensorManager.SENSOR_DELAY_NORMAL
    //private val delay: Int = SensorManager.SENSOR_DELAY_FASTEST
    private val delay: Int = 5000  // per 5000 micro seconds event
    private val type: Int = Sensor.TYPE_LINEAR_ACCELERATION
    private val thresholdX = 0.01
    private val thresholdY = 0.008
    private val thresholdZ = 0.01
    private val stopAccelerationThreshold = 0.05
    //private val stopVelocityThreshold = 0.02

    private lateinit var distanceView: TextView
    var distance: Double = 0.0
    var x: Double = 0.0
    var y: Double = 0.0
    var z: Double = 0.0
    var dx: Double = 0.0
    var dy: Double = 0.0
    var dz: Double = 0.0
    private var vx: Double = 0.0
    private var vy: Double = 0.0
    private var vz: Double = 0.0
    private var ax: Double = 0.0
    private var ay: Double = 0.0
    private var az: Double = 0.0

    // Slide Window
    private val windowSize: Int = 6
    private var windowIndex: Int = 0
    private var slideWindowX: DoubleArray = DoubleArray(windowSize)
    private var slideWindowY: DoubleArray = DoubleArray(windowSize)
    private var slideWindowZ: DoubleArray = DoubleArray(windowSize)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        distanceView = findViewById(R.id.distance)

        val measureButton: Button = findViewById(R.id.measure_button)
        measureButton.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    manager.registerListener(this, sensor, delay)
                    prevTimeStamp = 0
                    measureButton.text = getString(R.string.stop)

                    timer = Timer()
                    timer.scheduleAtFixedRate(object: TimerTask() {
                        override fun run() {
                            handler.post {
                                printDistance()
                            }
                        }
                    }, 100, 100)
                    Log.i(TAG, "ACTION_DOWN")
                }
                MotionEvent.ACTION_UP -> {
                    manager.unregisterListener(this)
                    calculateRemainingSlideWindow()
                    printDistance()
                    measureButton.text = getString(R.string.measure)
                    timer.cancel()
                    Log.i(TAG, "ACTION_UP")
                }
            }
            true
        }

        val clearButton: Button = findViewById(R.id.clear_button)
        clearButton.setOnClickListener {
            clear()
            printDistance()
        }
        printDistance()

        manager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = manager.getDefaultSensor(type)
    }

    override fun onSensorChanged(event: SensorEvent) {
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

    private fun updateAcceleration() {
        ax = slideWindowX.sum() / windowSize
        ay = slideWindowY.sum() / windowSize
        az = slideWindowZ.sum() / windowSize
        Log.i(TAG, "ax=$ax, ay=$ay, az=$az")
    }

    private fun updateVelocity() {
        //vx = if (abs(ax) < stopAccelerationThreshold && abs(vx) < stopVelocityThreshold) 0.0 else vx + ax * interval
        //vy = if (abs(ay) < stopAccelerationThreshold && abs(vy) < stopVelocityThreshold) 0.0 else vy + ay * interval
        //vz = if (abs(az) < stopAccelerationThreshold && abs(vz) < stopVelocityThreshold) 0.0 else vz + az * interval
        vx = if (abs(ax) > stopAccelerationThreshold) vx + ax * interval else 0.0
        vy = if (abs(ay) > stopAccelerationThreshold) vy + ay * interval else 0.0
        vz = if (abs(az) > stopAccelerationThreshold) vz + az * interval else 0.0
        Log.i(TAG, "vx=$vx, vy=$vy, vz=$vz")
    }

    private fun updateVelocityWithoutThreshold() {
        vx += ax * interval
        vy += ay * interval
        vz += az * interval
    }

    private fun updateDistance() {
        dx = vx * interval + (ax * interval * interval) / 2
        dy = vy * interval + (ay * interval * interval) / 2
        dz = vz * interval + (az * interval * interval) / 2
        Log.i(TAG, "dx=$dx, dy=$dy, dz=$dz")
    }

    fun updateDirectDistance() {
        x += dx
        y += dy
        z += dz
        Log.i(TAG, "x=$x, y=$y, z=$z")
    }

    private fun calculateXYZ(event: SensorEvent, interval: Double) {
        slideWindow(event)
        Log.i(TAG, "interval=$interval")

        updateDistance()
    }

    private fun slideWindow(event: SensorEvent) {
        //val filteredX = when {
        //    event.values[0] > thresholdX   -> event.values[0] - thresholdX / 2
        //    event.values[0] < - thresholdX -> event.values[0] + thresholdX / 2
        //    else -> 0.0
        //}
        //val filteredY = when {
        //    event.values[1] > thresholdY   -> event.values[1] - thresholdY / 2
        //    event.values[1] < - thresholdY -> event.values[1] + thresholdY / 2
        //    else -> 0.0
        //}
        //val filteredZ = when {
        //    event.values[2] > thresholdZ   -> event.values[2] - thresholdZ / 2
        //    event.values[2] < - thresholdZ -> event.values[2] + thresholdZ / 2
        //    else -> 0.0
        //}
        val filteredX: Double = when {
            event.values[0] > thresholdX   -> event.values[0].toDouble()
            event.values[0] < - thresholdX -> event.values[0].toDouble()
            else -> 0.0
        }
        val filteredY = when {
            event.values[1] > thresholdY   -> event.values[1].toDouble()
            event.values[1] < - thresholdY -> event.values[1].toDouble()
            else -> 0.0
        }
        val filteredZ = when {
            event.values[2] > thresholdZ   -> event.values[2].toDouble()
            event.values[2] < - thresholdZ -> event.values[2].toDouble()
            else -> 0.0
        }

        slideWindowX[windowIndex] = filteredX
        slideWindowY[windowIndex] = filteredY
        slideWindowZ[windowIndex] = filteredZ
        windowIndex = (windowIndex + 1) % windowSize

        updateAcceleration()
        updateVelocity()
    }

    private fun calculateRemainingSlideWindow() {
        for (i in 0..windowSize) {
        //for (i in 0 until windowSize) {
            updateAcceleration()
            updateVelocityWithoutThreshold()
            updateDistance()
            calculateDistance()

            slideWindowX[windowIndex] = 0.0
            slideWindowY[windowIndex] = 0.0
            slideWindowZ[windowIndex] = 0.0
            windowIndex = (windowIndex + 1) % windowSize
        }
    }

    open fun calculateDistance() {
        distance += dx + dy + dz
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
        for (i in 0 until windowSize) {
            slideWindowX[i] = 0.0
            slideWindowY[i] = 0.0
            slideWindowZ[i] = 0.0
        }
        prevTimeStamp = 0
    }

    fun printDistance() {
        distanceView.text = getString(R.string.distance_format).format(distance)
    }
}
