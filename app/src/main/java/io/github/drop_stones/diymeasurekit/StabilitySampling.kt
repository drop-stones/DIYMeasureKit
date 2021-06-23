package io.github.drop_stones.diymeasurekit

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import kotlin.math.abs

class StabilitySampling : SensorEventListener {
    val TAG: String? = StabilitySampling::class.simpleName
    var accuracy: Int = 0

    // Remove noise
    var stabilityCount: Int = 0
    var stabilityX: FloatArray = FloatArray(3 * 60 * 200)
    var stabilityY: FloatArray = FloatArray(3 * 60 * 200)
    var stabilityZ: FloatArray = FloatArray(3 * 60 * 200)
    var stabilityMaxX: Float = 0F
    var stabilityMaxY: Float = 0F
    var stabilityMaxZ: Float = 0F

    override fun onSensorChanged(event: SensorEvent) {
        samplingStability(event)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        this.accuracy = accuracy
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