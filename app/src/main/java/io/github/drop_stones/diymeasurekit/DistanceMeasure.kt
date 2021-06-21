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

    val delay: Int = SensorManager.SENSOR_DELAY_NORMAL
    val type: Int = Sensor.TYPE_LINEAR_ACCELERATION
    //val type: Int = Sensor.TYPE_LINEAR_ACCELERATION
    val threshold = 0.0
    val beta: Double = 0.9999

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
    var sx: Double = 0.0
    var sy: Double = 0.0
    var sz: Double = 0.0
    // For removing gravity
    var gx: Double = 0.0
    var gy: Double = 0.0
    var gz: Double = 0.0

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
        gx = alpha * gx + (1 - alpha) * event.values[0]
        gy = alpha * gy + (1 - alpha) * event.values[1]
        gz = alpha * gz + (1 - alpha) * event.values[2]
        ax = event.values[0] - gx
        ay = event.values[1] - gy
        az = event.values[2] - gz
        //ax = if (ax < - threshold || threshold < ax) ax else 0.0
        //ay = if (ay < - threshold || threshold < ay) ay else 0.0
        //az = if (az < - threshold || threshold < az) az else 0.0
        //ax = alpha * ax + (1 - alpha) * event.values[0]
        //ay = alpha * ay + (1 - alpha) * event.values[1]
        //az = alpha * az + (1 - alpha) * event.values[2]
        //ax = event.values[0].toDouble()
        //ay = event.values[1].toDouble()
        //az = event.values[2].toDouble()
        vx = vx * beta + ax * interval
        vy = vy * beta + ay * interval
        vz = vz * beta + az * interval
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
        prevTimeStamp = 0
    }

    fun printDistance() {
        distanceView.text = "%.2f m".format(distance)
    }
}